package com.witte.lozify.presentation.editor

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.core.common.RichTextUtils
import com.witte.lozify.core.preferences.UserPreferencesManager
import com.witte.lozify.domain.model.Note
import com.witte.lozify.domain.model.Tag
import com.witte.lozify.domain.repository.AttachmentRepository
import com.witte.lozify.domain.repository.NoteRelationRepository
import com.witte.lozify.domain.repository.NoteRepository
import com.witte.lozify.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for note editor.
 *
 * Handles note creation, saving to Room database, and draft persistence.
 *
 * Stage 4: Integrated automatic #tag extraction and database persistence.
 * Stage 6: Integrated image attachment handling with AttachmentRepository.
 * Stage 9 Refactor: Upgraded to TextFieldValue + activeFormats for WYSIWYG editing.
 * Stage 17: Draft auto-saving and restoring.
 * Stage 41: Real-time tag autocompletion filtered to active (non-deleted) tags.
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val attachmentRepository: AttachmentRepository,
    private val noteRelationRepository: NoteRelationRepository,
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    /**
     * Stage 41: Available active tags for #tag autocompletion.
     * Only includes tags that have active note references or are pinned.
     * Deleted tags or tags with only deleted notes (trash) are automatically excluded.
     */
    val availableTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .map { tags ->
            tags.filter { it.usageCount > 0 || it.isPinned }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Events emitted by the editor.
     */
    sealed class EditorEvent {
        data class NoteSaved(val noteId: Long, val tagCount: Int) : EditorEvent()
        data class SaveError(val message: String) : EditorEvent()
    }

    private val _events = MutableSharedFlow<EditorEvent>()
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    /**
     * Active formatting states for WYSIWYG editing.
     * When a format is in this set, newly typed text will have that format applied.
     */
    private val _activeFormats = MutableStateFlow<Set<RichTextUtils.FormatType>>(emptySet())
    val activeFormats: StateFlow<Set<RichTextUtils.FormatType>> = _activeFormats.asStateFlow()

    /**
     * Toggle a format in the active formats set.
     * Used by toolbar buttons to lock/unlock formatting modes.
     */
    fun toggleFormat(formatType: RichTextUtils.FormatType) {
        _activeFormats.value = if (_activeFormats.value.contains(formatType)) {
            _activeFormats.value - formatType
        } else {
            _activeFormats.value + formatType
        }
    }

    /**
     * Clear all active formats.
     * Called when editor is dismissed or note is saved.
     */
    fun clearActiveFormats() {
        _activeFormats.value = emptySet()
    }

    /**
     * Save a new note to the database with automatic tag extraction.
     *
     * Stage 4: Extracts all #tags from content, creates/links them to note.
     * Stage 5: Added noteId parameter for updating existing notes.
     * Stage 6: Added imageUris parameter for image attachment handling.
     * Stage 9 Refactor: Now accepts TextFieldValue instead of plain String.
     * Stage 43: Added fileUris parameter for generic document/file attachment handling.
     *
     * @param textFieldValue The note content as TextFieldValue
     * @param imageUris List of selected image URIs to attach
     * @param fileUris List of selected generic file URIs to attach
     * @param noteId Optional note ID for editing (null for new note)
     */
    fun saveNote(
        textFieldValue: TextFieldValue,
        imageUris: List<Uri> = emptyList(),
        fileUris: List<Uri> = emptyList(),
        noteId: Long? = null
    ) {
        val content = textFieldValue.text
        if (content.isBlank() && imageUris.isEmpty() && fileUris.isEmpty()) {
            return
        }

        viewModelScope.launch {
            try {
                val now = Instant.now()

                // Stage 7: Extract tags from content using RichTextUtils (supports rich text)
                val tagNames = extractTagsFromContent(content)

                // Get or create Tag entities for each extracted tag
                val tags = tagNames.map { tagName ->
                    val tagId = tagRepository.getOrCreateTag(tagName)
                    Tag(
                        id = tagId,
                        name = tagName,
                        createdAt = now,
                        usageCount = 0 // Will be incremented by repository
                    )
                }

                // Stage 8: Extract @mentions from content
                val mentions = RichTextUtils.extractMentionsFromContent(content)

                if (noteId != null) {
                    // Stage 5: Update existing note
                    val existingNote = noteRepository.getNoteById(noteId).first()
                    existingNote?.let { note ->
                        // Stage 7 Bug Fix: Clear old tag associations before setting new ones
                        tagRepository.setTagsForNote(noteId, tags.map { it.id })

                        // Stage 8: Clear old relations and insert new ones
                        noteRelationRepository.deleteRelationsForNote(noteId)
                        mentions.forEach { (toNoteId, mentionText) ->
                            noteRelationRepository.addRelation(
                                fromNoteId = noteId,
                                toNoteId = toNoteId,
                                mentionText = mentionText
                            )
                        }

                        // Process new image attachments
                        imageUris.forEach { uri ->
                            try {
                                attachmentRepository.addImageAttachment(noteId, uri)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // Process new generic file attachments (Stage 43)
                        fileUris.forEach { uri ->
                            try {
                                attachmentRepository.addGenericFileAttachment(noteId, uri)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        val updatedNote = note.copy(
                            content = content.trim(),
                            updatedAt = now,
                            tags = tags
                        )
                        noteRepository.updateNote(updatedNote)
                        _events.emit(EditorEvent.NoteSaved(noteId, tags.size))
                    }
                } else {
                    // Create new note
                    val note = Note(
                        id = 0, // Auto-generated by Room
                        content = content.trim(),
                        createdAt = now,
                        updatedAt = now,
                        isPinned = false,
                        isDeleted = false,
                        tags = tags, // Stage 4: Now includes extracted tags
                        attachments = emptyList(),
                        outgoingRelations = emptyList(),
                        incomingRelations = emptyList()
                    )

                    val newNoteId = noteRepository.insertNote(note)

                    // Stage 8: Insert mention relations for new note
                    mentions.forEach { (toNoteId, mentionText) ->
                        noteRelationRepository.addRelation(
                            fromNoteId = newNoteId,
                            toNoteId = toNoteId,
                            mentionText = mentionText
                        )
                    }

                    // Stage 6: Process image attachments with error handling
                    imageUris.forEach { uri ->
                        try {
                            attachmentRepository.addImageAttachment(newNoteId, uri)
                        } catch (e: Exception) {
                            // Continue processing other images even if one fails
                            e.printStackTrace()
                        }
                    }

                    // Stage 43: Process generic file attachments with error handling
                    fileUris.forEach { uri ->
                        try {
                            attachmentRepository.addGenericFileAttachment(newNoteId, uri)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    _events.emit(EditorEvent.NoteSaved(newNoteId, tags.size))
                }

                // Clear active formats and draft after successful save
                clearActiveFormats()
                preferencesManager.clearDraft()
            } catch (e: Exception) {
                _events.emit(EditorEvent.SaveError(e.message ?: "保存失败"))
            }
        }
    }

    /**
     * Retrieve currently saved draft text and image URIs if draft persistence is enabled.
     */
    fun getSavedDraft(): Pair<String, List<Uri>> {
        if (!preferencesManager.isDraftPersistenceEnabled.value) {
            return Pair("", emptyList())
        }
        val text = preferencesManager.draftText.value
        val uris = preferencesManager.draftImageUris.value.mapNotNull {
            try { Uri.parse(it) } catch (e: Exception) { null }
        }
        return Pair(text, uris)
    }

    /**
     * Save draft text and image URIs when editor content changes.
     */
    fun saveDraft(text: String, uris: List<Uri>) {
        if (preferencesManager.isDraftPersistenceEnabled.value) {
            preferencesManager.saveDraft(text, uris.map { it.toString() })
        }
    }

    /**
     * Clear draft from preferences.
     */
    fun clearDraft() {
        preferencesManager.clearDraft()
    }

    /**
     * Extract tags from content, stripping any Markdown formatting markers.
     *
     * Stage 7: Uses RichTextUtils to handle rich text content.
     *
     * @param content Raw content with possible Markdown markers
     * @return List of tag names (without # prefix)
     */
    private fun extractTagsFromContent(content: String): List<String> {
        // Strip formatting markers to get clean text for tag extraction
        val cleanContent = RichTextUtils.stripFormatting(content)

        // Extract tags from clean text
        val tagPattern = Regex("""#([a-zA-Z0-9一-龥_]+)""")
        return tagPattern.findAll(cleanContent)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }
}

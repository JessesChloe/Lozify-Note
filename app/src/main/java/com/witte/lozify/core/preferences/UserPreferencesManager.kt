package com.witte.lozify.core.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global User Preferences Manager for Lozify.
 * Manages UI display preferences (e.g., card collapse lines) and draft persistence.
 *
 * Stage 17: Unified Settings and Preferences.
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _maxCollapseLines = MutableStateFlow(
        prefs.getInt(KEY_MAX_COLLAPSE_LINES, DEFAULT_MAX_COLLAPSE_LINES)
    )
    val maxCollapseLines: StateFlow<Int> = _maxCollapseLines.asStateFlow()

    private val _isDraftPersistenceEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DRAFT_PERSISTENCE_ENABLED, true)
    )
    val isDraftPersistenceEnabled: StateFlow<Boolean> = _isDraftPersistenceEnabled.asStateFlow()

    private val _draftText = MutableStateFlow(
        prefs.getString(KEY_DRAFT_TEXT, "") ?: ""
    )
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    private val _draftImageUris = MutableStateFlow(
        prefs.getStringSet(KEY_DRAFT_IMAGE_URIS, emptySet())?.toList() ?: emptyList()
    )
    val draftImageUris: StateFlow<List<String>> = _draftImageUris.asStateFlow()

    fun setMaxCollapseLines(lines: Int) {
        val safeLines = lines.coerceIn(3, 8)
        prefs.edit().putInt(KEY_MAX_COLLAPSE_LINES, safeLines).apply()
        _maxCollapseLines.value = safeLines
    }

    fun setDraftPersistenceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DRAFT_PERSISTENCE_ENABLED, enabled).apply()
        _isDraftPersistenceEnabled.value = enabled
        if (!enabled) {
            clearDraft()
        }
    }

    fun saveDraft(text: String, imageUris: List<String>) {
        if (!_isDraftPersistenceEnabled.value) return
        prefs.edit()
            .putString(KEY_DRAFT_TEXT, text)
            .putStringSet(KEY_DRAFT_IMAGE_URIS, imageUris.toSet())
            .apply()
        _draftText.value = text
        _draftImageUris.value = imageUris
    }

    fun clearDraft() {
        prefs.edit()
            .remove(KEY_DRAFT_TEXT)
            .remove(KEY_DRAFT_IMAGE_URIS)
            .apply()
        _draftText.value = ""
        _draftImageUris.value = emptyList()
    }

    companion object {
        private const val PREFS_NAME = "lozify_user_preferences"
        private const val KEY_MAX_COLLAPSE_LINES = "key_max_collapse_lines"
        private const val KEY_DRAFT_PERSISTENCE_ENABLED = "key_draft_persistence_enabled"
        private const val KEY_DRAFT_TEXT = "key_draft_text"
        private const val KEY_DRAFT_IMAGE_URIS = "key_draft_image_uris"

        const val DEFAULT_MAX_COLLAPSE_LINES = 5
    }
}

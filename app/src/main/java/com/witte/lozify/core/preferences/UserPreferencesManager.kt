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

    // --- WebDAV Cloud Sync Preferences ---
    private val _webdavServerUrl = MutableStateFlow(
        prefs.getString(KEY_WEBDAV_SERVER_URL, DEFAULT_WEBDAV_SERVER_URL) ?: DEFAULT_WEBDAV_SERVER_URL
    )
    val webdavServerUrl: StateFlow<String> = _webdavServerUrl.asStateFlow()

    private val _webdavUsername = MutableStateFlow(
        prefs.getString(KEY_WEBDAV_USERNAME, "") ?: ""
    )
    val webdavUsername: StateFlow<String> = _webdavUsername.asStateFlow()

    private val _webdavPassword = MutableStateFlow(
        prefs.getString(KEY_WEBDAV_PASSWORD, "") ?: ""
    )
    val webdavPassword: StateFlow<String> = _webdavPassword.asStateFlow()

    private val _webdavRemoteDir = MutableStateFlow(
        prefs.getString(KEY_WEBDAV_REMOTE_DIR, DEFAULT_WEBDAV_REMOTE_DIR) ?: DEFAULT_WEBDAV_REMOTE_DIR
    )
    val webdavRemoteDir: StateFlow<String> = _webdavRemoteDir.asStateFlow()

    private val _webdavAutoSync = MutableStateFlow(
        prefs.getBoolean(KEY_WEBDAV_AUTO_SYNC, false)
    )
    val webdavAutoSync: StateFlow<Boolean> = _webdavAutoSync.asStateFlow()

    private val _webdavLastSyncTime = MutableStateFlow(
        prefs.getLong(KEY_WEBDAV_LAST_SYNC_TIME, 0L)
    )
    val webdavLastSyncTime: StateFlow<Long> = _webdavLastSyncTime.asStateFlow()

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

    fun saveWebDavConfig(
        serverUrl: String,
        username: String,
        password: String,
        remoteDir: String = DEFAULT_WEBDAV_REMOTE_DIR
    ) {
        val cleanUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        val cleanDir = if (remoteDir.startsWith("/")) remoteDir else "/$remoteDir"
        val formattedDir = if (cleanDir.endsWith("/")) cleanDir else "$cleanDir/"

        prefs.edit()
            .putString(KEY_WEBDAV_SERVER_URL, cleanUrl)
            .putString(KEY_WEBDAV_USERNAME, username.trim())
            .putString(KEY_WEBDAV_PASSWORD, password.trim())
            .putString(KEY_WEBDAV_REMOTE_DIR, formattedDir)
            .apply()

        _webdavServerUrl.value = cleanUrl
        _webdavUsername.value = username.trim()
        _webdavPassword.value = password.trim()
        _webdavRemoteDir.value = formattedDir
    }

    fun setWebDavAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WEBDAV_AUTO_SYNC, enabled).apply()
        _webdavAutoSync.value = enabled
    }

    fun setWebDavLastSyncTime(timestamp: Long) {
        prefs.edit().putLong(KEY_WEBDAV_LAST_SYNC_TIME, timestamp).apply()
        _webdavLastSyncTime.value = timestamp
    }

    fun clearWebDavConfig() {
        prefs.edit()
            .remove(KEY_WEBDAV_SERVER_URL)
            .remove(KEY_WEBDAV_USERNAME)
            .remove(KEY_WEBDAV_PASSWORD)
            .remove(KEY_WEBDAV_REMOTE_DIR)
            .remove(KEY_WEBDAV_AUTO_SYNC)
            .remove(KEY_WEBDAV_LAST_SYNC_TIME)
            .apply()

        _webdavServerUrl.value = DEFAULT_WEBDAV_SERVER_URL
        _webdavUsername.value = ""
        _webdavPassword.value = ""
        _webdavRemoteDir.value = DEFAULT_WEBDAV_REMOTE_DIR
        _webdavAutoSync.value = false
        _webdavLastSyncTime.value = 0L
    }

    companion object {
        private const val PREFS_NAME = "lozify_user_preferences"
        private const val KEY_MAX_COLLAPSE_LINES = "key_max_collapse_lines"
        private const val KEY_DRAFT_PERSISTENCE_ENABLED = "key_draft_persistence_enabled"
        private const val KEY_DRAFT_TEXT = "key_draft_text"
        private const val KEY_DRAFT_IMAGE_URIS = "key_draft_image_uris"

        // WebDAV keys
        private const val KEY_WEBDAV_SERVER_URL = "key_webdav_server_url"
        private const val KEY_WEBDAV_USERNAME = "key_webdav_username"
        private const val KEY_WEBDAV_PASSWORD = "key_webdav_password"
        private const val KEY_WEBDAV_REMOTE_DIR = "key_webdav_remote_dir"
        private const val KEY_WEBDAV_AUTO_SYNC = "key_webdav_auto_sync"
        private const val KEY_WEBDAV_LAST_SYNC_TIME = "key_webdav_last_sync_time"

        const val DEFAULT_MAX_COLLAPSE_LINES = 5
        const val DEFAULT_WEBDAV_SERVER_URL = "https://dav.jianguoyun.com/dav/"
        const val DEFAULT_WEBDAV_REMOTE_DIR = "/Lozify/"
    }
}

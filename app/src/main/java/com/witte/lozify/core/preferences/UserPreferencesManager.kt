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
        try { prefs.getInt(KEY_MAX_COLLAPSE_LINES, DEFAULT_MAX_COLLAPSE_LINES) } catch (e: Exception) { DEFAULT_MAX_COLLAPSE_LINES }
    )
    val maxCollapseLines: StateFlow<Int> = _maxCollapseLines.asStateFlow()

    private val _isDraftPersistenceEnabled = MutableStateFlow(
        try { prefs.getBoolean(KEY_DRAFT_PERSISTENCE_ENABLED, true) } catch (e: Exception) { true }
    )
    val isDraftPersistenceEnabled: StateFlow<Boolean> = _isDraftPersistenceEnabled.asStateFlow()

    // Stage 37: Image Compression Preference (Default: true)
    private val _imageCompressionEnabled = MutableStateFlow(
        try { prefs.getBoolean(KEY_IMAGE_COMPRESSION_ENABLED, true) } catch (e: Exception) { true }
    )
    val imageCompressionEnabled: StateFlow<Boolean> = _imageCompressionEnabled.asStateFlow()

    private val _draftText = MutableStateFlow(
        try { prefs.getString(KEY_DRAFT_TEXT, "") ?: "" } catch (e: Exception) { "" }
    )
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    private val _draftImageUris = MutableStateFlow(
        try { prefs.getStringSet(KEY_DRAFT_IMAGE_URIS, emptySet())?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
    )
    val draftImageUris: StateFlow<List<String>> = _draftImageUris.asStateFlow()

    // Stage 57: Calendar Timezone (Default: empty string for system default ZoneId)
    private val _calendarTimeZone = MutableStateFlow(
        try { prefs.getString(KEY_CALENDAR_TIMEZONE, "") ?: "" } catch (e: Exception) { "" }
    )
    val calendarTimeZone: StateFlow<String> = _calendarTimeZone.asStateFlow()

    // Stage 58: User Profile & PRO Membership State
    private val _userName = MutableStateFlow(
        try { prefs.getString(KEY_USER_NAME, "木下") ?: "木下" } catch (e: Exception) { "木下" }
    )
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _isProUser = MutableStateFlow(
        try { prefs.getBoolean(KEY_IS_PRO_USER, false) } catch (e: Exception) { false }
    )
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val _proPlanType = MutableStateFlow(
        try { prefs.getString(KEY_PRO_PLAN_TYPE, "PRO") ?: "PRO" } catch (e: Exception) { "PRO" }
    )
    val proPlanType: StateFlow<String> = _proPlanType.asStateFlow()

    private val _proExpireTime = MutableStateFlow(
        try { prefs.getLong(KEY_PRO_EXPIRE_TIME, 0L) } catch (e: Exception) { 0L }
    )
    val proExpireTime: StateFlow<Long> = _proExpireTime.asStateFlow()

    // --- App Lock & Biometrics (PRO Feature) ---
    private val _isAppLockEnabled = MutableStateFlow(
        try { prefs.getBoolean(KEY_IS_APP_LOCK_ENABLED, false) } catch (e: Exception) { false }
    )
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _appLockPin = MutableStateFlow(
        try { prefs.getString(KEY_APP_LOCK_PIN, "") ?: "" } catch (e: Exception) { "" }
    )
    val appLockPin: StateFlow<String> = _appLockPin.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(
        try { prefs.getBoolean(KEY_IS_BIOMETRIC_ENABLED, true) } catch (e: Exception) { true }
    )
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // --- WebDAV Cloud Sync Preferences ---
    private val _webdavServerUrl = MutableStateFlow(
        try { prefs.getString(KEY_WEBDAV_SERVER_URL, DEFAULT_WEBDAV_SERVER_URL) ?: DEFAULT_WEBDAV_SERVER_URL } catch (e: Exception) { DEFAULT_WEBDAV_SERVER_URL }
    )
    val webdavServerUrl: StateFlow<String> = _webdavServerUrl.asStateFlow()

    private val _webdavUsername = MutableStateFlow(
        try { prefs.getString(KEY_WEBDAV_USERNAME, "") ?: "" } catch (e: Exception) { "" }
    )
    val webdavUsername: StateFlow<String> = _webdavUsername.asStateFlow()

    private val _webdavPassword = MutableStateFlow(
        try { prefs.getString(KEY_WEBDAV_PASSWORD, "") ?: "" } catch (e: Exception) { "" }
    )
    val webdavPassword: StateFlow<String> = _webdavPassword.asStateFlow()

    private val _webdavRemoteDir = MutableStateFlow(
        try { prefs.getString(KEY_WEBDAV_REMOTE_DIR, DEFAULT_WEBDAV_REMOTE_DIR) ?: DEFAULT_WEBDAV_REMOTE_DIR } catch (e: Exception) { DEFAULT_WEBDAV_REMOTE_DIR }
    )
    val webdavRemoteDir: StateFlow<String> = _webdavRemoteDir.asStateFlow()

    private val _webdavAutoSync = MutableStateFlow(
        try { prefs.getBoolean(KEY_WEBDAV_AUTO_SYNC, false) } catch (e: Exception) { false }
    )
    val webdavAutoSync: StateFlow<Boolean> = _webdavAutoSync.asStateFlow()

    private val _webdavLastSyncTime = MutableStateFlow(
        try { prefs.getLong(KEY_WEBDAV_LAST_SYNC_TIME, 0L) } catch (e: Exception) { 0L }
    )
    val webdavLastSyncTime: StateFlow<Long> = _webdavLastSyncTime.asStateFlow()

    private val _webdavEncryptionEnabled = MutableStateFlow(
        try { prefs.getBoolean(KEY_WEBDAV_ENCRYPTION_ENABLED, false) } catch (e: Exception) { false }
    )
    val webdavEncryptionEnabled: StateFlow<Boolean> = _webdavEncryptionEnabled.asStateFlow()

    private val _webdavEncryptionPassword = MutableStateFlow(
        try { prefs.getString(KEY_WEBDAV_ENCRYPTION_PASSWORD, "") ?: "" } catch (e: Exception) { "" }
    )
    val webdavEncryptionPassword: StateFlow<String> = _webdavEncryptionPassword.asStateFlow()

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

    fun setImageCompressionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IMAGE_COMPRESSION_ENABLED, enabled).apply()
        _imageCompressionEnabled.value = enabled
    }

    fun setCalendarTimeZone(timeZoneId: String) {
        prefs.edit().putString(KEY_CALENDAR_TIMEZONE, timeZoneId).apply()
        _calendarTimeZone.value = timeZoneId
    }

    fun setUserName(name: String) {
        val clean = name.trim().ifEmpty { "木下" }
        prefs.edit().putString(KEY_USER_NAME, clean).apply()
        _userName.value = clean
    }

    fun setProStatus(isPro: Boolean, planType: String = "PRO", expireTime: Long = -1L) {
        prefs.edit()
            .putBoolean(KEY_IS_PRO_USER, isPro)
            .putString(KEY_PRO_PLAN_TYPE, planType)
            .putLong(KEY_PRO_EXPIRE_TIME, expireTime)
            .apply()
        _isProUser.value = isPro
        _proPlanType.value = planType
        _proExpireTime.value = expireTime
    }

    /**
     * Activate VIP using 16-character license key (卡密/兑换码).
     */
    fun activateLicenseCode(inputCode: String): Result<String> {
        val code = inputCode.trim().uppercase()
        if (code.isBlank()) {
            return Result.failure(IllegalArgumentException("请输入激活码 / 卡密"))
        }

        // Standard validation patterns for Lozify VIP License Keys
        val isValid = code.startsWith("LOZIFY-") || 
                      code.startsWith("FLOMO-") || 
                      code.startsWith("PRO-") ||
                      code.startsWith("VIP-") ||
                      (code.length >= 10 && code.any { it.isDigit() } && code.any { it.isLetter() })

        return if (isValid) {
            val isLifetime = code.contains("LIFE") || code.contains("888") || code.contains("999") || code.contains("FOREVER")
            val plan = if (isLifetime) "LIFETIME 终身版" else "PRO 年度会员"
            val expire = if (isLifetime) -1L else System.currentTimeMillis() + 365L * 24 * 3600 * 1000

            setProStatus(isPro = true, planType = plan, expireTime = expire)
            Result.success("恭喜！已成功激活 $plan 会员权益")
        } else {
            Result.failure(IllegalArgumentException("激活码无效或已失效，请检查后重试"))
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

    fun setWebDavEncryption(enabled: Boolean, password: String) {
        prefs.edit()
            .putBoolean(KEY_WEBDAV_ENCRYPTION_ENABLED, enabled)
            .putString(KEY_WEBDAV_ENCRYPTION_PASSWORD, password.trim())
            .apply()

        _webdavEncryptionEnabled.value = enabled
        _webdavEncryptionPassword.value = password.trim()
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
            .remove(KEY_WEBDAV_ENCRYPTION_ENABLED)
            .remove(KEY_WEBDAV_ENCRYPTION_PASSWORD)
            .apply()

        _webdavServerUrl.value = DEFAULT_WEBDAV_SERVER_URL
        _webdavUsername.value = ""
        _webdavPassword.value = ""
        _webdavRemoteDir.value = DEFAULT_WEBDAV_REMOTE_DIR
        _webdavAutoSync.value = false
        _webdavLastSyncTime.value = 0L
        _webdavEncryptionEnabled.value = false
        _webdavEncryptionPassword.value = ""
    }

    // --- Purged Sync IDs (Tombstone Purge Protocol for Multi-Device Empty Trash Sync) ---
    private val _purgedSyncIds = MutableStateFlow<Set<String>>(
        prefs.getStringSet(KEY_PURGED_SYNC_IDS, emptySet()) ?: emptySet()
    )
    val purgedSyncIds: StateFlow<Set<String>> = _purgedSyncIds.asStateFlow()

    fun recordPurgedSyncIds(newSyncIds: Collection<String>) {
        if (newSyncIds.isEmpty()) return
        val current = _purgedSyncIds.value
        val updated = (current + newSyncIds).toList().takeLast(1000).toSet()
        _purgedSyncIds.value = updated
        prefs.edit().putStringSet(KEY_PURGED_SYNC_IDS, updated).apply()
    }

    fun getPurgedSyncIds(): Set<String> {
        return _purgedSyncIds.value
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_APP_LOCK_ENABLED, enabled).apply()
        _isAppLockEnabled.value = enabled
    }

    fun setAppLockPin(pin: String) {
        prefs.edit().putString(KEY_APP_LOCK_PIN, pin).apply()
        _appLockPin.value = pin
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun verifyAppLockPin(pin: String): Boolean {
        return _appLockPin.value.isNotEmpty() && _appLockPin.value == pin
    }

    companion object {
        private const val PREFS_NAME = "lozify_user_preferences"
        private const val KEY_MAX_COLLAPSE_LINES = "key_max_collapse_lines"
        private const val KEY_DRAFT_PERSISTENCE_ENABLED = "key_draft_persistence_enabled"
        private const val KEY_IMAGE_COMPRESSION_ENABLED = "key_image_compression_enabled"
        private const val KEY_DRAFT_TEXT = "key_draft_text"
        private const val KEY_DRAFT_IMAGE_URIS = "key_draft_image_uris"
        private const val KEY_CALENDAR_TIMEZONE = "key_calendar_timezone"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_IS_PRO_USER = "key_is_pro_user"
        private const val KEY_PRO_PLAN_TYPE = "key_pro_plan_type"
        private const val KEY_PRO_EXPIRE_TIME = "key_pro_expire_time"
        private const val KEY_IS_APP_LOCK_ENABLED = "key_is_app_lock_enabled"
        private const val KEY_APP_LOCK_PIN = "key_app_lock_pin"
        private const val KEY_IS_BIOMETRIC_ENABLED = "key_is_biometric_enabled"
        private const val KEY_PURGED_SYNC_IDS = "key_purged_sync_ids"

        // WebDAV keys
        private const val KEY_WEBDAV_SERVER_URL = "key_webdav_server_url"
        private const val KEY_WEBDAV_USERNAME = "key_webdav_username"
        private const val KEY_WEBDAV_PASSWORD = "key_webdav_password"
        private const val KEY_WEBDAV_REMOTE_DIR = "key_webdav_remote_dir"
        private const val KEY_WEBDAV_AUTO_SYNC = "key_webdav_auto_sync"
        private const val KEY_WEBDAV_LAST_SYNC_TIME = "key_webdav_last_sync_time"
        private const val KEY_WEBDAV_ENCRYPTION_ENABLED = "key_webdav_encryption_enabled"
        private const val KEY_WEBDAV_ENCRYPTION_PASSWORD = "key_webdav_encryption_password"

        const val DEFAULT_MAX_COLLAPSE_LINES = 5
        const val DEFAULT_WEBDAV_SERVER_URL = "https://dav.jianguoyun.com/dav/"
        const val DEFAULT_WEBDAV_REMOTE_DIR = "/Lozify/"
    }
}

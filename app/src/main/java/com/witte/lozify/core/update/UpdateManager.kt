package com.witte.lozify.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.witte.lozify.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppUpdateInfo - Data class holding release details of a newer version.
 */
data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseDate: String,
    val changelog: String,
    val downloadUrl: String,
    val githubDownloadUrl: String? = null,
    val isForceUpdate: Boolean = false
)

/**
 * UpdateManager - Gitee (Primary) + GitHub (Fallback) Dual-Channel In-App Update Engine.
 *
 * Stage 28: Online Version Detection & Push System.
 */
@Singleton
class UpdateManager @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Gitee Primary Source (Fast domestic connection in China)
    private val giteeVersionUrl = "https://gitee.com/JessesChloe/Lozify/raw/master/version.json"

    // GitHub Fallback Source (International secondary mirror)
    private val githubVersionUrl = "https://raw.githubusercontent.com/JessesChloe/Lozify/master/version.json"

    /**
     * Check for newer app version with Gitee -> GitHub automatic failover.
     *
     * @param currentVersionCode Current local build version code (default: BuildConfig.VERSION_CODE)
     * @return Result containing AppUpdateInfo if an update exists, null if already up to date.
     */
    suspend fun checkForUpdate(
        currentVersionCode: Int = BuildConfig.VERSION_CODE
    ): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        // 1. Try Gitee primary source first (fast domestic access in China)
        val giteeResult = fetchVersionJson(giteeVersionUrl)
        val jsonString = if (giteeResult.isSuccess) {
            giteeResult.getOrNull()
        } else {
            // 2. Failover to GitHub secondary source
            val githubResult = fetchVersionJson(githubVersionUrl)
            githubResult.getOrNull()
        }

        if (jsonString.isNullOrBlank()) {
            return@withContext Result.failure(
                giteeResult.exceptionOrNull() ?: IllegalStateException("无法连接至版本更新服务器，请检查网络连接")
            )
        }

        try {
            val json = JSONObject(jsonString)
            val remoteVersionCode = json.optInt("versionCode", 0)
            val remoteVersionName = json.optString("versionName", "1.0.0")
            val releaseDate = json.optString("releaseDate", "")
            val changelog = json.optString("changelog", "性能优化与体验改进")
            val downloadUrl = json.optString("downloadUrl", "")
            val githubDownloadUrl = json.optString("githubDownloadUrl", "").ifBlank { null }
            val isForceUpdate = json.optBoolean("isForceUpdate", false)

            if (remoteVersionCode > currentVersionCode) {
                val updateInfo = AppUpdateInfo(
                    versionCode = remoteVersionCode,
                    versionName = remoteVersionName,
                    releaseDate = releaseDate,
                    changelog = changelog,
                    downloadUrl = downloadUrl,
                    githubDownloadUrl = githubDownloadUrl,
                    isForceUpdate = isForceUpdate
                )
                Result.success(updateInfo)
            } else {
                // Already latest version
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse raw JSON string into AppUpdateInfo (helper for testing and custom sources).
     */
    fun parseUpdateJson(jsonString: String, currentVersionCode: Int): AppUpdateInfo? {
        val json = JSONObject(jsonString)
        val remoteVersionCode = json.optInt("versionCode", 0)
        if (remoteVersionCode <= currentVersionCode) return null

        return AppUpdateInfo(
            versionCode = remoteVersionCode,
            versionName = json.optString("versionName", ""),
            releaseDate = json.optString("releaseDate", ""),
            changelog = json.optString("changelog", ""),
            downloadUrl = json.optString("downloadUrl", ""),
            githubDownloadUrl = json.optString("githubDownloadUrl", "").ifBlank { null },
            isForceUpdate = json.optBoolean("isForceUpdate", false)
        )
    }

    /**
     * Open download URL in user's browser.
     */
    fun openDownloadUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchVersionJson(url: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Lozify-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Result.success(body)
            } else {
                Result.failure(IllegalStateException("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

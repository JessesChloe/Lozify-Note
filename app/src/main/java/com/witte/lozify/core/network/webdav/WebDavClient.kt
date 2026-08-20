package com.witte.lozify.core.network.webdav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebDavFileItem - Representation of a remote WebDAV resource.
 */
data class WebDavFileItem(
    val href: String,
    val isDirectory: Boolean,
    val contentLength: Long = 0L,
    val lastModified: String = ""
)

/**
 * WebDavClient - Production-grade WebDAV client powered by OkHttp.
 *
 * Supports:
 * - Jianguoyun (Nutstore) & Standard WebDAV RFC 4918 servers
 * - HTTP Basic Authentication with App Passwords
 * - PROPFIND (Depth 0/1), MKCOL, PUT, GET, HEAD, DELETE
 * - Robust error classification (401 Auth Error, 404 Not Found, Network Timeout)
 *
 * Stage 26: WebDAV Cloud Sync.
 */
@Singleton
class WebDavClient @Inject constructor() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Test connection and credentials against WebDAV server.
     */
    suspend fun testConnection(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(serverUrl)
            val credential = Credentials.basic(username, password)

            val request = Request.Builder()
                .url(normalizedUrl)
                .method("PROPFIND", "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\"><D:prop><D:resourcetype/></D:prop></D:propfind>".toRequestBody("application/xml; charset=utf-8".toMediaTypeOrNull()))
                .header("Authorization", credential)
                .header("Depth", "0")
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful || response.code == 207 -> Result.success(true)
                    response.code == 401 -> Result.failure(IOException("用户名或应用授权密码错误 (HTTP 401 Unauthorized)"))
                    response.code == 403 -> Result.failure(IOException("服务器拒绝访问或权限不足 (HTTP 403 Forbidden)"))
                    response.code == 404 -> Result.failure(IOException("服务器 WebDAV 根路径不存在 (HTTP 404 Not Found)"))
                    else -> Result.failure(IOException("服务器响应异常 (HTTP ${response.code} ${response.message})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(classifyExceptionMessage(e), e))
        }
    }

    /**
     * Ensure a remote directory structure exists on WebDAV server (creates via MKCOL if missing).
     */
    suspend fun ensureDirectory(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val credential = Credentials.basic(username, password)
            val segments = remotePath.trim('/').split('/').filter { it.isNotEmpty() }
            var currentPath = ""

            for (segment in segments) {
                currentPath += "/$segment"
                val dirUrl = combineUrl(serverUrl, currentPath) + "/"

                // Check if directory exists
                val checkReq = Request.Builder()
                    .url(dirUrl)
                    .method("PROPFIND", null)
                    .header("Authorization", credential)
                    .header("Depth", "0")
                    .build()

                val exists = client.newCall(checkReq).execute().use { response ->
                    response.isSuccessful || response.code == 207 || response.code == 405
                }

                if (!exists) {
                    val mkcolReq = Request.Builder()
                        .url(dirUrl)
                        .method("MKCOL", null)
                        .header("Authorization", credential)
                        .build()

                    client.newCall(mkcolReq).execute().use { response ->
                        if (!response.isSuccessful && response.code != 201 && response.code != 405) {
                            return@withContext Result.failure(
                                IOException("创建远程目录失败 $currentPath (HTTP ${response.code})")
                            )
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IOException(classifyExceptionMessage(e), e))
        }
    }

    /**
     * Upload in-memory ByteArray to a remote path via PUT.
     */
    suspend fun uploadBytes(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String,
        bytes: ByteArray,
        contentType: String = "application/json; charset=utf-8"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = combineUrl(serverUrl, remotePath)
            val credential = Credentials.basic(username, password)
            val body = bytes.toRequestBody(contentType.toMediaTypeOrNull())

            val request = Request.Builder()
                .url(fullUrl)
                .put(body)
                .header("Authorization", credential)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 201 || response.code == 204) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("上传文件失败 (HTTP ${response.code} ${response.message})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(classifyExceptionMessage(e), e))
        }
    }

    /**
     * Download in-memory ByteArray from a remote path via GET.
     */
    suspend fun downloadBytes(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = combineUrl(serverUrl, remotePath)
            val credential = Credentials.basic(username, password)

            val request = Request.Builder()
                .url(fullUrl)
                .get()
                .header("Authorization", credential)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.bytes() ?: ByteArray(0)
                    Result.success(body)
                } else if (response.code == 404) {
                    Result.failure(FileNotFoundException("远端文件不存在: $remotePath"))
                } else {
                    Result.failure(IOException("下载文件失败 (HTTP ${response.code} ${response.message})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(classifyExceptionMessage(e), e))
        }
    }

    /**
     * Upload local File to remote path via PUT.
     */
    suspend fun uploadFile(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String,
        localFile: File,
        contentType: String = "image/jpeg"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!localFile.exists()) {
                return@withContext Result.failure(IOException("本地待上传文件不存在: ${localFile.absolutePath}"))
            }

            val fullUrl = combineUrl(serverUrl, remotePath)
            val credential = Credentials.basic(username, password)
            val body = localFile.asRequestBody(contentType.toMediaTypeOrNull())

            val request = Request.Builder()
                .url(fullUrl)
                .put(body)
                .header("Authorization", credential)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 201 || response.code == 204) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("上传文件失败 ${localFile.name} (HTTP ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(classifyExceptionMessage(e), e))
        }
    }

    /**
     * Download remote file to local File via streaming GET.
     */
    suspend fun downloadFile(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String,
        targetFile: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = combineUrl(serverUrl, remotePath)
            val credential = Credentials.basic(username, password)

            val request = Request.Builder()
                .url(fullUrl)
                .get()
                .header("Authorization", credential)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                        ?: return@withContext Result.failure(IOException("远端返回空数据流"))

                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("下载文件失败 (HTTP ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(classifyExceptionMessage(e), e))
        }
    }

    /**
     * List all files within a remote directory via PROPFIND (Depth: 1).
     */
    suspend fun listFiles(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String
    ): Result<List<WebDavFileItem>> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = combineUrl(serverUrl, remotePath).let { if (it.endsWith("/")) it else "$it/" }
            val credential = Credentials.basic(username, password)

            val request = Request.Builder()
                .url(fullUrl)
                .method("PROPFIND", "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\"><D:prop><D:resourcetype/><D:getcontentlength/><D:getlastmodified/></D:prop></D:propfind>".toRequestBody("application/xml; charset=utf-8".toMediaTypeOrNull()))
                .header("Authorization", credential)
                .header("Depth", "1")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 207) {
                    val xml = response.body?.string() ?: ""
                    val items = parsePropfindXml(xml)
                    Result.success(items)
                } else if (response.code == 404) {
                    Result.success(emptyList())
                } else {
                    Result.failure(IOException("查询远端目录失败 (HTTP ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(classifyExceptionMessage(e), e))
        }
    }

    /**
     * Check if a remote file exists.
     */
    suspend fun exists(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = combineUrl(serverUrl, remotePath)
            val credential = Credentials.basic(username, password)

            val request = Request.Builder()
                .url(fullUrl)
                .head()
                .header("Authorization", credential)
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> Result.success(true)
                    response.code == 404 -> Result.success(false)
                    else -> Result.failure(IOException("检查文件状态异常 (HTTP ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(classifyExceptionMessage(e), e))
        }
    }

    private fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        return if (clean.endsWith("/")) clean else "$clean/"
    }

    private fun combineUrl(baseUrl: String, relativePath: String): String {
        val cleanBase = normalizeUrl(baseUrl).removeSuffix("/")
        val cleanPath = if (relativePath.startsWith("/")) relativePath else "/$relativePath"
        return cleanBase + cleanPath
    }

    private fun parsePropfindXml(xml: String): List<WebDavFileItem> {
        val items = mutableListOf<WebDavFileItem>()
        val responsePattern = Pattern.compile("<(?:\\w+:)?response[\\s>](.*?)</(?:\\w+:)?response>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val hrefPattern = Pattern.compile("<(?:\\w+:)?href>(.*?)</(?:\\w+:)?href>", Pattern.CASE_INSENSITIVE)
        val collectionPattern = Pattern.compile("<(?:\\w+:)?collection\\s*/>", Pattern.CASE_INSENSITIVE)
        val lengthPattern = Pattern.compile("<(?:\\w+:)?getcontentlength>(\\d+)</(?:\\w+:)?getcontentlength>", Pattern.CASE_INSENSITIVE)
        val modifiedPattern = Pattern.compile("<(?:\\w+:)?getlastmodified>(.*?)</(?:\\w+:)?getlastmodified>", Pattern.CASE_INSENSITIVE)

        val matcher = responsePattern.matcher(xml)
        while (matcher.find()) {
            val block = matcher.group(1) ?: continue
            val hrefMatcher = hrefPattern.matcher(block)
            if (hrefMatcher.find()) {
                val rawHref = hrefMatcher.group(1)?.trim() ?: continue
                val decodedHref = try {
                    java.net.URLDecoder.decode(rawHref, java.nio.charset.StandardCharsets.UTF_8.name())
                } catch (e: Exception) {
                    rawHref
                }
                val isCollection = collectionPattern.matcher(block).find()
                var length = 0L
                val lengthMatcher = lengthPattern.matcher(block)
                if (lengthMatcher.find()) {
                    length = lengthMatcher.group(1)?.toLongOrNull() ?: 0L
                }
                var modified = ""
                val modifiedMatcher = modifiedPattern.matcher(block)
                if (modifiedMatcher.find()) {
                    modified = modifiedMatcher.group(1)?.trim() ?: ""
                }
                items.add(WebDavFileItem(href = decodedHref, isDirectory = isCollection, contentLength = length, lastModified = modified))
            }
        }
        return items
    }

    private fun classifyExceptionMessage(e: Exception): String {
        return when {
            e is java.net.UnknownHostException -> "无法解析服务器地址，请检查网络或 URL 拼写"
            e is java.net.SocketTimeoutException -> "网络连接超时，请检查网络环境或服务器状态"
            e is java.net.ConnectException -> "连接服务器失败，服务器可能未启动或端口不通"
            e is javax.net.ssl.SSLException -> "SSL 安全证书校验失败"
            e.message?.contains("401") == true -> "用户名或应用授权密码错误 (HTTP 401)"
            else -> e.message ?: "未知网络错误 (${e.javaClass.simpleName})"
        }
    }
}

class FileNotFoundException(message: String) : IOException(message)

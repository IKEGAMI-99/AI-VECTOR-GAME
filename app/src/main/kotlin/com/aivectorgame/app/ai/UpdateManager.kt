package com.aivectorgame.app.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.aivectorgame.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val context: Context) {
    data class ReleaseInfo(
        val version: String,
        val tag: String,
        val apkUrl: String,
        val apkName: String,
        val pageUrl: String,
        val notes: String,
    )

    enum class InstallResult { STARTED, PERMISSION_REQUIRED }

    companion object {
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/IKEGAMI-99/AI-VECTOR-GAME/releases/latest"
    }

    suspend fun checkLatest(): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open(LATEST_RELEASE_API)
            try {
                connection.connect()
                check(connection.responseCode in 200..299) {
                    "GitHub update check failed: HTTP ${connection.responseCode}"
                }
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(json)
                val tag = root.getString("tag_name")
                val version = tag.removePrefix("v")
                val assets = root.getJSONArray("assets")
                var apkUrl: String? = null
                var apkName: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkName = name
                        break
                    }
                }
                if (!isNewer(version, BuildConfig.VERSION_NAME)) return@runCatching null
                check(!apkUrl.isNullOrBlank() && !apkName.isNullOrBlank()) {
                    "Latest GitHub release does not contain an APK"
                }
                ReleaseInfo(
                    version = version,
                    tag = tag,
                    apkUrl = apkUrl,
                    apkName = apkName,
                    pageUrl = root.optString("html_url"),
                    notes = root.optString("body"),
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun download(
        release: ReleaseInfo,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val dir = File(base, "updates").apply { mkdirs() }
            val target = File(dir, release.apkName)
            val partial = File(dir, "${release.apkName}.part")
            if (partial.exists()) partial.delete()

            val connection = open(release.apkUrl)
            try {
                connection.connect()
                check(connection.responseCode in 200..299) {
                    "APK download failed: HTTP ${connection.responseCode}"
                }
                val total = connection.contentLengthLong.coerceAtLeast(1L)
                connection.inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                        var copied = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            onProgress((copied.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f))
                        }
                    }
                }
                check(partial.length() > 1_000_000L) { "Downloaded APK is unexpectedly small" }
                if (target.exists()) target.delete()
                check(partial.renameTo(target)) { "Could not finalize update APK" }
                onProgress(1f)
                target
            } finally {
                connection.disconnect()
            }
        }
    }

    fun install(apk: File): InstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return InstallResult.PERMISSION_REQUIRED
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return InstallResult.STARTED
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "AI-VECTOR-GAME/${BuildConfig.VERSION_NAME}")
        }

    private fun isNewer(remote: String, current: String): Boolean {
        fun parts(value: String): List<Int> = value
            .substringBefore('-')
            .removePrefix("v")
            .split('.')
            .map { it.toIntOrNull() ?: 0 }
            .let { it + List((3 - it.size).coerceAtLeast(0)) { 0 } }
            .take(3)
        val r = parts(remote)
        val c = parts(current)
        for (i in 0..2) {
            if (r[i] != c[i]) return r[i] > c[i]
        }
        return false
    }
}

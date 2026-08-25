package com.aivectorgame.app.ai

import android.content.Context
import com.aivectorgame.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModelManager(private val context: Context) {
    enum class Kind { CAUSAL, EMBEDDING }

    data class ModelSpec(
        val kind: Kind,
        val title: String,
        val subtitle: String,
        val fileName: String,
        val url: String,
        val approxMb: Int,
    )

    companion object {
        val CAUSAL = ModelSpec(
            kind = Kind.CAUSAL,
            title = "LFM2.5 230M",
            subtitle = "Next-token logits / Q4_K_M",
            fileName = "LFM2.5-230M-Q4_K_M.gguf",
            url = "https://huggingface.co/LiquidAI/LFM2.5-230M-GGUF/resolve/main/LFM2.5-230M-Q4_K_M.gguf?download=true",
            approxMb = 153,
        )

        val EMBEDDING = ModelSpec(
            kind = Kind.EMBEDDING,
            title = "LFM2.5 Embedding 350M",
            subtitle = "Semantic embedding / Q4_K_M",
            fileName = "LFM2.5-Embedding-350M-Q4_K_M.gguf",
            url = "https://huggingface.co/LiquidAI/LFM2.5-Embedding-350M-GGUF/resolve/main/LFM2.5-Embedding-350M-Q4_K_M.gguf?download=true",
            approxMb = 229,
        )
    }

    private val modelDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    fun fileFor(spec: ModelSpec): File = File(modelDir, spec.fileName)
    fun isInstalled(spec: ModelSpec): Boolean = fileFor(spec).let { it.exists() && it.length() > 1_000_000L }

    suspend fun download(
        spec: ModelSpec,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            modelDir.mkdirs()
            val target = fileFor(spec)
            val partial = File(modelDir, "${spec.fileName}.part")
            if (target.exists() && target.length() > 1_000_000L) return@runCatching target

            val connection = (URL(spec.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "AI-VECTOR-GAME/${BuildConfig.VERSION_NAME}")
            }

            try {
                connection.connect()
                check(connection.responseCode in 200..299) {
                    "Download failed: HTTP ${connection.responseCode}"
                }
                val total = connection.contentLengthLong.coerceAtLeast(1L)
                connection.inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
                        var copied = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            onProgress((copied.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f))
                        }
                        output.flush()
                    }
                }
                check(partial.length() > 1_000_000L) { "Downloaded model is unexpectedly small" }
                if (target.exists()) target.delete()
                check(partial.renameTo(target)) { "Could not finalize model file" }
                onProgress(1f)
                target
            } finally {
                connection.disconnect()
            }
        }
    }

    fun delete(spec: ModelSpec): Boolean {
        val a = fileFor(spec).delete()
        val b = File(modelDir, "${spec.fileName}.part").delete()
        return a || b
    }
}

package com.aivectorgame.app.ai

import org.json.JSONArray
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object NativeEngine {
    private val nativeReady = AtomicBoolean(false)
    private val causalLoaded = AtomicBoolean(false)
    private val embeddingLoaded = AtomicBoolean(false)

    init {
        nativeReady.set(runCatching {
            System.loadLibrary("ai_vector_native")
            true
        }.getOrDefault(false))
    }

    data class TokenPrediction(
        val tokenId: Int,
        val piece: String,
        val logit: Float,
        val probability: Float,
    )

    fun isNativeReady(): Boolean = nativeReady.get()
    fun isCausalLoaded(): Boolean = causalLoaded.get()
    fun isEmbeddingLoaded(): Boolean = embeddingLoaded.get()

    @Synchronized
    fun loadCausal(file: File): Result<Unit> = runCatching {
        check(nativeReady.get()) { "Native llama.cpp library is unavailable" }
        check(file.exists()) { "Causal model file is missing" }
        check(nativeLoadCausal(file.absolutePath)) { nativeLastError() }
        causalLoaded.set(true)
    }

    @Synchronized
    fun loadEmbedding(file: File): Result<Unit> = runCatching {
        check(nativeReady.get()) { "Native llama.cpp library is unavailable" }
        check(file.exists()) { "Embedding model file is missing" }
        check(nativeLoadEmbedding(file.absolutePath)) { nativeLastError() }
        embeddingLoaded.set(true)
    }

    @Synchronized
    fun topTokens(prompt: String, topK: Int = 6): Result<List<TokenPrediction>> = runCatching {
        check(causalLoaded.get()) { "Causal model is not loaded" }
        val raw = nativePredictTopTokens(prompt, topK)
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    TokenPrediction(
                        tokenId = obj.getInt("id"),
                        piece = obj.getString("piece"),
                        logit = obj.getDouble("logit").toFloat(),
                        probability = obj.getDouble("prob").toFloat(),
                    )
                )
            }
        }
    }

    @Synchronized
    fun embedding(text: String): Result<FloatArray> = runCatching {
        check(embeddingLoaded.get()) { "Embedding model is not loaded" }
        nativeEmbedding(text)
    }

    private external fun nativeLoadCausal(path: String): Boolean
    private external fun nativeLoadEmbedding(path: String): Boolean
    private external fun nativePredictTopTokens(prompt: String, topK: Int): String
    private external fun nativeEmbedding(text: String): FloatArray
    private external fun nativeLastError(): String
}

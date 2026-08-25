package com.aivectorgame.app.game

import com.aivectorgame.app.ai.NativeEngine

object GameData {
    data class EmbeddingQuestion(
        val target: String,
        val choices: List<String>,
        val demoScores: List<Float>,
    )

    data class LogitQuestion(
        val prompt: String,
        val demoPredictions: List<NativeEngine.TokenPrediction>,
    )

    val embeddingQuestions = listOf(
        EmbeddingQuestion(
            target = "宇宙",
            choices = listOf("銀河", "星", "ロケット", "海", "電話", "ケーキ"),
            demoScores = listOf(0.87f, 0.82f, 0.64f, 0.24f, 0.11f, 0.06f),
        ),
        EmbeddingQuestion(
            target = "犬",
            choices = listOf("猫", "狼", "ペット", "自動車", "冷蔵庫", "動物"),
            demoScores = listOf(0.82f, 0.78f, 0.76f, 0.18f, 0.08f, 0.80f),
        ),
        EmbeddingQuestion(
            target = "カメラ",
            choices = listOf("レンズ", "写真", "撮影", "テレビ", "包丁", "砂漠"),
            demoScores = listOf(0.86f, 0.84f, 0.80f, 0.45f, 0.09f, 0.04f),
        ),
        EmbeddingQuestion(
            target = "冬",
            choices = listOf("雪", "寒い", "夏", "コート", "熱帯", "キーボード"),
            demoScores = listOf(0.83f, 0.81f, 0.53f, 0.61f, 0.21f, 0.03f),
        ),
    )

    val logitQuestions = listOf(
        LogitQuestion(
            prompt = "日本の首都は",
            demoPredictions = demoTokens("東京" to 0.51f, "、" to 0.15f, "日本" to 0.09f, "京都" to 0.06f, "大阪" to 0.04f, "東" to 0.03f),
        ),
        LogitQuestion(
            prompt = "今日はとても",
            demoPredictions = demoTokens("暑い" to 0.29f, "良い" to 0.17f, "寒い" to 0.12f, "楽しい" to 0.09f, "忙しい" to 0.06f, "眠い" to 0.04f),
        ),
        LogitQuestion(
            prompt = "彼はドアを開けて",
            demoPredictions = demoTokens("、" to 0.22f, "中" to 0.16f, "部屋" to 0.13f, "外" to 0.08f, "入った" to 0.07f, "言った" to 0.04f),
        ),
        LogitQuestion(
            prompt = "猫が好きなので、家で",
            demoPredictions = demoTokens("飼って" to 0.25f, "猫" to 0.19f, "一緒" to 0.10f, "遊んで" to 0.08f, "寝て" to 0.05f, "仕事" to 0.03f),
        ),
    )

    private fun demoTokens(vararg values: Pair<String, Float>): List<NativeEngine.TokenPrediction> =
        values.mapIndexed { index, (piece, prob) ->
            NativeEngine.TokenPrediction(index, piece, kotlin.math.ln(prob), prob)
        }
}

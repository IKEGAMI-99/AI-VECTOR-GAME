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

    // DEMO is deliberately fixed so the game can be played before a model is downloaded.
    val embeddingDemoQuestions = listOf(
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

    // LIVE uses a different question bank. Scores below are only an emergency fallback;
    // successful LIVE inference replaces them with real model cosine similarities.
    val embeddingLiveQuestions = listOf(
        EmbeddingQuestion("量子", listOf("粒子", "物理", "コンピュータ", "料理", "砂浜", "靴"), listOf(0.84f, 0.80f, 0.63f, 0.09f, 0.05f, 0.03f)),
        EmbeddingQuestion("映画", listOf("映像", "俳優", "カメラ", "冷蔵庫", "数学", "歯ブラシ"), listOf(0.86f, 0.82f, 0.72f, 0.08f, 0.14f, 0.04f)),
        EmbeddingQuestion("雨", listOf("傘", "雲", "水", "砂漠", "CPU", "ピアノ"), listOf(0.82f, 0.78f, 0.70f, 0.24f, 0.04f, 0.05f)),
        EmbeddingQuestion("医者", listOf("病院", "患者", "看護師", "惑星", "ケーキ", "ギター"), listOf(0.84f, 0.82f, 0.80f, 0.06f, 0.05f, 0.04f)),
        EmbeddingQuestion("東京", listOf("日本", "都市", "大阪", "銀河", "レンズ", "猫"), listOf(0.86f, 0.80f, 0.71f, 0.08f, 0.05f, 0.04f)),
        EmbeddingQuestion("音楽", listOf("曲", "歌", "楽器", "エンジン", "砂", "数学"), listOf(0.88f, 0.84f, 0.80f, 0.08f, 0.04f, 0.12f)),
        EmbeddingQuestion("幸福", listOf("喜び", "笑顔", "感情", "配線", "金属", "冷蔵庫"), listOf(0.85f, 0.78f, 0.72f, 0.05f, 0.03f, 0.02f)),
        EmbeddingQuestion("海", listOf("海洋", "波", "船", "砂漠", "CPU", "鉛筆"), listOf(0.88f, 0.82f, 0.70f, 0.18f, 0.03f, 0.04f)),
    )

    val logitDemoQuestions = listOf(
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

    // LIVE prompts are intentionally different from DEMO prompts.
    val logitLiveQuestions = listOf(
        LogitQuestion("彼女はコーヒーを一口飲んで", fallbackLiveTokens("、", "から", "み", "笑", "席", "カップ")),
        LogitQuestion("雨が降りそうなので、傘を", fallbackLiveTokens("持", "持って", "用意", "持ち", "忘れ", "買")),
        LogitQuestion("カメラのレンズを交換して", fallbackLiveTokens("、", "撮影", "から", "写真", "み", "設定")),
        LogitQuestion("電車が駅に到着すると、乗客は", fallbackLiveTokens("降り", "、", "ホーム", "一斉", "席", "ドア")),
        LogitQuestion("朝起きて最初に", fallbackLiveTokens("する", "、", "水", "顔", "コーヒー", "スマホ")),
        LogitQuestion("AIモデルは大量のデータから", fallbackLiveTokens("学習", "、", "パターン", "知識", "情報", "答え")),
        LogitQuestion("夜空を見上げると、たくさんの", fallbackLiveTokens("星", "、", "光", "雲", "星が", "もの")),
        LogitQuestion("料理が完成したので、皿に", fallbackLiveTokens("盛り", "、", "移し", "入れ", "乗せ", "置")),
    )

    private fun demoTokens(vararg values: Pair<String, Float>): List<NativeEngine.TokenPrediction> =
        values.mapIndexed { index, (piece, prob) ->
            NativeEngine.TokenPrediction(index, piece, kotlin.math.ln(prob), prob)
        }

    private fun fallbackLiveTokens(vararg pieces: String): List<NativeEngine.TokenPrediction> {
        val probs = listOf(0.32f, 0.18f, 0.12f, 0.08f, 0.05f, 0.03f)
        return pieces.mapIndexed { index, piece ->
            val p = probs.getOrElse(index) { 0.02f }
            NativeEngine.TokenPrediction(10_000 + index, piece, kotlin.math.ln(p), p)
        }
    }
}

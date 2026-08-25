package com.aivectorgame.app.game

import com.aivectorgame.app.ai.NativeEngine
import kotlin.math.ln
import kotlin.random.Random

object QuestionFactory {
    data class EmbeddingQuestion(
        val target: String,
        val choices: List<String>,
        val demoScores: List<Float>,
        val seed: Int,
    )

    data class LogitQuestion(
        val prompt: String,
        val humanExpected: String,
        val demoPredictions: List<NativeEngine.TokenPrediction>,
        val seed: Int,
    )

    private data class SemanticCluster(val words: List<String>)

    private val semanticClusters = listOf(
        SemanticCluster(listOf("宇宙", "銀河", "惑星", "恒星", "星雲", "天体", "軌道", "ロケット", "宇宙船")),
        SemanticCluster(listOf("犬", "猫", "狼", "狐", "ペット", "動物", "肉球", "首輪", "散歩")),
        SemanticCluster(listOf("カメラ", "レンズ", "写真", "撮影", "シャッター", "センサー", "露出", "映像", "三脚")),
        SemanticCluster(listOf("雨", "雪", "雲", "雷", "台風", "天気", "傘", "湿度", "気温")),
        SemanticCluster(listOf("音楽", "歌", "曲", "楽器", "ピアノ", "ギター", "ドラム", "メロディ", "リズム")),
        SemanticCluster(listOf("医者", "病院", "患者", "看護師", "診察", "治療", "薬", "手術", "救急")),
        SemanticCluster(listOf("料理", "包丁", "鍋", "レシピ", "食材", "味", "皿", "キッチン", "焼く")),
        SemanticCluster(listOf("AI", "モデル", "学習", "推論", "データ", "GPU", "Transformer", "Attention", "Embedding")),
        SemanticCluster(listOf("電車", "駅", "線路", "ホーム", "乗客", "車両", "改札", "地下鉄", "切符")),
        SemanticCluster(listOf("幸福", "喜び", "笑顔", "安心", "感情", "満足", "希望", "感謝", "楽しい")),
        SemanticCluster(listOf("海", "波", "船", "港", "海岸", "潮", "魚", "島", "砂浜")),
        SemanticCluster(listOf("山", "登山", "頂上", "岩", "森", "谷", "高原", "雪山", "キャンプ")),
        SemanticCluster(listOf("サッカー", "ボール", "ゴール", "選手", "試合", "競技", "スタジアム", "監督", "チーム")),
        SemanticCluster(listOf("スマホ", "画面", "アプリ", "通知", "バッテリー", "充電", "Android", "通信", "タッチ")),
        SemanticCluster(listOf("本", "読書", "文章", "小説", "ページ", "文字", "図書館", "作家", "物語")),
        SemanticCluster(listOf("東京", "大阪", "都市", "日本", "駅", "ビル", "街", "首都", "地下鉄")),
    )

    fun embedding(seed: Int): EmbeddingQuestion {
        val random = Random(seed)
        val clusterIndex = random.nextInt(semanticClusters.size)
        val cluster = semanticClusters[clusterIndex]
        val target = cluster.words.random(random)
        val related = cluster.words
            .filterNot { it == target }
            .shuffled(random)
            .take(3)

        val distractors = semanticClusters
            .filterIndexed { index, _ -> index != clusterIndex }
            .shuffled(random)
            .map { it.words.random(random) }
            .distinct()
            .filterNot { it in related || it == target }
            .take(3)

        val choices = (related + distractors).shuffled(random)
        val demoScores = choices.map { word ->
            if (word in cluster.words) {
                0.72f + random.nextFloat() * 0.22f
            } else {
                0.03f + random.nextFloat() * 0.30f
            }
        }

        return EmbeddingQuestion(
            target = target,
            choices = choices,
            demoScores = demoScores,
            seed = seed,
        )
    }

    fun logit(seed: Int, surprise: Boolean = false): LogitQuestion {
        val random = Random(seed)
        val promptSeed = if (surprise) surprisePrompt(random) else normalPrompt(random)
        val predictions = makeDemoPredictions(
            random = random,
            expected = promptSeed.second,
            surprise = surprise,
        )
        return LogitQuestion(
            prompt = promptSeed.first,
            humanExpected = promptSeed.second,
            demoPredictions = predictions,
            seed = seed,
        )
    }

    fun humanMatches(modelPiece: String, humanExpected: String): Boolean {
        fun normalize(value: String): String = value
            .replace("▁", "")
            .replace(" ", "")
            .replace("\n", "")
            .replace("、", "")
            .replace("。", "")
            .replace("，", "")
            .replace("．", "")
            .trim()
            .lowercase()

        val model = normalize(modelPiece)
        val human = normalize(humanExpected)
        if (model.isBlank() || human.isBlank()) return false
        return model == human || human.startsWith(model) || model.startsWith(human)
    }

    private fun normalPrompt(random: Random): Pair<String, String> = when (random.nextInt(14)) {
        0 -> {
            val facts = listOf(
                "日本" to "東京",
                "フランス" to "パリ",
                "イタリア" to "ローマ",
                "韓国" to "ソウル",
                "中国" to "北京",
                "タイ" to "バンコク",
            )
            val (country, capital) = facts.random(random)
            "${country}の首都は" to capital
        }
        1 -> {
            val a = random.nextInt(2, 10)
            val b = random.nextInt(2, 10)
            "$a + $b =" to (a + b).toString()
        }
        2 -> {
            val pairs = listOf(
                "夜空にはたくさんの" to "星",
                "海岸には細かい" to "砂",
                "冬の朝には白い" to "霜",
                "森の中には高い" to "木",
            )
            pairs.random(random)
        }
        3 -> {
            val people = listOf("彼", "彼女", "友人", "学生", "カメラマン")
            "${people.random(random)}は眠かったので早めに" to "寝た"
        }
        4 -> {
            val items = listOf("傘", "財布", "鍵", "スマホ", "カメラ")
            val item = items.random(random)
            "家を出る前に${item}を" to "確認"
        }
        5 -> {
            val drinks = listOf("コーヒー", "紅茶", "水", "ジュース")
            "${drinks.random(random)}を一口飲んで" to "から"
        }
        6 -> {
            val gear = listOf("レンズ", "バッテリー", "メモリーカード", "三脚", "マイク")
            "撮影前に${gear.random(random)}を" to "確認"
        }
        7 -> "ニューラルネットワークは大量のデータから" to "学習"
        8 -> "電車が駅に到着するとドアが" to "開く"
        9 -> {
            val foods = listOf("カレー", "パスタ", "スープ", "オムライス")
            "${foods.random(random)}が完成したので皿に" to "盛る"
        }
        10 -> {
            val weather = listOf("雨", "雪", "強い風")
            "外は${weather.random(random)}なので今日は" to "傘"
        }
        11 -> {
            val device = listOf("スマホ", "ノートPC", "カメラ", "タブレット")
            "${device.random(random)}のバッテリーが少ないので" to "充電"
        }
        12 -> {
            val activity = listOf("読書", "勉強", "仕事", "撮影")
            "静かな部屋で${activity.random(random)}に" to "集中"
        }
        else -> {
            val subjects = listOf("AI", "カメラ", "音楽", "料理", "旅行")
            "最近は${subjects.random(random)}についてよく" to "考える"
        }
    }

    private fun surprisePrompt(random: Random): Pair<String, String> = when (random.nextInt(12)) {
        0 -> {
            val facts = listOf(
                "日本" to "東京",
                "フランス" to "パリ",
                "韓国" to "ソウル",
                "イタリア" to "ローマ",
            )
            val (country, capital) = facts.random(random)
            "${country}の首都は" to capital
        }
        1 -> {
            val a = random.nextInt(2, 12)
            val b = random.nextInt(2, 12)
            "$a + $b =" to (a + b).toString()
        }
        2 -> "赤信号では車は" to "止まる"
        3 -> "日本で一番高い山は" to "富士山"
        4 -> "1週間は全部で" to "7日"
        5 -> "水は0度付近で" to "凍る"
        6 -> "夜空で最も身近な衛星は" to "月"
        7 -> "写真を撮る機械は" to "カメラ"
        8 -> "犬の鳴き声は一般に" to "ワン"
        9 -> "火は触ると" to "熱い"
        10 -> "人間は水中では普通" to "呼吸できない"
        else -> "朝起きたらまず目を" to "開ける"
    }

    private fun makeDemoPredictions(
        random: Random,
        expected: String,
        surprise: Boolean,
    ): List<NativeEngine.TokenPrediction> {
        val pool = listOf(
            "、", "。", "東京", "大阪", "確認", "する", "から", "学習", "開く", "盛る",
            "寝た", "持つ", "星", "水", "光", "人", "AI", "カメラ", "充電", "考える",
            "7", "2", "月", "富士", "止まる", "熱い", "冷たい", "見る", "使う", "行く",
        )
        val alternatives = pool
            .filterNot { humanMatches(it, expected) }
            .shuffled(random)
            .distinct()
            .take(5)
            .toMutableList()

        val pieces = if (surprise) {
            val top = alternatives.removeAt(0)
            listOf(top, expected) + alternatives.take(4)
        } else {
            listOf(expected) + alternatives.take(5)
        }

        val probs = listOf(0.34f, 0.21f, 0.13f, 0.08f, 0.05f, 0.03f)
        return pieces.take(6).mapIndexed { index, piece ->
            val p = probs[index]
            NativeEngine.TokenPrediction(
                tokenId = 20_000 + random.nextInt(500_000) + index,
                piece = piece,
                logit = ln(p),
                probability = p,
            )
        }
    }
}

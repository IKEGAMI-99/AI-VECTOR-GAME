package com.aivectorgame.app.game

enum class GameFamily { EMBEDDING, LOGIT }

enum class GameMode(
    val family: GameFamily,
    val title: String,
    val shortTitle: String,
    val code: String,
    val instruction: String,
) {
    EMBEDDING_NEAREST(
        GameFamily.EMBEDDING,
        "Embedding：NEAREST",
        "NEAREST",
        "EMB/N",
        "ターゲットに最も近い単語を当てる",
    ),
    EMBEDDING_FARTHEST(
        GameFamily.EMBEDDING,
        "Embedding：FARTHEST",
        "FARTHEST",
        "EMB/F",
        "ターゲットから最も遠い単語を当てる",
    ),
    EMBEDDING_RANKING(
        GameFamily.EMBEDDING,
        "Embedding：RANKING",
        "RANKING",
        "EMB/R",
        "6単語を近い順に並べる",
    ),
    LOGIT_TOP_TOKEN(
        GameFamily.LOGIT,
        "Logit：TOP TOKEN",
        "TOP TOKEN",
        "LOG/T",
        "次トークン確率1位を当てる",
    ),
    LOGIT_RANKING(
        GameFamily.LOGIT,
        "Logit：RANKING",
        "RANKING",
        "LOG/R",
        "6候補を確率の高い順に並べる",
    ),
    LOGIT_LONG_FORM(
        GameFamily.LOGIT,
        "Logit：LONG FORM",
        "LONG FORM",
        "LOG/L",
        "6つの長文候補からAIが最も自然だと評価する続きを当てる",
    ),
    // Legacy mode kept for save/state compatibility. It is no longer exposed in the UI.
    LOGIT_SURPRISE(
        GameFamily.LOGIT,
        "Logit：SURPRISE",
        "SURPRISE",
        "LOG/S",
        "人間の予想とAI Top-1がズレた問題だけを解く",
    );

    val isRanking: Boolean
        get() = this == EMBEDDING_RANKING || this == LOGIT_RANKING
}

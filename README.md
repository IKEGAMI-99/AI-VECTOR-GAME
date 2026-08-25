# AI VECTOR GAME

Androidで、AIの **Embedding（意味空間）** と **Logit（次トークン予測）** をゲームとして体験するアプリです。

**Current version: v0.1.0**

## What you can play

### VECTOR SPACE — Embedding Mode

問題として1つの単語と6つの候補を表示し、Embeddingが最も近い単語を当てます。

回答後は次の情報を表示します。

- cosine similarity
- 元の高次元Embeddingの次元数
- classical MDSによる3次元への射影
- ドラッグ回転・ピンチズーム対応のインタラクティブ3D空間

LIVEモードでは **LiquidAI/LFM2.5-Embedding-350M-GGUF Q4_K_M** を llama.cpp から端末内実行します。基準語には `query:`、候補には `document:` プレフィックスを付けてEmbeddingを取得します。

> 3D表示はEmbedding本来の空間そのものではなく、高次元ベクトル間の距離をなるべく保つよう3次元へ射影した可視化です。

### NEXT TOKEN — Logit Mode

日本語の文章を表示し、6候補のうちLLMが次に出す確率が最も高い **token** を当てます。

LIVEモードでは **LiquidAI/LFM2.5-230M-GGUF Q4_K_M** を1回forwardし、llama.cppから最終位置のlogitを直接取得します。その後、語彙全体にSoftmaxをかけて確率を計算し、Top候補をゲームに使用します。

6択だけで再正規化していないため、表示される確率は実際の語彙全体に対する次トークン確率です。

## On-device models

モデル本体はAPKやGitリポジトリに同梱しません。アプリから必要なモデルだけを取得し、Androidのアプリ専用領域へ保存します。

| Mode | Model | Quantization | Approx. size |
| --- | --- | --- | ---: |
| Logit | LFM2.5-230M | Q4_K_M | ~153 MB |
| Embedding | LFM2.5-Embedding-350M | Q4_K_M | ~229 MB |

モデル未取得時やLIVE推論に失敗した場合も、明示的に `DEMO DATA` と表示したデモモードでゲームを確認できます。

## Architecture

```text
Android / Jetpack Compose
│
├─ Embedding Mode
│   └─ JNI → llama.cpp → LFM2.5 Embedding 350M
│       └─ normalized vectors
│           └─ cosine similarity
│               └─ classical MDS
│                   └─ interactive 3D Canvas
│
└─ Logit Mode
    └─ JNI → llama.cpp → LFM2.5 230M
        └─ final-position logits
            └─ full-vocabulary Softmax
                └─ Top-6 quiz + probability bars
```

Native runtime is pinned to **llama.cpp b10516** for reproducible builds.

## Build

Requirements:

- JDK 17
- Android SDK 35
- Android NDK `29.0.14206865`
- CMake `3.31.6`
- Gradle `8.9`

```bash
gradle :app:assembleDebug --stacktrace
```

CMake fetches the pinned llama.cpp source during the native build. v0.1.0 builds only `arm64-v8a` to keep the APK compact and target modern Android devices.

GitHub Actions builds every push to `main` and uploads the debug APK as the `ai-vector-game-v0.1.0-debug` artifact.

## Versioning

Semantic Versioning is used.

- `versionName`: `0.1.0`
- `versionCode`: `1`
- current version is shown on the home screen
- release changes are recorded in `CHANGELOG.md`

For future releases, update `versionName`, increment `versionCode`, update `CHANGELOG.md`, and update the CI artifact name.

## Privacy

Embedding and logit inference run on-device. Network access is used only to download selected model files.

## Credits / licenses

- llama.cpp: MIT License
- LiquidAI LFM2.5 model files are downloaded from the official Hugging Face repositories and are not redistributed in this repository.
- LFM model usage is subject to the LFM license terms.

## v0.1.0

First playable Android implementation with both game modes, real on-device inference hooks, interactive 3D Embedding visualization, model management, adaptive launcher icon, version display, and CI APK build.

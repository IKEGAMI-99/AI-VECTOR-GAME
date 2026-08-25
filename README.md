# AI VECTOR GAME

Androidで、AIの **Embedding（意味空間）** と **Logit（次トークン予測）** をゲームとして体験するアプリです。

**Current version: v0.2.0**

## What you can play

### VECTOR SPACE — Embedding Mode

1つの単語と6つの候補を表示し、Embeddingが最も近い単語を当てます。回答後は cosine similarity、高次元Embeddingの次元数、classical MDSによる3次元射影、インタラクティブ3D空間を表示します。

LIVEモードでは **LiquidAI/LFM2.5-Embedding-350M-GGUF Q4_K_M** を llama.cpp/JNI から端末内実行します。v0.2.0からLIVE専用問題セットを使い、成功した推論では `LIVE ENGINE • llama.cpp/JNI • DEMO SCORE TABLE NOT USED` を表示します。

> 3D表示はEmbedding本来の空間そのものではなく、高次元ベクトル間の距離をなるべく保つよう3次元へ射影した可視化です。

### NEXT TOKEN — Logit Mode

日本語の文章を表示し、6候補のうちLLMが次に出す確率が最も高い token を当てます。

LIVEモードでは **LiquidAI/LFM2.5-230M-GGUF Q4_K_M** を1回forwardし、llama.cppから最終位置のlogitを直接取得します。語彙全体にSoftmaxをかけた実確率からTop-6を生成します。LIVE専用プロンプトを使用し、成功時は実logitを使っていることを画面上に明示します。

## Game system

v0.2.0から両モードにゲーム進行要素を追加しました。

- Round counter
- Session score
- Consecutive Top-1 streak
- Top-1 bonus
- Top-2 / Top-3 partial score
- Correct-answer haptic feedback
- Reward card after every answer

Embeddingでは実cosine順位、Logitでは実Top-token順位がそのまま得点判定になります。

## On-device models

モデル本体はAPKやGitリポジトリに同梱しません。アプリから必要なモデルだけを取得し、Androidのアプリ専用領域へ保存します。

| Mode | Model | Quantization | Approx. size |
| --- | --- | --- | ---: |
| Logit | LFM2.5-230M | Q4_K_M | ~153 MB |
| Embedding | LFM2.5-Embedding-350M | Q4_K_M | ~229 MB |

モデル未取得時は明示的な `DEMO DATA` モードになります。LIVE推論エラー時だけ、そのラウンドをfallbackデータで継続します。

## In-app updates

ホーム画面の `APP UPDATE` カードがGitHub Releasesの最新版を確認します。新しいバージョンがあればAPKをアプリ内で取得し、Android標準のパッケージインストーラへ渡します。

初回だけAndroidの「この提供元のアプリを許可」が必要になる場合があります。

### Upgrade compatibility note

v0.1.0のGitHub Actions APKは、GitHub runnerが毎回生成するdebug keystoreで署名されていました。そのためビルドごとに署名が変わり、Androidでは同じpackageでも更新できませんでした。

v0.2.0以降は `app/keys/ai-vector-game-dev.jks` を使う固定署名です。**現在インストール済みのv0.1.0は一度だけアンインストールしてv0.2.0を入れ直す必要があります。以後は上書きアップデートできます。**

この鍵は個人開発・GitHub sideload向けのdevelopment keyです。Play Store公開用のproduction keyとしては使用しません。

## Architecture

```text
Android / Jetpack Compose
│
├─ Embedding Mode
│   └─ JNI → llama.cpp → LFM2.5 Embedding 350M
│       └─ normalized vectors → cosine similarity → classical MDS → 3D Canvas
│
├─ Logit Mode
│   └─ JNI → llama.cpp → LFM2.5 230M
│       └─ final-position logits → full-vocabulary Softmax → Top-6 quiz
│
└─ Update Manager
    └─ GitHub Releases API → signed APK → Android Package Installer
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
gradle :app:assembleDebug :app:assembleRelease --stacktrace
```

CMake fetches the pinned llama.cpp source during the native build. The APK targets `arm64-v8a`.

GitHub Actions performs Android Lint, builds debug/release APKs, verifies the APK structure/native JNI symbols/signature, uploads a build artifact, and publishes the current `versionName` as a GitHub Release asset.

## Versioning

Semantic Versioning is used.

- `versionName`: `0.2.0`
- `versionCode`: `2`
- current version is shown on the home screen
- release changes are recorded in `CHANGELOG.md`

## Privacy

Embedding and logit inference run on-device. Network access is used for model downloads, GitHub update checks, and update APK downloads.

## Credits / licenses

- llama.cpp: MIT License
- LiquidAI LFM2.5 model files are downloaded from the official Hugging Face repositories and are not redistributed in this repository.
- LFM model usage is subject to the LFM license terms.

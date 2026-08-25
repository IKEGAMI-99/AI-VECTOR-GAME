# AI VECTOR GAME

Androidで、AIの **Embedding（意味空間）** と **Logit（次トークン予測）** をゲームとして体験するアプリです。

**Current version: v0.3.0**

## Experience

v0.3.0ではUIを全面的に再設計しました。黒を基調にしたglass / telemetryスタイル、低彩度ネオン、compact HUD、独立したResultページを採用しています。

回答後に同じ問題画面の下へ結果を追加する方式を廃止し、**Question → Result → Next Question** の明確なゲームフローへ変更しました。Result画面のNEXTボタンは下部固定です。

### VECTOR — Embedding Mode

1つの単語と6つの候補を表示し、Embedding空間で最も近い単語を当てます。

LIVEモードでは **LiquidAI/LFM2.5-Embedding-350M-GGUF Q4_K_M** を llama.cpp/JNI から端末内実行します。回答後のResultページでは以下を表示します。

- cosine similarity順位
- 実Embeddingの次元数
- classical MDSによる3次元射影
- drag rotate / pinch zoom対応のインタラクティブ3D空間
- score / streak / reward

> 3D表示はEmbedding本来の高次元空間そのものではなく、ベクトル間距離をなるべく保つよう3次元へ射影した可視化です。

### TOKEN — Logit Mode

日本語文章を表示し、6候補のうちLLMが次に出す確率が最も高いtokenを当てます。

LIVEモードでは **LiquidAI/LFM2.5-230M-GGUF Q4_K_M** を1回forwardし、llama.cppから最終位置のlogitを直接取得します。語彙全体にSoftmaxをかけた実確率からTop-6を生成します。

Resultページでは以下を表示します。

- Top-1 token
- model confidence
- Top-6 probability distribution
- raw logits
- full-vocabulary Softmax probability
- score / streak / reward

## Game system

- Round counter
- Session score
- Consecutive Top-1 streak
- Streak bonus
- Top-2 / Top-3 partial score
- Correct-answer haptic feedback
- Dedicated result page after every answer
- Bottom-fixed NEXT action on result pages

Embeddingでは実cosine順位、Logitでは実Top-token順位がそのまま得点判定になります。

## LIVE verification

LIVE専用問題セットとDEMO専用問題セットは分離されています。

実モデル推論が成功したラウンドでは画面上にLIVE ENGINE / VERIFIED LIVEの表示が出ます。推論エラー時のみfallbackデータを使い、その状態も画面上に明示します。

## On-device models

モデル本体はAPKやGitリポジトリに同梱しません。アプリから必要なモデルだけを取得し、Androidのアプリ専用領域へ保存します。

| Mode | Model | Quantization | Approx. size |
| --- | --- | --- | ---: |
| Token | LFM2.5-230M | Q4_K_M | ~153 MB |
| Vector | LFM2.5-Embedding-350M | Q4_K_M | ~229 MB |

モデル未取得時は明示的なDEMOモードになります。

## In-app updates

ホーム画面のupdate moduleがGitHub Releasesの最新版を確認します。新しいバージョンがある場合は、APK取得 → Android標準package installerへ進めます。

初回だけAndroidの「この提供元のアプリを許可」が必要になる場合があります。

### Upgrade compatibility

- v0.1.0はCIごとに異なるdebug署名だったため、一度アンインストールが必要でした。
- v0.2.0から `app/keys/ai-vector-game-dev.jks` の固定development署名へ移行しました。
- **v0.2.0 → v0.3.0はアンインストール不要で直接アップデートできます。**
- package名は引き続き `com.aivectorgame.app` です。
- v0.3.0は `versionCode 3` なのでAndroid上でもv0.2.0の正規アップデートとして扱われます。
- v0.2.0でダウンロード済みのモデルも、通常の上書き更新ならそのまま保持されます。

この鍵は個人開発・GitHub sideload向けのdevelopment keyです。Play Store公開用production keyとしては使用しません。

## Architecture

```text
Android / Jetpack Compose
│
├─ VECTOR mode
│   └─ JNI → llama.cpp → LFM2.5 Embedding 350M
│       └─ normalized vectors → cosine similarity → classical MDS → 3D Canvas
│
├─ TOKEN mode
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

GitHub Actions performs Android Lint, builds debug/release APKs, verifies APK structure/native JNI symbols/signature, uploads a build artifact, and publishes the current `versionName` as a GitHub Release asset.

## Versioning

Semantic Versioning is used.

- `versionName`: `0.3.0`
- `versionCode`: `3`
- current version is shown on the home screen
- release changes are recorded in `CHANGELOG.md`

## Privacy

Embedding and logit inference run on-device. Network access is used only for model downloads, GitHub update checks, and update APK downloads.

## Credits / licenses

- llama.cpp: MIT License
- LiquidAI LFM2.5 model files are downloaded from the official Hugging Face repositories and are not redistributed in this repository.
- LFM model usage is subject to the LFM license terms.

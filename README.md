# AI VECTOR GAME

Androidで、AIの **Embedding（意味空間）** と **Logit（次トークン予測）** をゲームとして体験するアプリです。

**Current version: v0.4.0**

## v0.4.0

v0.4.0では固定問題セットを主役から外し、端末内の `QuestionFactory` が毎ラウンド問題を再構成する方式へ変更しました。LIVE時は生成された問題を実モデルで採点します。

画面もさらにcompact化し、Question画面は基本的に1画面内で回答できる構成です。Result画面も3D / Top-6情報を圧縮し、NEXTは引き続き画面下部固定です。

## Six game modes

### Embedding

- **NEAREST** — ターゲットに最も近い単語を当てる
- **FARTHEST** — ターゲットから最も遠い単語を当てる
- **RANKING** — 6単語をcosine similarityの高い順に並べる

Embedding問題は意味カテゴリ、ターゲット、関連語、無関係語を毎ラウンド再抽選します。LIVEモードでは **LiquidAI/LFM2.5-Embedding-350M-GGUF Q4_K_M** を llama.cpp/JNI から端末内実行し、実cosine similarityで正解を決定します。

RANKINGはスマホ向けにdrag操作ではなく、近いと思う順に6候補をtapして順位を組み立てます。Resultではpairwise accuracyと実順位を表示します。

Resultページでは実Embeddingをclassical MDSで3次元へ射影し、drag rotate / pinch zoom対応の3D空間として表示します。

### Logit

- **TOP TOKEN** — 次トークン確率1位を当てる
- **RANKING** — 6候補をSoftmax確率の高い順に並べる
- **SURPRISE** — 人間なら自然だと思う補完とAIのTop-1がズレた問題だけを出す

Logit問題も文章テンプレートと値を端末内で再構成します。LIVEモードでは **LiquidAI/LFM2.5-230M-GGUF Q4_K_M** を1回forwardし、最終位置logitから語彙全体Softmaxを計算してTop-6を生成します。

SURPRISEでは候補プロンプトを生成して実推論し、`humanExpected` とモデルTop-1を比較します。一致した問題は捨て、ズレた問題を見つけるまで最大12候補をscanします。これにより「SURPRISE」という名前だけの普通のTop-1問題にならないようにしています。

## Random question system

問題生成はクラウドAPIを使わず、APK内の軽量なprocedural generatorで行います。

```text
QuestionFactory
├─ Semantic clusters
│   └─ target + related words + distractors → randomized 6 choices
│
└─ Prompt recipes
    └─ randomized prompt + human expectation
        └─ LIVE: llama.cpp inference → Top-6
```

モデル未取得時も同じランダム問題構造でDEMO採点されるため、6モードすべて遊べます。

## Game system

- Random round generation
- Round counter
- Session score
- Consecutive perfect-answer streak
- Top-2 / Top-3 partial score for single-choice modes
- Pairwise ranking accuracy for RANKING modes
- Correct-answer haptic feedback
- Dedicated Result page
- Bottom-fixed RANDOM NEXT action

## Compact UI

ホームはEmbedding / Logitの2つのmodule deckにまとめ、それぞれ3モードへ直接入れる構成です。

Question画面は大きな縦スクロールを前提にせず、以下を1 viewportへ集約しています。

- compact top bar
- score HUD
- prompt / target panel
- 2列×3候補grid
- ranking tap-order slots

Result画面も大型の縦長一覧を避け、Embeddingはcompact 3D + 6順位、Logitはcompact Top-6 distributionを表示します。

## On-device models

モデル本体はAPKやGitリポジトリに同梱しません。アプリから必要なモデルだけを取得し、Androidのアプリ専用領域へ保存します。

| Module | Model | Quantization | Approx. size |
| --- | --- | --- | ---: |
| Logit | LFM2.5-230M | Q4_K_M | ~153 MB |
| Embedding | LFM2.5-Embedding-350M | Q4_K_M | ~229 MB |

## In-app updates

ホーム画面のupdate moduleがGitHub Releasesの最新版を確認します。新しいバージョンがある場合は、APK取得 → Android標準package installerへ進めます。

### Upgrade compatibility

- v0.2.0以降は `app/keys/ai-vector-game-dev.jks` の固定development署名を使用しています。
- package名は `com.aivectorgame.app` のままです。
- **v0.3.0 → v0.4.0はアンインストール不要で直接アップデート可能です。**
- v0.4.0は `versionCode 4` です。
- 通常の上書き更新なら、既に取得済みのモデルも保持されます。

この鍵は個人開発・GitHub sideload向けのdevelopment keyです。Play Store公開用production keyとしては使用しません。

## Architecture

```text
Android / Jetpack Compose
│
├─ QuestionFactory
│   ├─ randomized semantic questions
│   └─ randomized causal prompts / surprise scan
│
├─ EMBEDDING modes
│   └─ JNI → llama.cpp → LFM2.5 Embedding 350M
│       └─ cosine similarity → ranking → MDS 3D
│
├─ LOGIT modes
│   └─ JNI → llama.cpp → LFM2.5 230M
│       └─ final-position logits → full-vocabulary Softmax → Top-6
│
└─ Update Manager
    └─ GitHub Releases API → signed APK → Android Package Installer
```

Native runtime is pinned to the repository's configured llama.cpp revision for reproducible builds.

## Build

Requirements:

- JDK 17
- Android SDK 35
- Android NDK `29.0.14206865`
- CMake `3.31.6`
- Gradle `8.9`

```bash
gradle :app:lintDebug
gradle :app:assembleDebug :app:assembleRelease --stacktrace
```

The APK targets `arm64-v8a`.

GitHub Actions performs Android Lint, builds debug/release APKs, verifies APK structure/native JNI symbols/signature, uploads a build artifact, and publishes the current `versionName` as a GitHub Release asset.

## Versioning

- `versionName`: `0.4.0`
- `versionCode`: `4`
- current version is shown on the home screen
- release changes are recorded in `CHANGELOG.md`

## Privacy

Embedding and logit inference run on-device. Network access is used only for model downloads, GitHub update checks, and update APK downloads.

## Credits / licenses

- llama.cpp: MIT License
- LiquidAI LFM2.5 model files are downloaded from the official Hugging Face repositories and are not redistributed in this repository.
- LFM model usage is subject to the LFM license terms.

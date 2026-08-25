# AI VECTOR GAME

Androidで、AIの **Embedding（意味空間）** と **Logit（次トークン予測）** をゲームとして体験するアプリです。

**Current version: v0.5.1**

## v0.5.1

v0.5.1はRANKING画面の実機レイアウト修正と、回答比較の見やすさ改善が中心です。

- RANKING questionの選択済みslot / candidate / LOCK buttonをcompact化
- 日本語文字を切らずに、LOCK RANKINGをAndroid navigation areaより上へ収める
- Embedding resultの3D panelと縦spacingを再調整し、下部cosine順位を固定NEXTに隠さない
- 6項目RANKINGでは結果hero内の冗長なCORRECT / YOUR ANSWER小カードを廃止
- `AI ORDER` と `YOUR ORDER` を左右に並べ、6順位を同じ高さで比較
- 自分の各順位をpositionごとに緑✓ / 赤×で表示

## Six game modes

### Embedding

- **NEAREST** — ターゲットに最も近い単語を当てる
- **FARTHEST** — ターゲットから最も遠い単語を当てる
- **RANKING** — 6単語をcosine similarityの高い順に並べる

Embedding問題は意味カテゴリ、ターゲット、関連語、無関係語を毎ラウンド再抽選します。LIVEモードでは **LiquidAI/LFM2.5-Embedding-350M-GGUF Q4_K_M** を llama.cpp/JNI から端末内実行し、実cosine similarityで正解を決定します。

RANKINGはスマホ向けにdrag操作ではなく、近いと思う順に6候補をtapして順位を組み立てます。Resultではpairwise accuracyと `AI ORDER ↔ YOUR ORDER` を表示します。

Resultページでは実Embeddingをclassical MDSで3次元へ射影し、drag rotate / pinch zoom対応の3D空間として表示します。

### Logit

- **TOP TOKEN** — 次トークン確率1位を当てる
- **RANKING** — 6候補をSoftmax確率の高い順に並べる
- **SURPRISE** — 人間なら自然だと思う補完とAIのTop-1がズレた問題だけを出す

Logit問題も文章テンプレートと値を端末内で再構成します。LIVEモードでは **LiquidAI/LFM2.5-230M-GGUF Q4_K_M** を1回forwardし、最終位置logitから語彙全体Softmaxを計算してTop-6を生成します。

SURPRISEでは候補プロンプトを生成して実推論し、`humanExpected` とモデルTop-1を比較します。一致した問題は捨て、ズレた問題を見つけるまで最大12候補をscanします。

## Theme system

ホーム上部のtheme pillからLIGHT / DARKを切り替えられます。選択はSharedPreferencesへ保存され、次回起動時にも維持されます。

LIGHTでは背景を単純に白反転するのではなく、本文・補助文字・glass panel・stroke・accentを別paletteに切り替えます。3D Vector CloudはLIGHT UI内でも読みやすさを落とさないよう、暗いchart surface + 明るいchart accentを維持します。

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
- Side-by-side AI / player ranking comparison
- Correct-answer haptic feedback
- Dedicated Result page
- Bottom-fixed RANDOM NEXT action

## Compact UI

ホームはEmbedding / Logitの2つのmodule deckにまとめ、それぞれ3モードへ直接入れる構成です。

Question画面は大きな縦スクロールを前提にせず、compact top bar / score HUD / prompt panel / 2列×3候補grid / ranking tap-order slotsを1 viewportへ集約しています。

v0.5.1ではRANKING controlsの垂直サイズを再調整し、文字サイズを維持しながら操作ボタンが画面外へ落ちないようにしています。

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
- **v0.5.0 → v0.5.1はアンインストール不要で直接アップデート可能です。**
- v0.5.1は `versionCode 6` です。
- 通常の上書き更新なら、既に取得済みのモデルも保持されます。

この鍵は個人開発・GitHub sideload向けのdevelopment keyです。Play Store公開用production keyとしては使用しません。

## Architecture

```text
Android / Jetpack Compose
│
├─ ThemeController
│   └─ persistent LIGHT / DARK palette
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

- `versionName`: `0.5.1`
- `versionCode`: `6`
- current version is shown on the home screen
- release changes are recorded in `CHANGELOG.md`

## Privacy

Embedding and logit inference run on-device. Network access is used only for model downloads, GitHub update checks, and update APK downloads.

## Credits / licenses

- llama.cpp: MIT License
- LiquidAI LFM2.5 model files are downloaded from the official Hugging Face repositories and are not redistributed in this repository.
- LFM model usage is subject to the LFM license terms.

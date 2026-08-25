# AI VECTOR GAME

Androidで、AIの **Embedding（意味空間）** と **Logit（次トークン予測）** をゲームとして体験するアプリです。

**Current release target: v0.6.0**

## v0.6.0

v0.6.0ではLogitの `SURPRISE` をホーム画面から外し、より本質的な **LONG FORM** モードへ置き換えました。

- 6つの長文候補から、LFMが最も自然だと評価する続きを当てる
- 各候補を全文tokenizeし、先頭だけではなく最後のtokenまで実モデルで採点
- sequence probabilityの単純な積は長い候補ほど不利になるため、判定には **average token log-probability** を使用
- 結果画面で6候補の平均logP・token数・相対順位を表示
- 長文でもスクロールを増やさない2列×3候補UI
- 問題は複数シナリオと文章候補から端末内で毎ラウンド再構成

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
- **LONG FORM** — 6つの長文候補を全文採点し、モデルが最も高く評価する続きを当てる

TOP TOKEN / RANKINGでは **LiquidAI/LFM2.5-230M-GGUF Q4_K_M** を1回forwardし、最終位置logitから語彙全体Softmaxを計算します。

LONG FORMでは各候補を複数tokenへ分解し、promptに続く各tokenのconditional log probabilityを順番に取得します。候補ごとの総log probabilityも計算しますが、ゲーム順位には長さの影響を抑えるため `sum(log P(token)) / token_count` を使います。

```text
Prompt
  ↓
Candidate A → t1 → t2 → t3 → ... → avg logP
Candidate B → t1 → t2 → t3 → ... → avg logP
Candidate C → ...
  ↓
6候補を比較
  ↓
MODEL PREFERS
```

## Theme system

ホーム上部のtheme pillからLIGHT / DARKを切り替えられます。選択はSharedPreferencesへ保存され、次回起動時にも維持されます。

LIGHTでは背景を単純に白反転するのではなく、本文・補助文字・glass panel・stroke・accentを別paletteに切り替えます。3D Vector CloudはLIGHT UI内でも読みやすさを落とさないよう、暗いchart surface + 明るいchart accentを維持します。

## Random question system

問題生成はクラウドAPIを使わず、APK内のprocedural generatorで行います。

```text
QuestionFactory
├─ Semantic clusters
│   └─ target + related words + distractors
│
├─ Causal prompt recipes
│   └─ randomized prompt → LIVE Top-6
│
└─ LongFormQuestionFactory
    └─ scenario + prompt variant + 6 continuation candidates
        └─ LIVE: full multi-token sequence scoring
```

モデル未取得時も同じゲーム構造でDEMO採点されるため、6モードすべて遊べます。

## Game system

- Random round generation
- Round counter
- Session score
- Consecutive perfect-answer streak
- Top-2 / Top-3 partial score for single-choice modes
- Pairwise ranking accuracy for RANKING modes
- Side-by-side AI / player ranking comparison
- LONG FORM sequence likelihood ranking
- Correct-answer haptic feedback
- Dedicated Result page
- Bottom-fixed RANDOM NEXT action

## Compact UI

ホームはEmbedding / Logitの2つのmodule deckにまとめ、それぞれ3モードへ直接入れる構成です。

Question画面は大きな縦スクロールを前提にせず、compact top bar / score HUD / prompt panel / 2列×3候補grid / ranking tap-order slotsを1 viewportへ集約しています。

LONG FORMも長文候補を2列×3段へ収め、回答後は専用Result画面へ移動します。

## On-device models

モデル本体はAPKやGitリポジトリに同梱しません。アプリから必要なモデルだけを取得し、Androidのアプリ専用領域へ保存します。

| Module | Model | Quantization | Approx. size |
| --- | --- | --- | ---: |
| Logit / Long Form | LFM2.5-230M | Q4_K_M | ~153 MB |
| Embedding | LFM2.5-Embedding-350M | Q4_K_M | ~229 MB |

## In-app updates

ホーム画面のupdate moduleがGitHub Releasesの最新版を確認します。新しいバージョンがある場合は、APK取得 → Android標準package installerへ進めます。

### Upgrade compatibility

- v0.2.0以降は同じ固定development署名を使用しています。
- package名は `com.aivectorgame.app` のままです。
- **v0.5.1 → v0.6.0はアンインストール不要で直接アップデート可能**です。
- v0.6.0 release APKは `versionCode 7` でビルドします。
- 通常の上書き更新なら、既に取得済みのLFMモデルも保持されます。

## Architecture

```text
Android / Jetpack Compose
│
├─ ThemeController
│   └─ persistent LIGHT / DARK palette
│
├─ Question generators
│   ├─ randomized semantic questions
│   ├─ randomized causal prompts
│   └─ randomized long continuations
│
├─ EMBEDDING
│   └─ JNI → llama.cpp → LFM2.5 Embedding 350M
│       └─ cosine similarity → ranking → MDS 3D
│
├─ LOGIT
│   └─ JNI → llama.cpp → LFM2.5 230M
│       ├─ final-position logits → Softmax → Top-6
│       └─ per-token continuation logits → sequence avg logP
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

GitHub Actions performs Android Lint, builds debug/release APKs, verifies APK structure/native JNI symbols/signature, uploads a build artifact, and publishes the release APK.

## Versioning

- release target: `0.6.0`
- release `versionCode`: `7`
- current version is shown on the home screen
- release changes are recorded in `CHANGELOG.md`

## Privacy

Embedding, next-token inference, and LONG FORM sequence scoring run on-device. Network access is used only for model downloads, GitHub update checks, and update APK downloads.

## Credits / licenses

- llama.cpp: MIT License
- LiquidAI LFM2.5 model files are downloaded from the official Hugging Face repositories and are not redistributed in this repository.
- LFM model usage is subject to the LFM license terms.

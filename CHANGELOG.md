# Changelog

All notable changes to AI VECTOR GAME are recorded here.

The project follows Semantic Versioning.

## [0.5.0] - 2026-08-25

### Added

- Persistent LIGHT / DARK theme switch on the home screen.
- Dedicated light palette for backgrounds, panels, text hierarchy, borders, and accent colors.
- Theme-aware Android status/navigation bar icon appearance.
- Position-by-position comparison for Embedding and Logit RANKING result screens.

### Changed

- Increased global typography scale and raised key title/HUD/candidate/result text sizes.
- Enlarged RANKING selected-order slots from 46dp to 62dp and added explicit line height so Japanese glyphs are no longer clipped.
- Single-answer result cards now show large `✓ CORRECT` or `✕ WRONG` verdicts and color the player's answer independently.
- Ranking results now distinguish `PERFECT ORDER` from `PARTIAL MATCH` and keep pairwise accuracy visible.
- Vector Cloud labels and node accents were strengthened for better readability, including when the surrounding UI is in LIGHT mode.
- Filled action-button foreground colors now adapt to the active theme.

### Upgrade

- Version increased to `0.5.0` (`versionCode 5`).
- Package name remains `com.aivectorgame.app`.
- Uses the same stable development signature as v0.2.0-v0.4.0, so v0.4.0 can update directly without uninstalling or deleting downloaded models.
- Pre-release feature-branch CI passed Android Lint, debug/release APK builds, JNI verification, APK structure checks, and signature verification before release to main.

## [0.4.0] - 2026-08-25

### Added

- Six explicit game modes: Embedding NEAREST / FARTHEST / RANKING and Logit TOP TOKEN / RANKING / SURPRISE.
- `QuestionFactory` for randomized on-device question generation instead of repeating a fixed sequence.
- Procedural semantic questions assembled from target clusters, related words, and cross-cluster distractors.
- Procedural causal prompts with randomized values and sentence recipes.
- SURPRISE scanner that performs real model inference and rejects prompts where the human expectation matches the model Top-1, keeping mismatches for play.
- Tap-order ranking editor optimized for one-handed phone use.
- Pairwise ranking accuracy scoring for both ranking modes.

### Changed

- Home screen now exposes all six modes directly inside compact Embedding and Logit module decks.
- Question screens were compressed into a mostly single-viewport layout with 2×3 candidate grids.
- Result pages use a compact result summary, smaller 3D projection, compact cosine rank strip, and compact Top-6 distribution.
- Result NEXT action now generates a new random round.

### Upgrade

- Version increased to `0.4.0` (`versionCode 4`).
- Package name remains `com.aivectorgame.app`.
- Uses the same stable development signature as v0.2.0/v0.3.0, so v0.3.0 can update directly without uninstalling or deleting downloaded models.
- Pre-release feature-branch CI passed Android Lint, debug/release APK builds, JNI verification, APK structure checks, and signature verification before merging to main.

## [0.3.0] - 2026-08-25

### Changed

- Rebuilt the visual system around a darker glass/telemetry aesthetic with atmospheric gradients, low-saturation neon accents, compact HUD typography, and more deliberate hierarchy.
- Redesigned the home screen, update module, model cards, quiz screens, result screens, score HUD, answer tiles, probability visualization, and 3D vector view.
- Embedding and Logit modes now use dedicated question and result pages. Selecting an answer immediately leaves the question page and opens a result page instead of appending results below the choices.
- Result pages keep the NEXT action fixed at the bottom so the player does not need to scroll to continue.
- Embedding result view emphasizes the interactive MDS projection and cosine ranking.
- Logit result view emphasizes model confidence, Top-1 token, full Top-6 distribution, raw logits, and full-vocabulary Softmax probability.

### Upgrade

- Version increased to `0.3.0` (`versionCode 3`).
- Package name remains `com.aivectorgame.app`.
- v0.3.0 uses the same stable development signing key introduced in v0.2.0, so **v0.2.0 can be updated directly without uninstalling the app or deleting downloaded models**.
- The v0.2.0 in-app GitHub updater can discover and install the v0.3.0 GitHub Release once CI publishes it.

## [0.2.0] - 2026-08-25

### Added

- Separate LIVE and DEMO question banks so model-backed sessions no longer begin with the same questions as demo mode.
- Visible LIVE proof badges after successful native inference (`llama.cpp/JNI`, demo table not used).
- Session score, round counter, streak bonuses, Top-2/Top-3 partial rewards, and stronger success feedback.
- Haptic reward on Top-1 / closest-vector hits.
- In-app GitHub update checker, APK downloader, and Android package-installer handoff.
- GitHub Release publishing from CI so the in-app updater has a stable release source.
- Stable sideload signing key for update-compatible APKs from v0.2.0 onward.

### Fixed

- Content now respects Android safe drawing insets and no longer sits under the status bar / display cutout.
- Version increased to `0.2.0` (`versionCode 2`).

### Important

- v0.1.0 CI APKs were signed by GitHub runner-generated debug keys. Because those signatures changed between builds, Android reported an app conflict. v0.2.0 introduces a stable development signature. The currently installed v0.1.0 must be uninstalled once; v0.2.0 and later builds can update each other normally.
- The committed signing key is intentionally a development/sideload key, not a Play Store production key.

## [0.1.0] - 2026-08-25

### Added

- First playable Android build.
- Embedding Mode with six-choice semantic similarity game.
- LIVE inference using LFM2.5-Embedding-350M Q4_K_M through llama.cpp/JNI.
- Cosine similarity scoring from real normalized embeddings.
- Classical MDS projection from high-dimensional embeddings into interactive 3D.
- Drag-to-rotate and pinch-to-zoom 3D visualization.
- Logit Mode with six-choice next-token prediction game.
- LIVE inference using LFM2.5-230M Q4_K_M through llama.cpp/JNI.
- Full-vocabulary Softmax probabilities and raw logit display.
- Clearly labeled demo data fallback when models are not installed or LIVE inference fails.
- In-app model download and app-private model storage.
- Adaptive launcher icon based on a connected vector-space motif.
- Visible app version on the home screen.
- GitHub Actions debug APK build.

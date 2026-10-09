# Verified progress

2026-10-09 (Asia/Calcutta). Work in progress; the complete V6 goal is not done.

- Requested upstream cloned into the workspace. Repository was empty.
- Connected device: I2501, Android 16 API 36, arm64-v8a, serial 10BFAT1UT2000XP.
- JDK 21.0.12.1, Gradle 9.6.0, cached AGP 9.4.1, platform API37 and build-tools36.
- `:sdk:compileDebugKotlin`, `:sdk:testDebugUnitTest`, and `assembleDebug` pass.
- Capture uses Android's real `AudioRecord`, `AudioTrack`, SensorManager, and VibratorManager APIs. On API31+ it requests the identical finite waveform on every exposed vibrator ID through `CombinedVibration`; independent motor-axis control remains unavailable.
- API research and actual Laya/Kaggle inspection are underway.
- Root/sdk/apk AGENTS preserve offline, CPU, honest measurement and app exclusions.
2026-10-09 device validation:

- APK install succeeded on `10BFAT1UT2000XP`, and the first connected suite completed the real `HardwareCaptureTest` successfully. It verified non-silent microphone PCM, accelerometer/gyroscope events, cancellation cleanup, acoustic and haptic request markers, DSP evidence and private capture persistence. It did not prove physical speaker transducer activation or independent motor-axis behavior.
- The same suite's Compose showcase test failed because no Compose hierarchy was visible while the device activity was stopped/off-screen. A later targeted rerun hit a transient ddmlib property timeout and classified the connected API as unknown before executing tests. Do not count either as product success.
- Host training safety checks pass: `sdk/training/test_training.py` ran 10 tests successfully. These are schema/safety tests, not physical model evidence.
External blocker now actionable: user selected a rigid closable case for the first labeled physical experiment. No capture files or labels exist yet. Collection must follow `sdk/docs/experiments.md`: enrollment A–D, five unchanged repositioned repeats per position, controlled removed-item repeats, restored unchanged repeats, and preserved invalid/noisy cases. Training remains refused until those real files and labels are copied into the private capture root.
2026-10-09 initial scan UX update (superseded by the four-position update below): Scan opened anonymously and recorded two sides without comparison. It reported `INCONCLUSIVE`; no validated recognition model existed.
Earlier 2026-10-09 full verification with device awake: `:apk:connectedDebugAndroidTest` passed 3/3 tests (real hardware capture/cancellation, anonymous scan entry, menu/capture navigation); `:sdk:connectedDebugAndroidTest` passed 2/2 storage/vault tests. `:sdk:lintDebug`, `:apk:lintDebug`, and `testDebugUnitTest` passed. The instrumentation rule grants microphone permission for the real capture test. These checks prove acquisition/UI entry only—not object identification or trained-model behavior.

2026-10-09 public-data training:

- Downloaded the CC BY 4.0 UCI 846 dataset into ignored training artifacts and
  trained a CPU-only three-class fan-configuration baseline in
  `sdk/training/artifacts/venv` using PyTorch 2.14.1+cpu. Held-out speed settings
  were separated from train/validation. Test: 414 windows, 67.63% accuracy,
  65.52% macro-F1, 33.33% chance. These are dataset-specific window metrics from
  one fan; not phone or object-level evidence.
- The generated checkpoint is a research candidate only; it does not implement
  V6 VibraNet and is not integrated. User instruction: use licensed pretrained
  models wherever possible; test MOMENT-1-small before promoting this baseline.
- Public paired KAIST rotating-machine data is a possible multimodal pretraining
  source. The user-selected rigid-case experiment still has no real captures.

2026-10-09 four-position showcase update:

- Enrollment and scan now guide real captures at registered A–D points with a
  top-down placement diagram. Enrollment saves a Keystore-encrypted research
  baseline only after all four approved positions; it remains explicitly
  uncalibrated. Scan remains anonymous and `INCONCLUSIVE` because no validated
  identity or integrity comparison is available.
- Verified `cmd.exe /c gradlew.bat :apk:assembleDebug
  :apk:assembleDebugAndroidTest :apk:lintDebug :sdk:testDebugUnitTest` passed.
- With `$env:ANDROID_SERIAL='10BFAT1UT2000XP'`,
  `cmd.exe /c gradlew.bat :apk:connectedDebugAndroidTest` passed 3/3 on the
  verified device (one real hardware capture/cancellation test and two Compose
  tests). The
  Gradle task removed `dev.capybara.app` after the run; `adb install -r` restored
  it and the launcher succeeded. Existing app-private captures/references may
  have been removed with the package. Do not rerun this connected task until a
  path that preserves the installed app and its data is established.
- The UI tests checked scan entry and the A guide only; they did not complete
  four physical positions or create a new baseline.
- The training venv's Python cannot start; its base path is the Store Python
  3.13.14. The bundled 3.12.14 runtime launches but is not a substitute for the
  venv's ML packages. MOMENT-1-small remains unevaluated and uncached.
- No user physical-case captures, trained Capybara specialists, calibrated
  decisions or held-out physical evaluation exist.

2026-10-09 profile API slice:

- Added the SDK's typed profile catalog for the one physically specified
  `rigid_case` workflow and positions A–D. Physical captures reject unsupported
  profiles and positions before opening hardware; the Compose flow and
  encrypted-baseline validator now read the same catalog.
- The profile is an acquisition contract only. No calibration, model readiness
  or integrity result is inferred from it.
- All 3 `ProfileEngineTest` JVM contract tests passed. The checked-in command
  `cmd.exe /c gradlew.bat :sdk:testDebugUnitTest :sdk:lintDebug
  :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebug` passed, including
  both module lint tasks and APK assembly. APK unit tests have no source set.
  No connected tests were run because the earlier connected task removed the
  app package and may have cleared private data.

2026-10-09 fixed probe planning slice:

- Added `ProbeBrain` as a fixed, profile-checked policy. It always requests the
  acoustic chirp for `rigid_case` and requests haptics only when Android reports
  a vibrator. The selected plan and its fixed-selection basis are stored with
  capture metadata; this is not signal-adaptive planning.
- All 2 `ProbeBrainTest` and 3 `ProfileEngineTest` tests pass in the latest SDK
  unit-test results; full host SDK/APK unit, lint and APK assembly command
  completed successfully. No device suite was run.
- Rechecked training inputs: neither `sdk/captures/` nor a prepared
  `sdk/training/artifacts/dataset-v1/` exists. No eligible physical records are
  available, so no Laya fine-tune was launched. The public UCI fan labels remain
  unrelated to the physical integrity task.
- A read-only check of `dev.capybara.app` on serial `10BFAT1UT2000XP` confirmed
  the package is installed but `no_backup/capybara/captures` does not exist.
- Push remains blocked: the configured GitHub account name matches the remote
  owner, but `gh auth status` reports its token is invalid and GitHub API access
  failed. No push was attempted without verifiable authentication.
- `sdk/docs/architecture.md` now maps each required architecture stage to its
  implementation state and specific evidence/deployment gate. The full V6
  definition of done remains unmet.

2026-10-09 pretrained-model continuation (supersedes earlier environment notes):

- The isolated `sdk/training/artifacts/venv` now runs bundled Python 3.12.14,
  Torch 2.14.1+cpu, NumPy 2.2.6, Transformers 4.57.1, MOMENT 0.1.5, Laya 0.4.1,
  ONNX 1.23.2 and ONNX Runtime 1.31.0. CUDA is unavailable. This is an observed
  environment, not a complete dependency lock.
- Frozen MOMENT-1-small embeddings plus a linear probe reached 55.56% accuracy,
  52.97% macro-F1 and 0.9220 cross-entropy on 414 UCI fan windows held out by
  speed. It underperformed the scratch CNN on the same split (67.63%, 65.52%,
  0.8002) and is rejected for this transfer. Neither result is Capybara evidence.
- The user's selected Laya path is prepared: pinned 0.4.1 source and official
  English base checkpoint loaded on CPU; weight SHA256 is recorded in
  `sdk/training/README.md`. Upstream ONNX export succeeded and ONNX Runtime CPU
  matched eager CPU on one generic email example. The external FP32 weights are
  1,685,258,240 bytes. Base temperature clamping makes confidence uncalibrated;
  this is not a physical integrity model and was not integrated into Android.
- No case captures or annotations exist. All five physical specialists,
  fine-tuned Capybara-Laya, calibration, TrustGate, Android parity, measured
  phone resource use and held-out physical evaluation remain incomplete.
- `cmd.exe /c gradlew.bat :sdk:testDebugUnitTest :sdk:lintDebug
  :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebug` passed on the host.
  The first restricted run could not open Gradle's wrapper lock under the user
  cache; rerunning with that cache available succeeded. No connected-device
  checks were run; preserve the installed app because connected instrumentation
  previously removed `dev.capybara.app` and may have cleared private data.
2026-10-09 generic showcase flow:

- Added a separate `showcase_object` profile with two sides so the judge-facing
  app accepts any stable box or object without pretending the rigid-case A–D
  protocol is universal.
- Enrollment now asks for an object name, captures and reviews side 1, asks the
  user to scan the other side, captures and reviews side 2, then merges the two
  saved capture hashes into a provenance fingerprint and displays an animated
  object-added result.
- Scan uses the same two-side capture/review flow without a name prompt and
  reports `INCONCLUSIVE` because comparison specialists and calibration remain
  unavailable. The app requests stereo audio and all exposed vibrator IDs;
  physical speaker/motor onset remains unmeasured.
- Host SDK tests, lint, APK unit-test task and debug assembly passed after this
  change. Connected UI validation remains pending because the prior connected
  task removed the package and may clear private data.

2026-10-10 generic baseline persistence and public-data audit:

- `SignatureVault` now validates the registered position count for each profile:
  four A–D captures for `rigid_case`, two sides for `showcase_object`. Both are
  AES-GCM encrypted `RESEARCH_BASELINE_UNCALIBRATED` references.
- The showcase home list now derives only from saved references. Incomplete or
  rejected raw captures remain private research data and cannot appear as
  enrolled objects. The app shows “Object added” only after encrypted reference
  creation succeeds; failure is explicit.
- Added an Android storage regression test for a two-side reference, its stored
  two captures/status, and rejection of incomplete or duplicate-side input.
  SDK/APK host unit tests, both lint tasks, APK assembly, and both
  instrumentation APK compile tasks passed. The storage instrumentation was
  compiled but not run because the historical connected task removed the app
  package and may clear its data.
- Installed and launched the rebuilt app on `10BFAT1UT2000XP`; the home UI
  rendered. A read-only `run-as dev.capybara.app` check found no
  `no_backup/capybara` capture directory. The home list displayed a saved
  research reference, but its underlying captures, labels and provenance were
  not inspected or verified. Preserve it; it is not accepted as training
  evidence, and no data was copied or exported.
- Audited the licensed KAIST paired industrial dataset, CC BY 4.0 smartphone
  variance-change data, and CC0 ORION-AE bolt-loosening data. Their labels or
  sensors do not match phone-to-box integrity. Kaggle CLI 2.2.4 auth worked;
  quota showed 0/30 GPU and 0/20 TPU hours used. The exact public-data DOI
  search found no Kaggle mirror. No remote job or public-data model was launched.
- Code commit `3c57a15` was pushed to `codex/v6-handoff`. Full V6 training,
  calibration, and physical evaluation remain blocked on actual labeled
  iQOO captures and multiple independent objects.

## Next acceptance milestones

1. The user collects and labels real rigid-case A–D enrollment, unchanged
   repositioned repeats, removed-item changes, restored repeats and invalid
   captures using `sdk/docs/experiments.md`. These physical labels cannot be
   supplied by code or public fan data.
2. Develop and evaluate EchoNet, VibraNet, PhysFusion, StateNet and SpatialNet
   only from modality-matched real evidence with object/session/run-separated
   splits. Continue only if repeated-state separability supports the task.
3. Fine-tune the pinned Laya lineage on actual versioned specialist evidence,
   calibrate on separate cases, and evaluate untouched physical test objects.
4. Reduce or qualify the 1.69 GB FP32 export only with held-out accuracy and
   calibration evidence; then implement Android CPU tokenizer/runtime parity,
   TrustGate, memory/latency measurement and offline full-system checks.
5. Resolve preservation-safe connected test execution before any future device
   suite; host build/lint/unit checks remain available.

No task-specific specialist weights, labeled physical-integrity corpus,
calibration, or integrity accuracy currently exists. Keep unavailable decisions
as `INCONCLUSIVE`; do not substitute the UCI classifier, raw base Laya output or
an unvalidated threshold.

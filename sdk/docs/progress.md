# Verified progress

2026-10-10 (Asia/Calcutta). Work in progress; the complete V6 goal is not done.

- Requested upstream cloned into the workspace. Repository was empty.
- Connected device: iQOO I2501, Android 16 API 36, arm64-v8a, serial `10BFBH1BH0001EY`.
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

2026-10-10 enrollment terminology correction:

- Replaced the enrollment prompt “Create a physical fingerprint” with “Create a
  research reference”; the stored digest is provenance, not identity.
- `cmd.exe /c gradlew.bat :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebug`
  completed successfully. `:apk:testDebugUnitTest` was `NO-SOURCE`; APK compile
  and lint passed, but no device enrollment/capture flow was run.
- Commit `9e6d26e` was pushed to `codex/v6-handoff`.

2026-10-10 trailing-spectrum DSP correction:

- A synthetic regression first failed when a nonzero PCM sample at the final
  index contributed zero band energy. The engine now includes one zero-padded
  tail window only when it contains samples beyond complete windows, normalizes
  using observed Hann energy, and weights that window by its observed fraction.
- Focused `PhysicsEngineTest.unrepresentedTrailingSampleContributesToSpectrum`
  passed. Full host/lint/APK build:
  ```powershell
  cmd.exe /c gradlew.bat :sdk:testDebugUnitTest :sdk:lintDebug :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebug
  ```
  passed; `:apk:testDebugUnitTest` was `NO-SOURCE`.
- This is a synthetic DSP boundary check, not physical transfer-response
  calibration or integrity evidence. Commit `a476743` was pushed to
  `codex/v6-handoff`.

2026-10-10 showcase UX and motion update:

- Renamed the home action to `Scan`. Added a small startup logo scale, a pulse
  visible only while a real SDK capture/analysis operation is busy, and an
  animated result mark: check only after the encrypted research baseline saves;
  `?` for the existing `INCONCLUSIVE` scan; dash when the reference is not saved.
  The capture digest is explicitly labeled provenance, not a physical identity
  fingerprint.
- `cmd.exe /c gradlew.bat :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebugAndroidTest :apk:assembleDebug`
  passed. APK unit tests were `NO-SOURCE`; instrumentation compiled, not run.
- Installed with `adb -s 10BFAT1UT2000XP install -r ...`, launched the actual
  app, and visually verified the home and scan-placement screens. No physical
  capture or saved-reference inspection/export was performed.
- Commit `a6cf03d` was pushed to `codex/v6-handoff`.

## Public object-audio license audit (2026-10-10)

- Corrected `research.md`: ObjectFolder-Real's data license is unverified;
  ObjectFolder 2.0's CC BY 4.0 statement is for a separate release.
- The closest fill-state lead, FillImpact, is described in its paper as over
  5,000 real-container recordings with fill labels, but no dataset URL or data
  license is provided. SonicSense has relevant inventory-state work and a
  contact-microphone modality, but its linked recordings have no separate data
  license. The YCB-impact OSF record has `node_license: null`. RealImpact's
  repository license covers code, not a documented data license.
- VibeCheck provides an active-acoustic method precedent (piezo speaker and
  contact microphone coupled through an object), but its hardware differs from
  the phone's air-coupled speaker/microphone path. Paper/project sources do not
  release a dataset, and its code repository does not provide dataset terms;
  no data or model assets were reused.
- No data was downloaded and no model was trained. None of these sources
  satisfies both verified data rights and Capybara's active phone-probe task;
  model training and physical decision validation remain blocked.

## Additional public-data candidates (2026-10-10)

- CORSMAL has CC BY-NC 4.0 content/fill labels and object-separated splits, but
  uses an eight-microphone array while people pour or shake containers. Its
  published ACC TensorFlow weights are separately CC BY 4.0 but were trained on
  that dataset; neither the model nor the data were downloaded or evaluated.
- EPFL's CC BY 4.0 audio/tactile dataset has 300 robot trials across five
  container contents and two motions, but one plastic container, a hand-mounted
  microphone, and condition-dependent collection dates; it has no object-held-out
  evaluation or active phone probe.
- The coin-tap paper reports 101,070 48 kHz samples from nine bonded-plate
  specimens across normal and six defect classes. Its GitHub README provides a
  Baidu link without dataset terms, and the paper uses sample-random folds rather
  than specimen-held-out evaluation; it was not downloaded.
- A surfaced TU Wien record was verified as manufacturing work-step audio, not
  the claimed composite tap-test dataset, and was discarded.
- LMSD is CC BY 4.0 but contains averaged FRFs from one CFRP plate, measured
  with an impact hammer and seven accelerometers; its added-mass cases are
  damage surrogates, not phone audio/IMU captures or object-held-out data.
- The CC0 4TU CFRP acoustic-emission dataset has contact-coupled 100–900 kHz
  sensors sampling at 2 MHz during compression tests, outside Capybara's phone
  path.
- No candidate satisfies Capybara's active phone-probe and physical-integrity
  requirements. No data or model was downloaded or trained; task-specific
  training remains blocked on a suitable licensed corpus or labeled iQOO captures.

## Authorized reference inspection attempt (2026-10-10)

- The user authorized read-only inspection of the saved Capybara reference and
  selected the rigid-case A–D pilot.
- `adb -s 10BFAT1UT2000XP shell run-as dev.capybara.app ls -la no_backup/capybara/references`
  returned `device '10BFAT1UT2000XP' not found`. `adb devices -l` listed only
  `emulator-5554`; no command was run on the emulator. The reference remains
  uninspected and unchanged.
- The earlier handset was disconnected during that reference-inspection attempt; the new phone connected later. Its fresh app installation reports zero captures and zero baselines. The earlier reference remains uninspected and was not copied.
- The physical pilot remains unrun because object setup, repositioning/removal and labeling require the user.

## New handset hardware inventory and app smoke (2026-10-10)

- Read-only ADB identity on `10BFBH1BH0001EY` confirms iQOO/I2501, Android 16/API 36 and `arm64-v8a`.
- `cmd.exe /c gradlew.bat :sdk:testDebugUnitTest :sdk:lintDebug :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebug` passed; `:apk:testDebugUnitTest` was `NO-SOURCE`.
- `adb install -r` and package-scoped launch succeeded. The home, status and technical-details views rendered. Status showed microphone permission needed, zero captures and zero baselines.
- Android reports 48 kHz output, a stereo built-in speaker, two exposed vibrator IDs and nominal 479.85 Hz accelerometer/gyroscope maxima. Both vibrator IDs report amplitude control; physical speaker/motor onset and achieved sensor rates remain unmeasured.

## Showcase UI and acquisition smoke (2026-10-10)

- `2287498` fixes enrollment text-entry dismissal/keyboard insets, keeps the
  Scan capture action visible on the landscape device, and places review
  decisions before waveform/evidence details. `:sdk:testDebugUnitTest`,
  SDK/APK lint, APK assembly and `:apk:testDebugUnitTest` passed; the APK test
  task is `NO-SOURCE`.
- The final APK was installed on `10BFBH1BH0001EY`; the Scan placement screen
  visibly showed the full `Capture side 1` action and side instructions. The
  measured-response review exposed approve/retry/reject; an unlabelled smoke
  acquisition was rejected without approval.
- The post-rejection Status view reported microphone permission granted,
  `Captures: 0`, `Baselines: 0`, and `Current operation: Ready`. No saved
  reference or accepted measurement was created. Object placement/state was
  not controlled or labeled; this is not rigid-case pilot or integrity evidence.
- Physical speaker/motor onset and achieved sensor rates remain unverified.
  The user-prepared rigid-case A–D pilot remains pending.
- Commit `2287498` was pushed to `codex/v6-handoff`.

## Evidence-to-Laya integration slice (2026-10-10)

- Added `Capybara.evaluate(referenceId, capturePaths)`. It authenticates the
  Keystore-protected research reference, validates complete registered positions,
  rejects reused capture IDs or sessions, reloads stored PCM/IMU, and verifies
  stored metadata plus SHA-256 hashes before feature derivation.
- The Evidence Engine now emits per-position `physics-v1` reference/current
  descriptors and deltas without object/session/condition/annotation labels.
  This is training-state-shaped evidence, not a tokenizer request or model
  output. Non-finite values remain JSON null.
- TrustGate is fail-closed: this path always returns `INCONCLUSIVE` with explicit
  missing-specialist, missing-Laya, uncalibrated-reference and calibration
  blockers. No MATCH/CHANGED threshold or confidence was added.
- Added Android regression coverage for stored feature comparison, reference
  metadata tampering and rejecting captures labeled as a different object.
- `Capybara.evaluate` requires current capture object labels to match the
  authenticated reference label. This prevents accidental cross-reference
  comparisons; it does not prove physical identity. A host unit test covers the
  shared label guard.
- Tracing the app caller revealed Scan assigned a fresh random candidate object
  label after reference selection, which the SDK correctly rejects. Scan now
  captures under the selected reference's label while retaining its new session;
  the app test checks that reference selection is required before capture.
- Host-only verification passed:
  `cmd.exe /c gradlew.bat :sdk:testDebugUnitTest :sdk:lintDebug :sdk:assembleDebugAndroidTest :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebug :apk:assembleDebugAndroidTest`.
  SDK unit regression, SDK/APK lint, SDK instrumentation compilation and APK
  assembly passed. Instrumentation was not executed; no iQOO tests were run.
- `sdk/captures/` does not exist. No real labeled captures, trained specialist
  evidence or calibrated task checkpoint are available, so host integration
  cannot safely call Laya or claim a decision. Upstream `laya-java` also states
  Android is not implemented; the only exported graph is an untrained 1.69 GB
  FP32 base artifact. See `sdk/docs/laya-integration.md`.
- `adb devices -l` showed serial `5XVSGE8HJBIF55T8` (CPH2781IN) and
  `emulator-5554`; neither is the authorized iQOO serial
  `10BFBH1BH0001EY`. No device or emulator test was run.
- The user instructed that no iQOO testing be done for now. Respect this pause
  even if the authorized phone reconnects; do not test it unless the user changes
  the instruction.

## Public-data fit review (2026-10-10)

- Reviewed primary records for impact-echo concrete defects, an iPhone motor-
  vibration dataset, multimodal bare-finger texture sensing and VibTac-12.
- The first three publish CC BY 4.0 records but do not jointly match Capybara's
  active phone-speaker/haptic contact, rigid-object states and object-separated
  generalization. VibTac-12 records texture classes using a probe and does not
  state reuse terms; its access requires an IEEE account.
- No dataset was downloaded or used for training. No candidate supports a
  Capybara integrity claim; continue to require real labeled phone captures.
- Sources, exact limitations and decision: `research.md`.

## Partial-audio baseline rejection (2026-10-10)

- Code-path review found that `CaptureEngine` can return a short PCM array after
  its acquisition deadline and the app can expose that measurement for review.
  `SignatureVault` previously checked complete profile positions but not complete
  audio counts, so a user-approved partial capture could be persisted as a
  research reference.
- `SignatureVault` now requires each stored PCM count to match both the requested
  and reported audio sample counts. Partial captures may remain raw research
  records but cannot become enrolled references.
- The pure-JVM sample-count regression and SDK unit/lint tasks passed;
  `SignatureVaultTest`'s persistence regression compiled. Instrumentation and
  iQOO tests were not run because the user paused device testing.
- Commit `f0054d6` was pushed to `codex/v6-handoff`.
- The updated SDK also passed `:sdk:assembleDebug :apk:assembleDebug :apk:lintDebug`.

## Training evidence leakage guard (2026-10-10)

- Audit found `check_values` rejected only exact normalized forbidden keys and
  accepted strings/booleans under innocuous feature names. This permitted nested
  annotation or identity fields such as `expectedLabel`, `conditionName`, and
  `objectIDHash` to evade checks or values like `"quality": "CHANGED"` to enter.
- The validator now rejects common annotation/identity key variants and accepts
  only finite numbers or `null` as evidence values. Regression coverage preserves
  real numeric features and null missing values; this lexical/value-type gate
  cannot prove semantic independence.
- `sdk/training/test_training.py` passed all 10 tests, including the capture-pair
  validation path. No physical data was used.

## Training capture format and digest verification (2026-10-10)

- The preparer hashed PCM/IMU files for its manifest but did not compare them
  with `audioSha256` and `imuSha256` embedded by `CaptureStore`. It also ignored
  `pcmEncoding`, allowing bytes from a same-sized unsupported encoding to pass.
- `prepare.capture` now requires the declared signed PCM16 little-endian mono
  format and verifies both embedded digests. Regressions mutate one byte in each
  artifact and change the encoding; all are rejected. All 10 training tests pass.
- Embedded hashes detect file/metadata inconsistency, not a malicious actor able
  to rewrite both the artifact and its unkeyed metadata digest.
- Digest enforcement was pushed as `c25035d`; format validation is in this slice.
- Format validation was pushed as `09a73c4`.

## Training incomplete-audio label guard (2026-10-10)

- Dataset preparation now applies the same stored/expected/reported sample-count
  invariant as `SignatureVault`: incomplete reference captures are rejected, and
  an incomplete current capture is admitted only with an `INCONCLUSIVE` label.
- All 10 training tests pass, including complete, partial-current, and partial-
  reference schema fixtures. No physical data was used.

## Specialist artifact hash admission (2026-10-10)

- Laya's training gate previously checked only SHA256 string syntax, not the
  specialist files those hashes claimed to identify.
- Both train and export now require a schema-1 local artifact manifest; the
  harness hashes files and requires evidence `modelVersions` to match those
  bytes. Artifact mutation is covered by a schema-fixture regression.
- `sdk/training/test_training.py` passes all 11 tests. This binds evidence to
  artifact bytes, not to valid training provenance; no model was trained.

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

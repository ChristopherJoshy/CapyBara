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
2026-10-09 scan UX update: Scan opens directly at placement guidance, creates an anonymous candidate label, records two approved sides without asking for a name, and states no identity comparison occurred. Saved enrollment labels are listed only as available data; no validated recognition model exists, so the result remains `INCONCLUSIVE`. The focused no-name Compose test and complete `ShowcaseTest` class now pass on the awake iQOO 15.
Final 2026-10-09 verification with device awake: `:apk:connectedDebugAndroidTest` passed 3/3 tests (real hardware capture/cancellation, anonymous scan entry, menu/capture navigation); `:sdk:connectedDebugAndroidTest` passed 2/2 storage/vault tests. `:sdk:lintDebug`, `:apk:lintDebug`, and `testDebugUnitTest` passed. The instrumentation rule grants microphone permission for the real capture test. These checks prove acquisition/UI entry only—not object identification or trained-model behavior.

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

## Next acceptance milestones

1. Build library + Compose acquisition app and verify real capture on device.
2. Preserve real PCM, IMU, timestamps, routing and labels in app-private storage.
3. Test deterministic DSP and real-device resource lifecycle.
4. Collect object/session separated physical data, nuisance cases and reference
   calibration. User-assisted experiments required; currently unavailable.
5. Train EchoNet, VibraNet, PhysFusion, StateNet, SpatialNet and actual Laya.
6. Validate CPU export parity, calibration, TrustGate and offline end-to-end result.

No task-specific specialist weights, labeled physical-integrity corpus,
calibration, or integrity accuracy currently exists. The ignored UCI baseline
does not satisfy any V6 model milestone. Do not replace missing intelligence with
an unvalidated threshold classifier.

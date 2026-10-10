# Capybara V6

*Give the physical world a memory.*

Capybara is an offline Android physical-measurement SDK and demonstration app. It uses the phone's real audio output, microphone, vibration actuators, accelerometer and gyroscope to record how a contacted object responds. Raw measurements remain private on the device and retain timing, routing, excitation-request and quality provenance.

## Current verified state

- Connected test device: iQOO I2501, Android 16 / API 36, `arm64-v8a`, serial `10BFBH1BH0001EY` (build fingerprint `iQOO/I2501i/I2501`).
- New-phone capability inventory: Android reports 48 kHz output / 144 frames and unprocessed-source support; built-in speaker type 2 advertises 48 kHz stereo and built-in microphone type 15 advertises 48 kHz. `VibratorManager` exposes IDs 0 and 1, both present with amplitude control; resonant frequency is `NaN` and envelope effects are unsupported. These are capability reports, not achieved-rate or physical-response measurements.
- Real `AudioRecord` microphone capture, stereo `AudioTrack` excitation request, IMU capture, finite haptic excitation, cancellation cleanup, private atomic capture storage and CPU DSP are implemented.
- The device exposes two vibrator IDs. On API 31+, Capybara requests the same finite waveform on all exposed IDs through `CombinedVibration`; Android does not expose independent-axis semantics for them.
- Encrypted immutable research baselines are profile-scoped and use Android
  Keystore AES-GCM: four positions for the controlled `rigid_case` protocol and
  two sides for the generic `showcase_object` flow. Both remain uncalibrated.
- The profile catalog separates `rigid_case` A–D research capture from the
  `showcase_object` SIDE_1/SIDE_2 acquisition flow. The latter accepts any stable
  box/object as raw input; this does not establish cross-object generalization.
- `ProbeBrain` records a fixed characterization plan: stereo audio requests the
  same excitation on left and right outputs, and haptics are requested on every
  exposed vibrator ID when Android reports them. These are requests, not proof
  of physical transducer onset.
- A previous handset capture on serial `10BFAT1UT2000XP` recorded 48 kHz PCM and approximately 462 Hz accelerometer/gyroscope event rates. On the new handset, one unlabelled acquisition reached review and was rejected; Status afterward showed zero persisted captures and baselines. Neither run is training or accuracy evidence.
- The isolated training environment now runs CPU PyTorch and the pinned Laya source. The licensed English Laya base checkpoint loaded locally; its host ONNX export (1,685,258,240-byte external FP32 weights) matched eager output on one generic email example. Its shipped temperature is invalid/clamped, so confidence is uncalibrated; this checkpoint is not packaged or used for physical decisions.
- MOMENT-1-small was evaluated as a frozen encoder on the public UCI fan split and underperformed the scratch fan baseline (55.56% versus 67.63% test accuracy). Neither fan result is Capybara integrity evidence; MOMENT is not promoted.

## What is not yet claimed

No task-specific V6 specialist weights, labeled physical-integrity dataset,
calibration, trained Capybara-Laya checkpoint, Android ONNX parity, TrustGate
calibration, integrity accuracy, or full offline MATCH/CHANGED decision is
currently verified. The base Laya host smoke test is not physical evidence. A
separate UCI fan-vibration candidate was trained on public labels, but it is not
a Capybara model and is not integrated. Missing prerequisites produce
unavailable/INCONCLUSIVE behavior; the app never substitutes a threshold
classifier or fabricated result.

The showcase accepts any stable box or object through two real contact-side
captures. Enrollment asks for a name, captures and reviews side 1, asks the user
to turn the phone, then captures and reviews side 2. Only a complete approved
pair creates an encrypted uncalibrated research reference and provenance digest;
partial/rejected captures are not listed as enrolled objects. Scan uses the same
two-side approve/retry/reject flow without a name and remains `INCONCLUSIVE`
until validated comparison models and calibration exist. The digest is not an
identity signature.

## Repository

- `sdk/` — Android library, acquisition, DSP, storage, training and evaluation.
- `apk/` — Compose demonstration app consuming `sdk/`.
- `sdk/docs/` — architecture, device capability, sensing, experiments, model and progress records.
- `sdk/training/` — real-data schema validation, pinned Laya preparation, training and export/parity workflow. It refuses incomplete or fabricated inputs.
- `CODEX_HANDOFF.md` — verified continuation state and next Codex actions.

## Build

```powershell
cmd.exe /c gradlew.bat testDebugUnitTest assembleDebug
```

## Latest verification (2026-10-10)

Full SDK/DSP host verification passed:

```powershell
cmd.exe /c gradlew.bat :sdk:testDebugUnitTest :sdk:lintDebug :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebug
```

Showcase UI verification also passed:

```powershell
cmd.exe /c gradlew.bat :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebugAndroidTest :apk:assembleDebug
```

The APK unit-test task is `NO-SOURCE`; Android instrumentation was compiled
but not executed. The APK was installed on `10BFBH1BH0001EY`; its home, status,
technical-details and Scan placement screens rendered. The final Scan view
showed the complete capture action. One unlabelled acquisition reached review
and was rejected; Status afterward reported microphone permission granted,
zero persisted captures/baselines and `Ready`. No accepted object-labeled data,
physical pilot or integrity decision resulted; capability reports do not prove
speaker or motor actuation.

Install only the Capybara package on the verified device:

```powershell
adb -s 10BFBH1BH0001EY install -r apk/build/outputs/apk/debug/apk-debug.apk
```

The runtime has no `INTERNET` permission. Do not operate on packages named Hacker Tracker or Register.

## Real-data training

Training requires supervised recordings from multiple physical objects, sessions
and controlled conditions. Public datasets can provide bounded pretraining, not
Capybara integrity labels. Use `sdk/training/README.md` and
`sdk/docs/experiments.md`. Diagnostics and schema fixtures must not be relabeled
as training data. Laya training remains blocked until all five specialist model
versions, calibration evidence and at least the required independent labeled cases
exist.

## Research

Published evidence and implementation limits are recorded in `research.md`, `sdk/docs/hardware-research.md`, `sdk/docs/sensing.md` and `sdk/docs/laya-integration.md`. In particular, Knocker and VibePhone motivate multimodal sensing but do not establish universal phone-only hidden-content integrity.

## License

Apache-2.0. See `LICENSE` and third-party notices before distributing model or dataset artifacts.

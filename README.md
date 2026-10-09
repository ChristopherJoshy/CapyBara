# Capybara V6

*Give the physical world a memory.*

Capybara is an offline Android physical-measurement SDK and demonstration app. It uses the phone's real audio output, microphone, vibration actuators, accelerometer and gyroscope to record how a contacted object responds. Raw measurements remain private on the device and retain timing, routing, excitation-request and quality provenance.

## Current verified state

- Connected test device: vivo/I2501, Android 16 / API 36, `arm64-v8a`, serial `10BFAT1UT2000XP`.
- Real `AudioRecord` microphone capture, stereo `AudioTrack` excitation request, IMU capture, finite haptic excitation, cancellation cleanup, private atomic capture storage and CPU DSP are implemented.
- The device exposes two vibrator IDs. On API 31+, Capybara requests the same finite waveform on all exposed IDs through `CombinedVibration`; Android does not expose independent-axis semantics for them.
- Encrypted immutable four-position research baselines use Android Keystore AES-GCM.
- The SDK profile catalog currently supports the physically specified `rigid_case` workflow at registered positions A–D. It is an acquisition contract; it does not make the profile calibrated or decision-ready.
- `cmd.exe /c gradlew.bat :sdk:testDebugUnitTest :sdk:lintDebug :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebug` passes on the host. The latest APK device suite passed all 3 tests (real capture/cancellation and two showcase UI cases) on the awake phone. The Gradle connected-test task then removed the target package; the APK was reinstalled and launched. That cleanup may have removed app-private data, so do not rerun the connected task until its cleanup behavior is understood.
- A prior real-device diagnostic recorded 48 kHz PCM, approximately 462 Hz accelerometer/gyroscope event rates, and CPU DSP timing. That diagnostic is not training or accuracy evidence.
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

The showcase guides four real captures at registered positions A–D with a top-down placement guide. Completing enrollment stores an encrypted research baseline marked uncalibrated; it cannot support an integrity decision. Scan starts anonymously and also captures A–D, but no validated model compares it with enrollments, so the outcome remains `INCONCLUSIVE`. Its digest is capture provenance, not an identity signature. The latest device UI test checked scan entry and the A guide only; a four-position physical enrollment/scan has not yet been completed.

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

Install only the Capybara package on the verified device:

```powershell
adb -s 10BFAT1UT2000XP install -r apk/build/outputs/apk/debug/apk-debug.apk
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

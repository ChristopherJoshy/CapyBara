# Codex handoff: Capybara V6

**Continuation snapshot (2026-10-10):** this is a status record, not a completion claim. Continue the full V6 goal until every definition-of-done item has current evidence.

## Read first

1. `AGENTS.md` — non-negotiable project and device rules.
2. `README.md`, `research.md`, and `mistakes.md` — current state, evidence and known failures.
3. `sdk/AGENTS.md` and `apk/AGENTS.md` — scoped ownership and safety rules.
4. `sdk/docs/product-requirements.md`, `sdk/docs/progress.md`, `sdk/docs/experiments.md`, `sdk/docs/architecture.md`, and `sdk/training/README.md` — acceptance criteria and data/model workflow.

Root `AGENTS.md` remains authoritative. Do not interpret this handoff as relaxing any rule or marking a milestone complete.

## User instructions to preserve

- Use licensed pretrained weights where they fit the real sensor modality and task. The user explicitly selected Laya for the final decision stage; preserve its pinned source/checkpoint lineage. MOMENT was tested and rejected for the UCI fan transfer.
- Kaggle CLI 2.2.4 authenticated successfully; quota reported 30 GPU-hours and 20 TPU-hours remaining but no accelerator allocation was tested. Searching the exact licensed Mendeley DOI found no Kaggle mirror. No cloud job was launched because the available public datasets do not match physical phone-to-object integrity labels. Do not upload private captures or credentials.
- Commit and push each **verified coherent slice**. Stage only intended files; never push captures, downloaded data, weights, virtual environments or credentials. GitHub authentication is valid and the current branch is `codex/v6-handoff`.
- The user has now paused all iQOO testing. Do not install, launch, capture, or
  otherwise test on the iQOO until the user changes this instruction, even if
  the authorized serial reconnects. Host-only work may continue.

## Safety invariants

- Never inspect, operate on, or alter apps named Hacker Tracker or Register, their packages, data or processes.
- Device operations are restricted to verified serial `10BFBH1BH0001EY` and Capybara packages `dev.capybara.app`, `dev.capybara.app.test`, and `dev.capybara.sdk.test`.
- No simulated measurements, fabricated results, dummy models or inferred accuracy. Missing models/calibration/data must remain unavailable or `INCONCLUSIVE`.
- Preserve the offline CPU-only V6 architecture. No `INTERNET` permission, cloud inference, camera dependency or hidden demo path.
- Training data and credentials stay private. Never print authentication material.
- Do not mark completion until every product-requirements criterion is evidenced, including real trained specialists, calibrated decisions and object/session-separated physical held-out evaluation on the phone.

## Verified project state

- Android SDK and Compose demo are present. The SDK uses real `AudioRecord`, `AudioTrack`, `SensorManager` and `VibratorManager` APIs, CPU DSP, cancellation cleanup, private atomic capture storage and Android Keystore-protected baselines.
- The current verified device is iQOO I2501 / Android 16 API 36 / `arm64-v8a`, serial `10BFBH1BH0001EY`. Android exposes vibrator IDs 0 and 1; the app requests the same finite waveform on both. Capability reporting does not establish independent axes or physical onset/speaker activation.
- Latest `adb devices -l` did not show the authorized iQOO serial; it showed
  `5XVSGE8HJBIF55T8` (CPH2781IN) and `emulator-5554`. Do not operate either.
- The current showcase uses two generic contact sides (`SIDE_1`, `SIDE_2`).
  Enrollment names the object and saves the approved raw captures plus an
  Android Keystore AES-GCM encrypted `RESEARCH_BASELINE_UNCALIBRATED` reference
  and provenance digest only after both registered sides are complete. Partial
  or rejected captures do not appear in the enrolled-object list. Scan is
  anonymous and stays `INCONCLUSIVE`.
- The latest host build compiled the new two-side storage regression and both
  showcase instrumentation APKs. SDK/APK host unit tests and both lint tasks,
  APK assembly, and instrumentation APK compilation passed. The storage
  instrumentation was not executed because the historical connected task
  removed the app package and may have cleared private data.
- Follow-up commit `9e6d26e` replaced the misleading enrollment prompt
  “Create a physical fingerprint” with “Create a research reference”.
  `:apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebug` passed;
  the unit-test task was `NO-SOURCE`. No device enrollment/capture was run.
- `a476743` includes the final unrepresented PCM samples in coarse spectral
  evidence through one reported, zero-padded tail window; a synthetic last-sample
  regression and full host/lint/assembly checks passed. This diagnostic remains
  uncalibrated and is not integrity evidence.
- On the previous serial `10BFAT1UT2000XP`, the home UI rendered and a scoped read-only probe found no capture directory. The home listed a saved research reference, but its captures, labels and provenance remain uninspected; do not use or export it without user approval. That unit is no longer connected.
- The new serial `10BFBH1BH0001EY` had no Capybara APK before the device milestone. The APK is installed; home, status and technical-details screens rendered. A real but unlabelled acquisition reached review and was rejected; post-rejection Status reported microphone permission granted, zero persisted captures/baselines and `Ready`. It produced no accepted data, reference or integrity decision; speaker/motor response remains unverified.
- On the previous serial `10BFAT1UT2000XP`, the installed home and side-1
  placement screens were visually inspected; no physical capture/review ran.
  Physical actuator onset, offline scan and verification remain unverified.
- `a6cf03d` renames the home action to `Scan`, adds startup/capture/result motion,
  and calls the capture-set hash a provenance digest. The active pulse is gated
  by real SDK busy state; the success check requires encrypted reference save,
  while scans still show `INCONCLUSIVE`. APK unit task was `NO-SOURCE`;
  instrumentation compiled but was not executed. Device install/render passed.
- `2287498` improves Compose enrollment keyboard/inset handling and keeps the
  Scan capture action and review decisions accessible before explanatory
  details. Full SDK/APK unit, lint and assembly checks passed; the APK unit task
  was `NO-SOURCE`. The final device build visibly showed the complete Scan
  action. Do not treat the rejected unlabelled acquisition as pilot data.
- All five specialists (EchoNet, VibraNet, PhysFusion, StateNet, SpatialNet),
  task-specific Capybara-Laya training, Android CPU parity, calibration,
  TrustGate and full offline MATCH/CHANGED behavior remain incomplete.
- The pinned Laya 0.4.1 base checkpoint loaded on host CPU; the official exporter created a graph plus 1,685,258,240 bytes of external FP32 weights. ONNX Runtime CPU matched eager CPU on one generic upstream email example. Its base confidence is uncalibrated; no graph or model is in the APK.
- `Capybara.evaluate` now checks that user-labeled current captures match the
  selected reference's object label (annotation consistency only). Host unit
  coverage exercises the guard; instrumentation also has mismatched-object and
  metadata-tampering paths. No real Laya inference is wired: upstream `laya-java`
  explicitly does not implement Android, and the only available graph is the
  untrained base FP32 export. Do not add it as a decision path.
- Latest host-only check passed the SDK unit test, SDK/APK lint, instrumentation
  APK compilation and APK assembly. The instrumentation did not run. The `sdk/captures/`
  directory is absent; no real training examples exist. The user paused iQOO
  testing, so do no device work until that instruction changes.

## Public data and trained baseline

The following assets are in ignored `sdk/training/artifacts/`; do not add them to Git:

- `venv/`: bundled Python 3.12.14; PyTorch `2.14.1+cpu`, NumPy `2.2.6`, Transformers `4.57.1`, `momentfm` `0.1.5`, Laya `0.4.1`, ONNX `1.23.2` and ONNX Runtime `1.31.0`. CUDA unavailable. These observed pins are not a complete lock.
- `accelerometer-uci-846.zip` and `uci-accelerometer.csv`: official UCI 846 download. Dataset record: [UCI Accelerometer](https://archive.ics.uci.edu/dataset/846/accelerometer), CC BY 4.0. It has 153,000 three-axis samples at 50 Hz from a single cooler fan, with three blade-weight configurations and 17 speeds. ZIP SHA256 `00557d031124defb8cebc6ae62cb1833e4cadde546c6c2b1997a8925d92816b0`; CSV SHA256 `296a94eaf40643e1188012a4ee37d0b2c397bbc04decd39cfbe9952b8c841674`.
- `uci-fan-baseline/`: a small CNN trained from scratch by
  `sdk/training/train_uci_vibration_baseline.py`. It classifies the fan's weight
  arrangement, not Capybara integrity. Non-overlapping 64-sample windows; full
  speed settings 30/60/90 held out for validation and 40/70/100 for test; best
  epoch 7. Test: 414 windows, accuracy `0.6763`, macro-F1 `0.6552`,
  cross-entropy `0.8002`, chance `0.3333`. Confusion matrix:
  `[[89,3,46],[39,53,46],[0,0,138]]`. Windows are correlated and share one
  fan/testbed. Weight SHA256
  `ea0a70c4f3f0b9ff1aa3f8c2bfb668d687e289a4a50c19d9ddd3393c894766c2`. Treat
  this as a scratch comparison baseline only; it is not V6 VibraNet and is not integrated.
- `Knocker Dataset.zip`: downloaded from the official KAIST page, but no dataset license was found in the archive or page. Do not train distributable weights from it without terms from the authors.

Reproduce the scratch baseline with:

```powershell
& sdk/training/artifacts/venv/Scripts/python.exe sdk/training/train_uci_vibration_baseline.py `
  --dataset sdk/training/artifacts/uci-accelerometer.csv `
  --out sdk/training/artifacts/uci-fan-baseline
```

## Pretrained-model research

- [MOMENT-1-small](https://huggingface.co/AutonLab/MOMENT-1-small) is MIT-licensed and pinned to revision `411e288267f82cce86296dbe4d6c8bc533cc162f`; checkpoint SHA256 `785e6c6f57ffa7cac7e2a1fff6369618d49f2f441563ddea76c866231e5aa877`. Its frozen UCI fan probe scored 55.56% accuracy and 52.97% macro-F1, below the scratch baseline's 67.63% / 65.52% on the same 414-window speed-held-out split. It is rejected for this transfer and is not a Capybara model.
- Laya source commit `1adc59f7e371deb601fcfa18a14e25db238addcc` and official English checkpoint revision `7b928d828b7b0e022f929d9bd2e44165aa270148` are recorded in `sdk/training/README.md`. The base tensor hash is `891102d372688fc2a094dac56a384bc537b87c63f21f9f3dac0be2b7cbc8d86c`. Host eager/ONNX output matched on one generic example; the checkpoint has an invalid base temperature, and the 1.69 GB FP32 ONNX external weights are not app-deployable. No Capybara fine-tune or physical evaluation exists.
- [YAMNet](https://www.tensorflow.org/hub/tutorials/yamnet) is a pretrained MobileNetV1 audio-event classifier trained for 521 AudioSet labels, with 16 kHz mono input. Its weight-asset license was not verified in the sources read. It is environmental-audio pretraining, not active phone/object response. Do not use until rights and domain transfer are checked.
- [Vibration measurements with observer-declared variance change points](https://github.com/witulska/vibration-changepoint-dataset) is CC BY 4.0 and contains nine smartphone inertial recordings (one person, motorbike and car), up to 100 Hz, with observer-declared variance shifts. It lacks active excitation, audio and object-integrity labels.
- [ORION-AE](https://doi.org/10.7910/DVN/FBRDU0) is CC0 1.0 and labels bolt-loosening levels using bonded 5 MHz acoustic-emission sensors and a laser vibrometer. This structural-health dataset is incompatible with phone microphone/IMU input.
- Additional 2026-10-10 dataset audit: the CC BY 4.0 bare-deck impact-echo
  dataset has relevant concrete labels but no verified phone sensor transfer;
  the CC BY 4.0 iPhone induction-motor set is one motor domain with attached
  phone IMU and no active phone excitation; the 20.9 GB bare-finger texture
  dataset uses skin-mounted sensors and texture labels; VibTac-12's license is
  unstated and access requires IEEE login. No new data was downloaded or used.
  See `research.md`; none supports Capybara integrity training.
- The latest host-only integration build passed. SDK/APK lint and SDK unit
  regression passed; Android instrumentation was compiled but not executed.
  Commit `3218733` was pushed to `codex/v6-handoff`.
- Current `Capybara.evaluate` derives authenticated per-position physics
  evidence and remains fail-closed `INCONCLUSIVE`. The scan caller now uses the
  selected reference's annotation label. No model inference or identity proof.
- Do not run iQOO experiments while the user pause remains in force.
- The SDK data/capture directory is absent; no real labeled Capybara captures,
  task-trained specialists, calibrated Laya checkpoint or full-system accuracy
  evidence exist.
- The licensed KAIST data and these adjacent datasets were not downloaded or used for task-model training; using their unrelated labels would not create Capybara specialists.

## Immediate continuation order

1. Keep the rebuilt Capybara APK installed and preserve its private data.
2. Do not test on the iQOO while the user's pause remains active. When the user
   resumes testing, use `sdk/docs/experiments.md` to collect the controlled
   earbuds-case experiment: five independent repeats per position and physical
   state; a single object is feasibility evidence only.
3. Obtain multiple independent object IDs, sessions, run labels and controlled
   physical states before training. Keep each object's captures and references
   within one train/calibration/test split.
4. Train EchoNet/VibraNet/PhysFusion/StateNet/SpatialNet from measured iQOO data
   only after separability and held-out evaluation are demonstrated; then
   fine-tune Laya on their versioned evidence.
5. Qualify quantization with held-out decision/calibration evidence before
   Android CPU parity, memory/latency measurements, TrustGate and offline
   end-to-end validation.
6. Resolve preservation-safe connected test execution before any connected
   Gradle suite; keep user app data intact.
7. Continue on `codex/v6-handoff`; commit and push each verified coherent slice.
   Keep captures, training artifacts, credentials and generated files out of Git.

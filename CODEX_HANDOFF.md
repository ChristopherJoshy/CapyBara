# Codex handoff: Capybara V6

**Transfer requested:** the user is moving the active goal from this assistant to Codex. This is a state snapshot, not a completion claim. Continue the full V6 goal until its definition of done is verified.

## Read first

1. `AGENTS.md` — non-negotiable project and device rules.
2. `README.md`, `research.md`, and `mistakes.md` — current state, evidence and known failures.
3. `sdk/AGENTS.md` and `apk/AGENTS.md` — scoped ownership and safety rules.
4. `sdk/docs/product-requirements.md`, `sdk/docs/progress.md`, `sdk/docs/experiments.md`, `sdk/docs/architecture.md`, and `sdk/training/README.md` — acceptance criteria and data/model workflow.

Root `AGENTS.md` remains authoritative. Do not interpret this handoff as relaxing any rule or marking a milestone complete.

## User instructions to preserve

- Use licensed pretrained weights where they fit the real sensor modality and task. The user explicitly selected Laya for the final decision stage; preserve its pinned source/checkpoint lineage. MOMENT was tested and rejected for the UCI fan transfer. Do not use Kaggle as a fallback unless the user redirects the work.
- Use the isolated `sdk/training/artifacts/venv` with its bundled Python 3.12.14; do not mix packages with global Python. No Kaggle job ran and no captures were uploaded. A prior attempt to upload a private Kaggle kernel source was rejected by the app reviewer because ownership of that external destination was not verified; the user then chose Laya. Do not retry that external upload.
- Commit and push after each **verified coherent slice**, not on a timer. Stage and inspect only intended files; never push captures, downloaded data, weights, virtual environments or credentials. A push of commit `74b8d86` to `origin codex/v6-handoff` was rejected by the app reviewer because repository ownership for `ChristopherJoshy/CapyBara` was not verified. Nothing was pushed; do not retry without resolving the ownership check.

## Safety invariants

- Never inspect, operate on, or alter apps named Hacker Tracker or Register, their packages, data or processes.
- Device operations are restricted to verified serial `10BFAT1UT2000XP` and Capybara packages `dev.capybara.app`, `dev.capybara.app.test`, and `dev.capybara.sdk.test`.
- No simulated measurements, fabricated results, dummy models or inferred accuracy. Missing models/calibration/data must remain unavailable or `INCONCLUSIVE`.
- Preserve the offline CPU-only V6 architecture. No `INTERNET` permission, cloud inference, camera dependency or hidden demo path.
- Training data and credentials stay private. Never print authentication material.
- Do not mark completion until every product-requirements criterion is evidenced, including real trained specialists, calibrated decisions and object/session-separated physical held-out evaluation on the phone.

## Verified project state

- Android SDK and Compose demo are present. The SDK uses real `AudioRecord`, `AudioTrack`, `SensorManager` and `VibratorManager` APIs, CPU DSP, cancellation cleanup, private atomic capture storage and Android Keystore-protected baselines.
- The connected iQOO is I2501 / Android 16 API 36 / `arm64-v8a`, serial `10BFAT1UT2000XP`. Two vibrator IDs are exposed; the app requests the same finite waveform on both. This does not establish independent actuator axes, physical onset or speaker activation.
- Showcase enrollment and scan guide four registered positions A–D. A complete enrollment stores an encrypted but uncalibrated research baseline; scan remains anonymous and `INCONCLUSIVE` because validated comparison models are unavailable.
- Earlier device evidence includes 3 APK and 2 SDK tests on the awake device. This turn's latest APK suite and its cleanup incident are recorded in `README.md` and `sdk/docs/progress.md`; all such checks establish acquisition/UI/storage behavior only, not model behavior or integrity accuracy.
- No user-selected rigid-case experiment captures or labels exist. The controlled A–D experiment and repetitions are specified in `sdk/docs/experiments.md`; human physical work remains a blocker.
- Latest APK connected suite passed 3/3 tests; its UI assertions checked scan entry and position A only, not a complete A–D run or baseline save. The Gradle task removed `dev.capybara.app`; the demo APK was reinstalled and launched. Private app data may have been cleared. Avoid that task until its cleanup can be prevented or a verified backup exists.
- All five specialists (EchoNet, VibraNet, PhysFusion, StateNet, SpatialNet), actual Capybara-Laya training, CPU export parity, calibration, TrustGate and full offline MATCH/CHANGED behavior remain incomplete.
- The pinned Laya 0.4.1 base checkpoint loaded on host CPU; the official exporter created a graph plus 1,685,258,240 bytes of external FP32 weights. ONNX Runtime CPU matched eager CPU on one generic upstream email example. Its base confidence is uncalibrated; no graph or model is in the APK.

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
- [KAIST rotating-machine data](https://data.mendeley.com/datasets/ztmf3m7h5x/6), CC BY 4.0, contains synchronized industrial acoustic/vibration signals and normal/bearing-fault/misalignment/unbalance labels. The record lists 45 MB acoustic and 2.48 GB vibration files; the study reports 51.2 kHz audio and 25.6 kHz vibration. Potential licensed PhysFusion pretraining, but not phone/container integrity and not downloaded.
- [ReaLISED](https://zenodo.org/records/6488321) has 2,479 real indoor event recordings at 44.1 kHz with event/action/material metadata, but its Zenodo license field is blank. Do not use it for weights intended for redistribution without clarified rights; it also lacks controlled excitation, paired IMU and reference/change labels.
- [ObjectFolder-Real](https://www.objectfolder.org/objectfolder-real-download) is CC BY 4.0 and has impact audio plus force profiles, but the first 10-object audio archive is about 36 GB and has no integrity-state labels.
- YCB-impact is publicly reachable but no dataset license was found. MIMII/DCASE machine-anomaly corpora located in research carry CC BY-NC-SA terms and are unsuitable for unrestricted commercial distribution. Do not treat public reachability as a data license.

## Immediate continuation order

1. Keep the restored Capybara APK installed. Find a device-test path that preserves its installation and private data before rerunning connected tests; host builds, lint and unit checks remain available.
2. Collect and label the real rigid-case experiment in `sdk/docs/experiments.md`: A–D enrollment, five unchanged repositioned repeats per position, removed-item repeats, restoration repeats, and invalid captures. This requires human physical manipulation and labeling.
3. Only after real capture evidence exists, build and evaluate each modality-matched specialist with object/session/run-separated splits, then fine-tune and calibrate Laya on versioned specialist evidence.
4. Qualify model size/quantization with held-out accuracy and calibration before implementing Android CPU tokenizer/runtime parity, TrustGate, phone memory/latency measurements and offline end-to-end evaluation.
5. Resolve preservation-safe connected test execution before any future device suite. Do not rerun the connected Gradle task until its package/data cleanup is understood.
6. Git history was empty (`main` unborn, `origin/main` absent) when this handoff
   began. The first checkpoint is on feature branch `codex/v6-handoff`; continue
   with local feature-branch commits after each verified coherent slice. Push is
   currently blocked by the ownership review described above.
   Review staged paths; keep `sdk/training/artifacts/`, captures, credentials and
   generated files out of commits.

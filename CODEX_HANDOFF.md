# Codex handoff: Capybara V6

**Transfer requested:** the user is moving the active goal from this assistant to Codex. This is a state snapshot, not a completion claim. Continue the full V6 goal until its definition of done is verified.

## Read first

1. `AGENTS.md` — non-negotiable project and device rules.
2. `README.md`, `research.md`, and `mistakes.md` — current state, evidence and known failures.
3. `sdk/AGENTS.md` and `apk/AGENTS.md` — scoped ownership and safety rules.
4. `sdk/docs/product-requirements.md`, `sdk/docs/progress.md`, `sdk/docs/experiments.md`, `sdk/docs/architecture.md`, and `sdk/training/README.md` — acceptance criteria and data/model workflow.

Root `AGENTS.md` remains authoritative. Do not interpret this handoff as relaxing any rule or marking a milestone complete.

## User instructions to preserve

- Use licensed pretrained weights wherever they fit the actual sensor modality and task. Pin model revisions and hashes; compare against leakage-safe held-out groups. Do not ship a pretrained model merely because it is available.
- Continue to use `sdk/training/artifacts/venv`; do not mix training packages with global Python. Kaggle training was previously requested, but no Kaggle training job has run. Do not upload private captures or credentials without explicit authorization.
- Commit and push after each **verified coherent slice**, not on a timer. Stage and inspect only intended files; never push captures, downloaded data, weights, virtual environments or credentials.
- The user asked to install the Kimi Browser Extension using its PowerShell installer. The script was inspected but not run: it downloads an unpinned, unverified latest daemon and installs agent skills; it does not itself install the browser extension. No Kimi files or integration were added. Existing isolated browser automation is available; verify binary provenance and necessity before any installation.

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

## Public data and trained baseline

The following assets are in ignored `sdk/training/artifacts/`; do not add them to Git:

- `venv/`: Python 3.13.14; PyTorch `2.14.1+cpu` installed. The recent training run emitted PyTorch's optional NumPy-initialization warning because NumPy is absent; the script itself does not use NumPy and completed successfully.
- Recheck: the venv interpreter currently cannot start because its configured Microsoft Store Python 3.13 base is unavailable to process creation. The separate bundled Python 3.12.14 launches but is not suitable for mixing with the venv's binary ML packages. MOMENT weights remain uncached and unevaluated.
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

- [MOMENT-1-small](https://huggingface.co/AutonLab/MOMENT-1-small) is a public MIT-licensed multivariate time-series foundation model with classification/embedding modes. Model revision `411e288267f82cce86296dbe4d6c8bc533cc162f`; safetensors SHA256 `785e6c6f57ffa7cac7e2a1fff6369618d49f2f441563ddea76c866231e5aa877`; file size 151,615,328 bytes; 37,901,512 parameters. It has not been installed, downloaded or evaluated in the venv. Next: evaluate it against the same UCI speed-group split, preferably as a frozen representation or teacher; measure transfer and resource cost. The checkpoint is likely too large for direct Android use. Distill only if a smaller student retains held-out behavior; then measure CPU export parity and device latency.
- [YAMNet](https://www.tensorflow.org/hub/tutorials/yamnet) is a pretrained MobileNetV1 audio-event classifier trained for 521 AudioSet labels, with 16 kHz mono input. Its weight-asset license was not verified in the sources read. It is environmental-audio pretraining, not active phone/object response. Do not use until rights and domain transfer are checked.
- [KAIST rotating-machine data](https://data.mendeley.com/datasets/ztmf3m7h5x/6), CC BY 4.0, contains synchronized industrial acoustic/vibration signals and normal/bearing-fault/misalignment/unbalance labels. The record lists 45 MB acoustic and 2.48 GB vibration files; the study reports 51.2 kHz audio and 25.6 kHz vibration. Potential licensed PhysFusion pretraining, but not phone/container integrity and not downloaded.
- [ReaLISED](https://zenodo.org/records/6488321), CC BY 4.0, has 2,479 real indoor event recordings at 44.1 kHz with event/action/material metadata. It lacks controlled excitation, paired IMU and reference/change labels.
- [ObjectFolder-Real](https://www.objectfolder.org/objectfolder-real-download) is CC BY 4.0 and has impact audio plus force profiles, but the first 10-object audio archive is about 36 GB and has no integrity-state labels.
- YCB-impact is publicly reachable but no dataset license was found. MIMII/DCASE machine-anomaly corpora located in research carry CC BY-NC-SA terms and are unsuitable for unrestricted commercial distribution. Do not treat public reachability as a data license.

## Immediate continuation order

1. Keep the restored Capybara APK installed. Find a device-test path that preserves its installation and private data before rerunning connected tests; host builds, lint and unit checks remain available.
2. Restore a working Python 3.13 interpreter for `sdk/training/artifacts/venv`, pin the official `momentfm` revision, and evaluate frozen MOMENT-1-small on the existing UCI speed-group split. Record hashes, package/model pins, resource cost and metrics; do not treat fan labels as package-integrity evidence.
3. Investigate licensed, modality-matched public data for acoustic response and paired fusion. Preserve the rule that unrelated labels cannot become package-integrity labels. Do not use Knocker/YCB training data without license terms.
4. Continue independent implementation while waiting for the user's physical case captures. Once available, collect and label real A–D sessions under the experiment plan, preserve failures, and split by connected object/session/run groups.
5. Train only specialists supported by actual modality/label evidence; do not train final Laya from primitive physics or unrelated public labels. Then implement CPU export/parity, calibration, TrustGate and offline end-to-end device evaluation.
6. Update `research.md`, `sdk/training/README.md`, `sdk/docs/progress.md`, `README.md` and meaningful `mistakes.md` entries as evidence changes.
7. Git history was empty (`main` unborn, `origin/main` absent) when this handoff
   began. The first checkpoint is on feature branch `codex/v6-handoff`; continue
   with feature-branch commits and push each verified coherent slice to `origin`.
   Review staged paths; keep `sdk/training/artifacts/`, captures, credentials and
   generated files out of commits.

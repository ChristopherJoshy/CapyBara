# Real physical training workflow

No trained Capybara weights or physical training set exist yet. These scripts
validate real recordings, preserve provenance, and call actual upstream Laya.
They do not manufacture sensor data or launch remote jobs. The schema tests use
temporary fixtures only; they are never training or evaluation evidence.

## Verified environment, 9 October 2026

- Kaggle CLI **2.2.4** is installed in the Python 3.13 Store site-packages.
- The WindowsApps `python.exe` launcher fails in this environment. Codex's bundled
  Python **3.12.14** works. Importing the installed Kaggle site-packages through it
  works for the verified CLI and read-only API calls; this is not a verified ML
  environment and must not be used to mix binary ML packages across Python versions.
- Credential presence was checked with the Kaggle skill's checker, with token
  output fully redacted and credential-permission mutation disabled.
- Live authentication succeeded as **chrisopherjoshy**, exactly this spelling.
- Quota: GPU **0.00 / 30.00 hours used**, TPU **0.00 / 20.00 hours used**. API refresh
  value was `2026-10-10T00:00:00`; it supplied no timezone, so none is inferred.
- Installed `kernels push --help` supports `--accelerator`; it does **not** show
  the newer documentation's `--no-run`. Use installed help to verify commands.
- No GPU was allocated, no training was launched, and no recordings were uploaded.
- Bundled Python has NumPy but no PyTorch, ONNX, ONNX Runtime, Transformers,
  Tokenizers, Safetensors, or Laya. ML execution is unverified.

This read-only check prints only the account name and quota, never credentials:

```powershell
$trainingPython = 'C:\Users\Chris\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
& $trainingPython -c 'import sys; sys.path.insert(0,r"C:\Users\Chris\AppData\Local\Packages\PythonSoftwareFoundation.Python.3.13_qbz5n2kfra8p0\LocalCache\local-packages\Python313\site-packages"); import kaggle; kaggle.api.authenticate(); print(kaggle.api.get_config_value("username")); kaggle.api.quota_view_cli()'
```

## Capture and annotation contract

Copy only Capybara's app-private capture directory to ignored `sdk/captures/`.
Each capture has `metadata.json`, mono signed PCM16 little-endian `audio.pcm`, and
`imu.csv` with `type,timestampNanos,x,y,z`. Metadata schema 1 includes captureId,
label `{objectId,sessionId,condition,position,profile}`, actual sampleRate and
sampleCount, boot-clock capture bounds, excitation request times, and quality
metadata. Preserve the original files. A diagnostic capture is not an experiment.

Create one human-annotated JSONL row per separate reference/comparison capture:

```json
{"capture":"current-id/metadata.json","reference":"enrollment-id/metadata.json","evidence":"current-id/evidence.json","runId":"physical-run-id","label":"MATCH","annotator":"experimenter-id","labelledAtUtc":"2026-10-09T12:00:00Z"}
```

This illustrates the **schema**, not a collected case. Choose labels from known
physical interventions and measurement validity, never from the model's guess.
An unchanged object recorded under invalid conditions should be INCONCLUSIVE.
Annotate the condition in capture metadata; condition, identity, label, and other
annotations never enter Laya's feature input. The selected profile does enter it.

Evidence file envelope:

```json
{"captureId":"current-id","referenceCaptureId":"enrollment-id","pipelineVersion":"physics-v1","values":{"feature_name":"actual derived value"}}
```

Use only implemented measured features. Missing features may remain absent/null;
never impute unavailable models with zeros. `physics-v1` can be prepared as
research data but is refused by final Laya training. Final evidence also needs
`modelVersions` mapping EchoNet, VibraNet, PhysFusion, StateNet, and SpatialNet to
their actual lowercase SHA256 digests, and a `calibrationVersion`. Names/digests
are traceability checks, not proof of a model's experimental quality.

The current index is a capture-pair contract. Four-position aggregate training
requires all four positions' source captures and reference hashes to be recorded
in the producer's provenance before using aggregated evidence. The current
primitive producer has not demonstrated SpatialNet evidence.

```powershell
& $trainingPython sdk/training/test_training.py
& $trainingPython sdk/training/prepare.py sdk/captures/labels.jsonl --capture-root sdk/captures --out sdk/training/artifacts/dataset-v1
```

Preparation checks labels, timestamps, actual PCM byte counts, matching object,
profile and position, reference/evidence identity, duplicate cases, finite values,
annotation leakage, and artifacts confined to the capture root. It hashes raw
metadata, PCM, IMU, and evidence. The validator cannot prove a recording was made
on physical hardware: supervised collection and retained logs establish that.

It joins rows connected by **either** an object, session, physical run, or shared
capture/reference. Entire connected components enter one split. Seed 20261009
allocates about 60/20/20 percent of components to training/calibration/test. This
is component-balanced, not necessarily sample-balanced. At least three independent
components are required; final training also needs all three labels in each split
and upstream's minimum ten calibration cases. Ten is a fitting floor, not evidence
of reliable scientific confidence. Collect more independent objects/sessions.

## Pinned Laya setup and execution

Official Laya source: **1adc59f7e371deb601fcfa18a14e25db238addcc**, release 0.4.1.
The ignored local checkout is `sdk/training/upstream/laya`. Recreate it with:

```powershell
git clone https://github.com/NandhaKishorM/laya sdk/training/upstream/laya
git -C sdk/training/upstream/laya checkout --detach 1adc59f7e371deb601fcfa18a14e25db238addcc
python -m venv sdk/training/artifacts/venv
& sdk/training/artifacts/venv/Scripts/python.exe -m pip install -r sdk/training/requirements.txt
```

Use a functioning installed Python 3.11+ for this environment. The scripts require
the pip installation's direct-url commit to match the pin. The upstream requirements
allow several dependency versions; every real run records resolved versions in
`training-report.json`. A tested ML dependency lock still requires the first real
job; this preparation has not established bitwise reproducibility.

Download the genuine English checkpoint once, at its verified revision:

```powershell
& sdk/training/artifacts/venv/Scripts/python.exe -c 'from huggingface_hub import snapshot_download; snapshot_download("convaiinnovations/laya", revision="7b928d828b7b0e022f929d9bd2e44165aa270148", local_dir="sdk/training/artifacts/laya-base", allow_patterns=["model.safetensors","rl_agent_config.json","encoder/*","tokenizer/*"])'
& sdk/training/artifacts/venv/Scripts/python.exe sdk/training/run_laya.py train sdk/training/artifacts/dataset-v1 --base sdk/training/artifacts/laya-base --out sdk/training/artifacts/capybara-laya-v1 --device cuda
& sdk/training/artifacts/venv/Scripts/python.exe sdk/training/run_laya.py export sdk/training/artifacts/dataset-v1 --checkpoint sdk/training/artifacts/capybara-laya-v1 --out sdk/training/artifacts/capybara-laya-onnx-v1
```

These are future commands. Do not run training until actual labeled specialist
evidence and calibration exist. CPU training is supported with `--device cpu`;
Android inference and export parity always use CPU. Base weights must match
SHA256 `891102d372688fc2a094dac56a384bc537b87c63f21f9f3dac0be2b7cbc8d86c`.
The base tensor file is 842,609,210 bytes with 421,293,830 parameters. FP32 runtime
weight storage alone is about 1.68 GB; graph overhead and activations add memory.

Training uses upstream load/train/calibration/save/evaluation APIs and RLCD,
sequential options, shuffled choice presentation, four epochs, and seed 20261009.
It prints the update budget, saves each epoch, reloads stored weights before
fitting temperatures, rejects collapsed training, and evaluates untouched test
components. It deliberately avoids upstream finetune's item-random calibration
split, which would mix correlated physical measurements. It records model-level
accuracy/ECE/Brier and baseline comparison. Full-system metrics remain separate.

Export invokes the pinned upstream opset-18 exporter. It compares logits and
action logits against eager CPU on every held-out case in batches of up to two,
refusing numeric drift above `atol=rtol=1e-4` or changed decisions. It verifies
batch-one execution too and reports graph/external-data hashes and total size.
This is host CPU parity, not proof of Android compatibility or speed.
No automatic INT8 quantization is performed: upstream measured serious decision
drift even with per-tensor dynamic INT8. Android must validate CPU/provider,
tokenizer, softmax/temperature parity, memory and latency independently.

## Public-data fan-vibration classifier baseline (not a V6 model)

UCI 846 is a CC BY 4.0 dataset of 153,000 three-axis fan-vibration samples at
50 Hz. It labels three fan blade-weight configurations, not object or package
integrity. Keep the downloaded ZIP, extracted CSV, model and report in ignored
`sdk/training/artifacts/`.

The script `sdk/training/train_uci_vibration_baseline.py` trains a small CPU 1D CNN from
scratch as a baseline. Re-run with the isolated environment:

```powershell
& sdk/training/artifacts/venv/Scripts/python.exe sdk/training/train_uci_vibration_baseline.py `
  --dataset sdk/training/artifacts/uci-accelerometer.csv `
  --out sdk/training/artifacts/uci-fan-baseline
```

The run used PyTorch 2.14.1+cpu, seed 20261009, 64-sample non-overlapping
windows, and held out complete fan-speed settings 30/60/90 for validation and
40/70/100 for test. Best epoch 7. Test: 414 windows, 67.63% accuracy, 65.52%
macro-F1, cross-entropy 0.8002, versus 33.33% chance. The test split prevents
speed-setting leakage, but windows still come from a single fan/testbed and are
not independent physical objects. The model SHA256 and full confusion matrix are
in the ignored `training-report.json`; the weights SHA256 is
`ea0a70c4f3f0b9ff1aa3f8c2bfb668d687e289a4a50c19d9ddd3393c894766c2`.

This result is a scratch baseline only. The user requires licensed pretrained
models wherever possible. Evaluate the pinned MIT MOMENT-1-small checkpoint
(`411e288267f82cce86296dbe4d6c8bc533cc162f`) on the same group split; its
151,615,328-byte checkpoint and 37,901,512 parameters are likely too large for
Android. Compare a frozen/finetuned encoder and, only if warranted, a distilled
student. Do not promote it without held-out evaluation and offline CPU/device
parity. No V6 specialist or integrity model is currently trained or integrated.

The training process emitted PyTorch's optional NumPy initialization warning
because this venv does not currently install NumPy; the script does not use NumPy,
and the run completed with the report above. Add NumPy only if a later
pretraining dependency requires it.

## Kaggle execution, when real data is ready

The official notebook uses two T4s and DDP; the shared upstream API used here
trains on one GPU. Both are actual Laya. Start with the simpler single GPU loop;
adopt the dual-GPU notebook only if measured training time requires it. Check
allocated hardware with `torch.cuda.is_available()`, `torch.cuda.device_count()`
and `torch.cuda.get_device_properties()` inside the job. Quota does not guarantee
availability. In metadata set `is_private: true`, `enable_gpu: true`,
`enable_internet: true`, and attach only authorized private data.

Verified command family for installed 2.2.4: `kaggle kernels push -p <job-folder>
--accelerator NvidiaTeslaT4`, `kernels status <account/slug>`, `kernels logs
<account/slug>`, `kernels output <account/slug> -p <ignored-output-folder>`.
Pushing uploads and runs a notebook and consumes quota. No job folder or private
resource was created here because there is no valid physical training set yet.
Do not run the official notebook's optional Hugging Face publishing cells.

Primary references: [Laya fine-tuning source](https://github.com/NandhaKishorM/laya/blob/1adc59f7e371deb601fcfa18a14e25db238addcc/laya/train.py),
[official Kaggle notebook](https://github.com/NandhaKishorM/laya/blob/1adc59f7e371deb601fcfa18a14e25db238addcc/notebooks/laya_finetune_typed_decisions_2xT4_kaggle.ipynb),
[ONNX export](https://github.com/NandhaKishorM/laya/blob/1adc59f7e371deb601fcfa18a14e25db238addcc/scripts/export_onnx.py),
[Kaggle CLI](https://github.com/Kaggle/kaggle-cli/blob/main/docs/kernels.md).

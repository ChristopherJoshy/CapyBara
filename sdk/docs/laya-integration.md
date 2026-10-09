# Laya integration: verified source, deployment still blocked

Investigated on 9 October 2026. Laya is the selected final decision-model lineage;
no substitute classifier is implemented. The official base checkpoint and a host
ONNX export now exist in ignored training artifacts, but no trained Capybara-Laya
checkpoint, calibrated physical dataset, Android Laya benchmark, or deployable
Capybara decision exists. The app must report this explicitly and return
INCONCLUSIVE when the required intelligence is unavailable.

## Architecture and checkpoint

The verified source commit is
`1adc59f7e371deb601fcfa18a14e25db238addcc` (release 0.4.1).
[DecisionModel source](https://github.com/NandhaKishorM/laya/blob/1adc59f7e371deb601fcfa18a14e25db238addcc/laya/common.py)
defines a bidirectional encoder, two transformer decision-head layers, question-type
embedding, marker scorer and action/escalation head. It produces typed decisions
in one forward pass; it does not generate free-form text. A fixed three-choice
integrity question fits this interface.

The English official checkpoint uses ModernBERT-large, 28 encoder layers,
hidden size 1024, 16 attention heads and vocabulary size 50,368. The pinned
upstream README reports **421,293,830 parameters** including Laya's added heads;
the loaded model's parameter iterator counts **421,293,827**, a three-parameter
discrepancy that has not been resolved. The actual
checkpoint config limits the combined question/evidence sequence to 512 tokens
and the question/options head to 192; the backbone's 8,192 position capacity is
not the deployed Laya input budget. The canonical evidence serializer must report
and refuse truncation, not discard physical evidence silently.

Hugging Face revision `7b928d828b7b0e022f929d9bd2e44165aa270148`
contains `model.safetensors` (842,609,210 bytes; mostly FP16),
`rl_agent_config.json`, `encoder/config.json`, `tokenizer/tokenizer.json` and
`tokenizer/tokenizer_config.json`. Its model SHA256 is
`891102d372688fc2a094dac56a384bc537b87c63f21f9f3dac0be2b7cbc8d86c`.
These values were retrieved from official API/config metadata and locally hashed,
not inferred from a filename. The base weights loaded and ran on host CPU; that
is not a Capybara evaluation.
[Official checkpoint](https://huggingface.co/convaiinnovations/laya/tree/7b928d828b7b0e022f929d9bd2e44165aa270148),
[configuration](https://huggingface.co/convaiinnovations/laya/blob/7b928d828b7b0e022f929d9bd2e44165aa270148/rl_agent_config.json).

The official multilingual variant is a genuine alternative Laya checkpoint, not
a smaller unrelated replacement: mmBERT-base with about 322M total parameters.
It ships uncalibrated. Current scripts choose the English checkpoint because
Capybara's structured evidence/question is English and its lineage is pinned.
Any switch needs its own fine-tuning, tokenizer/parity checks and held-out results.
[Multilingual model card](https://huggingface.co/convaiinnovations/laya-multilingual).

## Android inference contract

The official exporter uses ONNX opset 18 and a fused graph with:

| Name | Type | Shape |
| --- | --- | --- |
| input_ids | int64 | batch × sequence |
| attention_mask | int64 | batch × sequence |
| marker_pos | int64 | batch × options |
| marker_mask | bool | batch × options |
| qtype | int64 | batch; choice=0, score=1, noul=2 |
| logits | float32 output | batch × options |
| act_logits | float32 output | batch × 2 |

The sequence includes classification/separator tokens, typed question, one mask
marker per criterion, and serialized physical evidence. Token IDs, marker positions,
padding and temperature buckets must agree with the actual checkpoint. Criteria
order is **MATCH, CHANGED, INCONCLUSIVE**, consistently in preparation and runtime.
The action-head result is not an integrity score and must not be interpreted as one.
[Exporter](https://github.com/NandhaKishorM/laya/blob/1adc59f7e371deb601fcfa18a14e25db238addcc/scripts/export_onnx.py).

Only `option_layout="sequential"` is supported by the five-input ONNX path.
Upstream's parallel option layout needs additional masks/positions and its ONNX
runtime explicitly refuses those checkpoints. Training uses sequential layout
with choice-order augmentation rather than exporting incompatible weights.
[ONNX runtime guard](https://github.com/NandhaKishorM/laya/blob/1adc59f7e371deb601fcfa18a14e25db238addcc/laya/onnx_agent.py).

Reuse upstream `laya-java`'s tokenizer/sequence builder and golden parity fixtures
when implementing Android. Its pure Java BPE supports the real tokenizer's added
tokens and preprocessing; a hand-written whitespace tokenizer is incorrect.
However, its README explicitly says **Android is not implemented**, and it is
not published to Maven Central. The source targets JDK 17 and the desktop
`com.microsoft.onnxruntime:onnxruntime:1.20.0`. It uses platform APIs such as
`System.getLogger`; Android compatibility must be audited and tested, with the
Android AAR replacing desktop native libraries. Do not describe it as a drop-in
Android runtime. Preserve Apache notices for adapted source.
[Java status and parity](https://github.com/NandhaKishorM/laya/blob/1adc59f7e371deb601fcfa18a14e25db238addcc/laya-java/README.md).

Use `com.microsoft.onnxruntime:onnxruntime-android` with CPU execution. Android's
default provider is CPU; do not register NNAPI or GPU delegates. Record actual
session providers/profiling and on-device timings. Bundle the graph, any external
weight files, tokenizer files, config, fitted temperatures, provenance/version
manifest, and model licenses locally. No Hugging Face download or model router
fetch may occur during a phone scan.
[ONNX Runtime mobile guidance](https://onnxruntime.ai/docs/tutorials/mobile/).

The pinned upstream exporter completed after enabling UTF-8 output on Windows.
It produced a 3,306,003-byte ONNX graph and a 1,685,258,240-byte external FP32
weight file. ONNX Runtime loaded it on CPU; on the official generic email example,
the eager and ONNX APIs returned the same `0.879` result. The example exercises
export plumbing only, and its base temperature is invalid/clamped. No Android
tokenizer/operator parity, phone memory, or latency has been measured. The 1.69 GB
external weight file makes this FP32 export unsuitable for current APK packaging.
Dynamic INT8 is not automatically safe: upstream's exporter reports substantial
held-out decision drift, especially per-channel quantization. Only assess a
quantized graph after task-specific held-out accuracy/calibration evidence exists.

## Evidence, calibration and TrustGate

See [training workflow](../training/README.md) for schemas and commands. Physical
evidence must come from genuine captured signals and genuinely trained EchoNet,
VibraNet, PhysFusion, StateNet and SpatialNet. Primitive physics diagnostics are
useful for feasibility analysis but do not establish final V6 model evidence.
Record every source capture for four-position aggregation; diagnostic or human
annotation fields must never become predictive features.

Train/calibration/test separation uses connected object/session/run/reference
components. Upstream's automatic item-random calibration split is unsuitable for
correlated physical measurements, so the scripts use upstream lower-level APIs
with pre-separated calibration. Temperature fitting is performed on stored and
reloaded weights; inherited option-bucket calibration is replaced. Upstream's
`answer_confidence` is top probability, while entropy confidence is a different
scale. Confidence thresholds require real held-out validation.
[Calibration source](https://github.com/NandhaKishorM/laya/blob/1adc59f7e371deb601fcfa18a14e25db238addcc/laya/calibrate.py).

TrustGate must independently refuse missing models, incompatible versions,
unsupported hardware, poor quality, OOD evidence, evidence truncation and
uncalibrated confidence. An upstream Laya prediction never authorizes an automatic
MATCH. Model-level metrics do not establish package-integrity accuracy.

## License and outstanding verification

Laya source and published Laya weights declare Apache-2.0; ModernBERT's model,
architecture and training code declare Apache-2.0. ONNX Runtime uses MIT. These
licenses allow an Apache-2.0 Capybara distribution with required notices; dataset
rights and each new downloaded artifact still need provenance review before
redistribution. No third-party model is committed to this repository.
[Laya license](https://github.com/NandhaKishorM/laya/blob/1adc59f7e371deb601fcfa18a14e25db238addcc/LICENSE),
[ModernBERT model card](https://huggingface.co/answerdotai/ModernBERT-large#license),
[ONNX Runtime license](https://github.com/microsoft/onnxruntime/blob/main/LICENSE).

Verified locally: pinned source and base checkpoint, weight hash, CPU eager load,
successful ONNX export, CPU ONNX Runtime load and one generic host output-parity
example, ten data-safety tests, and Python syntax. Still required: real object
data, specialist training, Laya fine-tuning, resolved ML dependency lock, Android
tokenizer/operator parity, phone memory and CPU latency, calibration, TrustGate,
and held-out full-system offline evaluation. No neural target is claimed achieved.

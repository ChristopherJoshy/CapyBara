# Capybara research record

Published findings, engineering hypotheses and measured device results are distinct.
Targets in the product specification are not measured benchmarks.

## Build system (published documentation, 2026-10-09)

Question: which verified installed toolchain can build the two Android modules?
Source: [Android Gradle plugin 9.4 release notes](https://developer.android.com/build/releases/agp-9-4-0-release-notes), Google, September 2026.
AGP9.4 supports API37, requires Gradle9.6, build-tools36 and JDK17 or newer.
[Built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)
removes the separate kotlin-android plugin. The Compose compiler plugin must match
Kotlin; [Compose BOM](https://developer.android.com/develop/ui/compose/bom) pins
compatible Compose libraries (2026.09.00).
Decision: reuse installed Gradle9.6, cached AGP9.4.1, Kotlin2.2.10, API37 compile
with target36 and min29. Native code awaits measured need. Device build pending.

## Actual device audit (measured ADB metadata, 2026-10-09)

Connected I2501 reports Android16/API36, arm64-v8a and 15,583,752kB MemTotal.
Sensor inventory reports lsm6dsvx accelerometer and vsen_gyro gyroscope,
nominal maximum479.85Hz. These are advertised limits, not measured acquisition
rates. Audio formats, processing, actuator frequencies and synchronization must
be probed through the app. Phone marketed identity is not inferred from a blank
marketname property. See sdk/docs/device-capabilities.md for subsequent probes.

## Physical-state experiment hypothesis

A closed plastic earbud case with vs without earbuds is a controlled first case.
Same case/lid state, table, contact point, phone orientation, power state and audio
route must be maintained. Repeat unchanged captures with repositioning and separate
object/session IDs. State separability is an unverified hypothesis. A single case
cannot demonstrate generalization, calibrated accuracy or hidden-content location.
## Updated sensing and deployment evidence (published sources, 2026-10-09)

Question: does the requested two-actuator/two-speaker probe have a supported Android control path?

Sources: [AudioRecord API](https://developer.android.com/reference/android/media/AudioRecord), [VibratorManager API](https://developer.android.com/reference/android/os/VibratorManager), [CombinedVibration API](https://developer.android.com/reference/android/os/CombinedVibration), [SensorManager API](https://developer.android.com/reference/android/hardware/SensorManager), and [ONNX Runtime mobile deployment](https://onnxruntime.ai/docs/tutorials/mobile/).

Finding: `AudioRecord` requires `RECORD_AUDIO`, pulls samples through `read`, and exposes route/timestamp metadata. `VibratorManager` can address all exposed vibrator IDs, while `CombinedVibration.startParallel()` can schedule explicit effects in parallel. `SensorManager` requires `HIGH_SAMPLING_RATE_SENSORS` above 200 Hz. ONNX Runtime recommends starting quantized models with the CPU execution provider, but that is a deployment starting point, not proof of acceptable task accuracy.

Measured Capybara consequence: the iQOO diagnostic exposes two vibrator IDs (0 and 1), so capture now requests the same finite waveform on both through `CombinedVibration`; independent axis semantics remain unavailable. Capture also requests stereo output with identical left/right excitation. The stereo layout and vibration request are not yet physical transducer activation measurements; loopback and actuator onset remain open experiments.

Decision: preserve actual raw capture, explicit request metadata and honest
limitations. No task-specific physical training corpus exists. A separate CPU
baseline was trained on UCI fan-vibration labels; it is not a Capybara model.
Pretrained transfer must be tested against an unchanged held-out split before
promoting any public-data weights. No model is enabled until task-specific
specialists, held-out calibration, export parity and on-device CPU
memory/latency/decision-drift evidence exist.

## Public datasets and pretrained transfer (2026-10-09)

The [UCI Accelerometer dataset](https://archive.ics.uci.edu/dataset/846/accelerometer)
is CC BY 4.0 and contains 153,000 three-axis measurements of a cooler fan at 50 Hz,
with three blade-weight configurations over 17 speed settings. The official
downloaded ZIP SHA256 was
`00557d031124defb8cebc6ae62cb1833e4cadde546c6c2b1997a8925d92816b0`; extracted
CSV SHA256 was
`296a94eaf40643e1188012a4ee37d0b2c397bbc04decd39cfbe9952b8c841674`.

A small 1D CNN was trained from scratch as a baseline, not as a V6 specialist.
It used non-overlapping 64-sample windows and held out complete speed settings
40%, 70%, and 100% for testing. On 414 test windows it reached 67.63% accuracy
and 65.52% macro-F1 versus 33.33% chance. The test still uses one fan/testbed and
correlated windows; it does not establish object/session generalization, phone
transfer, or package-integrity performance. Weights remain ignored under
`sdk/training/artifacts/`; their SHA256 is
`ea0a70c4f3f0b9ff1aa3f8c2bfb668d687e289a4a50c19d9ddd3393c894766c2`.

The [KAIST rotating-machine dataset](https://data.mendeley.com/datasets/ztmf3m7h5x/6)
is CC BY 4.0. It provides paired acoustic and vibration measurements with machine
health labels; the record lists 45 MB of acoustic data and 2.48 GB of vibration
data. The related [Data in Brief paper](https://doi.org/10.1016/j.dib.2023.109049)
reports synchronized acquisition at 51.2 kHz for acoustic and 25.6 kHz for
vibration, with normal, bearing-fault, misalignment and unbalance conditions.
This is a possible PhysFusion pretraining source, not a phone/container integrity
dataset.

[ReaLISED](https://zenodo.org/records/6488321) contains 2,479 real indoor
recordings at 44.1 kHz with event, action and material metadata. Its Zenodo
record currently leaves the license field blank, so it is excluded from
training intended for redistribution until reuse rights are clarified. In any
case it has no controlled probe, paired IMU, or reference/change labels.

The public [MOMENT-1-small checkpoint](https://huggingface.co/AutonLab/MOMENT-1-small)
is MIT-licensed and exposes pretrained time-series representations and a
classification task. At revision
`411e288267f82cce86296dbe4d6c8bc533cc162f`, its safetensors file is
151,615,328 bytes with 37,901,512 parameters (SHA256
`785e6c6f57ffa7cac7e2a1fff6369618d49f2f441563ddea76c866231e5aa877`). That size
is a likely obstacle for Android; evaluate it as a frozen encoder or teacher and
distill only if a smaller student preserves held-out behavior.

[YAMNet](https://www.tensorflow.org/hub/tutorials/yamnet) is a pretrained
MobileNetV1 classifier for 521 AudioSet event classes, using 16 kHz mono audio.
The tutorial establishes its input/task but not the weight asset's license here;
verify that before reuse. Environmental-event pretraining is not evidence of
active object-response or integrity recognition.

The [Knocker data](https://nmsl.kaist.ac.kr/projects/knocker/) and YCB-impact
archive were publicly reachable, but no dataset license was found in their
download records. Do not train distributable weights from them unless the authors
grant terms. The licensed [ObjectFolder-Real](https://www.objectfolder.org/objectfolder-real-download)
contains controlled impact recordings and contact-force profiles, but its first
10-object audio archive is about 36 GB and has no integrity-state labels.
Public-data use is bounded by source labels, licensing, domain gap, and measured
device validation; it cannot replace human-labeled Capybara captures.

## Pretrained transfer and Laya host evaluation (2026-10-09)

The isolated training environment now runs bundled Python 3.12.14 with
PyTorch 2.14.1+cpu, NumPy 2.2.6, Transformers 4.57.1, `momentfm` 0.1.5, Laya
0.4.1, ONNX 1.23.2 and ONNX Runtime 1.31.0. CUDA is unavailable. These are
observed package versions, not a complete dependency lock.

The [official MOMENT model card](https://huggingface.co/AutonLab/MOMENT-1-small)
and [official repository](https://github.com/moment-timeseries-foundation-model/moment)
support a frozen embedding comparison. On 2,346 UCI fan windows, a frozen
MOMENT-1-small encoder plus train-only normalized linear probe, with fan speeds
40/70/100 held out exactly as the scratch baseline, scored 55.56% accuracy,
52.97% macro-F1 and 0.9220 cross-entropy on 414 test windows. The scratch CNN
scored 67.63%, 65.52% and 0.8002 on the same test split. MOMENT did not transfer
well in this setup; it is not promoted. Both datasets are one fan's
speed-configuration windows, not independent objects or Capybara evidence.

The user selected Laya for the final decision stage. The Apache-2.0 source is
pinned to commit `1adc59f7e371deb601fcfa18a14e25db238addcc` (0.4.1). The official
English base checkpoint is downloaded at revision
`7b928d828b7b0e022f929d9bd2e44165aa270148`; `model.safetensors` is 842,609,210
bytes and SHA256
`891102d372688fc2a094dac56a384bc537b87c63f21f9f3dac0be2b7cbc8d86c`.
[`sdk/docs/laya-integration.md`](sdk/docs/laya-integration.md) records its
input/output contract and limits. The loaded Torch model counted 421,293,827
parameters, three fewer than the upstream README's 421,293,830; this discrepancy
is unresolved.

The pinned upstream exporter generated a 3,306,003-byte ONNX graph and an
external FP32 weight file of 1,685,258,240 bytes. A host ONNX Runtime CPU session
ran the same generic upstream email example as eager CPU and produced the same
`0.879` output. This validates one generic host export/runtime path only. Laya
warned that a base temperature `0.1005828` was clamped to `0.5`; confidence is
uncalibrated. The 1.69 GB FP32 export is not packaged or used by Android, and
there are no physical labels, five trained specialists, trained Capybara-Laya
weights, phone parity/resource measurements or integrity metrics.

The UCI evaluation implementation is `sdk/training/evaluate_moment_uci.py`; its
small contract tests are `sdk/training/test_evaluate_moment_uci.py`. The reports,
weights, checkpoints and captures remain ignored under training artifacts.

## Jev substitution check (2026-10-09)

The user asked whether TypeSafe Jev could replace Laya as the final decision
model. TypeSafe's [API reference](https://docs.typesafe.ai/api) documents a
hosted `POST https://api.typesafe.ai/v1/systemone` endpoint authenticated by a
bearer API key. Its [model page](https://docs.typesafe.ai/models) says Jev accepts
text-only state, does not accept audio, and is not fine-tuned per customer; the
same model weights serve every account. The [first-party announcement](https://typesafe.ai/blog/introducing-system-one-models-and-jev)
positions Jev as a structured-decision model.

The official material reviewed documents API access; it does not provide a
downloadable Jev checkpoint or an offline runtime. Even if derived capture
features were serialized into text/JSON, inference would require network access
and send derived physical evidence to a hosted service. This conflicts with the
Capybara offline/no-cloud runtime invariant. Jev also has no demonstrated
phone-to-case integrity evaluation here and cannot replace the five physical
specialists, ReferenceGuard, or human-labeled object/session-separated data.
Decision: do not integrate Jev into the SDK/app; retain it only as a possible
non-production research comparator if the product's offline constraint changes.

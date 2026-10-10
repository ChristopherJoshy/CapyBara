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
is CC BY 4.0, authored by Jung, Kim, Yun, Bae and Park (published 8 February
2023). The linked [Data in Brief paper](https://pmc.ncbi.nlm.nih.gov/articles/PMC10036499/)
reports four industrial piezoelectric accelerometers at 25.6 kHz and one nearby
microphone at 51.2 kHz. Acoustic recordings cover five no-load machine states;
the vibration files cover more fault/load conditions. These are machine-fault
labels on an industrial testbed, not smartphone active-probe measurements or
object/session-separated package-state labels. License permits reuse, but the
task and sensor mismatch make it unsuitable for training V6 specialists.

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

The [Knocker data](https://nmsl.kaist.ac.kr/projects/knocker/) and
[YCB-impact OSF record](https://api.osf.io/v2/nodes/4tcp6/) are publicly
reachable but provide no dataset license. YCB reports `node_license: null`.
Do not use either dataset for training or distribution without explicit terms.

The [ObjectFolder-Real download page](https://www.objectfolder.org/objectfolder-real-download)
describes real-object impacts, force profiles and tactile captures, but states no
data license. The separate [ObjectFolder 2.0 download page](https://www.objectfolder.org/objectfolder2-0-download)
says ObjectFolder is CC BY 4.0; that does not establish terms for the Real archives.
Correction: ObjectFolder-Real is not verified as licensed. Do not download or
train on those recordings until their data terms are explicit.

Closest physical-audio leads audited 2026-10-10; none is currently usable:
- [FillImpact](https://arxiv.org/abs/2607.17773) is described as over 5,000
  48 kHz impact recordings from 88 real containers with hollow, water, rice and
  fill-level labels. The paper gives no dataset download URL or data license.
  Its DJI Mic/physical-striker setup also differs from Capybara's phone probe.
- [SonicSense](https://proceedings.mlr.press/v270/liu25h.html) studies container
  inventory and object properties with contact microphones. Its
  [Apache-2.0 code repository](https://github.com/generalroboticslab/SonicSense)
  links the recordings separately on Google Drive but states no dataset license.
- [YCB-impact](https://api.osf.io/v2/nodes/4tcp6/) has `node_license: null`.
  [RealImpact](https://github.com/samuel-clarke/RealImpact) publishes a separate
  MIT-licensed code repository and data download script, but no data license was
  found. Both are controlled impact-audio datasets without integrity-state
  labels and do not match the phone active-probe setup.

- [VibeCheck](https://arxiv.org/abs/2504.15535) is a closer active-sensing
  precedent: it transmits a 20 Hz–20 kHz sweep through a gripped object using a
  piezo emitter and receives it with a contact microphone; its tasks include
  object, internal-pose and contact-state inference with session-separated
  test sets. The transducers are mechanically coupled to the object, unlike
  Capybara's phone air-speaker/microphone path. The paper and
  [project page](https://roamlab.github.io/vibecheck/) provide no dataset
  download. The [code repository](https://github.com/roamlab/acoustic-sensing-public)
  has no dataset license; its Apache-2.0 file is nested under
  `src/keyboard_control`. Do not reuse its data or model assets.

- [CORSMAL Containers Manipulation](https://corsmal.github.io/containers_manip.html)
  has 1,140 recordings of 15 containers and 12 people, with content and fill-level
  labels and a 9/3/3 container-separated train/public-test/private-test split.
  Audio is captured by an eight-microphone array at 44.1 kHz during human pouring
  or shaking, not Capybara's active phone speaker/microphone probe. The dataset
  is CC BY-NC 4.0 (official [data record](https://doi.org/10.17636/101CORSMAL1)).
  Its [ACC pretrained weights](https://doi.org/10.5281/zenodo.4770061) are
  separately listed as CC BY 4.0 and trained on the dataset's train split, but
  the TensorFlow classifier uses the mismatched dataset/audio setup. No data or
  weights were downloaded, evaluated, or integrated.
- The [EPFL object-manipulation audio/tactile dataset](https://doi.org/10.5281/zenodo.6372438)
  is CC BY 4.0 and contains 300 robot trials for five contents and two motions.
  It uses a hand-mounted microphone, tactile skin and robot-joint data on one
  plastic container; collection dates vary by condition. It has no object-held-out
  split and no phone-generated active probe, so it is not V6 training evidence.
- The [coin-tap defect dataset](https://github.com/PPhub-hy/torch-tapnet), described
  in the [paper](https://arxiv.org/abs/2203.12594), has 101,070 labeled 48 kHz
  samples from nine bonded-plate specimens and seven normal/defect classes. It
  uses an electromagnet striker and microphone; the paper's three-fold split
  randomly partitions samples rather than specimens. The repository README links
  a Baidu download but states no dataset license. The paper's arXiv license does
  not establish dataset reuse rights; do not download or train on it.
- [LMSD 2021 plate-damage data](https://doi.org/10.48804/GDE9TW) is CC BY 4.0
  and contains measured frequency-response functions from one CFRP plate. An
  impact hammer excites the plate and seven mounted accelerometers receive its
  response; six added-mass cases are damage surrogates. The files are averaged
  8,192-bin FRFs through 1,600 Hz, not phone audio/IMU time series or
  object-separated measurements, so they cannot train a Capybara specialist.
- [Acoustic Emission monitoring in CFRP compression tests](https://doi.org/10.4121/21621381.v1)
  is CC0 and provides full waveforms plus damage-mode labels from CFRP compression
  and impact tests. Its contact-coupled piezoelectric sensors operate at
  100–900 kHz and record at 2 MHz; this is outside the phone microphone/IMU and
  active-probe path. It is not V6 training evidence.
- The [TU Wien MSDv1 record](https://researchdata.tuwien.ac.at/records/hv20e-zzb35)
  is a smartphone/laptop manufacturing work-step sound dataset, not the composite
  tap-test defect dataset suggested by a search result; it was discarded.

These are task-adjacent leads only. Their different excitation/contact geometry,
limited object coverage, or licensing do not support a Capybara model or product
deployment. No candidate was downloaded, evaluated, trained, or integrated.
Public-data use remains bounded by source labels, artifact-specific licensing,
domain gap, and measured device validation; it cannot replace human-labeled
Capybara captures.

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

## Generic showcase and training execution status (2026-10-09)

Question: can the judge-facing flow accept arbitrary boxes/objects without
silently claiming a universal integrity model?

Measured implementation decision: keep `rigid_case` A–D as the controlled
research protocol, and add a separate `showcase_object` acquisition profile
with `SIDE_1` and `SIDE_2`. The Compose flow records real stereo audio requests
and all exposed vibrator IDs on both sides, asks the user to approve/retry/
reject each capture, and persists an encrypted uncalibrated research reference
only after both registered sides are complete. Its provenance digest is not a
physical identity signature.

Training decision: local CPU training remains the only completed training path.
The Kaggle CLI is installed and masked credentials are available, but no cloud
job was launched because no authorized, labeled Capybara physical dataset
exists. Public UCI/MOMENT results remain transfer baselines only; using Kaggle
with unrelated public labels would not train a valid Capybara specialist.

## Licensed-data audit and training decision (2026-10-10)

Question: can another public dataset support a Capybara specialist or Laya
fine-tune without treating unrelated labels as object-integrity evidence?

The [smartphone vibration changepoint dataset](https://github.com/witulska/vibration-changepoint-dataset)
is explicitly CC BY 4.0 ([license](https://github.com/witulska/vibration-changepoint-dataset/blob/main/LICENSE)).
Its nine recordings (one person, motorbike and car) have observer-declared
variance-change intervals and phone inertial channels nominally sampled at up to
100 Hz. It has no microphone, active excitation, object state, or repeated
object-integrity condition; it is not a V6 training set.

[ORION-AE](https://doi.org/10.7910/DVN/FBRDU0) is CC0 1.0 according to its
[Harvard Dataverse record](https://dataverse.harvard.edu/api/datasets/:persistentId/?persistentId=doi:10.7910/DVN/FBRDU0).
It measures bolt-loosening states with attached acoustic-emission sensors and a
laser vibrometer at 5 MHz under a 100 Hz shaker. The integrity-like labels are
valuable structural-monitoring evidence, but the sensors and bandwidth are
incompatible with the phone's microphone/IMU capture path.

Kaggle CLI 2.2.4 authentication, account-owned kernel listing, quota, and kernel
metadata syntax were checked on 2026-10-10. The exact Mendeley DOI search found
no Kaggle dataset mirror. Quota reported 0/30 GPU-hours and 0/20 TPU-hours used;
quota does not prove a GPU can be allocated, and no allocation was tested. No
Kaggle kernel, remote job, or dataset upload was created.

Decision: do not train or quantize a model from these mismatched labels. The
existing UCI fan classifier remains a local transfer baseline only. Train the
V6 specialists and fine-tune Laya only after real iQOO captures have profile
labels, independent object/session/run splits, and specialist evidence.

## Compose motion and truthfulness (official Android sources, observed 2026-10-10)

Question: which Compose motion APIs can polish the showcase without implying
progress or a successful physical-integrity decision?

Sources: [Value-based animations](https://developer.android.com/develop/ui/compose/animation/value-based),
Android Developers, undated (observed 2026-10-10); [Compose Animation release
notes](https://developer.android.com/jetpack/androidx/releases/compose-animation),
version 1.2.0 (27 July 2022). The official guide documents state-driven
`animateFloatAsState` and repeating transitions; the release notes state that
Compose animation honors the Android Animator duration-scale setting.
`androidx.compose.animation:animation` resolved to 1.12.1 under BOM 2026.09.00
in the app's `debugRuntimeClasspath`.

Decision: keep startup and result transitions finite; show a non-waveform pulse
only while the real SDK operation is busy, alongside the reported capture stage.
The success check appears only after reference persistence; a scan with missing
trained/calibrated comparison remains `INCONCLUSIVE`, with no match/not-found
animation. The UI installed and rendered on iQOO; this pass did not exercise a
physical capture, so the active pulse and result animation were not observed on
hardware.

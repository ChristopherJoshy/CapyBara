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

## Prior handset audit (measured ADB metadata, 2026-10-09)

The earlier unit, serial `10BFAT1UT2000XP`, reported I2501, Android 16/API 36,
arm64-v8a and 15,583,752 kB `MemTotal`. Its sensor inventory listed an
LSM6DSVX accelerometer and vsen_gyro gyroscope with nominal maxima of 479.85 Hz.
Those are advertised limits, not measured acquisition rates. The market-name
field was blank, so its brand was not inferred. This is historical evidence for
the previous unit, not the currently connected phone.

## New iQOO hardware inventory (ADB and SDK capability report, 2026-10-10)

The current serial `10BFBH1BH0001EY` reports manufacturer `vivo`, model `I2501`,
Android 16/API 36 and ABI `arm64-v8a`. Its build fingerprint starts
`iQOO/I2501i/I2501:16/`, verifying the iQOO identity.

The installed Capybara technical-details view reports
`unprocessedReported=true`, output rate 48,000 Hz and 144 output frames. The
audio-device inventory lists a built-in speaker (type 2, ID 3) at 48 kHz with
two channels and built-in microphone endpoints (type 15) advertising 48 kHz
with one- and two-channel support. These are advertised formats, not proof of
actual stream routing or transducer frequency response.

Android's sensor inventory lists an LSM6DSVX accelerometer and vsen_gyro
gyroscope, each with a nominal maximum of 479.85 Hz. `VibratorManager` exposes
IDs 0 and 1; both report present with amplitude control, Q factor 11.0,
resonant frequency `NaN`, unsupported envelope effects and no frequency
profile. The existing capture path requests the same finite waveform on both
IDs in parallel; no capture was run to verify either motor's physical onset.
Advertised sensor rates are not achieved capture rates.

The fresh app installation renders its status as microphone permission needed,
zero captures and zero baselines. No permission was granted and no physical
capture, speaker check or haptic check was performed on this handset. Treat
this as capability/readiness evidence only; the rigid-case pilot still needs
user-prepared physical conditions and labeled repeats.

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

[ReaLISED](https://zenodo.org/records/6488321) provides event, action and
source-material labels. Its README describes 44.1 kHz stereo; the downloaded
archive passed the published MD5 and all 2,479 FLAC files decoded as 44.1 kHz
mono with SoundFile 0.13.1.
Its canonical [Zenodo record API](https://zenodo.org/api/records/6488321)
declares CC BY 4.0 and lists a 280,551,607-byte archive (MD5
`fd75c906a022a2c82890547152867504`; local SHA-256
`e1f3b897b94cbd60e00ed78a72ae4c9d436a7edc84293e3f81795536f3d6dbd2`). The
recordings use external Olympus
recorders 30–40 cm from sources, not Capybara's active phone probe; they have no
paired phone IMU, reference/change labels or object/session identifiers. The
implemented classifier trains only the dataset's indoor-event task; any use is
auxiliary audio research, not V6 integrity training or object-separated
validation. Keep the archive out of Git under repository data rules; preserve
the license attribution in any derived use.

The associated [Electronics paper](https://doi.org/10.3390/electronics11121811)
frames it as sound-event classification data, consistent with its filename labels.

The full-dataset local CPU pipeline smoke (seed 20261010, one epoch, event-ID
groups) covered 1,735 train, 372 validation and 372 test clips. Test accuracy
was 32.80% and macro-F1 26.82%; this one-epoch smoke validates decoding,
partitioning and training execution, not a selected sound-event model or physical
integrity performance. The private Kaggle CUDA run (version 2, 25 epochs,
selected epoch 21, 400.7 seconds) scored 75.27% accuracy and 72.07% macro-F1 on
372 held-out event-ID clips; validation was 76.88% / 74.13%. Model SHA256:
`6c8986389426b6812935fba171953be14ef6bbdd9385403b52c59f9c7d8af867`. The split
is not object/session separated, and these metrics do not measure Capybara
integrity.

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

Closest public-data leads audited 2026-10-10; none matches Capybara's iQOO
speaker-plus-haptic acquisition and generic enrolled-object integrity task:
- [The Boombox](https://proceedings.mlr.press/v164/chen22c.html), Chen et al.,
  CoRL 2022, reconstructs contents from vibrations recorded by contact
  microphones on boxes. The authors publish audio/video data for three box
  shapes through their [project page](https://boombox.cs.columbia.edu/) and
  [code repository](https://github.com/BoyuanChen/boombox). The code is MIT,
  but no separate dataset license is stated. It is the closest hidden-content
  task, but do not train on or redistribute its data until data rights are
  explicit; it uses attached contact microphones, not a phone's speakers or
  haptics.
- The [EPFL object-manipulation audio/tactile dataset](https://doi.org/10.5281/zenodo.6372438)
  (Prabhakar, Billard and Reber, 2021-10-09; official
  [Zenodo record](https://zenodo.org/api/records/6372438)) is CC BY 4.0. It
  contains 300 robot trials, five contents (empty, vitamins, gummies, cornflakes,
  rice), two motions, microphone audio, tactile force/pressure, and joint data
  for one plastic container. This is a legally reusable auxiliary content-state
  benchmark, not phone-probe evidence. There is no held-out object; trials are
  grouped by three collection dates, with some motion/content combinations
  confined to particular dates. Random trial splitting risks session confounds.
- ROS ingestion uses `rosbags==0.11.6` (Apache-2.0, Python >=3.10;
  [PyPI metadata](https://pypi.org/project/rosbags/0.11.6/)). Its
  [AnyReader API](https://ternaris.gitlab.io/rosbags/topics/highlevel.html)
  auto-registers embedded message definitions, allowing this legacy bag format
  to be read without a ROS installation. This parses the public dataset; it adds
  no Capybara sensor evidence.
- [CORSMAL Containers Manipulation](https://corsmal.github.io/containers_manip.html)
  has 1,140 recordings of 15 containers and 12 people, with content and fill-level
  labels and a 9/3/3 container-separated train/public-test/private-test split.
  Audio is captured by an eight-microphone array at 44.1 kHz during human pouring
  or shaking, not Capybara's active phone speaker/microphone probe. The dataset
  is CC BY-NC 4.0 (official [data record](https://doi.org/10.17636/101CORSMAL1)).
  Its [ACC pretrained weights](https://doi.org/10.5281/zenodo.4770061) are
  separately listed as CC BY 4.0; that separate license does not change the
  dataset license. Neither the weights nor dataset were evaluated or integrated.
- [Knocker](https://miil.kaist.ac.kr/projects/knocker/) publishes smartphone
  knock-response data from 20 in-lab users and 10 in-the-wild users, using a
  Pixel 2 microphone (48 kHz), accelerometer and gyroscope (400 Hz); each row
  contains 4,096 audio and 32 samples per inertial axis. Its 23 main and 12
  similar-object labels identify objects, not changes in a known object's
  contents or physical state. The official [paper](https://doi.org/10.1145/3351240)
  evaluates impact-driven knocks, not Capybara's active speaker/haptic probe.
  The archive was downloaded only to ignored
  `sdk/training/artifacts/KnockerDataset.zip` to inspect its README; the archive
  provides no dataset license, so it is not trained on or redistributed. Local
  archive SHA256:
  `9f9f190612786b5631b9acca0c206c8e18debbc6fbb8b3f4956266cb4b208847`.
- [YCB-impact](https://api.osf.io/v2/nodes/4tcp6/) has no explicit dataset
  license (`node_license: null`) and uses controlled impact audio. The paper
  license does not establish dataset reuse rights; do not train on it.
- [ObjectFolder-Real](https://objectfolder.stanford.edu/objectfolder-real-download)
  describes real-object impacts, force profiles and tactile captures, but states
  no data license. The separate [ObjectFolder 2.0 download page](https://www.objectfolder.org/objectfolder2-0-download)
  says ObjectFolder is CC BY 4.0; that does not establish terms for the Real
  archives. [RealImpact](https://github.com/samuel-clarke/RealImpact) publishes
  MIT-licensed code and a data download script, but no dataset license was
  found. Do not treat code licenses as dataset licenses.
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
deployment. The initial audit preceded the EPFL-only experiment below. No other
candidate dataset or weights were downloaded, evaluated, trained, or integrated.
Public-data use remains bounded by source labels, artifact-specific licensing,
domain gap, and measured device validation; it cannot replace human-labeled
Capybara captures.

## EPFL source-domain content experiment (2026-10-10)

The official [Zenodo record](https://zenodo.org/api/records/6372438) declares
CC BY 4.0. The 795,903,546-byte archive was downloaded to ignored artifacts;
its MD5 (`60ac6f12924764dc813977c053934c98`), SHA-256
(`ec534a68ced0e36926b901af17aa10408805a3d57f115a086100acc37d88e4e2`), and ZIP
CRC check passed. The selected ROS1 bags expose 16 kHz mono MP3 audio and
robot-mounted Tekscan pressure frames (2,944 values per frame). `rosbags==0.11.6`
reads the embedded message definitions without ROS installed; its POSIX
message-type path handling needs a `PurePosixPath` compatibility substitution
on Windows.

The separate `sdk/training/train_epfl_mml_source.py` CNN uses only the three
contents present on both selected dates: empty, gummies and rice. Its split is
72 training and 18 validation trials from 2021-08-25, then 90 test trials from
2021-08-26. The test date also changes the manipulation motion, so the two
effects are confounded; no object is held out. Training, validation, and test
each scored 33.33% accuracy and 16.67% macro-F1, equal to one-in-three chance.
The classifier predicted `empty` for every trial, including all 72 training
trials, so it failed to learn this source task and is rejected. This is not an
integrity result, phone transfer, fine-tuning of Capybara, or venue-collected
data.

The host's libmpg123 decoder emitted repeated `part2_3_length` errors while
returning waveform arrays. Audio continuity was not verified, further limiting
the experiment. Keep the archive, extracted bags, checkpoint, and generated
report under ignored `sdk/training/artifacts/`; no dataset or model is shipped.

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

## Adjacent public acoustic and vibration datasets (2026-10-10)

Question: does a public dataset provide both reusable rights and task/sensor
alignment strong enough to train a Capybara specialist before iQOO captures?

The Mendeley Data record for [Annotated Impact Echo Dataset (bare
decks)](https://data.mendeley.com/datasets/44rb96872r/1) (Sattar Dorafshan and
Hoda Azari; published 19 February 2020; DOI
`10.17632/44rb96872r.1`) reports 2,016 impact-echo signals from eight
laboratory-made concrete specimens, labeled sound or defected, under CC BY 4.0.
These are task-relevant concrete defect labels, but the record does not establish
phone microphone/IMU capture, phone-generated excitation, or transfer to rigid
consumer objects. The data archive was not downloaded; do not treat it as V6
training data.

The [smartphone-based induction-motor vibration
dataset](https://data.mendeley.com/datasets/rs4vz8n3t5/1) (Govind Vashishtha,
Merve Ertarğın, Ahmet Orhan, Özal Yıldırım and Sumika Chauhan; published
2 January 2026; DOI `10.17632/rs4vz8n3t5.1`) reports six motor fault states,
three speeds and two load conditions from an iPhone 15 Pro Max attached to a
motor housing; tri-axial acceleration is sampled at 100 Hz. The record states
CC BY 4.0 and provides CSV/MAT files. Its phone IMU and fault labels are closer
than laboratory-only vibration sets, but the attached-phone rotating-motor
domain has no acoustic/haptic phone excitation or separate consumer-object
identity. It cannot establish general physical-integrity decisions; no data was
downloaded or model trained.

The [bare-finger tactile/audio/visual dataset](https://doi.org/10.6084/m9.figshare.26965267.v1)
(Devillard et al., *Scientific Data* 12, 484; published 23 March 2025; DOI
`10.1038/s41597-025-04670-0`) is public CC BY 4.0. Its paper describes ten
textured surfaces, ten participants, directional microphones and accelerometers
mounted to the finger; the Figshare record lists 20.9 GB of files. The labels
identify textures and interaction trials, not object integrity; contact
microphones/skin-mounted accelerometers during sliding are not the phone's
active speaker-to-object response. Access and files were not downloaded.

The IEEE DataPort [VibTac-12 record](https://doi.org/10.21227/kwsy-x398)
(Kursun and Patooghy, 2020) describes a rotating texture drum, probe-mounted
accelerometer at 200 Hz and microphone at 8 kHz, with 12 texture classes and one
20-second recording per class. Files require an IEEE account, and the record
does not state a reuse license. Both licensing and sample-level independence
are unresolved; do not download or train from it.

Decision: none of these records authorizes training a Capybara physical-integrity
model. Impact-echo has relevant defect labels but unverified device/sensor
transfer; the smartphone motor set has relevant phone IMU but a single
application domain and no active excitation; the tactile datasets classify
surfaces under different contact mechanics, and VibTac-12's rights are
unresolved. Keep them as documented research leads only. Wait for labeled,
object/session-separated Capybara measurements before specialist or Laya
training.

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

## Audio route and excitation provenance (official Android API, 2026-10-10)

Question: does a preferred audio device prove which route actually carried
Capybara's excitation, and can current captures establish that the haptic motor
physically activated?

Sources: Android [AudioRouting API](https://developer.android.com/reference/android/media/AudioRouting#getRoutedDevice()),
updated 2026-08-03, says `getRoutedDevice()` identifies current AudioTrack/AudioRecord
routing, is valid only while playback/recording is active, and may return null
otherwise; `setPreferredDevice()` only specifies a preferred route. Android
[AudioDeviceInfo API](https://developer.android.com/reference/android/media/AudioDeviceInfo#TYPE_BUILTIN_SPEAKER_SAFE)
defines built-in microphone and speaker-system route types. Its safe speaker
type may be the same physical speaker system driven differently for the safety
use case. These APIs are available above the SDK's minSdk 29.

Finding and decision: `CaptureEngine` already requested the built-in mic/speaker
and recorded route events, but `PhysicsEngine` did not require a confirmed route.
It now records routed device types while streams are active, requires the built-in
microphone, and accepts either built-in speaker-system route type. It also reports
limitations when no haptic waveform request exists or physical haptic onset is
unmeasured. Android's route type does not prove both transducers emitted sound;
that requires hardware evidence.

Capybara verification: `:sdk:testDebugUnitTest :sdk:lintDebug` passed after the
change. No iQOO routing or haptic response was measured in that pass; device
testing was paused then. The current acquisition's `hapticPhysicalOnset` remains
`unavailable`, so these captures are not demonstrated as verified multimodal
excitation.

## Current iQOO identity and showcase UI smoke (2026-10-10)

Question: is the connected target actually the specified iQOO 15, and does the
current APK expose the intended home and two-side Scan entry on that device?

Sources: serial-scoped `adb devices -l` and `getprop` observations reported
manufacturer `vivo`, brand `iQOO`, model `I2501`, Android 16/API 36 and ABI
`arm64-v8a`. Android's [`Build`](https://developer.android.com/reference/android/os/Build)
API documents model, manufacturer, SDK and supported ABI properties; the ADB
property values are direct device observations.

Finding: the debug APK installed successfully to the verified target. The
Capybara-only instrumentation command
`:apk:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.capybara.app.ShowcaseTest`
passed both `scanRequiresAReferenceBeforeCapture` and
`conciseShowcaseKeepsStatusInMenuAndRestoresSetup` (2 tests, 0 failures, device
`I2501 - 16`). This verifies home/menu state, the no-reference Scan gate, and
navigation into the four-position setup.

Limitations and decision: no object capture, route inspection, speaker-output
measurement, haptic-onset measurement, model inference, or physical data
collection was performed. The test confirms UI navigation only. Continue to
request genuine, object-separated labeled recordings before training; do not
interpret the generic showcase as validated generalization to arbitrary objects.

## Combined-vibrator cancellation

Android documents `VibratorManager.cancel()` as turning all vibrators off; the method is available from API 31: https://developer.android.com/reference/android/os/VibratorManager#cancel().

`CaptureEngine` can start one `CombinedVibration` across all exposed vibrator IDs, but cleanup previously cancelled only the default `Vibrator`. Cleanup now uses `VibratorManager.cancel()` on API 31+ and retains default-vibrator cancellation on older releases. No device was connected during this host check, so physical haptic cancellation remains unverified.

## Real iQOO audio, IMU, and vibrator diagnostic (2026-10-10)

Question: do the authorized I2501's Android APIs deliver a non-silent capture
while the SDK requests stereo speaker excitation and synchronized output to
every exposed vibrator?

Sources: direct serial-scoped `getprop`, `dumpsys sensorservice`,
`dumpsys vibrator_manager`, and the `HardwareCaptureTest` runtime report;
Android [`AudioDeviceInfo`](https://developer.android.com/reference/android/media/AudioDeviceInfo),
[`Sensor`](https://developer.android.com/reference/android/hardware/Sensor),
and [`VibratorManager`](https://developer.android.com/reference/android/os/VibratorManager)
APIs; SDK acquisition in `sdk/src/main/kotlin/dev/capybara/sdk/CaptureEngine.kt`.

Measured result: manufacturer/brand/model `vivo` / `iQOO` / `I2501`, Android
16/API 36, `arm64-v8a`. `HardwareCaptureTest.realCaptureAndCancellationReleaseHardware`
passed. Three logged runs were labeled hardware diagnostics; contact condition
was not controlled or recorded. Each measured 48 kHz mono `UNPROCESSED`
microphone input (route type 15), 288,000 samples, 12 audio timestamp anchors,
and no audio silencing. Run A: RMS 0.00467869, baseline noise RMS 0.00090969,
zero clipped samples, accelerometer/gyroscope 476.9476 Hz, capture 6,384.85 ms,
and `PhysicsEngine` analysis 860.74 ms. Run B: RMS 0.00443769, noise RMS
0.00236456, zero clipped samples, accelerometer/gyroscope 476.9486 Hz, capture
6,230.07 ms, and `PhysicsEngine` analysis 121.26 ms. Run C: RMS 0.00319538,
noise RMS 0.00104014, zero clipped samples, accelerometer/gyroscope 476.9272
Hz, capture 6,251.34 ms, and `PhysicsEngine` analysis 120.57 ms. No neural
inference ran.

`VibratorManager` listed IDs `0,1`. The SDK requested a synchronized finite
waveform on both; `dumpsys vibrator_manager` logged the completed combined
request with both IDs. Android advertised amplitude/frequency-control flags and
a legacy 140–160 Hz profile, but its current frequency-profile data were
`NaN`/null and envelope support size was zero. The capture still records
`hapticPhysicalOnset=unavailable`; a service-accepted command is not a
measurement of mechanical response.

The built-in speaker-system route and stereo channel request do not identify
separate physical speaker drivers or prove that each driver emitted. The
internal capture does not establish phone-on-object coupling, repeatability,
or useful integrity separability. Audio sample-loss counters are unavailable.
This diagnostic is not labeled training/evaluation data, no neural model ran,
and no accuracy or AI-inference result is claimed.

The full serial-scoped Gradle verification on this device passed APK 3/3 and SDK
5/5 tests, including unit tests, lint, assembly, and both connected test suites.
After the dashboard-navigation change, `:apk:connectedDebugAndroidTest` passed
3/3 tests on the same serial; this includes one hardware-capture/cancellation
test and two showcase UI tests. The updated APK installed and launched. Live
visual inspection covered Home, the no-reference Scan gate, and the two-side
enrollment prompt; startup and capture/result animations were not inspected.
No controlled-object enrollment, stored-reference evaluation, or physical
state decision was performed.

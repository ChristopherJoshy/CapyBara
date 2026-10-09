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

[ReaLISED](https://zenodo.org/records/6488321) is CC BY 4.0 and contains 2,479
real indoor recordings at 44.1 kHz with event, action and material metadata.
It can support generic acoustic-event pretraining, but has no controlled probe,
paired IMU, or reference/change labels.

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

# Device capability record

**Status (2026-10-10):** Serial-scoped checks confirmed `vivo` / `iQOO` /
A real, unlabeled hardware diagnostic capture exercised the microphone and
current Android speaker-system route, measured IMU rates, and requested output
on both exposed vibrator IDs. Two UI tests and five SDK instrumentation tests
also passed. The capture condition was not controlled or recorded; no claim of
object separability, separate physical speaker drivers, or calibrated haptic
onset follows from these checks.

## What the SDK queries (API surface, not device findings)

`Capabilities.kt` reads `Build.MANUFACTURER`, `Build.MODEL`, API level and supported ABIs; enumerates sensors and their advertised timing/range/resolution/FIFO fields; reads Android audio properties and enumerated audio devices; and queries vibrator presence, exposed IDs, amplitude control, resonance, and API 36 envelope/profile support. `CaptureEngine` requests 48 kHz mono PCM16 input, stereo output to the built-in speaker route, a finite chirp, and a finite waveform on exposed haptic actuators where available. It records actual Android routes while the streams are active. `PhysicsEngine` marks missing/non-built-in routes, absent haptic requests, and unmeasured physical haptic onset as limitations.

The diagnostic below records two logged capture runs. Android exposes one
built-in speaker-system route, not a mapping from output channels to separate
transducers. Physical two-driver emission, haptic acceleration/onset, and
phone-on-object response remain unverified.

## Measured device diagnostic (2026-10-10)

`HardwareCaptureTest.realCaptureAndCancellationReleaseHardware` passed on the
authorized device. Capture condition was not controlled or recorded. The test
labels the run as a hardware diagnostic, not object/state data, training data,
or integrity evidence.

| Observation | Measured result |
| --- | --- |
| AudioRecord input | Both runs: 48,000 Hz mono, 288,000 samples (6 seconds), `UNPROCESSED`, input route type 15 (built-in microphone), `audioSilencedEver=false`, 12 timestamp anchors |
| AudioTrack output | Identical chirp samples requested on left and right stereo channels; active route type 2 (built-in speaker system). The API does not expose the number or channel mapping of physical drivers. |
| Captured signal quality | Run A: RMS 0.00467869, noise RMS 0.00090969; run B: RMS 0.00443769, noise RMS 0.00236456. Both had 0 clipped samples. |
| IMU | Run A: accelerometer/gyroscope 476.9476 Hz; run B: 476.9486 Hz. These are two observed rates, not a stability guarantee. |
| Timing | Run A capture 6,384.85 ms / Physics Engine 860.74 ms; run B 6,230.07 ms / 121.26 ms. No neural inference ran; this is not an AI latency result. |
| Haptic request | `VibratorManager` IDs `0,1` received the synchronized finite waveform request; the system logged completion for both IDs. Physical onset/amplitude at the phone-object interface was not measured. |

Both vibrator services advertise amplitude/frequency-control flags and a legacy
140–160 Hz profile (150 Hz resonance, 1 Hz resolution), but the modern frequency
profile contains `NaN`/null values and the supported envelope size is zero.
Do not treat the legacy profile or capability flags as validated frequency
control. The actual capture reports haptic onset as unavailable.

The system log confirms that Android accepted and completed the combined
two-vibrator request. It does not measure haptic acceleration or establish a
physical response in a contacted object. Likewise, the stereo stream and
built-in speaker route prove channel/route requests, not separate speaker-driver
emission. Keep both limitations explicit until independently measured.

The diagnostic stores its raw capture privately on-device; no raw samples or
diagnostic artifacts were uploaded. `AudioRecord` exposes no reliable overrun
counter in this pipeline, so sample loss remains unknown.

## Earlier Android capability inventory (2026-10-10)

This table records the initial API inventory before the real capture above.
Its values are not additional physical measurements.

### Initial iQOO API report (2026-10-10)

| Capability | Recorded result |
| --- | --- |
| Manufacturer/model, Android release/API, ABI | `vivo` / `I2501`, Android 16 / API 36, `arm64-v8a`; fingerprint prefix `iQOO/I2501i/I2501:16/` |
| Memory | Not recorded |
| Accelerometer/gyroscope | LSM6DSVX / vsen_gyro; each advertised nominal maximum 479.85 Hz. Achieved capture rates not measured. |
| Microphone | Built-in type 15 advertised 48 kHz, one- and two-channel support; unprocessed source was reported supported. Actual routed capture format and processing not validated. |
| Speaker | Built-in type 2, ID 3 advertised 48 kHz, two channels; output properties reported 48,000 Hz / 144 frames. Actual active route and physical transducer output not validated. |
| Haptics | IDs 0 and 1 reported present with amplitude control; Q factor 11.0, resonant frequency `NaN`, envelope effects unsupported, no frequency profile. |
| Physical haptic onset and IMU response | Not measured; capture metadata reports onset unavailable. |
| End-to-end capture, synchronization, repeatability, offline behavior | No physical pilot or accepted capture. One unlabelled acquisition reached review and was rejected; no integrity result. |

The earlier API inventory does not establish separate physical-speaker output,
haptic onset, or useful object response. Current routed capture results are
reported above. Android's built-in-speaker route type describes a speaker
system, not a physical driver count or left/right transducer mapping.

## Required device pass

For the next physical data pass, use only the verified iQOO serial and
Capybara-owned packages. Measure phone-on-object response, repeatability,
contact quality, haptic onset/acceleration, audio sample loss, and separate
speaker-driver emission only if a suitable independent measurement method is
available. Keep `AudioDeviceInfo` route types distinct from proof of separate
speaker drivers. Preserve unavailable values; do not infer support from
marketing material, advertised API fields, or emulator results.

Published Android API behavior and implementation decisions are documented in `hardware-research.md` and `research.md`.

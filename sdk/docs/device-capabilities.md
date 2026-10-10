# Device capability record

**Status (2026-10-10):** A current ADB identity check confirmed `vivo` / `iQOO` /
`I2501`, Android 16/API 36, `arm64-v8a`. The Capybara-only UI smoke passed; it
did not capture an object or verify physical audio/haptic output. The historical
capability inventory below is not current transducer validation.

## What the SDK queries (API surface, not device findings)

`Capabilities.kt` reads `Build.MANUFACTURER`, `Build.MODEL`, API level and supported ABIs; enumerates sensors and their advertised timing/range/resolution/FIFO fields; reads Android audio properties and enumerated audio devices; and queries vibrator presence, exposed IDs, amplitude control, resonance, and API 36 envelope/profile support. `CaptureEngine` requests 48 kHz mono PCM16 input, stereo output to the built-in speaker route, a finite chirp, and a finite waveform on exposed haptic actuators where available. It records actual Android routes while the streams are active. `PhysicsEngine` marks missing/non-built-in routes, absent haptic requests, and unmeasured physical haptic onset as limitations.

The SDK's queries and requests describe Android-reported properties, not physical response or current device state. The historical report below preserves the advertised values and observed identity from its dated inventory; actual routed capture, physical speaker emission, haptic onset, and useful sensor response remain unverified.

## Historical iQOO report (2026-10-10; capability inventory only)

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

These historical API-reported values do not establish current availability, achieved rates, active routing, audio bandwidth, two-speaker emission, haptic activation, or useful sensor response. Android's built-in-speaker route type can describe a mono or stereo speaker system; route metadata cannot prove both physical transducers emitted the signal.

## Required device pass

For the next physical data pass, use only the verified iQOO serial and
Capybara-owned packages. Record the date, model/API/ABI/memory, complete sensor
inventory, actual audio routes and negotiated stream formats, preprocessing
status, exposed haptic capabilities, and raw capture quality metadata here.
Verify the active playback route during playback, test the real built-in
microphone and speaker system, and measure haptic onset from actual sensor
evidence. Keep `AudioDeviceInfo` route types distinct from proof of separate
speaker drivers. Preserve failures and unavailable values; do not infer support
from marketing material, advertised API fields, or emulator results.

Published Android API behavior and implementation decisions are documented in `hardware-research.md` and `research.md`.

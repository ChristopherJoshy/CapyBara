# Device capability record

**Status: iQOO 15 capability results are not currently measured.** The user's pause on iQOO testing remains active. Do not query, install to, capture from, or claim capabilities for a device until the pause is lifted and the authorized serial is verified.

## What the SDK queries (API surface, not device findings)

`Capabilities.kt` reads `Build.MANUFACTURER`, `Build.MODEL`, API level and supported ABIs; enumerates sensors and their advertised timing/range/resolution/FIFO fields; reads Android audio properties and enumerated audio devices; and queries vibrator presence, exposed IDs, amplitude control, resonance, and API 36 envelope/profile support. `CaptureEngine` requests 48 kHz mono PCM16 input, stereo output to the built-in speaker route, a finite chirp, and a finite waveform on exposed haptic actuators where available. It records actual Android routes while the streams are active. `PhysicsEngine` marks missing/non-built-in routes, absent haptic requests, and unmeasured physical haptic onset as limitations.

These are API requests and capability queries. They do not establish the iQOO 15's actual values, audio bandwidth, two-speaker emission, haptic activation, or useful sensor response. Android's built-in-speaker route type can describe either a mono or stereo speaker system; route metadata cannot prove both physical transducers emitted the signal.

## iQOO-specific results

| Capability | Result |
| --- | --- |
| Manufacturer/model, Android release/API, ABI, memory | Not measured |
| Accelerometer/gyroscope inventory and achieved sample rates | Not measured |
| Microphone routes, unprocessed source, actual format and processing | Not measured |
| Built-in speaker route, channel layout, physical transducer output | Not measured |
| Haptic IDs, amplitude/frequency/envelope support on this device | Not measured |
| Physical haptic onset and IMU response | Not measured; current capture metadata reports onset unavailable |
| End-to-end capture, synchronization, repeatability, offline behavior | Not measured |

## Required device pass

After the pause is lifted, use the repository's authorized-device procedure and verified iQOO serial only. Record the date, model/API/ABI/memory, complete sensor inventory, actual audio routes and negotiated stream formats, preprocessing status, exposed haptic capabilities, and raw capture quality metadata in this file. Verify the active playback route during playback, test the real built-in microphone and speaker system, and measure haptic onset from actual sensor evidence. Keep `AudioDeviceInfo` route types distinct from proof of separate speaker drivers. Preserve failures and unavailable values; do not infer device support from marketing material, advertised API fields, or emulator results.

Published Android API behavior and implementation decisions are documented in `hardware-research.md` and `research.md`. No iQOO-specific hardware result is claimed here.

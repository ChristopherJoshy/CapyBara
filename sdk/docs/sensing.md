# Capture and physical evidence

Capybara preserves microphone PCM16, IMU samples with sensor-event timestamps,
excitation request markers and capture metadata. `PhysicsEngine.analyze(scan)`
runs deterministic Kotlin DSP on the CPU. It supplies evidence, not an integrity
decision, contact-quality score or calibrated confidence.

## Evidence units and definitions

| Field | Meaning |
| --- | --- |
| `audioRms` | RMS of all recorded mono samples divided by 32768; includes DC and every capture stage. |
| `noiseRms` | RMS of exactly the pre-excitation prefix identified by `baselineSampleCount`; unavailable prefixes produce NaN. |
| `peak` | Largest absolute PCM sample divided by 32768. |
| `clippedSamples` | Count at either PCM16 rail. Rail occupancy can indicate saturation but does not prove its cause. |
| `acousticBandEnergy` | Eight integrated one-sided PSD bands in normalized PCM amplitude squared. Band i spans `[i*sampleRate/16, (i+1)*sampleRate/16)` Hz; the final band includes Nyquist. |
| `accelerationRms` | RMS vector deviation after subtracting each axis's mean, in m/s². |
| `gyroRms` | RMS uncentered angular-velocity vector magnitude, in rad/s. |
| `accelerometerHz`, `gyroscopeHz` | `(count-1)/(last timestamp-first timestamp)` from each recorded stream. |
| `durationSeconds` | Entire acquisition envelope from recorded BOOTTIME start/end; not the PCM length or end-to-end verification duration. |
| `limitations` | Missing data, capture-state errors and scientific interpretation limits. |

NaN denotes unavailable numerical evidence, distinct from measured zero. Consumers
must check finiteness before serializing strict JSON or comparing values. Invalid
audio sample rates raise `IllegalArgumentException`. Empty/invalid IMU values or
non-increasing timestamps do not become plausible RMS/rate evidence.

Centering acceleration removes a constant gravity/bias vector only while the
phone orientation remains stable. It is not an orientation-aware gravity estimate
and also removes genuine static acceleration. Rotating or repositioning during
capture changes gravity components and can inflate the result. Gyroscope RMS
includes sensor bias; neither number establishes contact quality.

## Spectral computation

The FFT is a radix-2 CPU implementation. Segments use a periodic Hann window,
per-segment mean subtraction and 50% overlap. Segment length is the largest power
of two no greater than 1024 or the recording length. Fewer than eight samples
cannot produce spectral evidence. One incomplete trailing window is included
only when samples extend beyond the last full window: it is zero-padded for the
FFT, mean-subtracted over observed samples, scaled by observed Hann energy, and
weighted by its observed fraction. This approximate edge policy is reported;
frequency resolution is `sampleRate/segmentLength`, and zero padding does not
create additional physical resolution.

For an N-point segment with s observed samples, the integrated contribution of
bin k is `alpha*c[k]*abs(FFT(window*(x-mean)))²/(N*sum(window[0:s]²))`, where c
is 1 at DC/Nyquist and 2 elsewhere; alpha is 1 for full windows and s/N for the
trailing window. Full windows use Welch normalization; the approximate partial
window policy is not calibrated boundary handling.
[Welch method and scaling](https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.welch.html).

These bands describe the complete recording, including silence and haptic stages.
They are not isolated object transfer responses, inferred material properties,
acoustic pressure in pascals or ultrasound capability. The signal includes the
phone, object, support surface, microphone processing and ambient noise. Phase,
coherence, impulse response and damping require validated excitation alignment,
coupling and signal-to-noise conditions; they are not claimed by this module.

## Timing and capture validity

IMU timestamps and AudioRecord BOOTTIME timestamps share elapsed-realtime units.
AudioTrack presentation timestamps use MONOTONIC. Cross-clock mapping must retain
its uncertainty; excitation requests do not timestamp physical onset. Framework
audio timestamps are estimates of the available pipeline point, not a guaranteed
phase lock. [AudioTimestamp](https://developer.android.com/reference/android/media/AudioTimestamp).

Requested IMU sampling periods are hints. The rate is derived from actual event
timestamps. A gap above three median intervals is flagged descriptively; this
heuristic has not been calibrated and does not count lost samples. No resampling
or IMU spectral interpretation occurs here.
[SensorManager](https://developer.android.com/reference/android/hardware/SensorManager).

Capture must preserve `audioSilencedEver`, `audioRouteChanged`,
`audioConfigurationChanged`, `audioTimestampCount` and `baselineSampleCount`.
Unknown states are reported as unavailable. A baseline prefix consists only of
samples already read before the first excitation request, never an assumed first
percentage of the recording. A route/effect change invalidates comparisons even
when the same client sample format is returned.
[Capture configuration changes](https://developer.android.com/media/platform/sharing-audio-input).

`audioExpectedSamples` is compared with the actual PCM length, and
`audioSampleCount` is cross-checked when present. Partial or inconsistent captures
are explicit limitations; successful PCM reads do not establish sample continuity.
When start/end clock audit metadata is present, an offset change exceeding half
the sum of both clock-read bracket widths is flagged as unstable cross-clock
timing. This bound covers clock-read uncertainty, not unknown hardware latency.
Positive RMS or spectral energy cannot establish good contact or object/self
response separation; these remain unavailable without experimental calibration.

## Route and haptic excitation guards

`CaptureEngine` records the actual `AudioRouting.getRoutedDevice()` types while
input/output streams are active; an accepted preferred-device request is not
treated as the routed device. Android only guarantees this query while the
stream is recording or playing ([AudioRouting API](https://developer.android.com/reference/android/media/AudioRouting#getRoutedDevice())).
`PhysicsEngine` reports a limitation unless both the built-in microphone and
built-in speaker system routes were observed. This identifies Android's route
type, not individual speaker transducers or proof that both physical speakers
emitted the signal.

`haptic_finite_waveform` is an excitation request marker, not measured actuation.
No request and the current `hapticPhysicalOnset=unavailable` each produce a
limitation. The actual motor response has not been verified on the iQOO during
the current device-test pause; contact and haptic onset remain uncalibrated.

## Validation scope

`PhysicsEngineTest` uses clearly named synthetic fixtures to check tone energy,
frequency-band placement, DC removal, Nyquist normalization, baseline indexing,
PCM rails, gravity centering, timestamp-derived rate, invalid evidence and
route/haptic policy guards. These tests establish arithmetic and admission-policy
behavior only. Real-device synchronization, signal separability, repeatability
and object/session-held-out integrity accuracy still require physical measurements
and calibration.

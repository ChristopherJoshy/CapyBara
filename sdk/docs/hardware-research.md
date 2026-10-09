# Hardware/API research — 2026-10-09

This note records published API behavior and research findings. It makes no
iQOO-specific measured capability or accuracy claim. The real on-device capture
report owns measured findings. Protected apps Hacker Tracker and Register must
never be targeted, launched, stopped, queried or modified.

| Research question and primary source | Finding and implementation consequence | Verification / limitation |
| --- | --- | --- |
| [SensorEvent timestamp](https://developer.android.com/reference/android/hardware/SensorEvent) | Timestamp is event time on the elapsed-realtime clock. Preserve it instead of callback arrival time. | Public API contract; synchronization precision remains unmeasured. |
| [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord) | BOOTTIME timestamps map frame position onto the IMU clock. Missing timestamp availability, read errors, restart frame resets and lifecycle are explicit. | Client format alone does not establish native hardware acquisition. |
| [AudioTimestamp](https://developer.android.com/reference/android/media/AudioTimestamp) | BOOTTIME includes sleep; AudioTrack uses MONOTONIC. Audio timestamp quality is implementation best effort. | Bracketed cross-clock offset estimation is an engineering method requiring uncertainty and drift checks. |
| [Android audio input sharing](https://developer.android.com/media/platform/sharing-audio-input) | Register recording callback before starting; retain silencing, route, preprocessing and actual/client format changes. | Successful reads can contain policy-silenced audio. No requirement to stop or inspect another app. |
| [Recording guidance](https://developer.android.com/media/platform/mediarecorder) | Query unprocessed-source support. If absent, explicitly use VOICE_RECOGNITION and record fallback. | Requested UNPROCESSED can otherwise become DEFAULT; vendor processing still needs characterization. |
| [AudioDeviceInfo](https://developer.android.com/reference/android/media/AudioDeviceInfo) | Reported rate/channel/encoding support is useful for stream probing. An empty rates list means arbitrary rates. | Format support does not prove speaker or microphone frequency response. |
| [MicrophoneInfo](https://developer.android.com/reference/android/media/MicrophoneInfo) | Active channel mapping can distinguish direct/processed contributors; retain unknown metadata honestly. | Multiple physical microphones do not necessarily expose independent channels. |
| [Sensor API](https://developer.android.com/reference/android/hardware/Sensor) and [sensor overview](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview) | Retain range/resolution/minDelay/FIFO; >200 Hz requires HIGH_SAMPLING_RATE_SENSORS. Derive achieved rate from timestamps. | Advertised rates are not measured rates; privacy toggles can impose further rate limits. |
| [VibratorManager](https://developer.android.com/reference/android/os/VibratorManager) | Enumerate exposed actuator IDs and each device's support. | IDs do not expose a standard X/Z axis mapping. Dual-axis marketing cannot establish independent axis control. |
| [Vibrator](https://developer.android.com/reference/android/os/Vibrator) | Query presence, amplitude, primitives, envelopes and profiles; preserve null/NaN for unavailable data. | Support query is separate from measured physical activation. No precise motor-onset timestamp is established here. |
| [VibratorFrequencyProfile](https://developer.android.com/reference/android/os/vibrator/VibratorFrequencyProfile) | A profile requires independent frequency control and describes supported frequency/maximum acceleration. | This is actuator capability metadata, not an object's measured response. |
| [WaveformEnvelopeBuilder](https://developer.android.com/reference/android/os/VibrationEffect.WaveformEnvelopeBuilder) and [envelope limits](https://developer.android.com/reference/android/os/vibrator/VibratorEnvelopeEffectInfo) | API36 envelopes require supported frequencies, amplitude 0–1 and reported timing/size limits. Out-of-range frequency can produce no vibration, without fallback. | Must check hardware capabilities before constructing physical experiments. |
| [VibrationEffect waveform](https://developer.android.com/reference/android/os/VibrationEffect) | Finite legacy pulses specify durations/amplitudes. | Their repetition rate is not carrier frequency; without amplitude control nonzero values act as on. |
| [AAudio API](https://developer.android.com/ndk/reference/group/audio) | Native input/output timestamps support CLOCK_BOOTTIME; XRun counters can expose discontinuities. | Some input devices always return zero XRuns. AAudio does not synchronize sensors. |
| [Official Oboe duplex example](https://github.com/google/oboe/wiki/Using-FullDuplexStream-for-Synchronized-IO) | Coordinates simultaneous input/output buffering. | No library adopted solely for research; native complexity requires a measured reason. |
| [Android audio latency](https://developer.android.com/ndk/guides/audio/audio-latency) | Latency and clock-rate differences require loopback measurement. | Low-latency feature flags/configuration are not device timing measurements. |

## Documentation discrepancy

The [haptics guide](https://developer.android.com/develop/ui/views/haptics/haptics-apis)
currently recommends `VibrationEffect.Builder` for Android 16 26Q4, but the
[Builder API reference](https://developer.android.com/reference/android/os/VibrationEffect.Builder)
currently marks introduction as version 37.2. Confirm the installed Android SDK
before using these examples. API36 `WaveformEnvelopeBuilder` is independently
documented; legacy finite waveforms remain usable with accurately named limits.

The capture implementation was checked against installed SDK37 signatures by the capture track. Resonant frequency/Q queries are guarded at API34, envelope/profile queries at API36. Current acquisition records a 48 kHz mono request, a finite 300–8000 Hz chirp duplicated to a stereo `AudioTrack` output, and finite legacy haptic pulses. On API31+, when at least two vibrator IDs are exposed, acquisition sends the same measured finite waveform through `CombinedVibration.startParallel()` to every exposed ID; otherwise it records an explicit default-vibrator fallback. This uses synchronized actuator scheduling, not independent-axis control. It reports advanced haptic capabilities without pretending those pulses are frequency-controlled envelopes.
Built-in microphone/speaker preferences are local to Capybara's streams; accepted preferences and actual routing are separately recorded. Stereo is a requested output channel layout, not proof that both physical transducers emitted independently. Session AEC/AGC/NS disablement is attempted only with control, and before/after/final states and failure status are retained. These are implementation decisions, not proof of physical activation, emitted bandwidth, complete processing removal or accuracy.

## Original sensing research

**Knocker — Taesik Gong, Hyunsung Cho, Bowon Lee, Sung-Ju Lee, IMWUT 2019.**
[Original paper](https://nmsl.kaist.ac.kr/pdf/IMWUT19_Knocker.pdf),
[author project](https://nmsl.kaist.ac.kr/projects/knocker/).
Recognizes objects from phone knocks using microphone/accelerometer/gyro. The
prototype used 48 kHz audio and 400 Hz IMU; peak alignment handled observed audio
latency. Its published 98% lab/83% wild object-recognition result is not a Capybara
integrity benchmark. It motivates multimodal response capture and nuisance-aware
evaluation, without establishing active-haptic hidden-state detection.

**VibePhone — Jungchan Cho, Inhwan Hwang, Songhwai Oh, Pattern Analysis and
Applications 2016, DOI 10.1007/s10044-015-0460-8.**
[Publisher paper](https://link.springer.com/article/10.1007/s10044-015-0460-8).
Uses a built-in vibrator and accelerometer for contacted surface recognition,
with compact selected features. This motivates mechanical coupling experiments
and efficient DSP, not universal hidden-content discrimination. The accessible
publisher abstract does not establish this device's excitation settings.

**The Boombox — Boyuan Chen, Mia Chiquier, Hod Lipson, Carl Vondrick,
CoRL proceedings/PMLR 2022.**
[Original paper](https://proceedings.mlr.press/v164/chen22c/chen22c.pdf),
[publication record](https://proceedings.mlr.press/v164/chen22c.html).
Reconstructs container contents from collision vibrations using four contact piezo
microphones and visual training supervision. This is different hardware,
excitation and supervision from a phone's air microphones and internal motor.
The paper motivates hidden-state research but does not prove phone-only spatial
reconstruction, calibrated integrity decisions or Capybara performance.

## Engineering hypotheses to test

- Concurrent capture should bracket baseline, excitation and decay. Requested
  playback/vibration markers are distinct from measured physical onset.
- Windowed finite audio at modest app gain and finite haptic pulses support an
  initial controlled probe; no universal acoustic safety level is established.
- Speaker/microphone bandwidth, clipping, self-response, contact variation and
  session drift determine useful excitation bands. Do not assume ultrasound.
- Simultaneous acoustic/haptic excitation mixes contributions. Staged and
  combined probes must be distinctly labeled and compared experimentally.
- Effective IMU sampling and filtering constrain mechanical frequency analysis.
  Interpolation/zero padding cannot recover aliased or missing physical signals.

All hypotheses remain unverified until recorded on the actual phone. No physical
accuracy target, synchronization precision or signal-separation claim is achieved
by documentation or deterministic synthetic DSP tests.

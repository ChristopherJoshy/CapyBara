# Capybara V6

*Give the physical world a memory.*

Capybara is an offline Android physical-measurement SDK and demonstration app. It uses the phone's real audio output, microphone, vibration actuators, accelerometer and gyroscope to record how a contacted object responds. Raw measurements remain private on the device and retain timing, routing, excitation-request and quality provenance.

## Last verified implementation and evidence

- Last successful iQOO identity report (2026-10-10): I2501, Android 16 / API 36, `arm64-v8a`, serial `10BFBH1BH0001EY` (build fingerprint `iQOO/I2501i/I2501`). A later recorded ADB listing showed only `5XVSGE8HJBIF55T8` (CPH2781IN) and `emulator-5554`; neither was used for Capybara testing. Current device connectivity is unknown because iQOO testing is paused.
- Historical capability report (2026-10-10): Android advertised 48 kHz output / 144 frames and unprocessed-source support; built-in speaker type 2 advertised 48 kHz stereo and built-in microphone type 15 advertised 48 kHz. `VibratorManager` exposed IDs 0 and 1, both present with amplitude control; resonant frequency was `NaN` and envelope effects unsupported. These reports do not establish current availability, achieved rates, routing or physical response.
- Real `AudioRecord` microphone capture, stereo `AudioTrack` excitation request, IMU capture, finite haptic excitation, cancellation cleanup, private atomic capture storage and CPU DSP are implemented.
- The 2026-10-10 inventory reported two vibrator IDs. On API 31+, Capybara requests the same finite waveform on every exposed ID through `CombinedVibration`; Android does not expose independent-axis semantics for them.
- Encrypted immutable research baselines are profile-scoped and use Android
  Keystore AES-GCM: four positions for the controlled `rigid_case` protocol and
  two sides for the generic `showcase_object` flow. Both remain uncalibrated.
- Baseline enrollment rejects captures whose stored PCM count does not match
  both the requested and reported sample counts; incomplete captures remain raw
  research data and cannot create an enrolled reference.
- `Capybara.evaluate(referenceId, paths)` authenticates the encrypted reference, checks sealed capture metadata and PCM/IMU hashes, requires the current capture's user-entered object label to match the selected reference label (not proof of physical identity), derives per-position reference/current physics features and deltas on CPU, and returns explicit TrustGate blockers. Its current result remains `INCONCLUSIVE`; it does not run Laya inference.
- The profile catalog separates `rigid_case` A–D research capture from the
  `showcase_object` SIDE_1/SIDE_2 acquisition flow. The latter accepts any stable
  box/object as raw input; this does not establish cross-object generalization.
- `ProbeBrain` records a fixed characterization plan: stereo audio requests the
  same excitation on left and right outputs, and haptics are requested on every
  exposed vibrator ID when Android reports them. These are requests, not proof
  of physical transducer onset.
- A historical capture on serial `10BFAT1UT2000XP` recorded 48 kHz PCM and approximately 462 Hz accelerometer/gyroscope event rates. The 2026-10-10 iQOO smoke acquisition was unlabelled, reached review and was rejected; Status afterward showed zero persisted captures and baselines. Neither run is training or accuracy evidence.
- The isolated training environment now runs CPU PyTorch and the pinned Laya source. The licensed English Laya base checkpoint loaded locally; its host ONNX export (1,685,258,240-byte external FP32 weights) matched eager output on one generic email example. Its shipped temperature is invalid/clamped, so confidence is uncalibrated; this checkpoint is not packaged or used for physical decisions.
- MOMENT-1-small was evaluated as a frozen encoder on the public UCI fan split and underperformed the scratch fan baseline (55.56% versus 67.63% test accuracy). Neither fan result is Capybara integrity evidence; MOMENT is not promoted.

## Physical-integrity decision status

Capybara captures and compares real recordings, but the current TrustGate result
is `INCONCLUSIVE`. No trained EchoNet, VibraNet, PhysFusion, StateNet or
SpatialNet weights, Capybara-Laya checkpoint, calibration set, or held-out
physical-integrity evaluation exists. Public classifiers in research artifacts
address unrelated tasks and are not integrated. No data collected at the
hackathon venue is present; venue-based training is a goal, not an observed
result.

The showcase accepts any stable box or object through two real contact-side
captures. Enrollment asks for a name, captures and reviews side 1, asks the user
to turn the phone, then captures and reviews side 2. Only a complete approved
pair creates an encrypted uncalibrated research reference and provenance digest;
partial/rejected captures are not listed as enrolled objects. Scan requires an
explicit saved reference, then the SDK derives actual per-position current and
reference features. TrustGate remains `INCONCLUSIVE` because trained specialists,
Capybara-Laya inference and calibration are unavailable; no confidence is shown.
The digest is not an identity signature.

## Repository

- `sdk/` — Android library, acquisition, DSP, storage, training and evaluation.
- `apk/` — Compose demonstration app consuming `sdk/`.
- `apk/docs/design.md` — verified screen flow, permissions, motion, test procedure and screenshot policy.
- `sdk/docs/` — architecture, device capability, sensing, experiments, model and progress records.
- `sdk/training/` — real-data schema validation, pinned Laya preparation, training and export/parity workflow. It refuses incomplete or fabricated inputs.
- `CODEX_HANDOFF.md` — verified continuation state and next Codex actions.

## SDK integration

The SDK is currently a local Gradle module, not a published Maven artifact:

```kotlin
dependencies {
    implementation(project(":sdk"))
}
```

The consumer must target API 29 or newer and request `RECORD_AUDIO` at runtime.
The library manifest declares `RECORD_AUDIO`, `VIBRATE`, and
`HIGH_SAMPLING_RATE_SENSORS`; Android users must grant microphone access before
capture. Call capture from a lifecycle-aware coroutine and save files on an IO
dispatcher:

```kotlin
import android.content.Context
import dev.capybara.sdk.Capybara
import dev.capybara.sdk.CaptureLabel
import dev.capybara.sdk.ProfileEngine
import dev.capybara.sdk.ResearchBaseline
import dev.capybara.sdk.IntegrityAssessment
import dev.capybara.sdk.Scan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

suspend fun recordShowcaseReference(
    context: Context,
    objectName: String,
    approveCapture: suspend (String, Scan) -> Boolean,
): ResearchBaseline? {
    val capybara = Capybara(context)
    val profile = capybara.getProfile(ProfileEngine.SHOWCASE_OBJECT_ID)
    val sessionId = UUID.randomUUID().toString()
    val paths = mutableListOf<String>()

    for (position in profile.positions) {
        val scan = capybara.capture(
            CaptureLabel(objectName, sessionId, "initial", position, profile.id)
        )
        if (!approveCapture(position, scan)) return null
        paths += withContext(Dispatchers.IO) { capybara.saveCapture(scan) }
    }

    return withContext(Dispatchers.IO) { capybara.saveResearchBaseline(paths) }
}

suspend fun scanShowcaseReference(
    context: Context,
    referenceId: String,
    approveCapture: suspend (String, Scan) -> Boolean,
): IntegrityAssessment? {
    val capybara = Capybara(context)
    val reference = withContext(Dispatchers.IO) {
        capybara.listResearchBaselines().firstOrNull { it.id == referenceId }
    } ?: return null
    val profile = capybara.getProfile(reference.profile)
    val sessionId = UUID.randomUUID().toString()
    val paths = mutableListOf<String>()

    for (position in profile.positions) {
        val scan = capybara.capture(
            CaptureLabel(reference.objectId, sessionId, "verification", position, profile.id)
        )
        if (!approveCapture(position, scan)) return null
        paths += withContext(Dispatchers.IO) { capybara.saveCapture(scan) }
    }

    return capybara.evaluate(referenceId, paths)
}
```

Both helpers abort on a rejected side; production UI should retain per-side state
to retry without restarting the pair. `approveCapture` must await a real user
decision. Rejection creates no baseline; earlier approved captures remain private
research data. The scan helper uses a fresh session ID, captures every position,
and evaluates that set against the selected reference. Its decision is
`INCONCLUSIVE` without trained specialists and calibration; this API does not
claim a physical identity fingerprint or calibrated verification.
See [`sdk/docs/experiments.md`](sdk/docs/experiments.md) and
[`sdk/training/README.md`](sdk/training/README.md) for data and evaluation rules.

## Build

```powershell
cmd.exe /c gradlew.bat testDebugUnitTest assembleDebug
```

## Latest verification (2026-10-10)

Full SDK/DSP host verification passed:

```powershell
cmd.exe /c gradlew.bat :sdk:testDebugUnitTest :sdk:lintDebug :sdk:assembleDebugAndroidTest :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebug :apk:assembleDebugAndroidTest
```

Showcase UI and instrumentation APK compilation:

```powershell
cmd.exe /c gradlew.bat :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebugAndroidTest :apk:assembleDebug
```

Partial-audio baseline rejection verification:

```powershell
cmd.exe /c gradlew.bat :sdk:testDebugUnitTest :sdk:lintDebug :sdk:assembleDebugAndroidTest
```

SDK unit tests, lint and instrumentation APK compilation passed in the recorded
host runs. The vault instrumentation was compiled but not run.

Downstream SDK/APK integration build and APK lint also passed:

```powershell
cmd.exe /c gradlew.bat :sdk:assembleDebug :apk:assembleDebug :apk:lintDebug
```

Historical device smoke (2026-10-10; not a current test): the APK was installed
on `10BFBH1BH0001EY`; its home, status, technical-details and Scan placement
screens rendered. One unlabelled acquisition reached review and was rejected;
Status afterward reported microphone permission granted, zero persisted
captures/baselines and `Ready`. No accepted object-labeled data, physical pilot
or integrity decision resulted; capability reports do not prove speaker or
motor actuation.

Current iQOO 15 identity/UI smoke (2026-10-10): serial-scoped properties report
`vivo` / `iQOO` / `I2501`, Android 16/API 36, `arm64-v8a`. The focused
`:apk:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.capybara.app.ShowcaseTest`
run passed 2/2 tests on `I2501 - 16`; it verified home/status/setup navigation
and that Scan requires a saved reference before capture. No object capture,
speaker-route/output measurement, haptic-onset test, model inference or offline
verification occurred. Do not infer physical speaker/motor activation from API
requests or UI success. Further device operations must target the verified
serial and Capybara-owned packages only.

The runtime has no `INTERNET` permission. Do not operate on packages named Hacker Tracker or Register.

## Public-data training

Public data supports source-specific research, not a substitute for Capybara's
iQOO speaker-and-haptic measurements. The CC BY 4.0
[EPFL container-manipulation dataset](https://doi.org/10.5281/zenodo.6372438)
contains audio and tactile trials for five contents in one plastic container.
The CC BY-NC 4.0 [CORSMAL dataset](https://doi.org/10.17636/101CORSMAL1)
contains audio, inertial data and content labels for 15 containers, but uses
human manipulation and an external microphone array. Neither establishes
phone-probe integrity performance. No data collected at the hackathon venue is
present in this repository; no model is described as venue-trained. See
[`research.md`](research.md) and [`sdk/training/README.md`](sdk/training/README.md)
for provenance and limitations.

An auxiliary CPU experiment trained a separate three-class content classifier on
180 EPFL trials (72 training, 18 validation, 90 held-out). It scored 33.33%
held-out accuracy and 16.67% macro-F1, equal to one-in-three chance, and
predicted `empty` for every held-out trial. MP3 decoding emitted frame-length
errors, so audio continuity is unverified. This model is rejected and is neither
a Capybara integrity classifier nor a phone-deployment candidate. No venue data
or venue fine-tuning is claimed.

## Research

Published evidence and implementation limits are recorded in `research.md`, `sdk/docs/hardware-research.md`, `sdk/docs/sensing.md` and `sdk/docs/laya-integration.md`. In particular, Knocker and VibePhone motivate multimodal sensing but do not establish universal phone-only hidden-content integrity.

## License

Apache-2.0. See `LICENSE` and third-party notices before distributing model or dataset artifacts.

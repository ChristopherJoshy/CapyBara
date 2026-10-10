# Measurement application

Palette: obsidian #121517, charcoal #1D2327, sand #F0E5D7, copper #D8AE86,
restrained cyan #98CDC9, muted text #B3BABD. Serif mission typography contrasts
with readable platform sans controls. The distinctive element is the large
mission statement; evidence and controls are quiet and aligned.

The judge-facing home is intentionally sparse. No visible V6 label or model-training
explanation appears on the main screens. A hamburger-only modal sidebar contains
Status, Logs, Capture setup, and scientific details.
Missing specialist checkpoints and calibration are shown truthfully in Status.
Calibrated verification is unavailable; Scan remains a research
capture/comparison flow and ends `INCONCLUSIVE`.
The showcase supports any stable box or object with two real contact-side
captures. Enrollment asks for a name, captures side 1, prompts the user to turn
the phone, captures side 2, and reviews both responses. Only a complete approved
pair creates a Keystore-encrypted uncalibrated research reference and provenance
digest; incomplete or rejected captures are not listed as enrolled. Scan uses
the same side-1/side-2 approve/retry/reject flow without a name. It ends
`INCONCLUSIVE`: the digest is not an identity signature and validated comparison
models/calibration are unavailable. The four-position research path remains
separate. Actual capture callbacks drive progress; results follow completed SDK
operations. Raw captures stay private.

Microphone permission is requested only when needed. Cancelling or leaving the
activity cancels acquisition and releases hardware. No INTERNET permission exists.
Buttons have normal accessible touch targets. Waveforms have a semantic label.
Scrolling/insets support large text and real device cutouts. Research enrollment
and scan capture remain available; absent validated models/calibration, the app
withholds MATCH/CHANGED and reports `INCONCLUSIVE`.

Required future flows: full validated enrollment/reference management, four-point
verification, actual model stages, TrustGate result and calibrated evidence. Do
not turn the data collector into a scripted demonstration of those features.

## Motion and outcome semantics

The home mascot scales in with the mission text. A small non-waveform pulse is
shown only while the SDK reports a showcase capture/analysis operation as busy,
alongside its real stage text. The result mark animates after work completes:
check only after the encrypted research reference is saved, `?` for an
`INCONCLUSIVE` scan, and a dash when enrollment did not persist. There is no
match/not-found animation until validated decision models and calibration exist.
The hash is a capture-provenance digest, never a physical identity fingerprint.
Motion uses Compose value-based animations
([official guide](https://developer.android.com/develop/ui/compose/animation/value-based))
and respects the resolved Compose Animation 1.12.1 Animator duration-scale
support ([release notes](https://developer.android.com/jetpack/androidx/releases/compose-animation)).

## Navigation and runtime behavior

- Home opens **Enroll object** or **Scan**. Enrollment asks for a name, captures
  and reviews side 1, prompts the user to turn the object, then captures and
  reviews side 2. Scan first requires a saved two-side research reference and
  uses the same real capture/review flow without asking for a new name.
- Each captured side can be approved, retried, or rejected. Only approval moves
  the flow forward; only two approved sides create an encrypted research
  reference. A completed capture is not a calibrated physical fingerprint.
- The hamburger menu contains Status, Logs, and Capture setup. Status shows
  actual capability reports and model/calibration blockers; technical details
  expose recorded capture metadata. Logs are limited to Capybara operations.
- The app requests `RECORD_AUDIO` at the first capture that needs it. The SDK
  declares `VIBRATE` and `HIGH_SAMPLING_RATE_SENSORS`; microphone permission is
  runtime-gated. The APK requires microphone hardware and declares no camera or
  internet dependency.

## Test procedure and evidence boundary

Host checks use the checked-in wrapper:

```powershell
cmd.exe /c gradlew.bat :apk:testDebugUnitTest :apk:lintDebug :apk:assembleDebugAndroidTest :apk:assembleDebug
```

Instrumentation compilation is not device execution. A physical procedure must
use the verified Capybara device and package, obtain microphone permission, and
follow `sdk/docs/experiments.md`; never describe emulator or host output as
physical evidence. The current Capybara-only iQOO UI smoke passed two tests, but
no object capture, routing, or transducer output was tested. `screenshots/` is
currently empty; add only genuine application screenshots from authorized
device runs.

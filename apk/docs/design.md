# Measurement application

Palette: obsidian #121517, charcoal #1D2327, sand #F0E5D7, copper #D8AE86,
restrained cyan #98CDC9, muted text #B3BABD. Serif mission typography contrasts
with readable platform sans controls. The distinctive element is the large
mission statement; evidence and controls are quiet and aligned.

The judge-facing home is intentionally sparse. No visible V6 label or model-training
explanation appears on the main screens. A hamburger-only modal sidebar contains
Status, Logs, Capture setup and scientific details. Missing checkpoints, calibration
and unavailable verification remain truthfully reported in Status; verification
stays disabled. Logs contain only actual Capybara operations, never other apps' logs.
The showcase supports any stable box or object with two real contact-side
captures. Enrollment asks for a name, captures side 1, asks the user to turn the
phone, captures side 2, reviews each response, merges provenance hashes and
shows an object-added result. Scan uses the same side-1/side-2 approve/retry/
reject flow without asking for a name. It ends `INCONCLUSIVE`: the provenance
fingerprint is not an identity signature and no validated model comparison or
calibration is available. The four-position research path remains separate.
Actual capture callbacks drive progress. Results never appear before SDK operations complete. Raw recordings stay private.

Microphone permission is requested only when needed. Cancelling or leaving the
activity cancels acquisition and releases hardware. No INTERNET permission exists.
Buttons have normal accessible touch targets. Waveforms have a semantic label.
Scrolling/insets support large text and real device cutouts. Enrollment and
verification are visibly disabled while trained models/calibration are missing.

Required future flows: full validated enrollment/reference management, four-point
verification, actual model stages, TrustGate result and calibrated evidence. Do
not turn the data collector into a scripted demonstration of those features.

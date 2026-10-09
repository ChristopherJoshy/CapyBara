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
The capture guide supports two honest showcase actions: Enroll object and Scan object. Enrollment asks for a label before two real side captures; Scan starts at the placement prompt with a generated anonymous candidate ID and never requests a name. Each flow has a review gate (approve, retry or reject), then displays measured evidence and a digest derived from saved capture provenance. The digest is not an identity signature. Scan ends `INCONCLUSIVE`: saved names/references exist, but no validated model comparison or identity decision is available. The existing four-position research path remains available.
Actual capture callbacks drive progress. Results never appear before SDK operations complete. Raw recordings stay private.

Microphone permission is requested only when needed. Cancelling or leaving the
activity cancels acquisition and releases hardware. No INTERNET permission exists.
Buttons have normal accessible touch targets. Waveforms have a semantic label.
Scrolling/insets support large text and real device cutouts. Enrollment and
verification are visibly disabled while trained models/calibration are missing.

Required future flows: full validated enrollment/reference management, four-point
verification, actual model stages, TrustGate result and calibrated evidence. Do
not turn the data collector into a scripted demonstration of those features.

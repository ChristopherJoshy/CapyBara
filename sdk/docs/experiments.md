# Physical experiments and model acceptance

Status, 9 October 2026: real device diagnostics may verify acquisition, but there
is no labeled physical-integrity dataset. The user is currently unavailable to
manipulate objects. Do not relabel phone diagnostics or schema-test fixtures as
training data. Do not launch Kaggle training until the prerequisites below exist.

## First controlled experiment

Start with a rigid closable case containing removable safe items. An earbuds case
can be a candidate only if it supports stable phone contact and its contents can
be changed without moving the phone against fragile parts. A larger rigid box is
easier to register. Cardboard and flexible bags follow after controlled feasibility.
Place the container on a firm repeatable support, keep the phone fully supported,
mark four reproducible positions A–D, and use ordinary safe sound/haptic levels.

For each object/session:

1. Give the object a stable pseudonymous ID and the acquisition session a new ID.
   Record profile, contents, supporting surface, four marked positions and device
   calibration/version. Separate physical run IDs for each contents manipulation.
2. Capture enrollment at A–D. Record each raw capture and its actual excitation,
   timing, routing, quality and sample-loss metadata. Do not silently replace it.
3. Remeasure unchanged contents at A–D, lifting/repositioning between repeats.
   Begin with five independent repeats per position to expose coupling variation.
4. Remove one item, close the case, and repeat five times per position. Record the
   exact intervention before measuring. Restore contents and repeat unchanged.
5. Inspect actual distributions and interference before expanding the matrix.
   If within-state repositioning variation dominates changed-state separation,
   report that result and improve contact/excitation rather than boost confidence.

Five repeats are an initial feasibility exercise, not a statistically justified
accuracy sample size. Repeated readings from one object cannot establish held-out
object generalization. Keep timestamps and operator labels independently of model
outputs, and preserve every failed/inconclusive recording.

## Required condition matrix

| Condition | Purpose and labeling |
| --- | --- |
| Unchanged and ordinary re-enrollment | Baseline repeatability; MATCH only under validated quality |
| Lift/reposition and normal support changes | Contact/environment nuisance; can be INCONCLUSIVE if invalid |
| Benign external deformation | Profile-dependent nuisance; distinguish from target changes |
| Removed internal item | Controlled change, expected CHANGED if reliably observable |
| Replaced item | Physical substitution, independent of packaging appearance |
| Same-weight substitute | Tests response beyond gross mass differences |
| Rearranged contents | Include only if the chosen integrity profile intends to detect this |
| Ambient noise | Quality rejection; never convert failure into MATCH |
| Unstable contact | ReferenceGuard/TrustGate rejection target |
| Unseen material/object/profile | OOD behavior; requires appropriate abstention |

Ground truth is the actual physical state/intervention plus measurement validity.
Do not force every degraded reading to CHANGED merely because it differs, or to
MATCH because the contents are unchanged. Record disputed labels and keep them
out of confident training targets until resolved. Do not infer probability labels
from uncalibrated heuristics; Laya's `expected` schema supports human hard labels.

## Specialized model prerequisites

- **EchoNet:** real excitation/response audio, device/self-response calibration,
  quality annotations, positive unchanged and negative changed pairs. Train a
  acoustic embedding objective; evaluate unseen objects and nuisance conditions.
- **VibraNet:** actual haptic timing and supported IMU streams/sample rates;
  quality/coupling annotation and state pairs. Reject requests for frequency/axis
  control the public Android API/hardware does not expose.
- **PhysFusion:** paired audio/IMU evidence from synchronized captures, dropout and
  contamination conditions, an objective that uses both modalities. Test modality
  ablations on real held-out data rather than claim fusion from concatenation.
- **StateNet:** profile-specific state relationships. Invariance to repositioning
  must not erase the physical changes the profile is meant to detect.
- **SpatialNet:** registered A–D captures from the same physical run, corresponding
  references and full source provenance. Evaluate held-out object sets. Per-position
  deviations do not establish precise internal localization.
- **Capybara-Laya:** evidence from the genuine trained/versioned specialists,
  separate calibration, human outcomes and three constrained choices. Training
  code is ready for valid inputs; primitive physics alone is not final V6 evidence.

No architecture/training recipe for a specialist is validated merely by this
document. First establish actual sensor bandwidth, signal separability and useful
feature representation. Preserve objectives, sample-rate/window definitions,
source hashes, licensing, optimizer budgets, model hashes and export parity.

## Leakage prevention

Split by connected groups across **object ID, session ID, physical run ID and
shared enrollment/capture IDs**. An object appearing in multiple sessions and a
session containing multiple objects can create transitive connections: keep the
entire connected component in one split. Never split four positions or overlapping
windows from one recording across train/test. A shared enrollment reference is
also shared input and must not cross splits. Plan separate objects and sessions
for training, calibration and final test in advance.

Fit nuisance normalization, covariance/statistics, model hyperparameters,
temperature/abstention thresholds and contact-quality calibration without access
to final held-out outcomes. Freeze models, thresholds and profiles before the
final test. Additional iteration after inspecting test failures needs a new final
test set. Record the resulting split manifest and both raw/derived hashes.

## Required reporting

Report confusion counts for all three outputs and coverage (fraction returning
MATCH/CHANGED), INCONCLUSIVE rate, accuracy among accepted decisions, false-change
rate on unchanged objects and false-match rate on changed objects. A changed
object returned MATCH is a missed change; report this explicitly alongside
changed-object abstentions. State denominators instead of letting terminology
hide abstentions. Also report ECE/Brier for calibrated model probabilities,
performance per condition/profile/position and repeated-measurement reliability.

Laya model accuracy, full-system accepted accuracy, false matches and coverage
are different quantities. Provide object-level uncertainty intervals when there
are enough independent objects; window-level sample counts inflate evidence.
No current dataset supports 90%, 98% or any other accuracy claim.

On the actual phone, record CPU execution/provider, resident memory, cold/warm
model-load time, per-stage inference time and full verification time. The latter
starts before the first position and includes all four captures, human movement,
DSP, every neural stage, Laya and TrustGate. Test with network unavailable after
all local models/references are provisioned. Targets remain <60 s total, preferably
<5 s post-capture inference and <=500 ms Laya; do not subtract failed scans or
movement to meet them.

## Published evidence and its limits

[Knocker](https://nmsl.kaist.ac.kr/projects/knocker/) and
[VibePhone](https://doi.org/10.1007/s10044-015-0460-8) motivate multimodal/active
surface sensing, not universal sealed-package integrity. The
[Boombox paper](https://proceedings.mlr.press/v164/chen22c.html) motivates hidden-state
acoustic information but uses a different hardware setup. None is a measured
Capybara result or a labeled iQOO training corpus. Public-domain transfer learning
requires verified actual data/license, domain comparison and independent phone
evaluation; public sound classification labels cannot establish physical change
labels. See research.md for the source audit and implementation decisions.

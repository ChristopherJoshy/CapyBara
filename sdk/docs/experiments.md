# Physical experiments and model acceptance

Status, 10 October 2026: there is no labeled physical-integrity dataset. The
user must manipulate and label physical objects before task-specific training
can begin. Public-data audit and dataset decisions are in `research.md`. Do not
relabel phone diagnostics or schema-test fixtures as training data, and do not
launch a Kaggle job with unrelated labels.

For an initial controlled run, use a rigid closable earbuds case with safe,
removable earbuds only if it supports stable phone contact and safe contents
changes. Use a firm table, four marked positions A–D, and the app's `rigid_case`
research flow. Keep the lid closed and phone orientation, support, power state
and audio route consistent. Capture one reference set at A–D, then five
unchanged A–D passes, five changed A–D passes with the earbuds removed, and five
restored A–D passes. Use a fresh `sessionId` for each complete set. Give each
comparison pass a unique `runId`, reused for its four position-level annotation
rows. Use the app's `with_earbuds` / `without_earbuds` labels only when those
states are physically true; never relabel a generic object as an earbuds case.

In Capture setup, keep the object ID stable and tap **New session** before each
A–D set. Select the physically correct condition chip for every capture. Save a
baseline only for the first reference set; comparison sets stay raw captures.
The UI does not record `runId`, so keep each comparison set's unique ID in the
operator log for later annotation.

This single-object experiment tests acquisition and within-object separability
only. It cannot support an accuracy claim or train a generalizable model. The
minimum initial object-separated pilot is ten distinct physical cases: six
training, two calibration, and two untouched test objects, with all captures,
references and sessions of each object confined to one split. Each split needs
`MATCH`, `CHANGED` and physically justified `INCONCLUSIVE` outcomes. This is a
minimum pilot, not a statistically adequate accuracy sample. Raw recordings
remain in the app-private capture store until explicitly copied to local
`sdk/captures/`; they must not be uploaded to Kaggle without specific
authorization.

## First controlled experiment

Start with a rigid closable case containing removable safe items. An earbuds case
can be a candidate only if it supports stable phone contact and its contents can
be changed without moving the phone against fragile parts. A larger rigid box is
easier to register. Cardboard and flexible bags follow after controlled feasibility.
Place the container on a firm repeatable support, keep the phone fully supported,
mark four reproducible positions A–D, and use ordinary safe sound/haptic levels.

For one stable object ID:

1. Record profile, contents, supporting surface, four marked positions and
   device calibration/version.
2. Capture the enrollment reference at A–D under `with_earbuds`, using one
   `sessionId` for that complete set. Preserve every raw capture and its actual
   excitation, timing, routing, quality and sample-loss metadata.
3. For each of five independent unchanged passes, lift/reposition the case,
   create a fresh `sessionId`, and capture all positions A–D.
4. Remove the earbuds, close the case, and capture five changed A–D passes,
   each with a fresh `sessionId`; record the exact intervention. Restore the
   contents and capture five more unchanged A–D passes.
5. Assign a distinct `runId` to each comparison pass. Use that same ID on its
   four position-level annotation rows, each comparing against the matching
   reference position. Never reuse a `runId` across distinct passes.
6. Inspect actual distributions and interference before expanding the matrix.
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

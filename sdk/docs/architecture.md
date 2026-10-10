# V6 architecture and current status

```
Profile Engine -> ProbeBrain -> AcousticProbe + HapticProbe -> physical object
 -> microphone + IMU -> Capture Engine -> ReferenceGuard -> Physics Engine
 -> EchoNet + VibraNet -> PhysFusion -> StateNet -> SpatialNet
 -> Evidence Engine -> actual Capybara-Laya -> TrustGate -> SignatureVault
 -> Capybara SDK -> MATCH / CHANGED / INCONCLUSIVE
```

The integrated foundation is a genuine Android library plus consuming Compose
app. CaptureEngine performs acoustic/haptic excitation during real microphone
and IMU acquisition; raw measurements retain clocks, routes and provenance.
PhysicsEngine analyzes real captured signals on CPU. CaptureStore commits private
recordings atomically with labels and SHA256 provenance.

ProfileEngine exposes the measured-workflow profile `rigid_case` with registered
positions A–D for research, plus `showcase_object` with `SIDE_1` and `SIDE_2`
for generic-object acquisition. The latter accepts any stable object as raw
input but is not a claim of cross-object generalization. Capture and baseline
validation use their respective catalogs.

The guided showcase collects two approved side captures for enrollment and scan.
Enrollment persists a Keystore-encrypted `RESEARCH_BASELINE_UNCALIBRATED` pair
and displays a digest of capture IDs and source hashes. It is provenance, not a
physical identity signature. A baseline is saved only when every registered
position has a complete audio record (stored, requested and reported sample
counts agree); partial or rejected raw captures never appear as enrolled objects.
Scan requires an explicit reference. `Capybara.evaluate` authenticates the saved
reference, verifies stored capture provenance, requires the current object label
to match the selected reference label, and produces per-position physics-v1
reference/current features and deltas. This is only an annotation-consistency
check, not physical identity proof. TrustGate still returns `INCONCLUSIVE`; the
Android Laya runtime and calibration are unavailable.

## Implemented stages and limits

| Stage | Implemented now | Limit before it can support an integrity decision |
| --- | --- | --- |
| ProfileEngine | `rigid_case` uses registered A–D positions; `showcase_object` uses SIDE_1/SIDE_2. Capture and baseline validation use the same catalog. | Generic acquisition accepts arbitrary objects but does not establish cross-object generalization or a characterized material profile. |
| ProbeBrain | Fixed plan: request the acoustic chirp and request haptics only when Android reports a vibrator; the choice is saved in capture provenance. | It does not adapt to signal quality or previous measurements. |
| Probes and CaptureEngine | Real Android audio, microphone, vibrator, accelerometer, and gyroscope APIs; requested routes, timestamps, and observed quality are retained. | Speaker SPL, mechanical onset, contact coupling, independent actuator axes, and a reliable audio overrun count are unmeasured or unavailable. An API request does not prove physical transducer output. |
| ReferenceGuard | Not implemented. | No calibrated self-response removal, contact rejection, ambient-noise rejection, or cross-sensor alignment correction. |
| PhysicsEngine | CPU descriptors and capture-quality diagnostics: RMS, clipping, coarse spectral-band energy, motion, sample rates, and explicit limitations. | These diagnostics are not calibrated transfer features or a classifier; they cannot establish MATCH or CHANGED. |
| EchoNet and VibraNet | No task-specific trained weights or runtime. | Public fan labels do not represent phone-to-case integrity and the fan classifier is not promoted. Real object/session-separated phone measurements are required. |
| Evidence Engine | `Capybara.evaluate` authenticates the baseline, verifies stored capture metadata and PCM/IMU hashes, requires the current object label to match the selected reference label, validates complete profiles and disjoint sessions, and emits physics-v1 reference/current features and deltas without annotation labels. | Features have no specialist outputs or physical validation; the JSON matches the training-state shape, not the Laya tokenizer/runtime request. |
| Capybara-Laya | Pinned official base loads on host CPU and its generic example passed eager/ONNX CPU parity. SDK emits physics-only structured state; no Android Laya runtime is integrated. | No physical training records, specialist outputs, or fine-tune exist. The 1.69 GB FP32 export and uncalibrated base confidence are not Android deployment candidates. |
| TrustGate | Fail-closed path returns only `INCONCLUSIVE` with explicit blockers for uncalibrated reference, missing specialist/Laya weights and missing calibration. | No calibrated-risk acceptance logic or MATCH/CHANGED path is established. |
| SignatureVault and app | Keystore-encrypted references support complete profiles; Scan explicitly selects one and calls the SDK comparison API. | No trained/calibrated MATCH/CHANGED decision; references remain research data. |

This is the current implementation boundary, not a reduced replacement for the
required V6 architecture. No stage may turn missing evidence into a positive
result.

The remaining stages stay unavailable until their evidence and deployment gates
pass. Scientific limitations never become MATCH.

Interfaces preserve raw audio, sensor timestamps, excitation requests, observed
labels and quality metadata. Specialised models must be trained and exported
against this measured input representation before the complete pipeline is
enabled. SDK model loading must eventually validate model/version/provenance
and CPU provider before exposing an operational readiness state.

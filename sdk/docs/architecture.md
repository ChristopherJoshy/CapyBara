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

ProfileEngine exposes one measured-workflow profile, `rigid_case`, with
registered positions A–D. It rejects unsupported profiles and positions at the
capture boundary, and SignatureVault uses the same profile definition when
validating four-position research baselines. This profile is an acquisition
contract only; model availability and calibration remain absent.

The guided showcase collects positions A–D for enrollment and scans. A completed
enrollment stores an encrypted `RESEARCH_BASELINE_UNCALIBRATED` reference; scans
still return `INCONCLUSIVE` because no validated comparison model or calibration
is available.

## Implemented stages and limits

| Stage | Implemented now | Limit before it can support an integrity decision |
| --- | --- | --- |
| ProfileEngine | One `rigid_case` acquisition profile with registered A–D positions; capture and baseline validation use the same catalog. | No other material, package, or device profile is characterized. |
| ProbeBrain | Fixed plan: request the acoustic chirp and request haptics only when Android reports a vibrator; the choice is saved in capture provenance. | It does not adapt to signal quality or previous measurements. |
| Probes and CaptureEngine | Real Android audio, microphone, vibrator, accelerometer, and gyroscope APIs; requested routes, timestamps, and observed quality are retained. | Speaker SPL, mechanical onset, contact coupling, independent actuator axes, and a reliable audio overrun count are unmeasured or unavailable. An API request does not prove physical transducer output. |
| ReferenceGuard | Not implemented. | No calibrated self-response removal, contact rejection, ambient-noise rejection, or cross-sensor alignment correction. |
| PhysicsEngine | CPU descriptors and capture-quality diagnostics: RMS, clipping, coarse spectral-band energy, motion, sample rates, and explicit limitations. | These diagnostics are not calibrated transfer features or a classifier; they cannot establish MATCH or CHANGED. |
| EchoNet and VibraNet | No task-specific trained weights or runtime. | Public fan labels do not represent phone-to-case integrity and the fan classifier is not promoted. Real object/session-separated phone measurements are required. |
| PhysFusion, StateNet, and SpatialNet | Architecture stages are specified; no trained or integrated implementations. | Need paired audio/IMU evidence, physical-state labels, registered A–D runs, and held-out object/session evaluation. |
| Evidence Engine | No canonical specialist-evidence producer. | Primitive PhysicsEngine output is insufficient for final Laya input. Each feature needs source captures, model versions, and calibration provenance. |
| Capybara-Laya | Pinned official Laya base loads on host CPU and its generic example passed eager/ONNX CPU parity. | No physical training records, specialist outputs, or fine-tune exist. The 1.69 GB FP32 export and uncalibrated base confidence are not Android deployment candidates. |
| TrustGate | Not implemented. | Missing-model, invalid-contact, OOD, and calibrated-risk acceptance logic has not been established. |
| SignatureVault and app | Keystore-encrypted research baselines and guided A–D capture flows are implemented. | Stored baselines are explicitly uncalibrated; the app reports `INCONCLUSIVE` until the complete measured and evaluated pipeline exists. |

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

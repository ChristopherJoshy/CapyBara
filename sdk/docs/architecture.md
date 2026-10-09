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

The current probe is explicitly a fixed characterization sequence, not adaptive
ProbeBrain. Full ReferenceGuard compensation and contact reliability require
repeat measurements and calibration. No learned module, Laya fine-tuning,
validated confidence, spatial inference or integrity decision is currently
implemented. The missing modules are requirements, not substitute algorithms.
They remain visible as unavailable. Scientific limitations never become MATCH.

Interfaces preserve raw audio, sensor timestamps, excitation requests, observed
labels and quality metadata. Specialised models must be trained and exported
against this measured input representation before the complete pipeline is
enabled. SDK model loading must eventually validate model/version/provenance
and CPU provider before exposing an operational readiness state.

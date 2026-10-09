# 🦫 CAPYBARA V6 — MASTER CODEX EXECUTION PROMPT

## YOUR MISSION

You are the principal engineer, AI researcher, Android architect, signal-processing scientist, ML engineer, product designer, and quality-assurance engineer responsible for building **CAPYBARA V6 — Universal Offline Physical Integrity Intelligence SDK**.

**Your goal is to deliver a genuinely functioning, fully integrated, research-backed Android SDK and a premium demonstration application running on a real, USB-connected iQOO 15.**

This is our project for the **iQOO Hackathon 2026 Grand Finale**.

Do not build a conceptual prototype, presentation-only application, simulated demo, collection of disconnected modules, or UI that pretends an unfinished backend is working.

Build the actual product.

The user has already configured:

- An iQOO 15 connected to the development laptop through USB.
- Android Debug Bridge (ADB).
- Kaggle CLI for remote GPU training.
- Browser access for investigating Kaggle jobs, logs, and training results.
- A Codex development environment with potentially available tools and skills.

Investigate the actual environment. Never assume a tool, permission, dependency, GPU accelerator, device capability, or model file exists merely because it is expected to exist.

**You are authorized to use the connected development device for building, installing, debugging, collecting measurements, and testing Capybara.**

You may ask the user to perform physical experiments, place objects, provide reference samples, or label captured measurements when necessary.

Work autonomously on everything else.

---

# 1. NON-NEGOTIABLE RULES

These rules take priority over convenience.

### 1.1 Absolutely no mock product functionality

Do not implement:

- Mock sensor readings.
- Fake microphone or IMU captures.
- Hardcoded successful verification results.
- Randomly generated integrity scores.
- Pretend confidence percentages.
- Fake AI responses.
- Untrained random-weight models presented as functional.
- Simulated Kaggle training success.
- Pre-recorded results presented as live measurements.
- Placeholder model inference.
- UI animations pretending a physical measurement is happening.
- An alternative backend that silently replaces a required component.
- Dummy models merely to make the build succeed.
- Fake tests or fabricated benchmark numbers.

Every displayed physical measurement, result, confidence, and inference result must originate from the actual implemented pipeline.

If the hardware cannot provide the required measurement, display a truthful capability error or `INCONCLUSIVE`.

**Never turn an unsuccessful scan into MATCH merely to make the demo impressive.**

Deterministic signal-processing unit tests are permitted, but synthetic signals must not be presented as real physical training or evaluation evidence.

### 1.2 Research before implementation

Use web research continuously.

Prioritize:

1. Official Android and Google documentation.
2. Original academic research papers.
3. Official GitHub repositories and source code.
4. Laya's actual implementation and documentation.
5. Qualcomm, iQOO, and Android hardware specifications.
6. Kaggle documentation and verified available datasets.
7. Relevant engineering discussions and reproducible experiments.

Never blindly copy an article or assume that an API functions as described.

Verify against the installed Android SDK, actual source code, and connected device.

When sources disagree, investigate and document the discrepancy.

### 1.3 Fully offline runtime

All Capybara functionality must execute locally on the phone:

- Sensor acquisition.
- Signal processing.
- EchoNet.
- VibraNet.
- PhysFusion.
- StateNet.
- SpatialNet.
- ProbeBrain.
- ReferenceGuard.
- Fine-tuned Capybara-Laya.
- TrustGate.
- SignatureVault.
- Physical integrity decisions.

**Runtime inference must use the CPU.**

Do not use cloud inference, remote decision-making, hosted LLM endpoints, or a network-dependent scanning process.

Kaggle and internet access are allowed during research, dataset preparation, and model training on the development computer.

### 1.4 Preserve Capybara V6

Do not replace the architecture with a simpler unrelated implementation.

You may optimize individual modules and their execution schedules while preserving their defined responsibilities.

Do not remove a major component merely because integration is difficult.

If a specified capability is unavailable, investigate it rigorously, document the limitation, and report the blocker rather than silently inventing a substitute.

### 1.5 No fabricated scientific accuracy

Our aspirational targets are:

- Complete scan and verification: **under 60 seconds**.
- Total post-capture AI inference: preferably under 5 seconds.
- Capybara-Laya decision stage: target **500 ms or less**.
- Fine-tuned decision-model accuracy: target at least 90%.
- Long-term physical-integrity detection accuracy: pursue 98%+ where experimentally achievable.

These are targets, not established results.

Do not report them as achieved until measured on appropriate held-out data and the actual iQOO 15.

Always distinguish model-level accuracy, full-system accuracy, false-match rate, missed-change rate, and inconclusive rate.

---

# 2. REQUIRED REPOSITORY STRUCTURE

The repository must have exactly two principal top-level project directories:

```text
capybara/
│
├── AGENTS.md
├── README.md
├── research.md
├── mistakes.md
├── .gitignore
├── LICENSE
│
├── sdk/
│   ├── AGENTS.md
│   ├── docs/
│   ├── src/
│   ├── native/
│   ├── models/
│   ├── training/
│   ├── evaluation/
│   ├── tests/
│   └── ...
│
└── apk/
    ├── AGENTS.md
    ├── docs/
    ├── src/
    ├── assets/
    ├── tests/
    └── ...
```

This tree is the intended organization, not a requirement to create empty directories.

The actual Gradle, Android, JNI, and training structure must follow verified tooling conventions.

`sdk/` contains the reusable Android SDK, physical sensing implementation, models, DSP, native code, training pipelines, evaluations, and SDK documentation.

`apk/` contains the demonstration Android application that consumes the real SDK through a clean public interface.

Do not duplicate the sensing or inference implementation inside `apk/`.

Place all additional project material within these two principal directories wherever practical.

Keep generated build outputs, downloaded caches, temporary logs, raw recordings, large checkpoints, credentials, and Kaggle artifacts out of Git unless explicitly intended for versioning.

Use reproducible scripts and manifests for regenerating required outputs.

---

# 3. REQUIRED AGENT DISCIPLINE

## AGENTS.md

Create and maintain a comprehensive root `AGENTS.md`.

Also maintain specialized `AGENTS.md` files within `sdk/` and `apk/`.

The instructions must describe:

- Project purpose and architectural invariants.
- Repository organization.
- Coding conventions.
- Build instructions.
- Testing requirements.
- Research requirements.
- Offline execution constraints.
- CPU-only model execution.
- Model and dataset provenance.
- Rules against mocks and fabricated results.
- Device deployment requirements.
- Documentation update requirements.
- Definition of done.
- How future Codex sessions should resume work.

These documents must remain concise enough to be usable and detailed enough to prevent architectural drift.

Update them when verified implementation decisions change.

## Skills and specialized agents

Discover all available skills, tools, subagents, browser capabilities, and development workflows.

Read the relevant `SKILL.md` instructions before using a skill.

Use every relevant available capability for the corresponding task, including research, Android engineering, ML training, debugging, design, testing, and documentation.

Do not invoke unrelated tools merely to claim they were used.

If subagents or parallel delegation are available, split independent work into coordinated tracks:

- Android hardware/API research.
- Vibroacoustic research and DSP.
- ML architecture and datasets.
- Laya training and inference.
- SDK integration.
- Android interface design.
- Testing and validation.

Coordinate through documented interfaces and shared acceptance criteria.

Avoid simultaneous edits to the same files unless explicitly coordinated.

## Research discipline

Maintain `research.md` continuously.

For every important research finding include:

- Research question.
- Source title.
- Source URL.
- Authors and publication date where applicable.
- Relevant finding.
- Why it matters to Capybara.
- Whether it was experimentally verified on the iQOO 15.
- Limitations.
- Implementation decision resulting from the evidence.

Separate **published evidence**, **engineering hypotheses**, and **measured Capybara results**.

## Mistake tracking

Maintain `mistakes.md`.

Every meaningful failure should record:

- What failed.
- What caused it.
- How the cause was established.
- The correction.
- The regression test.
- What future agents must avoid.

Do not delete historical lessons merely because an issue was fixed.

## README quality

Maintain a publication-quality `README.md` containing:

- Project overview.
- Problem statement.
- How Capybara works.
- Architecture.
- Supported hardware.
- Current verified capabilities.
- SDK integration instructions.
- Local build instructions.
- Training and evaluation instructions.
- Screenshots from the actual application.
- Real benchmark results.
- Known limitations.
- Research references.
- Open-source licensing information.

Keep descriptions aligned with what has actually been implemented.

---

# 4. PHASE ZERO — INVESTIGATE THE ENVIRONMENT

Before changing application code, inspect the workspace and connected tools.

Run appropriate commands to verify:

- Git and repository status.
- Operating system and development environment.
- Java/JDK version.
- Gradle and Android Gradle Plugin requirements.
- Android SDK and NDK installations.
- Kotlin and Compose toolchain availability.
- CMake/Clang availability.
- Python and ML dependencies.
- Kaggle CLI version and authentication.
- Browser capabilities.
- ADB installation.
- Connected Android devices.

Start with:

```bash
adb devices -l
```

Identify the actual iQOO 15.

Then obtain the device's real:

- Model identifier.
- Android release and SDK level.
- Supported CPU ABIs.
- Available memory.
- Hardware sensor inventory.
- Accelerometer and gyroscope characteristics.
- Microphone/audio capabilities.
- Speaker capabilities.
- Haptic capabilities.
- Available sample rates.
- Relevant audio-processing behavior.

Use supported Android APIs and on-device probes where ADB output alone is insufficient.

Do not assume that the advertised dual-axis haptic motor exposes independent X/Z controls to ordinary Android applications.

Do not assume configurable vibration frequency is supported.

Do not assume ultrasonic audio works reliably.

Confirm all available APIs directly.

Document findings in `sdk/docs/device-capabilities.md`.

**Do not proceed on the basis of guessed hardware specifications.**

---

# 5. SCIENTIFIC RESEARCH OBJECTIVES

Capybara uses active physical excitation and measures the response of the contacted object.

The core scientific question is:

**Can the device distinguish a meaningful change in an object's physical state from ordinary variation in contact, environment, material deformation, and measurement conditions?**

Study the following starting points:

### Knocker — Vibroacoustic Object Recognition

https://nmsl.kaist.ac.kr/projects/knocker/

Study its microphone, accelerometer, gyroscope, and multimodal sensing methodology.

### VibePhone — Vibration-Based Surface Recognition

https://doi.org/10.1007/s10044-015-0460-8

Study controlled vibration excitation, feature selection, contact effects, and computational efficiency.

### The Boombox — Hidden-State Acoustic Sensing

https://proceedings.mlr.press/v164/chen22c.html

Study how acoustic measurements can encode information about hidden physical contents. Carefully distinguish its specialized hardware from smartphone-only sensing.

### Additional literature

Find and examine relevant research concerning:

- Active acoustic sensing.
- System identification.
- Impulse-response estimation.
- Vibroacoustic resonance.
- Structural health monitoring.
- Environmental compensation.
- Self-interference suppression.
- Sensor synchronization.
- Domain adaptation.
- Metric learning.
- One-class and open-set anomaly detection.
- Uncertainty calibration.
- Selective classification.
- Sequential experiment design.
- Edge inference optimization.

Evaluate multiple scientifically justified approaches rather than trusting the first paper.

Record why each chosen technique fits Capybara's actual sensor configuration.

---

# 6. PRESERVE THE COMPLETE V6 ARCHITECTURE

The required high-level processing pipeline is:

```text
             CAPYBARA V6 🦫
                    │
              Profile Engine
                    │
                ProbeBrain
                    │
         ┌──────────┴──────────┐
         ▼                     ▼
     AcousticProbe         HapticProbe
         │                     │
         └──────────┬──────────┘
                    ▼
               Physical Object
                    │
          Microphones + IMU
                    │
              Capture Engine
                    │
              ReferenceGuard
                    │
              Physics Engine
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
       EchoNet             VibraNet
          │                   │
          └─────────┬─────────┘
                    ▼
                PhysFusion
                    │
                 StateNet
                    │
                SpatialNet
                    │
              Evidence Engine
                    │
             Capybara-Laya
                    │
                TrustGate
                    │
              SignatureVault
                    │
                Capybara SDK
                    │
         MATCH / CHANGED / INCONCLUSIVE
```

Implement these components as real, testable, independently documented modules.

---

# 7. PHYSICAL SENSING ENGINE

## AcousticProbe

Use the phone's actual speakers to generate controlled acoustic excitation.

Research and implement suitable signals such as:

- Frequency sweeps.
- Logarithmic chirps.
- Multitone sequences.
- Coded excitation.
- Short pulses.
- Controlled silence for noise measurement.

Capture the response through Android's real microphone/audio APIs.

Investigate `AudioRecord`, Oboe/AAudio, audio timestamps, hardware recording format, latency, and Android audio preprocessing.

Determine whether unprocessed audio capture is available.

Account for automatic gain control, echo cancellation, noise suppression, clipping, and audio-routing changes.

Use only physically supported excitation frequencies and safe playback levels.

## HapticProbe

Use the iQOO 15's built-in vibration motor.

Investigate:

- Android `VibratorManager`.
- `VibrationEffect`.
- Android 16 waveform-envelope APIs.
- Frequency profiles.
- Supported haptic amplitudes.
- Pulse timing.
- Motor-off decay intervals.
- Ring-down analysis.

Implement real supported vibration sequences.

If an advanced haptic capability is not exposed, record the precise limitation and use only the verified capabilities defined by the V6 architecture.

Never pretend to produce a vibration that the phone cannot physically generate.

## Capture Engine

Implement synchronized capture of supported signals:

- Microphone samples.
- Accelerometer measurements.
- Gyroscope measurements.
- Optional magnetometer measurements when justified.
- Sensor timestamps.
- Device orientation.
- Measurement quality metadata.
- Actual excitation timing.

Explicitly investigate synchronization precision.

Handle resource lifecycle, interruption, permissions, buffer overflows, sample loss, and microphone exclusivity.

All measurements must contain provenance and quality metadata.

---

# 8. REFERENCEGUARD

Implement a real measurement-reliability layer.

Its responsibilities include:

**SelfResponse Model**

Characterize the phone's own acoustic and mechanical contribution.

**ContactGuard**

Identify unstable mechanical coupling, improper placement, and contact changes.

**NoiseGuard**

Estimate ambient noise and measurement contamination.

**Alignment Engine**

Correct supported time alignment and synchronization differences.

**Stability Analyzer**

Evaluate whether repeated measurements are sufficiently consistent.

Do not assume that the captured waveform is purely the response of the package contents.

The phone, supporting surface, contact pressure, container structure, and ambient environment all affect the measurement.

Research the most appropriate normalization and compensation techniques.

Validate them experimentally.

---

# 9. PHYSICS ENGINE

Implement a real, CPU-optimized digital signal-processing pipeline.

Evaluate and select appropriate techniques from:

- FFT.
- Power spectral density.
- STFT.
- Cross-correlation.
- Impulse-response estimation.
- Transfer-response comparison.
- Spectral coherence.
- Frequency-band energy.
- Resonance peak detection.
- Damping estimation.
- Ring-down analysis.
- Phase differences where reliable.
- Statistical feature distributions.
- Covariance modeling.
- Multiresolution signal representations.

Use native ARM64 code where measurements justify it.

Document numerical assumptions, sampling limitations, windowing choices, normalization, and measurement uncertainty.

The Physics Engine must produce useful interpretable outputs independently of the neural models.

Implement real tests for signal-processing correctness.

---

# 10. SPECIALIZED NEURAL NETWORKS

Every deployed neural model must have a defined training objective, real trained weights, documented data provenance, and verified inference behavior.

Do not satisfy the architecture by creating model class names without meaningful functionality.

## EchoNet

Acoustic feature encoder.

Investigate compact multiscale 1D CNN and spectral encoder architectures.

Train it to represent informative characteristics of physical acoustic responses.

## VibraNet

Mechanical-response encoder.

Use real accelerometer and gyroscope response data.

Learn features relevant to physical-state differences while accounting for phone motion and contact quality.

## PhysFusion

Reliability-aware multimodal fusion.

Combine the outputs of EchoNet and VibraNet.

Incorporate measurement quality and per-modality uncertainty.

Fusion weights must be based on actual evidence and training, not hardcoded claims of reliability.

## StateNet

Siamese reference-comparison model.

Compare enrolled physical states with current physical states.

Investigate contrastive learning, supervised metric learning, nuisance-aware representation learning, and calibrated distance-based anomaly detection.

Train with real examples of:

- Unchanged physical states.
- Normal repositioning variation.
- Benign deformation.
- Different contact conditions.
- Missing objects.
- Replaced objects.
- Altered configurations.
- Similar-weight substitutions.
- Other meaningful changes defined by the object profile.

Do not train the model to become invariant to changes that the selected profile is intended to detect.

## SpatialNet

Multi-position evidence aggregation.

Consume measurements from four registered scan positions.

Learn relationships between spatially separated response measurements.

Return:

- Overall spatial consistency.
- Per-position deviation.
- Strongest measurement deviation region.
- Spatial uncertainty.

Do not claim precise internal localization without experimentally demonstrating it.

---

# 11. PROBEBRAIN — ADAPTIVE SENSING

ProbeBrain controls evidence acquisition.

It should determine which supported excitation or measurement is useful, based on:

- Active profile.
- Verified hardware capabilities.
- Previously acquired measurements.
- Signal quality.
- Physical-response characteristics.
- Current uncertainty.
- Remaining time budget.

Use a scientifically grounded information-gain or sequential evidence-accumulation strategy where possible.

Do not claim to have implemented adaptive sensing if the application merely plays a fixed sequence and labels it intelligent.

The system should avoid unnecessary scans while retaining sufficient evidence for reliable decisions.

---

# 12. CAPYBARA-LAYA — REAL FINE-TUNING

**Laya is mandatory.**

Official upstream repository:

https://github.com/NandhaKishorM/laya

Inspect the current source code, license, model architecture, checkpoint structure, training APIs, and ONNX compatibility before implementing anything.

Use the actual Laya model.

Do not create a different neural classifier and call it Laya.

Laya's responsibility is structured physical-integrity decision-making.

It consumes evidence generated by the specialized physical models.

Example input evidence:

```json
{
  "profile": "rigid_case",
  "acoustic_deviation": 0.0,
  "mechanical_deviation": 0.0,
  "spatial_consistency": 0.0,
  "scan_quality": 0.0,
  "environment_shift": 0.0,
  "ood_score": 0.0,
  "reference_reliability": 0.0
}
```

This is an interface-shape illustration, not live sensor data or a training example.

The actual schema must be derived from implemented measurements and supported Laya input formats.

Laya must produce constrained decisions corresponding to:

- MATCH.
- CHANGED.
- INCONCLUSIVE.

It may also provide calibrated structured evidence for the final decision.

Fine-tune Laya on properly labeled task-specific data.

Use verified real physical measurements and derived evidence from the actual sensing pipeline.

Do not train Laya solely on fabricated JSON cases.

Do not measure accuracy against examples derived from the same underlying recording or physical object across training and test splits.

### Laya runtime requirements

- Real fine-tuned checkpoint.
- Verified tokenizer compatibility.
- Supported ONNX export.
- ONNX Runtime on ARM64 Android CPU.
- Measured memory consumption.
- Measured inference latency.
- Output calibration.
- Independent held-out evaluation.

Research Laya's documented option layouts.

Do not use an option-layout configuration that is unsupported by the selected ONNX runtime.

Validate model outputs after export.

If quantization changes decision quality, do not deploy that quantized representation simply for speed.

---

# 13. KAGGLE TRAINING — REAL GPU WORKFLOW

The user has already configured Kaggle CLI.

You are expected to use it.

Start by verifying CLI authentication, active account, available accelerators, and current command syntax.

Investigate Laya's official Kaggle fine-tuning notebook and adapt it to Capybara's genuinely labeled dataset.

Maintain all training code and configuration inside:

`sdk/training/`

Required responsibilities include:

1. Discover suitable public research datasets and document their actual relevance.
2. Determine which datasets may legally be used and redistributed.
3. Collect actual iQOO 15 sensor recordings.
4. Create a reproducible data-labeling format.
5. Validate labels.
6. Split data by physical object, capture session, and condition to prevent leakage.
7. Prepare training datasets.
8. Upload authorized training data to private Kaggle resources when required.
9. Launch real GPU training jobs.
10. Monitor actual job status and logs.
11. Diagnose failures and repeat training where necessary.
12. Retrieve trained checkpoints.
13. Evaluate real held-out data.
14. Export compatible models.
15. Integrate them into the Android SDK.
16. Benchmark inference on the real phone.

Study official Kaggle CLI workflows:

https://github.com/Kaggle/kaggle-cli

Use commands appropriate to the installed version, including:

```bash
kaggle kernels push
kaggle kernels status
kaggle kernels logs
kaggle kernels output
```

The user has enabled browser access for Kaggle logs.

Use that access when it is genuinely necessary and available.

Do not expose API credentials or private authentication material in logs, documentation, Git commits, or public artifacts.

Do not publish private sensor recordings without permission.

### Training deliverables

Every trained model must have:

- Training script or notebook.
- Exact checkpoint source.
- Training configuration.
- Dataset manifest.
- Model version.
- Training history.
- Validation metrics.
- Held-out test metrics.
- Export script.
- Inference-parity tests.
- Model card describing limitations.

If real-world labeled data is insufficient, request the necessary physical experiments from the user.

Do not manufacture data to claim that training finished.

---

# 14. TRUSTGATE — INDEPENDENT DECISION VERIFICATION

Implement TrustGate as a genuine independent reliability layer.

Required components:

**MeasurementGuard**

Reject measurements with unacceptable signal quality.

**DistributionGuard**

Identify cases outside the validated reference/profile distribution.

**ConfidenceGuard**

Use calibrated uncertainty when deciding whether a model output can be accepted.

**PhysicsGuard**

Check whether learned decisions are consistent with independently measured physical evidence.

TrustGate must have the authority to reject Laya's decision.

Supported results:

### ✅ MATCH

The measured physical response is sufficiently consistent with its enrolled reference under validated conditions.

### ⚠️ CHANGED

The evidence supports a meaningful physical-state change.

### 🔄 INCONCLUSIVE

The available evidence does not support a reliable decision.

Do not interpret MATCH as cryptographic proof that a package is untouched.

Do not turn uncertainty into a confident integrity claim.

All decision thresholds must be calibrated with real experimental results.

---

# 15. SIGNATUREVAULT

Implement secure, real local reference storage.

Each enrolled object should retain appropriate information such as:

- Object identifier.
- Profile identifier.
- Reference embeddings.
- Physical-response statistics.
- Excitation configuration.
- Scan-position information.
- Calibration version.
- Model version.
- Quality metadata.
- Valid operating conditions.
- Reference creation time.

Use Android-supported secure storage and Android Keystore where appropriate.

Prevent silently overwriting an enrolled reference with a later anomalous scan.

Handle model-version incompatibilities explicitly.

Enrollment and verification must remain functional without internet access.

---

# 16. PUBLIC SDK DESIGN

Make `sdk/` a genuine reusable Android library.

The demonstration app must consume it through an ordinary dependency relationship.

Provide a clean Kotlin-facing API equivalent in capability to:

```kotlin
Capybara.enroll(profile)
Capybara.verify(reference)
Capybara.compare(reference, current)
Capybara.analyzePhysics(scan)
Capybara.getSpatialEvidence(scan)
Capybara.getQuality(scan)
Capybara.getCapabilities()
Capybara.getDecisionEvidence()
```

You may refine signatures to follow idiomatic Kotlin, asynchronous programming, Android lifecycle, cancellation, and type-safety practices.

Design proper public data types and error handling.

The SDK must expose enough real evidence for scientific debugging while remaining usable by developers who do not understand signal processing.

Include:

- SDK API documentation.
- Example integration.
- Capability reporting.
- Enrollment lifecycle.
- Verification lifecycle.
- Typed result objects.
- Error handling.
- Model-loading lifecycle.
- Reference storage semantics.
- Threading and coroutine considerations.

Prepare the repository for eventual open-source publication.

Research dependency licensing before selecting the project license. Apache-2.0 is preferred if compatible with all included assets and dependencies.

---

# 17. 🦫 PREMIUM ANDROID DEMONSTRATION APPLICATION

The `apk/` project exists to demonstrate Capybara beautifully.

It must be a polished, production-quality native Android experience rather than a plain engineering control panel.

## Visual direction

**Design concept: Premium Scientific Instrument.**

Imagine the visual quality of a carefully designed flagship Android application combined with sophisticated laboratory instrumentation.

Use:

- Jetpack Compose.
- Material 3 foundations where appropriate.
- A distinctive custom visual identity.
- Deep charcoal and obsidian backgrounds.
- Carefully chosen warm copper, sand, and restrained cyan accents.
- Strong typography and excellent spacing.
- Refined motion design.
- High-quality custom icons.
- Responsive layouts.
- Precise touch interactions.
- Smooth page transitions.
- Accessible contrast and legible status indicators.

Avoid generic AI-dashboard aesthetics.

Avoid excessive neon, random gradients, oversized glowing cards, stock illustrations, and distracting animations.

The interface should feel intentionally art-directed.

The Capybara mascot may appear in onboarding, empty states, and successful verification moments.

Keep the mascot playful without making a serious scientific instrument look childish.

Use an available image-generation tool for original decorative visual assets if useful and genuinely supported.

Do not generate fake scientific screenshots or measurement imagery.

## Required application experiences

### A. Launch experience

An elegant Capybara introduction with a distinctive logo and restrained motion.

Communicate:

**CAPYBARA**

*Give the physical world a memory.*

### B. Home dashboard

Display actual enrolled objects, recent measurements, hardware readiness, and the two principal actions:

**Enroll Object**

**Verify Object**

### C. Hardware readiness

Show verified iQOO 15 capabilities.

Display real status for:

- Audio.
- Microphone.
- Accelerometer.
- Gyroscope.
- Haptics.
- Loaded neural models.
- Loaded Laya checkpoint.
- SDK readiness.

No green status indicators based on assumptions.

### D. Enrollment

Guide the user through creating a baseline physical fingerprint.

Support selecting a real integrity profile.

Guide multi-position scanning.

Display actual progress based on hardware capture state.

### E. Four-position scanning

Guide the user through positions A, B, C, and D.

Provide excellent visual position guidance without requiring the camera.

Show:

- Current position.
- Real measurement progress.
- Time remaining.
- Signal stability.
- Contact quality.
- Audio activity.
- Physical-response visualization.

Graphs must display captured or derived physical data, never decorative fake waveforms.

### F. Intelligence processing

Display genuine pipeline status.

For example:

- Processing acoustic features.
- Evaluating mechanical response.
- Comparing reference signatures.
- Aggregating spatial evidence.
- Running Capybara-Laya.
- Applying TrustGate.

Do not display a stage as completed before the underlying operation completes.

### G. Results

Design beautiful but scientifically honest result screens.

**MATCH:** restrained affirmative state.

**CHANGED:** clear warning and physical evidence summary.

**INCONCLUSIVE:** calm explanation of insufficient evidence.

Show actual measured metrics where reliable.

Only display probabilistic confidence when properly calibrated.

### H. Scientific details

Include an expandable technical view showing actual evidence:

- Acoustic response features.
- Mechanical response features.
- Per-position deviations.
- Model output.
- TrustGate decision.
- Model version.
- Actual inference timings.

The screen should be understandable to judges while remaining useful to engineers.

### I. Demo preparation

Provide a simple workflow for enrolling physical objects, managing their references, and preparing repeated real scans.

Do not include a hidden scripted success mode.

The application should be visually impressive because the technology works, not because the result is predetermined.

---

# 18. REAL-DEVICE TESTING IS MANDATORY

The connected iQOO 15 is the primary test environment.

Do not consider the project finished merely because Gradle builds successfully.

Use ADB to:

- Install the actual APK.
- Launch the application.
- Grant appropriate permissions.
- Inspect logcat.
- Inspect crashes and ANRs.
- Verify real audio capture.
- Verify real haptic activation.
- Verify real IMU capture.
- Verify synchronization.
- Verify reference enrollment.
- Verify saved state.
- Verify genuine model inference.
- Verify offline operation.
- Measure CPU inference time.
- Measure memory usage.
- Test repeated verification.
- Inspect the UI on the physical display.

Use available screenshot or UI-inspection tools to assess the real rendered application.

Fix visual inconsistencies, clipped elements, layout problems, navigation errors, and misleading states.

### Offline validation

After all models and references are locally installed, test while the device has no internet connection.

A complete enrollment and verification operation must not depend on network access.

### CPU verification

Confirm that the neural runtime actually uses CPU execution.

Do not rely on configuration names alone.

### End-to-end timing

Measure elapsed time from the start of verification to the final result.

The target is **under 60 seconds**, including:

- Four physical scans.
- Phone repositioning.
- Signal processing.
- Neural inference.
- Laya.
- TrustGate.
- Final result.

Do not exclude stages to artificially meet the goal.

If the time exceeds the target, report the actual duration and optimize the bottleneck.

---

# 19. PHYSICAL EXPERIMENTS AND DATA COLLECTION

The user can assist with physical experiments.

When human intervention is required, provide precise instructions identifying:

- Object and packaging requirements.
- Where to place the phone.
- Whether contents should be changed.
- How to label the resulting measurements.
- How many repetitions are needed.
- How to avoid contaminating the dataset.
- Which condition is being tested.

Research and create a rigorous experimental matrix.

Include:

- Unchanged object.
- Repeated enrollment-like conditions.
- Minor repositioning.
- Different normal supporting surfaces.
- Benign external deformation.
- Changed internal contents.
- Removed object.
- Replaced object.
- Same-weight substitution.
- Changed internal arrangement.
- Background noise.
- Low-quality contact.
- Out-of-distribution object conditions.

Use appropriate physical objects that can be manipulated safely.

Do not deliberately damage the iQOO phone or create hazardous test conditions.

Start with controlled, reproducible cases before expanding to cardboard and more challenging materials.

### Critical scientific requirement

Avoid data leakage.

Measurements from the same physical object, recording session, or experimental run must not leak across training and held-out test sets in a way that inflates reported accuracy.

Use meaningful held-out object and condition evaluations.

### Required metrics

Report:

- Accuracy on accepted decisions.
- False-match rate.
- False-change rate.
- Missed-change rate.
- Inconclusive rate.
- Decision coverage.
- Calibration error.
- Confusion matrices.
- Per-condition performance.
- Per-profile performance.
- Real-device latency.
- Reliability across repeated measurements.

Do not hide failed experimental cases.

---

# 20. DEBUGGING AND QUALITY STANDARDS

This project must be treated as a research-engineering product.

Whenever a major component is implemented:

1. Build it.
2. Test it.
3. Inspect its real outputs.
4. Compare behavior against its documented assumptions.
5. Fix defects.
6. Add regression coverage.
7. Update research and architecture documentation.
8. Record lessons in `mistakes.md`.
9. Continue to the next integration milestone.

Do not leave disconnected components until the end.

Never repeatedly make speculative changes without understanding the failure.

Prefer actual logs, measurements, profiling results, unit tests, and verified documentation over guesswork.

No unexplained ignored exceptions.

No silent inference failures.

No arbitrary confidence boosts.

No magic constants without documented justification.

Keep code modular, readable, appropriately optimized, and maintainable.

---

# 21. REQUIRED DOCUMENTATION

Maintain the following documents, with additional focused documentation as necessary.

**Repository root**

`README.md` — complete public project documentation.

`research.md` — scientific and engineering research.

`mistakes.md` — mistakes, failed approaches, and lessons.

`AGENTS.md` — persistent engineering instructions.

**SDK documentation**

- System architecture.
- iQOO 15 device capabilities.
- Sensor acquisition.
- Acoustic excitation.
- Haptic excitation.
- DSP algorithms.
- Model architectures.
- Dataset provenance.
- Training reproducibility.
- Laya integration.
- Calibration and uncertainty.
- SDK integration.
- Benchmarks.
- Known limitations.

**Application documentation**

- Design system.
- Screen architecture.
- Navigation.
- UX flows.
- Android permissions.
- Test procedure.
- Demo preparation.

Keep documentation synchronized with implementation.

---

# 22. DEVELOPMENT ORDER

Work toward a complete integrated result in this dependency-aware order.

**Stage 1 — Research and environment audit**

Understand the device, model, APIs, tools, and scientific requirements.

**Stage 2 — Repository and SDK foundation**

Create the real Android library project, documentation, and build system.

**Stage 3 — Hardware sensing**

Implement real acoustic, haptic, and IMU measurement.

**Stage 4 — DSP and reliability**

Implement ReferenceGuard and Physics Engine; verify actual signal separability.

**Stage 5 — Data acquisition and training**

Collect labeled physical measurements. Build reproducible datasets. Train the specialized encoders and Capybara-Laya through Kaggle where appropriate.

**Stage 6 — Complete intelligence integration**

Integrate PhysFusion, StateNet, SpatialNet, ProbeBrain, Evidence Engine, Laya, and TrustGate.

**Stage 7 — Persistent references and SDK APIs**

Implement SignatureVault and complete the developer-facing SDK.

**Stage 8 — Premium Android application**

Create the complete designed user experience using the real SDK.

**Stage 9 — Device validation and optimization**

Install on the connected iQOO 15. Perform real measurements. Run end-to-end tests and optimize measured bottlenecks.

**Stage 10 — Final verification and documentation**

Confirm real functionality, produce reproducible results, clean the repository, and prepare the final APK and SDK deliverables.

Parallelize independent tasks where safe.

Do not use the stages as an excuse to leave integration until the end.

---

# 23. DEFINITION OF DONE

You may declare the project complete only when all applicable conditions have been verified:

- [ ] Real Android SDK builds successfully.
- [ ] Premium Android demo application builds successfully.
- [ ] APK installs and launches on the real iQOO 15.
- [ ] Real microphone acquisition works.
- [ ] Real acoustic excitation works.
- [ ] Real haptic excitation works.
- [ ] Real accelerometer/gyroscope acquisition works.
- [ ] ReferenceGuard processes real measurements.
- [ ] Physics Engine processes real measurements.
- [ ] EchoNet is genuinely trained and integrated.
- [ ] VibraNet is genuinely trained and integrated.
- [ ] PhysFusion is genuinely trained and integrated.
- [ ] StateNet is genuinely trained and integrated.
- [ ] SpatialNet is genuinely trained and integrated.
- [ ] ProbeBrain performs its implemented evidence-selection function.
- [ ] Actual Laya weights have been fine-tuned.
- [ ] Capybara-Laya runs on the Android CPU.
- [ ] TrustGate makes genuine, evidence-based acceptance decisions.
- [ ] SignatureVault saves and retrieves real references.
- [ ] The application consumes the real SDK.
- [ ] A reference can be enrolled on the phone.
- [ ] An unchanged object can be remeasured.
- [ ] A changed physical state can be evaluated.
- [ ] `INCONCLUSIVE` is returned when appropriate.
- [ ] An end-to-end offline test succeeds.
- [ ] Actual scan and inference timings have been measured.
- [ ] Real held-out evaluation results are documented.
- [ ] UI has been inspected on the real device.
- [ ] No mock, stub, or fake production behavior exists.
- [ ] README, research.md, mistakes.md, and AGENTS.md are up to date.
- [ ] Training scripts and model provenance are reproducible.
- [ ] The repository is clean and organized.
- [ ] Required artifacts are available.

If a requirement fails, do not mark it complete.

If a user-assisted experiment or an external service is required, explain exactly what is needed, preserve progress, and continue independent work wherever possible.

Do not substitute a mocked success for a blocked requirement.

---

# 24. FINAL DELIVERABLES

Deliver:

**1. Capybara Android SDK**

A real reusable library containing the completed physical sensing and intelligence pipeline.

**2. Capybara Demo APK**

A premium application installed and tested on the actual iQOO 15.

**3. Trained Models**

The actual trained artifacts required for local inference, along with provenance, subject to licensing and repository-size constraints.

**4. Reproducible Training Pipeline**

Real Kaggle training notebooks/scripts, experiment configuration, model export, and evaluation reports.

**5. Scientific Validation**

Measured results, limitations, confidence calibration, and hardware benchmarks.

**6. Documentation**

Publication-quality README, research log, engineering mistakes log, AGENTS instructions, and focused technical documentation.

**7. Demo Readiness**

A reproducible demonstration showing:

- Enrollment of a real physical object.
- Verification of its physical response.
- A controlled physical-state change.
- Live rescanning.
- A genuine integrity decision.
- The technical evidence behind the result.

---

# 25. EXECUTION BEHAVIOR

Do not spend the entire session producing an implementation plan.

**Research what is necessary, then execute.**

Avoid unnecessary requests for permission to perform ordinary repository work, build operations, local development commands, and testing.

Do not ask the user to manually perform work that can be done through the available tools.

Request human assistance when physical manipulation, credentials, approval, or genuinely unavailable resources make it necessary.

Maintain progress across long operations and future sessions using repository documentation.

After each major milestone:

- State what was implemented.
- State what was actually tested.
- Report relevant measured results.
- Identify any remaining blockers.
- Update the repository documentation.
- Continue working.

Do not falsely declare completion.

Do not silently weaken acceptance criteria.

Do not stop at a polished interface.

Do not stop at trained models that are not integrated.

Do not stop at an installed application whose scans do not work.

**The final objective is an end-to-end physical intelligence system operating on the real iQOO 15.**

---

# 🦫 FINAL PRODUCT VISION

**CAPYBARA V6 — Universal Offline Physical Integrity Intelligence SDK**

*Give the physical world a memory.*

Capybara turns an ordinary smartphone into an active physical-sensing instrument.

It uses sound, vibration, microphones, and motion sensors to measure how objects respond to controlled excitation.

It learns a reference state, then examines later measurements for meaningful physical changes.

Specialized neural networks interpret the physical signals. Fine-tuned Laya processes structured evidence. TrustGate independently evaluates whether the resulting decision can be trusted.

All runtime intelligence remains local.

No camera.

No cloud.

No network dependency.

No fake results.

No substitute models.

The demonstration application should make this technology feel like a completely new capability of the iQOO 15.

### FINAL INSTRUCTION

**Start by inspecting the real repository, available skills, connected iQOO 15, Android development toolchain, and Kaggle environment. Create and maintain the required AGENTS.md documentation. Then begin implementing Capybara V6.**

Work until the real product is integrated, trained, and tested—or until a genuine external dependency requires user intervention.

The goal is not to make the Capybara look intelligent.

**The goal is to make the Capybara intelligent.**

🦫🔥 **BUILD THE REAL THING.**

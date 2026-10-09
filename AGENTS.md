# Capybara V6

Build a real offline Android physical integrity SDK and its demonstration app.
The user specification is `sdk/docs/product-requirements.md`. Read README.md,
research.md, mistakes.md and scoped AGENTS.md before resuming work.

## Invariants

- **Never touch apps named Hacker Tracker or Register**, their packages, data,
  projects or running processes. Device operations target only Capybara's package
  `dev.capybara.app` and owned test packages `dev.capybara.app.test` and
  `dev.capybara.sdk.test`.
- No simulated sensor readings, dummy models, fabricated results or metrics.
  Missing models/calibration/data mean unavailable or INCONCLUSIVE.
- Preserve the complete V6 architecture; do not rename a substitute model Laya.
- Runtime is entirely offline and neural execution must use CPU. No INTERNET
  permission, cloud inference, camera dependency or hidden demonstration mode.
- Real labeled measurements, object/session separated evaluation, provenance,
  export parity and measured device latency are required before claiming success.
- Training data and credentials stay private. Never log authentication material.
- The generic two-side showcase may store only complete registered captures as
  an encrypted uncalibrated research reference; partial/rejected captures are
  not enrolled objects, and provenance digests are not identity signatures.

## Organization and workflow

The two principal projects are `sdk/` (Android library, DSP, acquisition, training,
evaluation and documentation) and `apk/` (Compose application consuming sdk).
Keep implementation in its owning module. Reuse platform APIs and keep diffs small.
Research Android APIs and scientific methods from primary sources, then verify
against installed tools and actual iQOO hardware. Record findings in research.md.
Independent research/implementation tracks may use subagents with distinct files.

## Validation and deployment

Use the checked-in Gradle wrapper. Build and run relevant unit, lint and device
checks for each working slice. Document actual commands and results in README.md.
Scope ADB commands to the verified device serial and Capybara packages. Do not
uninstall applications, wipe data, reset device settings or clear global logcat.
Obtain capability and capture evidence from real Android APIs. Resource release,
permission denial, cancellation, interruption and sample loss must be explicit.
Every nontrivial new algorithm has a meaningful small test.

## Documentation and completion

Update research.md for sources/decisions, mistakes.md for meaningful failures,
and sdk/docs/progress.md for checked milestones and external blockers. Targets
are not benchmark results. Never mark this goal complete until the specification's
definition of done is verified, including all trained models and real physical
end-to-end held-out evaluation. Request precise human experiments when needed;
continue independent implementation while waiting.

## Codex handoff and iteration

When the user transfers this goal to Codex, read `CODEX_HANDOFF.md` after this
file. It is a status snapshot, not a completion claim; continue until the V6
definition of done above is verified.

Use licensed pretrained weights wherever they fit the measured sensor modality
and product constraints. Pin source revisions and hashes, compare against held-out
data, and reject models that do not transfer or fit offline CPU deployment.
After every repository change, run the relevant checks, commit it, and push it
before making another change. If verification or push fails, record the blocker
and do not start another repository change until the current change is pushed.
Keep captures, downloaded data, model artifacts and credentials out of Git.

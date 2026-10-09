# SDK

Read root AGENTS.md. All acquisition, DSP, reliability, model inference, decisions
and secure reference storage belong here. Expose a typed Kotlin API; the app must
not duplicate sensing logic. Preserve original measurements, timestamps, actual
excitation configuration, routing and quality metadata for reproducibility.

Use real microphone/IMU data, supported vibration APIs and modest acoustic levels.
Do not infer contact quality or confidence without calibration. Android CPU-only
models require real weights, source/license, dataset manifest, version, parity
tests and held-out metrics. Laya uses actual upstream implementation and weights.
Raw captures and downloaded checkpoints must be ignored by Git. Training inputs
remain private; never upload without the user's authorized training scope.

Use pure JVM tests for deterministic DSP and guarded decision rules; device tests
for hardware and Keystore. Record experimental limitations and model availability
explicitly. Advance one integrated slice at a time and maintain docs/progress.md.

`rigid_case` A–D is the controlled research profile. `showcase_object` SIDE_1/
SIDE_2 is an honest generic-object acquisition profile only; it must not expose
MATCH/CHANGED or calibrated confidence without real specialist evidence.

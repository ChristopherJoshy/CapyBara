# Engineering lessons

## 2026-10-09: host executables and restricted process startup

The PATH Python entry is a Windows Store alias that cannot run in this environment.
The bundled Python 3.12.14 executable works. Use the verified executable rather
than repeatedly invoking the alias. Initial sandboxed ADB startup failed; an
authorized host ADB command found the device. No application data was changed.
Recheck tools with explicit executable paths and `adb devices -l` when resuming.

## 2026-10-09: do not infer a codebase from its remote URL

The requested remote successfully cloned but has no commits or existing code.
Build and documentation must be created here; there is no upstream product to run.
## 2026-10-09: device UI test ran with no visible Compose hierarchy

The first `:apk:connectedDebugAndroidTest` run reported one passing hardware test and one failing Compose showcase test because the device activity was not visible to the test rule; the report explicitly says no Compose hierarchies were found. The device package was subsequently cleaned up by the test runner. Cause was test/device visibility state, not a source assertion failure. The APK was reinstalled and its activity started explicitly; a rendered screenshot still needs a visible/unlocked display before claiming UI inspection. Future runs must wake/unlock the authorized device first and verify the activity is visible, without touching unrelated packages or clearing global logs.
## 2026-10-09: targeted rerun hit transient ADB property timeout

After the first device suite had already completed the real hardware capture test, a targeted rerun failed before executing it because Gradle/ddmlib timed out reading device properties and classified the same connected iQOO as an unknown API level. A direct `adb devices -l` immediately afterward still reported serial `10BFAT1UT2000XP` as connected. No application data or device settings were changed. Treat the earlier hardware-test pass as the current device evidence and rerun only after the ADB transport is stable.
## 2026-10-09: scrollable Compose tests must bring controls into view

After waking the authorized device, the scan-without-name assertions passed, but the existing capture-navigation test could not click `Empty case`. A semantics-tree dump showed the test clicked the home capture action below the vertical viewport; it never entered that page. `performScrollTo()` on the offscreen navigation and filter controls fixed the test. Activity recreation also exposed duplicate object-ID semantics from the visible capture page and the offscreen drawer; the test now targets the visible node. Both `ShowcaseTest` cases pass on-device. Do not interpret an offscreen semantics node as an actionable UI control.

## 2026-10-09: API-31 haptic property needed an explicit SDK guard

Android lint rejected `VibratorManager.vibratorIds` at minSdk 29 despite the manager itself being conditionally acquired. The property read now sits inside its own `Build.VERSION.SDK_INT >= 31` branch; `:sdk:lintDebug` and `:apk:lintDebug` pass. Keep Android version guards around individual newer API calls.
## 2026-10-09: connected hardware tests need their own permission grant

The full instrumented suite initially failed before capture because `HardwareCaptureTest` assumed `RECORD_AUDIO` had been granted externally; the test process saw `PERMISSION_DENIED (-1)`. Added `GrantPermissionRule` and the AndroidX test-rules dependency so the test is self-contained. After explicitly waking/dismissing the device keyguard, the final complete run passed all APK and SDK device tests. Keep runtime permissions inside test setup rather than relying on a prior manual grant.
Keep new claims bounded to actual tested implementation.

## 2026-10-09: connected instrumentation cleanup removed the demo package

After `:apk:connectedDebugAndroidTest` passed 3/3 on verified serial
`10BFAT1UT2000XP`, `adb shell pm path dev.capybara.app` returned no installed
APK. The connected-test task appears to have uninstalled the app, which may also
have deleted its private captures and encrypted baselines. The built APK was
reinstalled and launched; no unrelated package was touched. Do not rerun the
connected task until its cleanup behavior can be avoided or a verified backup
exists. Host compile, lint and unit checks remain safe.

## 2026-10-09: isolated ML runtime and Windows ONNX output

The original ignored venv pointed to a Microsoft Store Python 3.13 base that
could not start. Recreating the isolated environment with the bundled Python
3.12.14 restored CPU ML execution; the old environment is preserved separately
as `venv-python313-unlaunchable`. Do not repair this by mixing compiled packages
with a different global interpreter.

The pinned Laya exporter initially failed only when its Windows cp1252 console
could not encode a Unicode checkmark. Setting `PYTHONUTF8=1` and
`PYTHONIOENCODING=utf-8` let the unchanged upstream export finish. ONNX Runtime
then loaded the graph and its 1.69 GB external weight file on CPU and matched the
eager result on one generic example. ONNX's Python checker still fails to
canonicalize that external-data path on this host (`Access is denied`) even
though Python can stat/read it and ONNX Runtime can execute it; keep the checker
limitation explicit instead of treating its error as a successful validation.
The base checkpoint's clamped temperature also means its confidence is not
calibrated.

## 2026-10-09: sandbox denied the Gradle wrapper cache lock

The first host wrapper attempt stopped before Gradle started because the
restricted process could not open the standard distribution lock under the
user's Gradle cache. The same checked-in wrapper and task set succeeded once
the requested cache access was available. This was a host sandbox boundary, not
a Gradle or source failure. Keep using the checked-in wrapper and record the
actual task result; do not modify wrapper files to work around the cache.

## 2026-10-09: verify the app's actual private capture root

The first read-only device lookup checked `files/capybara/captures` and found no
directory. `CaptureStore` uses `Context.noBackupFilesDir`, so the correct path is
`no_backup/capybara/captures`; that path also does not exist on the installed
Capybara package. No files were changed. Verify the storage API before treating
an absent path as evidence that no captures exist.

## 2026-10-09: a configured GitHub account is not proof of push access

The remote is `ChristopherJoshy/CapyBara`, but local `gh auth status` reports the
configured token invalid, and `gh api user` could not reach GitHub. Do not infer
authenticated repository ownership from a cached account name or retry a push
without valid identity verification.

## 2026-10-10: dataset rights do not inherit from code repositories

Earlier research incorrectly described ObjectFolder-Real as licensed based on
the separate ObjectFolder 2.0 CC BY 4.0 statement. The Real download page does
not state data terms. RealImpact's MIT and SonicSense's Apache-2.0 licenses are
for their code repositories; neither establishes rights for separately hosted
recordings. The YCB-impact OSF record reports no node license. Check the actual
data artifact's license before download, training, or redistribution.

## 2026-10-10: do not substitute an emulator for the missing physical device

Read-only inspection on authorized serial `10BFAT1UT2000XP` returned “device not
found”; `adb devices -l` showed only `emulator-5554`. No emulator command was
run. Reconnect the exact authorized phone before inspecting or capturing data.

## 2026-10-10: verify dataset records behind search results

A search result suggested a TU Wien composite tap-test dataset, but the official
record is MSDv1 manufacturing work-step audio, not defect data. It was rejected
before download. Open the primary data record and check its labels before treating
a search snippet as a candidate.

## 2026-10-10: scan labels must match the selected research reference

Tracing `MainActivity` into `Capybara.evaluate` found that Scan generated a
random `candidate-*` object label even after the user selected a named reference.
The SDK now rejects a current capture whose object label differs from that
reference, so every scan would have failed evaluation before returning even its
research-only `INCONCLUSIVE` assessment. The selection handler now copies the
chosen reference's object label into the capture state; the scan still uses a
fresh session. Added SDK tests for the label guard and an app test for the
reference-selection gate. Host unit/build/lint checks passed; instrumentation was
compiled but not run, and no iQOO test was performed. Treat the label as
annotation consistency, never physical identity evidence.

## 2026-10-10: do not enroll incomplete audio captures

Code-path review found that `CaptureEngine` can return a partial PCM buffer at
its deadline and the app permits user review. `SignatureVault` checked profile
positions but did not compare stored PCM length with requested/reported sample
counts, so approved partial audio could be included in an encrypted baseline.
The baseline validator now fails closed on any missing or mismatched count.
The host count-boundary test passes and an instrumentation regression checks
that the vault rejects such a capture; the instrumentation test was compiled,
not run, because iQOO testing is paused. Keep partial measurements as raw
research data only; never let user approval bypass sample completeness.

## 2026-10-10: annotation leakage checks must inspect keys and value types

`prepare.py` originally compared evidence keys to exact normalized names, so
nested `expectedLabel`, `conditionName`, and `objectIDHash` bypassed the check.
Strings and booleans under innocent feature names could also carry labels into
Laya inputs. The validator now rejects common annotation/identity key variants
and restricts evidence values to finite numbers or `null`. Training tests cover
obfuscated keys, text, booleans, null, and valid numeric features. This cannot
prove that a number is semantically independent of an annotation; review every
feature producer and never treat schema validation as proof of independence.

## 2026-10-10: training import must enforce capture format and embedded digests

`prepare.capture` hashed raw audio and IMU into the dataset manifest but did not
compare them with `audioSha256` and `imuSha256` in source metadata. It also ignored
`pcmEncoding`, so same-sized bytes could be accepted under an unsupported format.
The preparer now checks both embedded digests and requires
`signed_pcm16_little_endian_mono`. Regressions flip one byte in each artifact and
change the encoding label; all are rejected. These unkeyed hashes detect file
inconsistency, not an attacker rewriting both artifacts and metadata.

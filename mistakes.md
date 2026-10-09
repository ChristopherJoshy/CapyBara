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

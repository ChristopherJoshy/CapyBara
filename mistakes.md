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

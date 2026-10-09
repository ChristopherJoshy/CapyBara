package dev.capybara.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.capybara.sdk.*
import kotlinx.coroutines.*
import org.json.JSONObject
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real hardware diagnostics; these recordings are not labeled object training data. */
@RunWith(AndroidJUnit4::class)
class HardwareCaptureTest {
    @get:Rule
    val microphonePermission = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    @Test fun realCaptureAndCancellationReleaseHardware() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.RECORD_AUDIO))
        val sdk = Capybara(context)
        val label = CaptureLabel("hardware-diagnostic", "diagnostic-${System.currentTimeMillis()}", "hardware_diagnostic", "A")
        val recording = CompletableDeferred<Unit>()
        val cancelled = launch { sdk.capture(label) { if (it == "Recording ambient baseline") recording.complete(Unit) } }
        withTimeout(10_000) { recording.await() }
        delay(100)
        cancelled.cancelAndJoin()
        val start = System.nanoTime()
        val scan = withTimeout(15_000) { sdk.capture(label) }
        val captureMillis = (System.nanoTime() - start) / 1e6
        assertTrue("Real microphone frames missing", scan.pcm.size >= scan.sampleRate * 5)
        assertTrue("Accelerometer events missing", scan.imu.count { it.type == 1 } > 100)
        assertTrue("Gyroscope events missing", scan.imu.count { it.type == 4 } > 100)
        assertTrue("Actual PCM is entirely silent", scan.pcm.any { it.toInt() != 0 })
        assertTrue(scan.excitations.any { it.kind == "acoustic_chirp" })
        assertTrue(scan.excitations.any { it.kind == "haptic_finite_waveform" })
        assertEquals("false", scan.metadata["audioSilencedEver"])
        val dspStarted = System.nanoTime()
        val evidence = PhysicsEngine.analyze(scan)
        val dspMillis = (System.nanoTime() - dspStarted) / 1e6
        assertTrue(evidence.audioRms.isFinite())
        assertTrue(evidence.accelerometerHz > 0 && evidence.gyroscopeHz > 0)
        val capturePath = sdk.saveCapture(scan)
        val report = JSONObject().put("purpose", "hardware_diagnostic_not_training_or_accuracy_evidence")
            .put("capabilities", sdk.getCapabilities().toString()).put("capturePath", capturePath)
            .put("captureMillis", captureMillis).put("dspMillis", dspMillis)
            .put("sampleCount", scan.pcm.size).put("sampleRate", scan.sampleRate)
            .put("audioRms", evidence.audioRms).put("noiseRms", evidence.noiseRms)
            .put("peak", evidence.peak).put("clippedSamples", evidence.clippedSamples)
            .put("accelerometerHz", evidence.accelerometerHz).put("gyroscopeHz", evidence.gyroscopeHz)
            .put("accelerationRms", evidence.accelerationRms).put("gyroRms", evidence.gyroRms)
            .put("metadata", JSONObject(scan.metadata))
        File(context.filesDir, "hardware-report.json").writeText(report.toString(2))
    }
}

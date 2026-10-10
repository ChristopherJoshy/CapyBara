package dev.capybara.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntegrityEvaluationTest {
    @Test fun comparesStoredFeaturesButTrustGateRemainsInconclusive() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sdk = Capybara(context)
        val positions = ProfileEngine.get(ProfileEngine.SHOWCASE_OBJECT_ID).positions
        val capturePaths = mutableListOf<String>()
        var referenceId: String? = null
        try {
            val referencePaths = positions.map { position ->
                sdk.saveCapture(testScan("fixture-reference", "enrollment-session", "showcase_enrollment", position, 4096))
                    .also { capturePaths += it }
            }
            val reference = sdk.saveResearchBaseline(referencePaths)
            referenceId = reference.id
            val currentPaths = positions.map { position ->
                sdk.saveCapture(testScan("fixture-reference", "scan-session", "showcase_scan", position, 8192))
                    .also { capturePaths += it }
            }
            val wrongObjectPaths = positions.map { position ->
                sdk.saveCapture(testScan("other-object", "other-scan-session", "showcase_scan", position, 8192))
                    .also { capturePaths += it }
            }

            val result = sdk.evaluate(reference.id, currentPaths)

            assertEquals(IntegrityDecision.INCONCLUSIVE, result.decision)
            assertEquals(positions, result.positions.map { it.position })
            assertTrue(result.positions.all { it.current.audioRms > it.reference.audioRms })
            assertEquals("physics-v1", result.pipelineVersion)
            assertTrue(result.blockingReasons.contains("CAPYBARA_LAYA_NOT_TRAINED_OR_INSTALLED"))
            assertTrue(result.blockingReasons.contains("REFERENCE_BASELINE_UNCALIBRATED"))
            assertTrue(result.layaEvidenceStateJson.contains("\"delta\""))
            assertTrue(result.layaEvidenceStateJson.contains("audioRms"))
            assertFalse(result.layaEvidenceStateJson.contains("fixture-reference"))
            var rejectedWrongObject = false
            try {
                sdk.evaluate(reference.id, wrongObjectPaths)
            } catch (error: IllegalArgumentException) {
                rejectedWrongObject = error.message?.contains("object label differs") == true
            }
            assertTrue(rejectedWrongObject)
            val metadataFile = File(referencePaths.first(), "metadata.json")
            val changedMetadata = JSONObject(metadataFile.readText())
            changedMetadata.getJSONObject("label").put("objectId", "tampered-reference")
            metadataFile.writeText(changedMetadata.toString())
            var rejectedTampering = false
            try {
                sdk.evaluate(reference.id, currentPaths)
            } catch (error: IllegalArgumentException) {
                rejectedTampering = error.message?.contains("label changed") == true
            }
            assertTrue(rejectedTampering)
        } finally {
            referenceId?.let { assertTrue(File(context.noBackupFilesDir, "capybara/references/$it.ref").delete()) }
            capturePaths.forEach { assertTrue(File(it).deleteRecursively()) }
        }
    }


    private fun testScan(objectId: String, sessionId: String, condition: String, position: String, amplitude: Int) = Scan(
        label = CaptureLabel(objectId, sessionId, condition, position, ProfileEngine.SHOWCASE_OBJECT_ID),
        sampleRate = 8192,
        pcm = ShortArray(1024) { (amplitude * sin(2 * PI * 1152 * it / 8192)).toInt().toShort() },
        imu = emptyList(),
        excitations = emptyList(),
        startedBootNanos = 1_000_000_000,
        endedBootNanos = 2_000_000_000,

        metadata = mapOf(
            "baselineSampleCount" to "64",
            "audioExpectedSamples" to "1024",
            "audioSampleCount" to "1024",
            "audioTimestampCount" to "1",
            "audioSilencedEver" to "false",
            "audioRouteChanged" to "false",
            "audioConfigurationChanged" to "false",
            "clockMappingOffsetChangeNanos" to "0",
            "clockMappingBracketNanos" to "1",
            "clockMappingEndBracketNanos" to "1",
        ),
    )
}

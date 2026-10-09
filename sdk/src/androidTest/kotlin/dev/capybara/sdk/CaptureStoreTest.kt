package dev.capybara.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CaptureStoreTest {
    @Test fun commitsRawMeasurementsAndHashesInPrivateStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = CaptureStore(context)
        val scan = Scan(CaptureLabel("unit-test-only", "test-session", "format-test", "A"), 48_000,
            shortArrayOf(0x1234, -1, Short.MIN_VALUE), listOf(ImuSample(1, 2, 1f, 2f, 3f)),
            listOf(ExcitationEvent("test-only", 1)), 1, 3, mapOf("evidencePurpose" to "storage_format_test"))
        val folder = File(store.save(scan))
        try {
            assertArrayEquals(byteArrayOf(0x34, 0x12, -1, -1, 0, -128), File(folder, "audio.pcm").readBytes())
            assertTrue(File(folder, "imu.csv").readText().contains("1,2,1.0,2.0,3.0"))
            val metadata = JSONObject(File(folder, "metadata.json").readText())
            assertEquals(3, metadata.getInt("sampleCount"))
            assertEquals("unit-test-only", metadata.getJSONObject("label").getString("objectId"))
            assertEquals("0a2ee5da4682b74eef3ab6963ed6c5fce0032e8e86a2933d4b461bb193b9216e", metadata.getString("audioSha256"))
            assertTrue(folder.absolutePath in store.list())
            assertEquals(scan.label, store.label(folder.absolutePath))
            try { store.label(context.filesDir.absolutePath); throw AssertionError("Capture path escaped its store") }
            catch (_: IllegalArgumentException) { }
        } finally {
            assertTrue(folder.deleteRecursively())
        }
    }
}

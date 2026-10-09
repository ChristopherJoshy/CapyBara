package dev.capybara.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.crypto.AEADBadTagException

@RunWith(AndroidJUnit4::class)
class SignatureVaultTest {
    @Test fun measuredBaselineIsImmutableAuthenticatedAndPrivate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sdk = Capybara(context)
        val vault = SignatureVault(context)
        val paths = listOf("A", "B", "C", "D").map { p ->
            sdk.saveCapture(Scan(CaptureLabel("storage-test-only", "test-session", "format_test", p), 48_000,
                shortArrayOf(1), emptyList(), emptyList(), 1, 2, mapOf("purpose" to "test_fixture_not_physical_data")))
        }
        val reference = vault.saveResearchBaseline(paths)
        val another = vault.saveResearchBaseline(paths)
        val folder = File(context.noBackupFilesDir, "capybara/references")
        val file = File(folder, "${reference.id}.ref")
        try {
            assertNotEquals(reference.id, another.id)
            assertEquals("storage-test-only", vault.readResearchBaseline(reference.id).getString("objectId"))
            assertTrue(vault.readResearchBaseline(reference.id).isNull("modelVersion"))
            assertTrue(vault.listResearchBaselines().any { it.id == reference.id })
            val bytes = file.readBytes()
            assertFalse(String(bytes, Charsets.UTF_8).contains("storage-test-only"))
            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
            file.writeBytes(bytes)
            try { vault.readResearchBaseline(reference.id); fail("Tampered reference accepted") }
            catch (_: AEADBadTagException) { }
            try { vault.saveResearchBaseline(paths.take(3)); fail("Incomplete baseline accepted") }
            catch (_: IllegalArgumentException) { }
        } finally {
            assertTrue(file.delete())
            assertTrue(File(folder, "${another.id}.ref").delete())
            paths.forEach { assertTrue(File(it).deleteRecursively()) }
        }
    }
}

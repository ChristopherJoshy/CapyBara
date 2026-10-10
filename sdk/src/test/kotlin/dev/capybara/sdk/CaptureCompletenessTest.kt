package dev.capybara.sdk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureCompletenessTest {
    @Test fun baselineAudioMustMatchStoredExpectedAndReportedCounts() {
        assertTrue(hasCompleteAudioCapture(2, "2", "2"))
        assertFalse(hasCompleteAudioCapture(1, "2", "1"))
        assertFalse(hasCompleteAudioCapture(2, "2", "1"))
        assertFalse(hasCompleteAudioCapture(2, null, "2"))
        assertFalse(hasCompleteAudioCapture(0, "0", "0"))
        assertFalse(hasCompleteAudioCapture(2, "not-a-count", "2"))
    }
}

package dev.capybara.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class ProbeBrainTest {
    @Test fun rigidCaseAlwaysUsesAcousticProbeAndRequestsHapticsOnlyWhenAvailable() {
        val withHaptics = ProbeBrain.select(ProfileEngine.RIGID_CASE_ID, hapticsAvailable = true)
        val withoutHaptics = ProbeBrain.select(ProfileEngine.RIGID_CASE_ID, hapticsAvailable = false)

        assertTrue(withHaptics.requestHaptics)
        assertFalse(withoutHaptics.requestHaptics)
        assertEquals("rigid_case:acoustic_chirp;haptics_unavailable", withoutHaptics.description)
    }

    @Test fun unsupportedProfilesNeverReceiveAnImplicitProbePlan() {
        assertThrows(IllegalArgumentException::class.java) {
            ProbeBrain.select("unverified_profile", hapticsAvailable = true)
        }
    }
}

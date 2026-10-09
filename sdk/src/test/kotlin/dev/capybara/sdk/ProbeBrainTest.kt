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
        assertEquals("rigid_case:stereo_acoustic_chirp;haptics_unavailable", withoutHaptics.description)
    }

    @Test fun bothPhysicalProbeRequestsUseStereoAudioAndAvailableHaptics() {
        val withHaptics = ProbeBrain.select(ProfileEngine.SHOWCASE_OBJECT_ID, hapticsAvailable = true)
        assertTrue(withHaptics.requestHaptics)
        assertEquals("showcase_object:stereo_acoustic_chirp+all_exposed_haptic_waveforms", withHaptics.description)
    }

    @Test fun unsupportedProfilesNeverReceiveAnImplicitProbePlan() {
        assertThrows(IllegalArgumentException::class.java) {
            ProbeBrain.select("unverified_profile", hapticsAvailable = true)
        }
    }
}

package dev.capybara.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfileEngineTest {
    @Test fun exposesOnlyTheSpecifiedResearchProfileAndPositions() {
        val profile = ProfileEngine.get(ProfileEngine.RIGID_CASE_ID)
        assertEquals(listOf(ProfileEngine.RIGID_CASE_ID), ProfileEngine.supportedProfiles().map { it.id })
        assertEquals(listOf("A", "B", "C", "D"), profile.positions)
    }

    @Test fun rejectsUnknownProfilesAndUnregisteredPositions() {
        assertThrows(IllegalArgumentException::class.java) { ProfileEngine.get("unverified_profile") }
        assertThrows(IllegalArgumentException::class.java) {
            ProfileEngine.validateCapture(CaptureLabel("case", "session", "baseline", "E"))
        }
    }

    @Test fun callersCannotMutateTheSupportedProfileCatalog() {
        assertThrows(UnsupportedOperationException::class.java) {
            (ProfileEngine.get(ProfileEngine.RIGID_CASE_ID).positions as MutableList<String>).add("E")
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (ProfileEngine.supportedProfiles() as MutableList<IntegrityProfile>).clear()
        }
    }
}

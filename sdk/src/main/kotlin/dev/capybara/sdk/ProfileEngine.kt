package dev.capybara.sdk

import java.util.Collections

/** A measurement profile defines its registered positions, not a validated decision model. */
class IntegrityProfile internal constructor(
    val id: String,
    val positions: List<String>,
)

/** Catalog of physically specified measurement protocols currently supported by this SDK. */
object ProfileEngine {
    const val RIGID_CASE_ID = "rigid_case"
    const val SHOWCASE_OBJECT_ID = "showcase_object"

    private val profiles = Collections.unmodifiableList(
        listOf(
            IntegrityProfile(RIGID_CASE_ID, Collections.unmodifiableList(listOf("A", "B", "C", "D"))),
            IntegrityProfile(SHOWCASE_OBJECT_ID, Collections.unmodifiableList(listOf("SIDE_1", "SIDE_2"))),
        )
    )

    fun supportedProfiles(): List<IntegrityProfile> = profiles

    fun get(profileId: String): IntegrityProfile =
        profiles.firstOrNull { it.id == profileId }
            ?: throw IllegalArgumentException("Unsupported Capybara profile: $profileId")

    internal fun validateCapture(label: CaptureLabel) {
        val profile = get(label.profile)
        require(label.position in profile.positions) {
            "Position ${label.position} is not registered for profile ${profile.id}."
        }
    }
}

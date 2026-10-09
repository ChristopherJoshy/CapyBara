package dev.capybara.sdk

internal data class ProbePlan(
    val requestHaptics: Boolean,
    val description: String,
)

/** Fixed capability-aware planner; it does not adapt from signal quality or prior measurements. */
internal object ProbeBrain {
    fun select(profileId: String, hapticsAvailable: Boolean): ProbePlan =
        when (ProfileEngine.get(profileId).id) {
            ProfileEngine.RIGID_CASE_ID -> ProbePlan(
                requestHaptics = hapticsAvailable,
                description = if (hapticsAvailable) "rigid_case:acoustic_chirp+haptic_waveform"
                    else "rigid_case:acoustic_chirp;haptics_unavailable",
            )
            else -> error("No probe plan is defined for profile $profileId.")
        }
}

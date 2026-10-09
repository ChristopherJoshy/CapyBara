package dev.capybara.sdk

data class CaptureLabel(
    val objectId: String,
    val sessionId: String,
    val condition: String,
    val position: String,
    val profile: String = ProfileEngine.RIGID_CASE_ID,
) {
    init {
        require(listOf(objectId, sessionId, condition, position, profile).all {
            it.isNotBlank() && it.length <= 256 && it.none(Char::isISOControl)
        }) { "Capture labels must contain 1–256 printable characters." }
    }
}

data class ImuSample(val type: Int, val timestampNanos: Long, val x: Float, val y: Float, val z: Float)

/** A request timestamp is not a measured physical onset. */
data class ExcitationEvent(val kind: String, val requestedBootNanos: Long)

data class Scan(
    val label: CaptureLabel,
    val sampleRate: Int,
    val pcm: ShortArray,
    val imu: List<ImuSample>,
    val excitations: List<ExcitationEvent>,
    val startedBootNanos: Long,
    val endedBootNanos: Long,
    val metadata: Map<String, String>,
)

data class DeviceCapabilities(
    val model: String,
    val apiLevel: Int,
    val abi: String,
    val sensors: List<String>,
    val audio: String,
    val haptics: String,
)

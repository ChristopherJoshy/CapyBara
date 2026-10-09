package dev.capybara.sdk

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager

internal fun vibrator(context: Context): Vibrator = if (Build.VERSION.SDK_INT >= 31) {
    context.getSystemService(VibratorManager::class.java).defaultVibrator
} else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
}

internal fun audioDevice(device: AudioDeviceInfo?): String = device?.let {
    "id=${it.id};type=${it.type};name=${it.productName};rates=${it.sampleRates.joinToString().ifEmpty { "dynamic/unspecified" }};channels=${it.channelCounts.joinToString().ifEmpty { "dynamic/unspecified" }}"
} ?: "unavailable"

internal fun capabilities(context: Context): DeviceCapabilities {
    val sensors = context.getSystemService(SensorManager::class.java)
    val audio = context.getSystemService(AudioManager::class.java)
    val haptic = vibrator(context)
    // Report each actuator: the default vibrator may aggregate unlike hardware capabilities.
    fun describeVibrator(value: Vibrator): String = buildString {
        append("present=${value.hasVibrator()};amplitudeControl=${value.hasAmplitudeControl()}")
        if (Build.VERSION.SDK_INT >= 34) {
            append(";resonantHz=${value.resonantFrequency};q=${value.qFactor}")
        } else append(";resonanceApi=unavailable")
        if (Build.VERSION.SDK_INT >= 36) {
            append(";envelopeSupported=${value.areEnvelopeEffectsSupported()}")
            val profile = value.frequencyProfile
            append("\nfrequencyProfile=")
            if (profile == null) append("unavailable") else {
                append("minHz=${profile.minFrequencyHz};maxHz=${profile.maxFrequencyHz};maxAccelerationGs=${profile.maxOutputAccelerationGs}")
                append(";frequencyAccelerationMap=${profile.frequenciesOutputAcceleration}")
            }
            val info = value.envelopeEffectInfo
            append("\nenvelopeLimits=maxPoints=${info.maxSize};minSegmentMs=${info.minControlPointDurationMillis};maxSegmentMs=${info.maxControlPointDurationMillis};maxDurationMs=${info.maxDurationMillis}")
        } else append(";envelopeApi=unavailable")
    }
    val hapticDetails = buildString {
        append("default:${describeVibrator(haptic)};independentAxisControl=not_exposed")
        if (Build.VERSION.SDK_INT >= 31) {
            val manager = context.getSystemService(VibratorManager::class.java)
            append("\nvibratorIds=${manager.vibratorIds.joinToString()}")
            manager.vibratorIds.forEach { id -> append("\nvibratorId=$id;${describeVibrator(manager.getVibrator(id))}") }
        }
        append("\nphysicalSpeakerHapticOnset=uncalibrated")
    }
    return DeviceCapabilities(
        model = "${Build.MANUFACTURER} ${Build.MODEL}",
        apiLevel = Build.VERSION.SDK_INT,
        abi = Build.SUPPORTED_ABIS.joinToString(),
        sensors = sensors.getSensorList(Sensor.TYPE_ALL).map {
            "type=${it.type};${it.name};vendor=${it.vendor};minDelayUs=${it.minDelay};maxDelayUs=${it.maxDelay};range=${it.maximumRange};resolution=${it.resolution};fifoMax=${it.fifoMaxEventCount}"
        },
        audio = "unprocessedReported=${audio.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) ?: "unavailable"};" +
            "outputRate=${audio.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) ?: "unavailable"};" +
            "outputFrames=${audio.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: "unavailable"}\n" +
            (audio.getDevices(AudioManager.GET_DEVICES_INPUTS).toList() + audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList())
                .distinctBy { it.id }.joinToString("\n") { audioDevice(it) },
        haptics = hapticDetails,
    )
}

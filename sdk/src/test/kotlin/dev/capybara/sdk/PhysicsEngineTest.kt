package dev.capybara.sdk

import android.media.AudioDeviceInfo

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.*
import org.junit.Test

// Synthetic signals verify DSP arithmetic only; they are not physical sensing evidence.
class PhysicsEngineTest {
    private fun scan(pcm: ShortArray, imu: List<ImuSample> = emptyList(),
                     extra: Map<String, String> = emptyMap()) = Scan(
        label = CaptureLabel("synthetic", "unit-test", "arithmetic", "A"),
        sampleRate = 8192, pcm = pcm, imu = imu, excitations = emptyList(),
        startedBootNanos = 1_000_000_000L, endedBootNanos = 2_000_000_000L,
        metadata = mapOf("baselineSampleCount" to pcm.size.toString(), "audioTimestampCount" to "1",
            "audioExpectedSamples" to pcm.size.toString(), "audioSampleCount" to pcm.size.toString(),
            "audioSilencedEver" to "false", "audioRouteChanged" to "false",
            "audioConfigurationChanged" to "false") + extra
    )

    @Test fun tonePowerAndBandAreCorrect() {
        val pcm = ShortArray(4096) { (16384 * sin(2 * PI * 1152 * it / 8192)).toInt().toShort() }
        val evidence = PhysicsEngine.analyze(scan(pcm))
        assertEquals(0.5 / kotlin.math.sqrt(2.0), evidence.audioRms, 0.0001)
        assertEquals(0.125, evidence.acousticBandEnergy.sum(), 0.0001)
        assertTrue(evidence.acousticBandEnergy[2] > evidence.acousticBandEnergy.sum() * 0.99)
        assertEquals(0, evidence.clippedSamples)
    }

    @Test fun dcIsRemovedFromSpectrumAndNyquistIsNotDoubled() {
        val constant = PhysicsEngine.analyze(scan(ShortArray(1024) { 8192 }))
        assertEquals(0.25, constant.audioRms, 0.0)
        assertEquals(0.0, constant.acousticBandEnergy.sum(), 1e-15)
        val nyquist = PhysicsEngine.analyze(scan(ShortArray(1024) { if (it % 2 == 0) 8192 else -8192 }))
        assertEquals(0.0625, nyquist.acousticBandEnergy.sum(), 1e-12)
        assertEquals(0.0625, nyquist.acousticBandEnergy[7], 1e-12)
    }

    @Test fun baselineUsesOnlyTheDeclaredPrefixAndRailsAreCounted() {
        val pcm = shortArrayOf(8192, 8192, Short.MIN_VALUE, Short.MAX_VALUE)
        val evidence = PhysicsEngine.analyze(scan(pcm, extra = mapOf("baselineSampleCount" to "2")))
        assertEquals(0.25, evidence.noiseRms, 0.0)
        assertEquals(1.0, evidence.peak, 0.0)
        assertEquals(2, evidence.clippedSamples)
    }

    @Test fun unsupportedRoutesAndUnverifiedHapticsRemainLimitations() {
        val acousticOnly = scan(ShortArray(1024)).copy(
            excitations = listOf(ExcitationEvent("acoustic_chirp", 1L))
        )
        val missing = PhysicsEngine.analyze(acousticOnly)
        assertTrue(missing.limitations.any { "Built-in microphone route" in it })
        assertTrue(missing.limitations.any { "Built-in speaker route" in it })
        assertTrue(missing.limitations.any { "Haptic waveform request" in it })

        val routed = acousticOnly.copy(
            excitations = acousticOnly.excitations + ExcitationEvent("haptic_finite_waveform", 2L),
            metadata = acousticOnly.metadata + mapOf(
                "audioInputRoutedDeviceTypeObserved" to AudioDeviceInfo.TYPE_BUILTIN_MIC.toString(),
                "audioOutputRoutedDeviceTypeObserved" to AudioDeviceInfo.TYPE_BUILTIN_SPEAKER.toString(),
            ),
        )
        val evidence = PhysicsEngine.analyze(routed)
        assertFalse(evidence.limitations.any { "Built-in microphone route" in it })
        assertFalse(evidence.limitations.any { "Built-in speaker route" in it })
        assertFalse(evidence.limitations.any { "Haptic waveform request" in it })
        assertTrue(evidence.limitations.any { "Haptic physical onset" in it })

        val safeSpeaker = routed.copy(metadata = routed.metadata +
            ("audioOutputRoutedDeviceTypeObserved" to AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE.toString()))
        assertFalse(PhysicsEngine.analyze(safeSpeaker).limitations.any { "Built-in speaker route" in it })

        val externalOutput = routed.copy(metadata = routed.metadata +
            ("audioOutputRoutedDeviceTypeObserved" to AudioDeviceInfo.TYPE_WIRED_HEADSET.toString()))
        assertTrue(PhysicsEngine.analyze(externalOutput).limitations.any { "Built-in speaker route" in it })
    }

    @Test fun stationaryGravityIsCenteredAndRatesComeFromTimestamps() {
        val imu = (0..4).flatMap {
            val time = 1_000_000_000L + it * 2_500_000L
            listOf(ImuSample(1, time, if (it % 2 == 0) 1f else -1f, 0f, 9.81f),
                ImuSample(4, time, 0f, 0f, 2f))
        }
        val evidence = PhysicsEngine.analyze(scan(ShortArray(1024), imu))
        assertEquals(kotlin.math.sqrt(0.96), evidence.accelerationRms, 1e-9)
        assertEquals(2.0, evidence.gyroRms, 0.0)
        assertEquals(400.0, evidence.accelerometerHz, 0.0)
        assertEquals(400.0, evidence.gyroscopeHz, 0.0)
        assertEquals(1.0, evidence.durationSeconds, 0.0)
    }

    @Test fun missingDataAndInvalidProvenanceDoNotBecomeGoodMeasurements() {
        val evidence = PhysicsEngine.analyze(scan(ShortArray(0), extra = mapOf(
            "baselineSampleCount" to "999", "audioTimestampCount" to "0", "audioSilencedEver" to "true")))
        assertTrue(evidence.audioRms.isNaN())
        assertTrue(evidence.noiseRms.isNaN())
        assertTrue(evidence.accelerationRms.isNaN())
        assertTrue(evidence.acousticBandEnergy.isEmpty())
        assertTrue(evidence.limitations.any { "audioSilencedEver" in it })
        assertTrue(evidence.limitations.any { "timestamps unavailable" in it })
    }

    @Test fun duplicateTimesAndNonFiniteSensorsAreExplicit() {
        val imu = listOf(ImuSample(1, 1L, Float.NaN, 0f, 9.81f), ImuSample(1, 1L, 0f, 0f, 9.81f))
        val evidence = PhysicsEngine.analyze(scan(ShortArray(1024), imu))
        assertTrue(evidence.accelerationRms.isNaN())
        assertTrue(evidence.accelerometerHz.isNaN())
        assertTrue(evidence.limitations.any { "not strictly increasing" in it })
    }

    @Test fun gapsAndBadDurationAreReportedWithoutInventingLostSamples() {
        val imu = listOf(0L, 10L, 20L, 30L, 100L).map { ImuSample(1, it, 0f, 0f, 9.81f) }
        val evidence = PhysicsEngine.analyze(scan(ShortArray(1025), imu, mapOf(
            "audioExpectedSamples" to "2000", "clockMappingOffsetChangeNanos" to "100",
            "clockMappingBracketNanos" to "10", "clockMappingEndBracketNanos" to "10"
        )).copy(endedBootNanos = 0L))
        assertTrue(evidence.durationSeconds.isNaN())
        assertTrue(evidence.limitations.any { "sample loss is unverified" in it })
        assertTrue(evidence.limitations.any { "trailing spectral window" in it })
        assertTrue(evidence.limitations.any { "sample count mismatch" in it })
        assertTrue(evidence.limitations.any { "cross-clock timing unstable" in it })
    }

    @Test fun unrepresentedTrailingSampleContributesToSpectrum() {
        val pcm = ShortArray(1537)
        pcm[1536] = 8192
        val evidence = PhysicsEngine.analyze(scan(pcm))
        assertTrue(evidence.acousticBandEnergy.sum() > 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidAudioRateIsRejected() { PhysicsEngine.analyze(scan(ShortArray(8)).copy(sampleRate = 0)) }
}

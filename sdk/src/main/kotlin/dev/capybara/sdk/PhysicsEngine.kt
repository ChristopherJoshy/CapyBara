package dev.capybara.sdk

import android.media.AudioDeviceInfo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PhysicsEvidence(
    val audioRms: Double,
    val noiseRms: Double,
    val peak: Double,
    val clippedSamples: Int,
    val acousticBandEnergy: List<Double>,
    val accelerationRms: Double,
    val gyroRms: Double,
    val accelerometerHz: Double,
    val gyroscopeHz: Double,
    val durationSeconds: Double,
    val limitations: List<String>
)

object PhysicsEngine {
    fun analyze(scan: Scan): PhysicsEvidence {
        require(scan.sampleRate > 0) { "Audio sample rate must be positive" }
        val issues = mutableListOf<String>()
        val pcm = scan.pcm
        if (pcm.isEmpty()) issues += "No microphone samples"
        val expected = scan.metadata["audioExpectedSamples"]?.toIntOrNull()
        if (expected == null || expected < 1) issues += "Requested audio sample count unavailable"
        else if (pcm.size != expected) issues += "Audio sample count mismatch: ${pcm.size} of $expected requested"
        val reported = scan.metadata["audioSampleCount"]?.toIntOrNull()
        if (reported != null && reported != pcm.size) issues += "PCM length differs from reported sample count"
        val clockChange = scan.metadata["clockMappingOffsetChangeNanos"]?.toLongOrNull()
        val clockStart = scan.metadata["clockMappingBracketNanos"]?.toLongOrNull()
        val clockEnd = scan.metadata["clockMappingEndBracketNanos"]?.toLongOrNull()
        if (clockChange != null && clockStart != null && clockEnd != null && clockStart >= 0 && clockEnd >= 0) {
            if (abs(clockChange.toDouble()) > (clockStart.toDouble() + clockEnd) / 2)
                issues += "Clock offset changed beyond clock-read uncertainty; cross-clock timing unstable"
        } else issues += "Cross-clock drift audit unavailable"
        val baseline = scan.metadata["baselineSampleCount"]?.toIntOrNull()
        val noise = if (baseline != null && baseline in 1..pcm.size) rms(pcm, baseline) else {
            issues += "Pre-excitation noise prefix unavailable"
            Double.NaN
        }
        for (key in listOf("audioSilencedEver", "audioRouteChanged", "audioConfigurationChanged")) {
            when (scan.metadata[key]) {
                "true" -> issues += "Capture reported $key"
                "false" -> Unit
                else -> issues += "Capture status $key unavailable"
            }
        }
        if ((scan.metadata["audioTimestampCount"]?.toIntOrNull() ?: 0) == 0)
            issues += "Audio BOOTTIME frame timestamps unavailable"
        if (scan.metadata["audioInputRoutedDeviceTypeObserved"] != AudioDeviceInfo.TYPE_BUILTIN_MIC.toString())
            issues += "Built-in microphone route was not confirmed"
        if (scan.metadata["audioOutputRoutedDeviceTypeObserved"] != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER.toString())
            issues += "Built-in speaker route was not confirmed"
        if (scan.excitations.none { it.kind == "haptic_finite_waveform" })
            issues += "Haptic waveform request was not recorded"
        if (scan.metadata["hapticPhysicalOnset"] != "measured")
            issues += "Haptic physical onset is not measured"
        val acc = scan.imu.filter { it.type == 1 }
        val gyro = scan.imu.filter { it.type == 4 }
        val accRms = motionRms(acc, true, "Accelerometer", issues)
        val gyroRms = motionRms(gyro, false, "Gyroscope", issues)
        val accHz = rate(acc, "Accelerometer", issues)
        val gyroHz = rate(gyro, "Gyroscope", issues)
        val clipped = pcm.count { it == Short.MIN_VALUE || it == Short.MAX_VALUE }
        if (clipped > 0) issues += "PCM contains full-scale rail samples"
        val duration = if (scan.endedBootNanos > scan.startedBootNanos)
            (scan.endedBootNanos - scan.startedBootNanos) / 1e9 else {
                issues += "Invalid capture duration"
                Double.NaN
            }
        issues += "Synchronization, contact quality and object/self-response separation are not calibrated"
        return PhysicsEvidence(rms(pcm, pcm.size), noise,
            pcm.maxOfOrNull { abs(it.toInt()) / 32768.0 } ?: Double.NaN, clipped,
            bandEnergy(pcm, issues), accRms, gyroRms, accHz, gyroHz, duration, issues)
    }

    private fun rms(pcm: ShortArray, count: Int): Double = if (count == 0) Double.NaN else
        sqrt((0 until count).sumOf { val value = pcm[it] / 32768.0; value * value } / count)

    private fun motionRms(samples: List<ImuSample>, center: Boolean, name: String,
                          issues: MutableList<String>): Double {
        if (samples.size < 2 || samples.any { !it.x.isFinite() || !it.y.isFinite() || !it.z.isFinite() }) {
            issues += "$name samples missing or non-finite"
            return Double.NaN
        }
        val x = if (center) samples.sumOf { it.x.toDouble() } / samples.size else 0.0
        val y = if (center) samples.sumOf { it.y.toDouble() } / samples.size else 0.0
        val z = if (center) samples.sumOf { it.z.toDouble() } / samples.size else 0.0
        return sqrt(samples.sumOf {
            val dx = it.x - x; val dy = it.y - y; val dz = it.z - z
            dx * dx + dy * dy + dz * dz
        } / samples.size)
    }

    private fun rate(samples: List<ImuSample>, name: String, issues: MutableList<String>): Double {
        if (samples.size < 2) {
            issues += "$name rate needs at least two samples"
            return Double.NaN
        }
        val intervals = samples.zipWithNext { a, b -> b.timestampNanos - a.timestampNanos }
        if (samples.any { it.timestampNanos < 0 } || intervals.any { it <= 0 }) {
            issues += "$name timestamps are not strictly increasing"
            return Double.NaN
        }
        val median = intervals.sorted()[intervals.size / 2]
        if (intervals.max() / median.toDouble() > 3.0)
            issues += "$name timestamp gap exceeds three median intervals; sample loss is unverified"
        return (samples.size - 1) * 1e9 / intervals.sum().toDouble()
    }

    private fun bandEnergy(pcm: ShortArray, issues: MutableList<String>): List<Double> {
        val n = Integer.highestOneBit(pcm.size.coerceAtMost(1024))
        if (n < 8) { issues += "Too few audio samples for spectral evidence"; return emptyList() }
        val window = DoubleArray(n) { 0.5 - 0.5 * cos(2 * PI * it / n) }
        val bands = DoubleArray(8)
        val real = DoubleArray(n)
        val imag = DoubleArray(n)
        val twiddleReal = DoubleArray(n / 2) { cos(-2 * PI * it / n) }
        val twiddleImag = DoubleArray(n / 2) { sin(-2 * PI * it / n) }
        var totalWindowWeight = 0.0
        val hop = n / 2
        var start = 0
        var coveredUntil = 0
        while (start < pcm.size) {
            val valid = minOf(n, pcm.size - start)
            if (valid < n && coveredUntil >= pcm.size) break
            if (valid < n) issues += "Incomplete trailing spectral window zero-padded"
            val mean = (start until start + valid).sumOf { pcm[it].toDouble() } / valid
            var windowEnergy = 0.0
            for (i in 0 until n) {
                val weight = window[i]
                if (i < valid) {
                    real[i] = (pcm[start + i] - mean) / 32768.0 * weight
                    windowEnergy += weight * weight
                } else real[i] = 0.0
                imag[i] = 0.0
            }
            fft(real, imag, twiddleReal, twiddleImag)
            // ponytail: observed-fraction weighting is approximate; use a validated boundary PSD estimator if needed.
            val segmentWeight = valid.toDouble() / n
            val scale = n * windowEnergy
            for (k in 0..n / 2) {
                val sided = if (k == 0 || k == n / 2) 1.0 else 2.0
                bands[(k * 16 / n).coerceAtMost(7)] +=
                    segmentWeight * sided * (real[k] * real[k] + imag[k] * imag[k]) / scale
            }
            totalWindowWeight += segmentWeight
            if (valid < n) break
            coveredUntil = start + n
            start += hop
        }
        return bands.map { it / totalWindowWeight }
    }

    private fun fft(real: DoubleArray, imag: DoubleArray, twiddleReal: DoubleArray, twiddleImag: DoubleArray) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { val r = real[i]; real[i] = real[j]; real[j] = r }
        }
        var size = 2
        while (size <= n) {
            for (start in 0 until n step size) {
                for (k in 0 until size / 2) {
                    val a = start + k
                    val b = a + size / 2
                    val twiddle = k * n / size
                    val c = twiddleReal[twiddle]; val s = twiddleImag[twiddle]
                    val r = c * real[b] - s * imag[b]
                    val v = s * real[b] + c * imag[b]
                    real[b] = real[a] - r; imag[b] = imag[a] - v
                    real[a] += r; imag[a] += v
                }
            }
            size *= 2
        }
    }
}

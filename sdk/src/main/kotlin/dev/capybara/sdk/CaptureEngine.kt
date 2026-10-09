package dev.capybara.sdk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.AudioRouting
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.CombinedVibration
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** The digital amplitude is bounded; acoustic SPL and mechanical onset require calibration. */
internal fun acousticChirp(sampleRate: Int): ShortArray {
    require(sampleRate > 16_000) { "Negotiated sample rate cannot represent the 8 kHz excitation." }
    return ShortArray(sampleRate) { index ->
        val time = index.toDouble() / sampleRate
        val envelope = min(1.0, min(time / 0.02, (1.0 - time) / 0.02))
        (sin(2 * PI * (300 * time + 0.5 * 7_700 * time * time)) * envelope * 0.12 * Short.MAX_VALUE).toInt().toShort()
    }
}

internal class CaptureEngine(private val context: Context) {
    suspend fun capture(label: CaptureLabel, progress: (String) -> Unit): Scan = coroutineScope {
        check(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Microphone permission is required for a real capture."
        }
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val audioManager = context.getSystemService(AudioManager::class.java)
        val haptic = vibrator(context)
        val probePlan = ProbeBrain.select(label.profile, haptic.hasVibrator())
        val hapticManager = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java) else null
        val hapticIds = if (Build.VERSION.SDK_INT >= 31) hapticManager?.vibratorIds?.toList().orEmpty() else emptyList()
        val sensorThread = HandlerThread("CapybaraCapture").apply { start() }
        val handler = Handler(sensorThread.looper)
        val auditLock = Any()
        val metadata = ConcurrentHashMap<String, String>()
        val imu = mutableListOf<ImuSample>()
        val rotations = JSONArray()
        val accuracy = JSONArray()
        val configurations = JSONArray()
        val routes = JSONArray()
        val audioAnchors = JSONArray()
        val playbackAnchors = JSONArray()
        val events = mutableListOf<ExcitationEvent>()
        val effects = mutableListOf<Pair<String, AudioEffect>>()
        val sampleCount = AtomicInteger(0)
        val captureStart = AtomicLong(Long.MAX_VALUE)
        val acceptingSensors = AtomicBoolean(true)
        var record: AudioRecord? = null
        var track: AudioTrack? = null
        var callbackRegistered = false
        var recordRouteRegistered = false
        var trackRouteRegistered = false
        var previousConfiguration: String? = null
        val previousRoutes = mutableMapOf<String, Int>()
        var silenced: Boolean? = null
        var configurationChanged = false
        var routeChanged = false
        var failure: Throwable? = null

        fun observeConfiguration(configuration: AudioRecordingConfiguration?) = synchronized(auditLock) {
            if (configuration != null) {
                silenced = silenced == true || configuration.isClientSilenced
                val json = JSONObject().put("source", configuration.audioSource)
                    .put("clientSource", configuration.clientAudioSource)
                    .put("deviceFormat", configuration.format.toString())
                    .put("clientFormat", configuration.clientFormat.toString())
                    .put("device", audioDevice(configuration.audioDevice))
                    .put("silenced", configuration.isClientSilenced)
                    .put("effects", configuration.effects.joinToString { "${it.name}:${it.uuid}" })
                    .put("clientEffects", configuration.clientEffects.joinToString { "${it.name}:${it.uuid}" })
                val value = json.toString()
                if (previousConfiguration != value) {
                    if (previousConfiguration != null) configurationChanged = true
                    configurations.put(JSONObject().put("observedBootNanos", SystemClock.elapsedRealtimeNanos()).put("configuration", json))
                    previousConfiguration = value
                }
            }
        }

        fun observeRoute(kind: String, audio: AudioRouting) = synchronized(auditLock) {
            val device = audio.routedDevice
            val id = device?.id ?: -1
            if (previousRoutes[kind] != id) {
                if (previousRoutes[kind]?.let { it >= 0 } == true) routeChanged = true
                routes.put(JSONObject().put("kind", kind).put("observedBootNanos", SystemClock.elapsedRealtimeNanos())
                    .put("device", audioDevice(device)))
                previousRoutes[kind] = id
            }
        }

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.timestamp < captureStart.get() || !acceptingSensors.get()) return
                synchronized(auditLock) {
                    if (!acceptingSensors.get()) return
                    imu += ImuSample(event.sensor.type, event.timestamp, event.values[0], event.values[1], event.values[2])
                    if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        rotations.put(JSONObject().put("timestampNanos", event.timestamp)
                            .put("values", JSONArray(event.values.map(Float::toString))))
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, value: Int) {
                synchronized(auditLock) {
                    accuracy.put(JSONObject().put("type", sensor.type).put("accuracy", value)
                        .put("observedBootNanos", SystemClock.elapsedRealtimeNanos()))
                }
            }
        }
        val recordingCallback = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
                configs.firstOrNull { it.clientAudioSessionId == record?.audioSessionId }?.let(::observeConfiguration)
            }
        }
        val recordRoute = AudioRouting.OnRoutingChangedListener { observeRoute("input", it) }
        val trackRoute = AudioRouting.OnRoutingChangedListener { observeRoute("output", it) }

        try {
            progress("Opening microphone and motion sensors")
            val source = if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true") {
                MediaRecorder.AudioSource.UNPROCESSED
            } else MediaRecorder.AudioSource.VOICE_RECOGNITION
            val bufferBytes = AudioRecord.getMinBufferSize(48_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(bufferBytes > 0) { "48 kHz mono PCM16 microphone capture is unavailable: $bufferBytes" }
            val recorder = AudioRecord.Builder().setAudioSource(source).setAudioFormat(
                AudioFormat.Builder().setSampleRate(48_000).setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()
            ).setBufferSizeInBytes(maxOf(bufferBytes * 2, 19_200)).build()
            record = recorder
            check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Microphone initialization failed." }
            val rate = recorder.sampleRate
            val waveform = acousticChirp(rate)
            val stereoWaveform = ShortArray(waveform.size * 2) { waveform[it / 2] }
            val player = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(stereoWaveform.size * 2).build()
            track = player
            check(player.state != AudioTrack.STATE_UNINITIALIZED) { "Stereo speaker playback initialization failed." }
            check(player.write(stereoWaveform, 0, stereoWaveform.size, AudioTrack.WRITE_NON_BLOCKING) == stereoWaveform.size) {
                "The stereo excitation waveform could not be loaded completely."
            }
            metadata["audioOutputChannelsRequested"] = "stereo_same_excitation_left_right"
            val preferredInput = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
            val preferredOutput = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            metadata["audioPreferredInput"] = audioDevice(preferredInput)
            metadata["audioPreferredOutput"] = audioDevice(preferredOutput)
            metadata["audioPreferredInputAccepted"] = preferredInput?.let { recorder.setPreferredDevice(it).toString() } ?: "unavailable"
            metadata["audioPreferredOutputAccepted"] = preferredOutput?.let { player.setPreferredDevice(it).toString() } ?: "unavailable"
            fun disableEffect(name: String, available: Boolean, create: () -> AudioEffect?) {
                metadata["effect_${name}_available"] = available.toString()
                if (!available) return
                try {
                    val effect = create()
                    if (effect == null) { metadata["effect_${name}_status"] = "unavailable_for_session"; return }
                    effects += name to effect
                    metadata["effect_${name}_enabledBefore"] = effect.enabled.toString()
                    metadata["effect_${name}_hasControl"] = effect.hasControl().toString()
                    metadata["effect_${name}_disableStatus"] = if (effect.hasControl()) effect.setEnabled(false).toString() else "no_control"
                    metadata["effect_${name}_enabledAfter"] = effect.enabled.toString()
                } catch (error: IllegalStateException) {
                    metadata["effect_${name}_status"] = "unavailable:${error.javaClass.simpleName}"
                } catch (error: UnsupportedOperationException) {
                    metadata["effect_${name}_status"] = "unavailable:${error.javaClass.simpleName}"
                } catch (error: IllegalArgumentException) {
                    metadata["effect_${name}_status"] = "unavailable:${error.javaClass.simpleName}"
                }
            }
            disableEffect("aec", AcousticEchoCanceler.isAvailable()) { AcousticEchoCanceler.create(recorder.audioSessionId) }
            disableEffect("agc", AutomaticGainControl.isAvailable()) { AutomaticGainControl.create(recorder.audioSessionId) }
            disableEffect("ns", NoiseSuppressor.isAvailable()) { NoiseSuppressor.create(recorder.audioSessionId) }
            metadata["vendorPreprocessing"] = "unknown_except_reported_recording_effects"
            recorder.registerAudioRecordingCallback({ handler.post(it) }, recordingCallback)
            callbackRegistered = true
            recorder.addOnRoutingChangedListener(recordRoute, handler)
            recordRouteRegistered = true
            player.addOnRoutingChangedListener(trackRoute, handler)
            trackRouteRegistered = true
            listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_ROTATION_VECTOR).forEach { type ->
                val sensor = sensorManager.getDefaultSensor(type)
                metadata["sensor_${type}_registration"] = if (sensor == null) "unavailable" else {
                    val requestedPeriod = if (sensor.minDelay > 0) sensor.minDelay else SensorManager.SENSOR_DELAY_FASTEST
                    metadata["sensor_${type}_requestedPeriodUs"] = requestedPeriod.toString()
                    metadata["sensor_${type}_requestedPolicy"] = if (sensor.minDelay > 0) "advertised_minDelay" else "SENSOR_DELAY_FASTEST"
                    if (sensorManager.registerListener(sensorListener, sensor, requestedPeriod, 0, handler)) "registered" else "failed"
                }
                sensor?.let { metadata["sensor_${type}_minDelayUs"] = it.minDelay.toString() }
            }
            metadata["audioSourceRequested"] = source.toString()
            metadata["audioSourceName"] = if (source == MediaRecorder.AudioSource.UNPROCESSED) "UNPROCESSED" else "VOICE_RECOGNITION_fallback"
            metadata["audioSampleRateRequested"] = "48000"
            metadata["audioSampleRateActual"] = rate.toString()
            metadata["audioBufferFrames"] = recorder.bufferSizeInFrames.toString()
            metadata["audioInputTimebase"] = "BOOTTIME"
            metadata["audioOutputTimebase"] = "MONOTONIC"
            metadata["imuTimebase"] = "BOOTTIME"
            metadata["capturePurpose"] = "raw_measurement_only"
            metadata["probePlan"] = probePlan.description
            metadata["probePlanSelection"] = "fixed_profile_and_hardware_capabilities"
            metadata["contactQuality"] = "unavailable_without_calibration"
            metadata["acousticExcitation"] = "linear_chirp_300_8000Hz_1s_20ms_fades_digital_amplitude_0.12"
            metadata["acousticSpl"] = "unmeasured"
            metadata["hapticPhysicalOnset"] = "unavailable"
            metadata["systemMusicVolumeIndex"] = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toString()
            metadata["systemSystemVolumeIndex"] = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM).toString()
            val monoBefore = System.nanoTime()
            val boot = SystemClock.elapsedRealtimeNanos()
            val monoAfter = System.nanoTime()
            metadata["bootMinusMonotonicNanos"] = (boot - (monoBefore + (monoAfter - monoBefore) / 2)).toString()
            metadata["clockMappingBracketNanos"] = (monoAfter - monoBefore).toString()
            val started = SystemClock.elapsedRealtimeNanos()
            captureStart.set(started)
            recorder.startRecording()
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone did not enter recording state." }
            progress("Recording ambient baseline")
            val excitation = launch {
                delay(800)
                metadata["baselineSampleCount"] = sampleCount.get().toString()
                progress("Requesting acoustic chirp")
                events += ExcitationEvent("acoustic_chirp", SystemClock.elapsedRealtimeNanos())
                player.play()
                delay(300)
                val timestamp = AudioTimestamp()
                if (player.getTimestamp(timestamp)) playbackAnchors.put(JSONObject()
                    .put("framePosition", timestamp.framePosition).put("monotonicNanos", timestamp.nanoTime))
                observeRoute("output", player)
                delay(900)
                if (player.getTimestamp(timestamp)) playbackAnchors.put(JSONObject()
                    .put("framePosition", timestamp.framePosition).put("monotonicNanos", timestamp.nanoTime))
                observeRoute("output", player)
                metadata["playbackHeadFrames"] = player.playbackHeadPosition.toString()
                delay(700)
                if (probePlan.requestHaptics) {
                    progress("Requesting finite haptic pulses")
                    val timing = longArrayOf(0, 80, 120, 80, 120, 120)
                    val amplitude = if (haptic.hasAmplitudeControl()) intArrayOf(0, 64, 0, 96, 0, 128) else intArrayOf(0, -1, 0, -1, 0, -1)
                    metadata["hapticTimingsMillis"] = timing.joinToString(",")
                    metadata["hapticAmplitudes"] = amplitude.joinToString(",")
                    events += ExcitationEvent("haptic_finite_waveform", SystemClock.elapsedRealtimeNanos())
                    val effect = VibrationEffect.createWaveform(timing, amplitude, -1)
                    if (Build.VERSION.SDK_INT >= 31 && hapticManager != null && hapticIds.size >= 2) {
                        val combined = CombinedVibration.startParallel().apply {
                            hapticIds.forEach { addVibrator(it, effect) }
                        }.combine()
                        hapticManager.vibrate(combined, VibrationAttributes.Builder()
                            .setUsage(VibrationAttributes.USAGE_PHYSICAL_EMULATION).build())
                        metadata["hapticUsage"] = "PHYSICAL_EMULATION"
                        metadata["hapticActuatorsRequested"] = hapticIds.joinToString(",")
                        metadata["hapticMode"] = "all_exposed_vibrators_synchronized"
                    } else if (Build.VERSION.SDK_INT >= 33) {
                        haptic.vibrate(effect, VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_PHYSICAL_EMULATION).build())
                        metadata["hapticUsage"] = "PHYSICAL_EMULATION"
                        metadata["hapticActuatorsRequested"] = "default_only_fallback"
                        metadata["hapticMode"] = "default_vibrator_fallback"
                    } else {
                        @Suppress("DEPRECATION")
                        haptic.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
                        metadata["hapticUsage"] = "ASSISTANCE_SONIFICATION_api29_32"
                        metadata["hapticActuatorsRequested"] = "default_only_fallback"
                        metadata["hapticMode"] = "default_vibrator_fallback"
                    }
                } else metadata["hapticStatus"] = "unavailable"
                delay(640)
                progress("Recording physical decay")
            }
            val pcm = ShortArray(rate * 6)
            val buffer = ShortArray(2_048)
            val deadline = started + 6_200_000_000L
            var lastRead = started
            var maxHiatus = 0L
            var lastAudit = 0L
            var emptyReads = 0
            while (sampleCount.get() < pcm.size && SystemClock.elapsedRealtimeNanos() < deadline) {
                currentCoroutineContext().ensureActive()
                check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone capture was interrupted." }
                val count = recorder.read(buffer, 0, min(buffer.size, pcm.size - sampleCount.get()), AudioRecord.READ_NON_BLOCKING)
                check(count >= 0) { "Microphone read failed with Android error $count." }
                val now = SystemClock.elapsedRealtimeNanos()
                if (count > 0) {
                    buffer.copyInto(pcm, sampleCount.get(), 0, count)
                    sampleCount.addAndGet(count)
                    maxHiatus = maxOf(maxHiatus, now - lastRead)
                    lastRead = now
                } else emptyReads++
                if (now - lastAudit >= 500_000_000L) {
                    val timestamp = AudioTimestamp()
                    val status = recorder.getTimestamp(timestamp, AudioTimestamp.TIMEBASE_BOOTTIME)
                    if (status == AudioRecord.SUCCESS) audioAnchors.put(JSONObject().put("readSampleCount", sampleCount.get())
                        .put("framePosition", timestamp.framePosition).put("bootNanos", timestamp.nanoTime))
                    metadata["audioTimestampLastStatus"] = status.toString()
                    observeConfiguration(recorder.activeRecordingConfiguration)
                    observeRoute("input", recorder)
                    lastAudit = now
                }
                if (count == 0) delay(5)
            }
            currentCoroutineContext().ensureActive()
            excitation.join()
            val ended = SystemClock.elapsedRealtimeNanos()
            acceptingSensors.set(false)
            val endMonoBefore = System.nanoTime()
            val endBoot = SystemClock.elapsedRealtimeNanos()
            val endMonoAfter = System.nanoTime()
            metadata["bootMinusMonotonicNanosEnd"] = (endBoot - (endMonoBefore + (endMonoAfter - endMonoBefore) / 2)).toString()
            metadata["clockMappingEndBracketNanos"] = (endMonoAfter - endMonoBefore).toString()
            metadata["clockMappingOffsetChangeNanos"] = (metadata.getValue("bootMinusMonotonicNanosEnd").toLong() - metadata.getValue("bootMinusMonotonicNanos").toLong()).toString()
            observeConfiguration(recorder.activeRecordingConfiguration)
            observeRoute("input", recorder)
            effects.forEach { (name, effect) ->
                metadata["effect_${name}_enabledFinal"] = try { effect.enabled.toString() }
                    catch (error: IllegalStateException) { "unavailable:${error.javaClass.simpleName}" }
            }
            metadata["activeMicrophones"] = try {
                recorder.activeMicrophones.joinToString("\n") {
                    "id=${it.id};description=${it.description};type=${it.type};location=${it.location};position=${it.position.x},${it.position.y},${it.position.z};orientation=${it.orientation.x},${it.orientation.y},${it.orientation.z};mapping=${it.channelMapping};sensitivity=${it.sensitivity};response=${it.frequencyResponse}"
                }.ifEmpty { "unavailable" }
            } catch (error: java.io.IOException) { "unavailable:${error.javaClass.simpleName}" }
            metadata["audioSampleCount"] = sampleCount.get().toString()
            metadata["audioExpectedSamples"] = pcm.size.toString()
            metadata["audioReadMaxHiatusNanos"] = maxHiatus.toString()
            metadata["audioEmptyReadPolls"] = emptyReads.toString()
            metadata["audioTimestampCount"] = audioAnchors.length().toString()
            metadata["audioTimestampAnchors"] = audioAnchors.toString()
            metadata["playbackTimestampAnchors"] = playbackAnchors.toString()
            metadata["audioSampleLoss"] = "unknown_no_overrun_counter_in_AudioRecord"
            synchronized(auditLock) {
                metadata["audioSilencedEver"] = silenced?.toString() ?: "unavailable"
                metadata["audioConfigurationChanged"] = configurationChanged.toString()
                metadata["audioRouteChanged"] = routeChanged.toString()
                metadata["audioConfigurations"] = configurations.toString()
                metadata["audioRoutes"] = routes.toString()
                metadata["rotationVectorRaw"] = rotations.toString()
                metadata["sensorAccuracyEvents"] = accuracy.toString()
                val capturedImu = imu.filter { it.timestampNanos <= ended }
                capturedImu.groupBy { it.type }.forEach { (type, samples) ->
                    val deltas = samples.zipWithNext { a, b -> b.timestampNanos - a.timestampNanos }
                    val elapsed = samples.last().timestampNanos - samples.first().timestampNanos
                    metadata["sensor_${type}_count"] = samples.size.toString()
                    metadata["sensor_${type}_maxGapNanos"] = (deltas.maxOrNull() ?: 0L).toString()
                    metadata["sensor_${type}_nonMonotonicCount"] = deltas.count { it <= 0 }.toString()
                    metadata["sensor_${type}_rateHz"] = if (elapsed > 0) ((samples.size - 1) * 1e9 / elapsed).toString() else "unavailable"
                }
                progress("Real capture acquired; calibration and models are unavailable")
                Scan(label, rate, pcm.copyOf(sampleCount.get()), capturedImu, events.toList(), started, ended, metadata.toMap())
            }
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            acceptingSensors.set(false)
            val cleanupErrors = mutableListOf<Throwable>()
            fun cleanup(action: () -> Unit) { try { action() } catch (error: Throwable) { cleanupErrors += error } }
            cleanup { sensorManager.unregisterListener(sensorListener) }
            record?.let { recorder ->
                if (callbackRegistered) cleanup { recorder.unregisterAudioRecordingCallback(recordingCallback) }
                if (recordRouteRegistered) cleanup { recorder.removeOnRoutingChangedListener(recordRoute) }
                cleanup { if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop() }
                effects.forEach { (_, effect) -> cleanup { effect.release() } }
                cleanup { recorder.release() }
            }
            track?.let { player ->
                if (trackRouteRegistered) cleanup { player.removeOnRoutingChangedListener(trackRoute) }
                cleanup { if (player.playState == AudioTrack.PLAYSTATE_PLAYING) player.stop() }
                cleanup { player.release() }
            }
            cleanup { haptic.cancel() }
            cleanup { handler.removeCallbacksAndMessages(null); sensorThread.quitSafely(); sensorThread.join(1_000) }
            cleanupErrors.forEach { failure?.addSuppressed(it) }
            if (failure == null && cleanupErrors.isNotEmpty()) throw IllegalStateException("Capture resource release failed.", cleanupErrors.first())
        }
    }
}

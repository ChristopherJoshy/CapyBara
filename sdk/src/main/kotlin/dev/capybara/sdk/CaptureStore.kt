package dev.capybara.sdk

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID

internal class CaptureStore(context: Context) {
    private val root = File(context.noBackupFilesDir, "capybara/captures")

    fun save(scan: Scan): String {
        require(scan.sampleRate > 0 && scan.endedBootNanos >= scan.startedBootNanos)
        check(root.isDirectory || root.mkdirs()) { "Cannot create private capture storage." }
        val id = UUID.randomUUID().toString()
        val temporary = File(root, ".$id.partial")
        val destination = File(root, id)
        check(temporary.mkdir()) { "Cannot create capture directory." }
        try {
            val pcm = ByteBuffer.allocate(scan.pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            scan.pcm.forEach(pcm::putShort)
            write(File(temporary, "audio.pcm"), pcm.array())
            val imu = buildString {
                append("type,timestampNanos,x,y,z\n")
                scan.imu.forEach { append("${it.type},${it.timestampNanos},${it.x},${it.y},${it.z}\n") }
            }.toByteArray(Charsets.UTF_8)
            write(File(temporary, "imu.csv"), imu)
            val label = JSONObject().put("objectId", scan.label.objectId).put("sessionId", scan.label.sessionId)
                .put("condition", scan.label.condition).put("position", scan.label.position).put("profile", scan.label.profile)
            val metadata = JSONObject().put("schemaVersion", 1).put("captureId", id).put("label", label)
                .put("sampleRate", scan.sampleRate).put("sampleCount", scan.pcm.size)
                .put("pcmEncoding", "signed_pcm16_little_endian_mono")
                .put("startedBootNanos", scan.startedBootNanos).put("endedBootNanos", scan.endedBootNanos)
                .put("audioSha256", sha256(pcm.array())).put("imuSha256", sha256(imu))
                .put("metadata", JSONObject(scan.metadata))
                .put("excitations", JSONArray().apply {
                    scan.excitations.forEach {
                        put(JSONObject().put("kind", it.kind).put("requestedBootNanos", it.requestedBootNanos))
                    }
                })
            write(File(temporary, "metadata.json"), metadata.toString(2).toByteArray(Charsets.UTF_8))
            check(temporary.renameTo(destination)) { "Cannot commit capture." }
            return destination.absolutePath
        } catch (error: Throwable) {
            if (temporary.exists() && !temporary.deleteRecursively()) {
                error.addSuppressed(java.io.IOException("Cannot remove incomplete capture ${temporary.name}."))
            }
            throw error
        }
    }

    fun list(): List<String> = root.listFiles()?.filter {
        !it.name.startsWith(".") && File(it, "metadata.json").isFile && File(it, "audio.pcm").isFile && File(it, "imu.csv").isFile
    }?.sortedByDescending { it.lastModified() }?.map { it.absolutePath } ?: emptyList()

    fun label(path: String): CaptureLabel {
        val folder = File(path).canonicalFile
        require(folder.parentFile == root.canonicalFile) { "Capture is outside this application's private store." }
        val label = JSONObject(File(folder, "metadata.json").readText()).getJSONObject("label")
        return CaptureLabel(label.getString("objectId"), label.getString("sessionId"),
            label.getString("condition"), label.getString("position"), label.getString("profile"))
    }

    fun loadPath(path: String): StoredCapture {
        val folder = File(path).canonicalFile
        require(folder.parentFile == root.canonicalFile) { "Capture is outside this application's private capture store." }
        return loadId(folder.name)
    }

    fun loadId(captureId: String): StoredCapture {
        require(UUID.fromString(captureId).toString() == captureId) { "Invalid capture identifier." }
        val folder = File(root, captureId).canonicalFile
        require(folder.parentFile == root.canonicalFile && folder.isDirectory) { "Capture is missing from private storage." }
        val metadata = JSONObject(File(folder, "metadata.json").readText())
        require(metadata.getInt("schemaVersion") == 1 && metadata.getString("captureId") == captureId) {
            "Unsupported capture schema or identifier."
        }
        require(metadata.getString("pcmEncoding") == "signed_pcm16_little_endian_mono") {
            "Unsupported audio encoding."
        }
        val sampleRate = metadata.getInt("sampleRate")
        val sampleCount = metadata.getInt("sampleCount")
        require(sampleRate > 0 && sampleCount >= 0) { "Invalid stored audio dimensions." }
        val audioBytes = File(folder, "audio.pcm").readBytes()
        require(audioBytes.size.toLong() == sampleCount.toLong() * 2) { "Stored PCM length does not match metadata." }
        require(sha256(audioBytes) == metadata.getString("audioSha256")) { "Stored PCM provenance hash mismatch." }
        val pcmBuffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN)
        val pcm = ShortArray(sampleCount) { pcmBuffer.short }

        val imuBytes = File(folder, "imu.csv").readBytes()
        require(sha256(imuBytes) == metadata.getString("imuSha256")) { "Stored IMU provenance hash mismatch." }
        val imuRows = String(imuBytes, Charsets.UTF_8).lineSequence().toList()
        require(imuRows.firstOrNull() == "type,timestampNanos,x,y,z") { "Unsupported stored IMU format." }
        val imu = imuRows.drop(1).filter { it.isNotBlank() }.map { row ->
            val fields = row.split(',')
            require(fields.size == 5) { "Malformed stored IMU row." }
            ImuSample(fields[0].toInt(), fields[1].toLong(), fields[2].toFloat(), fields[3].toFloat(), fields[4].toFloat())
        }
        val labelJson = metadata.getJSONObject("label")
        val label = CaptureLabel(
            labelJson.getString("objectId"), labelJson.getString("sessionId"),
            labelJson.getString("condition"), labelJson.getString("position"), labelJson.getString("profile"),
        )
        val attributesJson = metadata.getJSONObject("metadata")
        val attributes = buildMap {
            val keys = attributesJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = attributesJson.get(key)
                require(value is String) { "Capture metadata values must be strings." }
                put(key, value)
            }
        }
        val excitationJson = metadata.getJSONArray("excitations")
        val excitations = List(excitationJson.length()) { index ->
            val event = excitationJson.getJSONObject(index)
            ExcitationEvent(event.getString("kind"), event.getLong("requestedBootNanos"))
        }
        val scan = Scan(
            label, sampleRate, pcm, imu, excitations,
            metadata.getLong("startedBootNanos"), metadata.getLong("endedBootNanos"), attributes,
        )
        return StoredCapture(captureId, scan, metadata)
    }

    fun requireMatchesSnapshot(capture: StoredCapture, snapshot: JSONObject) {
        require(jsonKeys(capture.metadata) == jsonKeys(snapshot)) { "Reference capture metadata fields changed after enrollment." }
        for (key in listOf(
            "schemaVersion", "captureId", "sampleRate", "sampleCount", "pcmEncoding",
            "startedBootNanos", "endedBootNanos", "audioSha256", "imuSha256",
        )) {
            require(capture.metadata.get(key).toString() == snapshot.get(key).toString()) {
                "Reference capture provenance changed after enrollment."
            }
        }
        require(stringObjectMatches(capture.metadata.getJSONObject("label"), snapshot.getJSONObject("label"))) {
            "Reference capture label changed after enrollment."
        }
        require(stringObjectMatches(capture.metadata.getJSONObject("metadata"), snapshot.getJSONObject("metadata"))) {
            "Reference capture quality metadata changed after enrollment."
        }
        val actualEvents = capture.metadata.getJSONArray("excitations")
        val savedEvents = snapshot.getJSONArray("excitations")
        require(actualEvents.length() == savedEvents.length()) { "Reference capture excitation list changed after enrollment." }
        for (index in 0 until actualEvents.length()) {
            val actual = actualEvents.getJSONObject(index)
            val saved = savedEvents.getJSONObject(index)
            require(jsonKeys(actual) == jsonKeys(saved) &&
                actual.getString("kind") == saved.getString("kind") &&
                actual.getLong("requestedBootNanos") == saved.getLong("requestedBootNanos")) {
                "Reference capture excitation provenance changed after enrollment."
            }
        }
    }

    private fun stringObjectMatches(actual: JSONObject, saved: JSONObject): Boolean {
        val keys = jsonKeys(actual)
        return keys == jsonKeys(saved) && keys.all { actual.getString(it) == saved.getString(it) }
    }

    private fun jsonKeys(json: JSONObject): Set<String> = buildSet {
        val keys = json.keys()
        while (keys.hasNext()) add(keys.next())
    }


    private fun write(file: File, bytes: ByteArray) {
        FileOutputStream(file).use { it.write(bytes); it.fd.sync() }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
}

internal data class StoredCapture(val id: String, val scan: Scan, val metadata: JSONObject)

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

    private fun write(file: File, bytes: ByteArray) {
        FileOutputStream(file).use { it.write(bytes); it.fd.sync() }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
}

package dev.capybara.sdk

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class ResearchBaseline(val id: String, val objectId: String, val profile: String, val createdAtMillis: Long)

/** Immutable measured baselines. They do not imply calibrated enrollment or integrity. */
class SignatureVault(context: Context) {
    private val captureRoot = File(context.noBackupFilesDir, "capybara/captures").canonicalFile
    private val root = File(context.noBackupFilesDir, "capybara/references")

    @Synchronized
    fun saveResearchBaseline(capturePaths: List<String>): ResearchBaseline {
        require(capturePaths.size == 4) { "A baseline needs exactly four measured positions A-D." }
        val captures = capturePaths.map { path ->
            val folder = File(path).canonicalFile
            require(folder.parentFile == captureRoot) { "Baseline captures must belong to this application's private capture store." }
            JSONObject(File(folder, "metadata.json").readText()).also {
                require(it.getInt("schemaVersion") == 1) { "Unsupported capture schema." }
            }
        }
        val labels = captures.map { it.getJSONObject("label") }
        val profileId = labels.first().getString("profile")
        val profile = ProfileEngine.get(profileId)
        require(labels.map { it.getString("position") }.toSet() == profile.positions.toSet()) {
            "Each registered position for ${profile.id} must occur once."
        }
        for (key in listOf("objectId", "sessionId", "condition")) {
            require(labels.map { it.getString(key) }.distinct().size == 1) { "Baseline $key differs between positions." }
        }
        require(labels.all { it.getString("profile") == profile.id }) { "Baseline profile differs between positions." }
        require(captures.map { it.getString("captureId") }.distinct().size == 4) { "A capture cannot be reused at another position." }
        val reference = ResearchBaseline(UUID.randomUUID().toString(), labels[0].getString("objectId"), profile.id, System.currentTimeMillis())
        val json = JSONObject().put("schemaVersion", 1).put("id", reference.id).put("objectId", reference.objectId)
            .put("profile", reference.profile).put("createdAtMillis", reference.createdAtMillis)
            .put("modelVersion", JSONObject.NULL).put("calibrationVersion", JSONObject.NULL)
            .put("status", "RESEARCH_BASELINE_UNCALIBRATED").put("captures", JSONArray(captures))
        check(root.isDirectory || root.mkdirs()) { "Cannot create private reference vault." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(reference.id.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        val destination = File(root, "${reference.id}.ref")
        val temporary = File(root, ".${reference.id}.partial")
        check(temporary.createNewFile()) { "Reference identifier already exists." }
        try {
            FileOutputStream(temporary).use { output ->
                output.write(cipher.iv.size)
                output.write(cipher.iv)
                output.write(ciphertext)
                output.fd.sync()
            }
            check(!destination.exists() && temporary.renameTo(destination)) { "Cannot commit immutable reference." }
        } finally { if (temporary.exists()) check(temporary.delete()) { "Cannot remove incomplete reference." } }
        return reference
    }

    @Synchronized
    fun readResearchBaseline(id: String): JSONObject {
        require(UUID.fromString(id).toString() == id) { "Invalid reference identifier." }
        val bytes = File(root, "$id.ref").readBytes()
        require(bytes.size > 29 && bytes[0].toInt() == 12) { "Invalid encrypted reference format." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        cipher.updateAAD(id.toByteArray(Charsets.UTF_8))
        val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(13, bytes.size)), Charsets.UTF_8))
        require(json.getInt("schemaVersion") == 1 && json.getString("id") == id) { "Unsupported reference schema or identifier." }
        require(json.getString("status") == "RESEARCH_BASELINE_UNCALIBRATED") { "Unsupported reference model/calibration status." }
        return json
    }

    @Synchronized
    fun listResearchBaselines(): List<ResearchBaseline> = root.listFiles()?.filter { it.extension == "ref" }?.map { file ->
        val json = readResearchBaseline(file.nameWithoutExtension)
        ResearchBaseline(json.getString("id"), json.getString("objectId"), json.getString("profile"), json.getLong("createdAtMillis"))
    }?.sortedByDescending { it.createdAtMillis } ?: emptyList()

    private fun key(): SecretKey = synchronized(keyLock) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as SecretKey?) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            generateKey()
        }
    }

    companion object {
        private const val ALIAS = "capybara.research.references.v1"
        private val keyLock = Any()
    }
}

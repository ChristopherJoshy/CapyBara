package dev.capybara.sdk

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/** Offline acquisition. No trained-model or integrity decision is implied by a capture. */
class Capybara(context: Context) {
    private val context = context.applicationContext
    private val store = CaptureStore(this.context)
    private val vault = SignatureVault(this.context)


    fun getCapabilities(): DeviceCapabilities = capabilities(context)

    fun getSupportedProfiles(): List<IntegrityProfile> = ProfileEngine.supportedProfiles()

    fun getProfile(profileId: String): IntegrityProfile = ProfileEngine.get(profileId)

    fun analyzePhysics(scan: Scan): PhysicsEvidence = PhysicsEngine.analyze(scan)

    fun saveResearchBaseline(capturePaths: List<String>): ResearchBaseline {
        capturePaths.forEach(store::loadPath)
        return vault.saveResearchBaseline(capturePaths)
    }

    fun listResearchBaselines(): List<ResearchBaseline> = vault.listResearchBaselines()


    /** Progress callbacks run on the capture dispatcher; cancellation releases all hardware. */
    suspend fun capture(label: CaptureLabel, onProgress: (String) -> Unit = {}): Scan {
        ProfileEngine.validateCapture(label)
        check(captureLock.tryLock()) { "A Capybara capture is already active." }
        try {
            return withContext(Dispatchers.IO) { CaptureEngine(context).capture(label, onProgress) }
        } finally {
            captureLock.unlock()
        }
    }

    /** Call from an IO dispatcher. Returns the private capture directory's absolute path. */
    fun saveCapture(scan: Scan): String = store.save(scan)

    fun listCaptures(): List<String> = store.list()

    fun getCaptureLabel(path: String): CaptureLabel = store.label(path)

    /** Build a reference comparison from stored measurements; returns INCONCLUSIVE until trained models/calibration exist. */
    suspend fun evaluate(referenceId: String, capturePaths: List<String>): IntegrityAssessment {
        require(capturePaths.isNotEmpty()) { "A verification needs every registered position." }
        val (baseline, referenceCaptures, currentCaptures) = withContext(Dispatchers.IO) {
            val reference = vault.readResearchBaseline(referenceId)
            val storedReference = reference.getJSONArray("captures").let { rows ->
                List(rows.length()) { index ->
                    val snapshot = rows.getJSONObject(index)
                    store.loadId(snapshot.getString("captureId")).also {
                        store.requireMatchesSnapshot(it, snapshot)
                    }
                }
            }
            Triple(reference, storedReference, capturePaths.map(store::loadPath))
        }
        return withContext(Dispatchers.Default) {
            EvidenceEngine.evaluate(referenceId, baseline, referenceCaptures, currentCaptures)
        }
    }


    companion object {
        private val captureLock = Mutex()
    }
}

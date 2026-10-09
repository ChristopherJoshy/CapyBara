package dev.capybara.sdk

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/** Offline acquisition. No trained-model or integrity decision is implied by a capture. */
class Capybara(context: Context) {
    private val context = context.applicationContext
    private val store = CaptureStore(this.context)

    fun getCapabilities(): DeviceCapabilities = capabilities(context)

    fun analyzePhysics(scan: Scan): PhysicsEvidence = PhysicsEngine.analyze(scan)

    /** Progress callbacks run on the capture dispatcher; cancellation releases all hardware. */
    suspend fun capture(label: CaptureLabel, onProgress: (String) -> Unit = {}): Scan {
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

    companion object {
        private val captureLock = Mutex()
    }
}

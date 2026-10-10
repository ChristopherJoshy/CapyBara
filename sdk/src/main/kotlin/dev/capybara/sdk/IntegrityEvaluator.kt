package dev.capybara.sdk

import org.json.JSONArray
import org.json.JSONObject

enum class IntegrityDecision { MATCH, CHANGED, INCONCLUSIVE }

data class PositionIntegrityEvidence(
    val position: String,
    val reference: PhysicsEvidence,
    val current: PhysicsEvidence,
)

data class IntegrityAssessment(
    val decision: IntegrityDecision,
    val referenceId: String,
    val profile: String,
    val pipelineVersion: String,
    /** Physics features in the training schema; not a tokenizer request or inference output. */
    val layaEvidenceStateJson: String,
    val positions: List<PositionIntegrityEvidence>,
    val blockingReasons: List<String>,
)

internal object EvidenceEngine {
    const val PIPELINE_VERSION = "physics-v1"

    fun evaluate(
        referenceId: String,
        baseline: JSONObject,
        referenceCaptures: List<StoredCapture>,
        currentCaptures: List<StoredCapture>,
    ): IntegrityAssessment {
        require(referenceId == baseline.getString("id")) { "Reference identifier does not match its authenticated baseline." }
        require(baseline.getString("status") == "RESEARCH_BASELINE_UNCALIBRATED") {
            "Only an authenticated research baseline is currently supported."
        }
        require(baseline.isNull("modelVersion") && baseline.isNull("calibrationVersion")) {
            "A calibrated baseline requires a versioned decision pipeline."
        }
        val profile = ProfileEngine.get(baseline.getString("profile"))
        val expectedPositions = profile.positions.toSet()
        currentCaptures.forEach {
            requireReferenceObjectLabel(baseline.getString("objectId"), it.scan.label.objectId)
        }
        validateSet("Reference", referenceCaptures, profile.id, expectedPositions)
        validateSet("Current scan", currentCaptures, profile.id, expectedPositions)
        require(referenceCaptures.map { it.id }.toSet().intersect(currentCaptures.map { it.id }.toSet()).isEmpty()) {
            "A reference capture cannot be reused as a verification capture."
        }
        require(referenceCaptures.map { it.scan.label.sessionId }.toSet()
            .intersect(currentCaptures.map { it.scan.label.sessionId }.toSet()).isEmpty()) {
            "Verification must use a separate capture session from enrollment."
        }


        val references = referenceCaptures.associateBy { it.scan.label.position }
        val currents = currentCaptures.associateBy { it.scan.label.position }
        val positions = profile.positions.map { position ->
            PositionIntegrityEvidence(
                position,
                PhysicsEngine.analyze(references.getValue(position).scan),
                PhysicsEngine.analyze(currents.getValue(position).scan),
            )
        }
        val input = JSONObject().put("profile", profile.id).put("evidence", JSONObject().apply {
            positions.forEach { comparison ->
                val reference = references.getValue(comparison.position).scan
                val current = currents.getValue(comparison.position).scan
                put(comparison.position, JSONObject()
                    .put("reference", features(reference, comparison.reference))
                    .put("current", features(current, comparison.current))
                    .put("delta", delta(reference, comparison.reference, current, comparison.current)))
            }
        }).toString()
        return TrustGate.evaluate(referenceId, profile.id, positions, input)
    }

    internal fun requireReferenceObjectLabel(referenceObjectId: String, captureObjectId: String) {
        require(captureObjectId == referenceObjectId) {
            "Current scan object label differs from the selected reference."
        }
    }

    private fun validateSet(
        name: String,
        captures: List<StoredCapture>,
        profile: String,
        expectedPositions: Set<String>,
    ) {
        require(captures.size == expectedPositions.size) { "$name must contain every registered position exactly once." }
        val labels = captures.map { it.scan.label }
        labels.forEach { ProfileEngine.validateCapture(it) }
        require(labels.all { it.profile == profile }) { "$name profile differs from its reference." }
        require(labels.map { it.position }.toSet() == expectedPositions) { "$name positions do not match the registered profile." }
        require(captures.map { it.id }.distinct().size == expectedPositions.size) { "$name reuses a capture." }
        require(labels.map { it.objectId }.distinct().size == 1) { "$name object label differs between positions." }
        require(labels.map { it.sessionId }.distinct().size == 1) { "$name session differs between positions." }
        require(labels.map { it.condition }.distinct().size == 1) { "$name condition differs between positions." }
    }

    private fun features(scan: Scan, evidence: PhysicsEvidence) = JSONObject()
        .put("audioSampleRateHz", scan.sampleRate)
        .put("audioRms", jsonNumber(evidence.audioRms))
        .put("noiseRms", jsonNumber(evidence.noiseRms))
        .put("peak", jsonNumber(evidence.peak))
        .put("clippedSamples", evidence.clippedSamples)
        .put("acousticBandEnergy", JSONArray().apply { evidence.acousticBandEnergy.forEach { put(jsonNumber(it)) } })
        .put("accelerationRms", jsonNumber(evidence.accelerationRms))
        .put("gyroRms", jsonNumber(evidence.gyroRms))
        .put("accelerometerHz", jsonNumber(evidence.accelerometerHz))
        .put("gyroscopeHz", jsonNumber(evidence.gyroscopeHz))
        .put("durationSeconds", jsonNumber(evidence.durationSeconds))

    private fun delta(reference: Scan, referenceEvidence: PhysicsEvidence, current: Scan, currentEvidence: PhysicsEvidence) =
        JSONObject()
            .put("audioSampleRateHz", current.sampleRate - reference.sampleRate)
            .put("audioRms", difference(currentEvidence.audioRms, referenceEvidence.audioRms))
            .put("noiseRms", difference(currentEvidence.noiseRms, referenceEvidence.noiseRms))
            .put("peak", difference(currentEvidence.peak, referenceEvidence.peak))
            .put("clippedSamples", currentEvidence.clippedSamples - referenceEvidence.clippedSamples)
            .put("acousticBandEnergy", if (currentEvidence.acousticBandEnergy.size == referenceEvidence.acousticBandEnergy.size &&
                currentEvidence.acousticBandEnergy.isNotEmpty()) JSONArray().apply {
                    currentEvidence.acousticBandEnergy.indices.forEach { index ->
                        put(difference(currentEvidence.acousticBandEnergy[index], referenceEvidence.acousticBandEnergy[index]))
                    }
                } else JSONObject.NULL)
            .put("accelerationRms", difference(currentEvidence.accelerationRms, referenceEvidence.accelerationRms))
            .put("gyroRms", difference(currentEvidence.gyroRms, referenceEvidence.gyroRms))
            .put("accelerometerHz", difference(currentEvidence.accelerometerHz, referenceEvidence.accelerometerHz))
            .put("gyroscopeHz", difference(currentEvidence.gyroscopeHz, referenceEvidence.gyroscopeHz))
            .put("durationSeconds", difference(currentEvidence.durationSeconds, referenceEvidence.durationSeconds))

    private fun difference(current: Double, reference: Double): Any =
        if (current.isFinite() && reference.isFinite()) current - reference else JSONObject.NULL

    private fun jsonNumber(value: Double): Any = if (value.isFinite()) value else JSONObject.NULL
}

private object TrustGate {
    fun evaluate(
        referenceId: String,
        profile: String,
        positions: List<PositionIntegrityEvidence>,
        evidenceStateJson: String,
    ) = IntegrityAssessment(
        decision = IntegrityDecision.INCONCLUSIVE,
        referenceId = referenceId,
        profile = profile,
        pipelineVersion = EvidenceEngine.PIPELINE_VERSION,
        layaEvidenceStateJson = evidenceStateJson,
        positions = positions,
        blockingReasons = buildList {
            add("REFERENCE_BASELINE_UNCALIBRATED")
            add("SPECIALIST_MODELS_UNAVAILABLE")
            add("CAPYBARA_LAYA_NOT_TRAINED_OR_INSTALLED")
            add("CALIBRATION_NOT_ESTABLISHED")
            if (positions.any { it.reference.limitations.isNotEmpty() || it.current.limitations.isNotEmpty() }) {
                add("MEASUREMENT_LIMITATIONS_PRESENT")
            }
        },
    )
}

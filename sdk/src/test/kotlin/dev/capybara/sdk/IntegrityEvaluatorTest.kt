package dev.capybara.sdk

import org.junit.Assert.assertThrows
import org.junit.Test

class IntegrityEvaluatorTest {
    @Test fun requiresScannedObjectLabelToMatchSelectedReference() {
        EvidenceEngine.requireReferenceObjectLabel("case-1", "case-1")
        assertThrows(IllegalArgumentException::class.java) {
            EvidenceEngine.requireReferenceObjectLabel("case-1", "case-2")
        }
    }
}

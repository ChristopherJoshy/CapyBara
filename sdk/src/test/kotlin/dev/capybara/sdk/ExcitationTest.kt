package dev.capybara.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExcitationTest {
    @Test fun chirpHasBoundedAmplitudeFadesAndIncreasingFrequency() {
        val rate = 48_000
        val chirp = acousticChirp(rate)
        assertEquals(rate, chirp.size)
        assertEquals(0, chirp.first().toInt())
        assertTrue(abs(chirp.last().toInt()) < 10)
        assertTrue(chirp.maxOf { abs(it.toInt()) } <= (Short.MAX_VALUE * 0.12).toInt())
        fun risingCrossings(start: Int, end: Int) = (start + 1 until end).count {
            chirp[it - 1] <= 0 && chirp[it] > 0
        }
        // 100 ms centered at 100/900 ms: approximately 107/723 cycles.
        assertTrue(risingCrossings(2_400, 7_200) in 105..109)
        assertTrue(risingCrossings(40_800, 45_600) in 721..725)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsRateBelowExcitationNyquist() { acousticChirp(16_000) }
}

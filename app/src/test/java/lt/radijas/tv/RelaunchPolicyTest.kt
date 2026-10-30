package lt.radijas.tv

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-function test, no Android: [RelaunchPolicy] exists so that "should this relaunch restart
 * the stream?" is decided once and can be asserted, instead of living inside an activity callback
 * that only a real TV can exercise.
 */
class RelaunchPolicyTest {

    @Test
    fun `a stopped player restarts the last station`() {
        assertTrue(RelaunchPolicy.shouldRestart("lietus", "lietus", playWhenReady = false))
    }

    @Test
    fun `a stream that is still running is not restarted`() {
        assertFalse(RelaunchPolicy.shouldRestart("lietus", "lietus", playWhenReady = true))
    }

    @Test
    fun `a different station always starts`() {
        assertTrue(RelaunchPolicy.shouldRestart("m1", "lietus", playWhenReady = true))
    }

    @Test
    fun `no station yet always starts`() {
        assertTrue(RelaunchPolicy.shouldRestart("lietus", null, playWhenReady = false))
    }
}

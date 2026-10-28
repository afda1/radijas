package lt.radijas.tv

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopPolicyTest {

    @Test
    fun `leaving via home stops playback`() = assertTrue(StopPolicy.shouldStop(userLeftHint = true))

    @Test
    fun `screensaver does not stop playback`() = assertFalse(StopPolicy.shouldStop(userLeftHint = false))
}

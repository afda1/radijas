package lt.radijas.tv

import android.view.KeyEvent
import lt.radijas.tv.ScreenOffAction.CONSUME
import lt.radijas.tv.ScreenOffAction.CONSUME_AND_WAKE
import lt.radijas.tv.ScreenOffAction.PASS
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The remote key rules for the black screen. The dangerous case is the wake press: if the screen
 * were restored on DOWN, the matching UP would reach the station tile and start a station the
 * user only wanted to wake the TV with.
 */
class ScreenOffPolicyTest {
    @Test
    fun `while the screen is off every key is consumed and the press wakes on release`() {
        assertEquals(CONSUME, screenOffAction(KeyEvent.ACTION_DOWN, screenOff = true))
        assertEquals(CONSUME_AND_WAKE, screenOffAction(KeyEvent.ACTION_UP, screenOff = true))
    }

    @Test
    fun `while the screen is on every key goes to the app`() {
        assertEquals(PASS, screenOffAction(KeyEvent.ACTION_DOWN, screenOff = false))
        assertEquals(PASS, screenOffAction(KeyEvent.ACTION_UP, screenOff = false))
    }
}

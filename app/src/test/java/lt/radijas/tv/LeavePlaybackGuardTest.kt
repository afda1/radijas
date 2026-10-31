package lt.radijas.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class LeavePlaybackGuardTest {
    @Test fun homeStopsAfterGracePeriod() {
        val guard = LeavePlaybackGuard()
        val token = guard.onUserLeave()
        assertEquals(true, guard.shouldStop(token))
    }

    @Test fun screensaverCancelsPendingLeaveEvenWhenSystemReportsUserLeave() {
        val guard = LeavePlaybackGuard()
        val token = guard.onUserLeave()
        guard.onDreamStarted()
        assertEquals(false, guard.shouldStop(token))
    }

    @Test fun returningToRadioCancelsStaleStop() {
        val guard = LeavePlaybackGuard()
        val token = guard.onUserLeave()
        guard.onResumed()
        assertEquals(false, guard.shouldStop(token))
    }

    @Test fun dreamBroadcastCanArriveBeforeUserLeaveCallback() {
        val guard = LeavePlaybackGuard()
        guard.onDreamStarted()
        val token = guard.onUserLeave()
        assertEquals(false, guard.shouldStop(token))
    }

    @Test fun laterHomeStillStopsAfterScreensaverEnds() {
        val guard = LeavePlaybackGuard()
        val previous = guard.onUserLeave()
        guard.onDreamStarted()
        guard.onResumed()
        val next = guard.onUserLeave()
        assertEquals(false, guard.shouldStop(previous))
        assertEquals(true, guard.shouldStop(next))
    }
}

package lt.radijas.tv

/** Invalidates a pending user-leave stop when Android starts a screensaver or returns to radio. */
internal class LeavePlaybackGuard {
    private var generation = 0
    private var dreaming = false
    fun onUserLeave(): Int = ++generation
    fun onDreamStarted() { dreaming = true; generation++ }
    fun onResumed() { dreaming = false; generation++ }
    fun shouldStop(token: Int): Boolean = !dreaming && token == generation
}

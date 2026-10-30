package lt.radijas.tv

/**
 * Whether coming back to an already running app has to restart the stream.
 *
 * Launching from the TV launcher while the activity is still alive arrives in
 * [MainActivity.onNewIntent] (`launchMode="singleTop"`). Usually Home has already stopped playback
 * through `onUserLeaveHint`, so the last station has to be started again. But playback can also
 * survive being backgrounded - some launchers and screensaver flows never call `onUserLeaveHint` -
 * and restarting a stream that is still audible rebuffers it and blanks the „kas groja" line for
 * no reason. Only a player that is no longer trying to play needs the restart.
 */
object RelaunchPolicy {
    fun shouldRestart(
        requestedStationId: String,
        currentStationId: String?,
        playWhenReady: Boolean
    ): Boolean = currentStationId != requestedStationId || !playWhenReady
}

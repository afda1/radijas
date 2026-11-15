package lt.radijas.tv

import android.view.KeyEvent

/** What happens to one remote key event while the black screen may be up. */
internal enum class ScreenOffAction {
    /** Hand the event to the app. */
    PASS,

    /** Swallow it; the screen stays off. */
    CONSUME,

    /** Swallow it and turn the screen back on. */
    CONSUME_AND_WAKE
}

/**
 * Every non-volume key is swallowed while the screen is off, and the wake happens on release:
 * waking on DOWN would let the matching UP reach a station tile and start a station the user
 * never asked for. Hardware volume and mute keys always pass through so the user can adjust
 * volume in the dark without waking the screen.
 */
internal fun screenOffAction(
    action: Int,
    screenOff: Boolean,
    keyCode: Int = KeyEvent.KEYCODE_UNKNOWN
): ScreenOffAction = when {
    !screenOff -> ScreenOffAction.PASS
    keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
        keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
        keyCode == KeyEvent.KEYCODE_VOLUME_MUTE -> ScreenOffAction.PASS
    action == KeyEvent.ACTION_UP -> ScreenOffAction.CONSUME_AND_WAKE
    else -> ScreenOffAction.CONSUME
}

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
 * Every key is swallowed while the screen is off, and the wake happens on release: waking on DOWN
 * would let the matching UP reach a station tile and start a station the user never asked for.
 */
internal fun screenOffAction(action: Int, screenOff: Boolean): ScreenOffAction = when {
    !screenOff -> ScreenOffAction.PASS
    action == KeyEvent.ACTION_UP -> ScreenOffAction.CONSUME_AND_WAKE
    else -> ScreenOffAction.CONSUME
}

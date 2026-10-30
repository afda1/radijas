package lt.radijas.tv.ui

/**
 * Bridge between the screen-off control in the hero and the station shelf.
 *
 * The shelf remembers which tile the D-pad left and owns the way back, because returning to an
 * off-screen tile needs a scroll before the tile can take focus. Without this, Compose's geometric
 * focus search landed on whatever tile sat under the control's x position — a different station.
 */
class ShelfFocusState {
    internal var requestFocusOnLastStation: (() -> Unit)? = null

    /** Puts focus back on the station the user left; no-op before the shelf is laid out. */
    fun focusLastFocusedStation() {
        requestFocusOnLastStation?.invoke()
    }
}

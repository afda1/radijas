package lt.radijas.tv

import lt.radijas.tv.ui.PlayerStatus

/** What pressing OK on a station tile does. */
internal enum class StationAction {
    /** Start the station on the tile. */
    SELECT,

    /** Reopen the current station from scratch, immediately. */
    RETRY,

    /** The press must not touch the player at all. */
    IGNORE
}

/**
 * Decides what OK on [selectedId] does while [currentId] is the station on screen.
 *
 * Pure so the rule can be tested without a player: this is the one press an elderly user makes by
 * accident, and it is also the only press that must be safe.
 *
 * The important case is [StationAction.IGNORE]: OK on the station that is already playing does
 * nothing (design spec §6.3). Treating it as a selection used to reopen the stream - a 1-3 s
 * silence and a blanked „kas groja" line for a press that was not asking for anything.
 *
 * Only the failing tile retries (spec 6.2); any other tile still switches, even mid-outage, so a
 * dead station never traps the user.
 */
internal fun stationAction(
    selectedId: String,
    currentId: String?,
    status: PlayerStatus
): StationAction = when {
    selectedId != currentId -> StationAction.SELECT
    status == PlayerStatus.PLAYING -> StationAction.IGNORE
    else -> StationAction.RETRY
}

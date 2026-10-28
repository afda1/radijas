package lt.radijas.tv.ui

import androidx.compose.runtime.Immutable
import lt.radijas.tv.data.model.RadioStation

/**
 * Everything the now-playing screen needs in one value.
 *
 * [trackTitle] is carried from the stream metadata; it stays null until metadata is plumbed
 * through, and the hero simply omits the line.
 */
@Immutable
data class NowPlayingUiState(
    val station: RadioStation,
    val status: PlayerStatus,
    val trackTitle: String? = null
)

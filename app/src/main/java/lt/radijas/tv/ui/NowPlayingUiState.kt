package lt.radijas.tv.ui

import androidx.compose.runtime.Immutable
import lt.radijas.tv.data.model.RadioStation

/**
 * Everything the now-playing screen needs in one value.
 *
 * [trackTitle] is the station's own ICY `StreamTitle`, already trimmed and validated by
 * `parseTrackTitle`; null means the hero shows its empty fixed-height line.
 */
@Immutable
data class NowPlayingUiState(
    val station: RadioStation,
    val status: PlayerStatus,
    val trackTitle: String? = null
)

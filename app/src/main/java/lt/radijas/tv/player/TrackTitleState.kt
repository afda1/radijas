package lt.radijas.tv.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Same-process handover of the station's ICY `StreamTitle` from the playback service to the UI.
 *
 * Media3 1.11.0 does not forward `Player.Listener.onMetadata` over a `MediaSession`: the
 * session's own player listener has no `onMetadata`, and the controller callback interface has no
 * metadata callback, so the decoded `IcyInfo` cannot reach [RadioPlayerManager]'s
 * `MediaController`. The service and every controller live in this app's single process, so the
 * title is published here instead. `MediaItem.mediaMetadata` is deliberately not used: the title
 * is transient UI state, not part of the station item.
 */
object TrackTitleState {
    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title.asStateFlow()

    /** Accepts a raw `IcyInfo.title`; the same trim/blank/length rules apply as in the parser. */
    fun update(rawIcyTitle: String?) {
        val parsed = parseTrackTitle(rawIcyTitle)
        if (_title.value != parsed) {
            _title.value = parsed
        }
    }

    fun clear() {
        _title.value = null
    }
}

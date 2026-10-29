package lt.radijas.tv

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * A real, minimal `Player` for tests: Media3's [SimpleBasePlayer], no renderers, no network.
 *
 * Tests that need a `MediaSession` must not mock it - the session is part of what is under test
 * (its player supplies the notification's station name, and its connection result defines the
 * controller command set). One ready media item is enough for both.
 */
internal class FakeRadioPlayer(
    looper: Looper,
    private val item: MediaItem? = null
) : SimpleBasePlayer(looper) {

    override fun getState(): State {
        val builder = State.Builder()
            .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
            .setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(if (item == null) Player.STATE_IDLE else Player.STATE_READY)
        if (item != null) {
            builder.setPlaylist(listOf(MediaItemData.Builder(0).setMediaItem(item).build()))
        }
        return builder.build()
    }

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()
}

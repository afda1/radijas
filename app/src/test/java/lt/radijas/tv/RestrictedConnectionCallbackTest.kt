package lt.radijas.tv

import android.content.Context
import android.os.Bundle
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.test.core.app.ApplicationProvider
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import lt.radijas.tv.player.RestrictedConnectionCallback
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Exercises the `MediaSession.Callback.onConnect` implementation the service actually installs
 * ([RestrictedConnectionCallback] wraps the service's library callback): the connection result is
 * what removes play/pause from Assistant, the system media controls and other apps.
 *
 * [SessionCommandPolicyTest] only covers the command set itself, so this test is what fails if the
 * restriction branch disappears from the callback or the package/notification-controller split
 * changes.
 */
@RunWith(RobolectricTestRunner::class)
class RestrictedConnectionCallbackTest {

    private lateinit var player: FakeRadioPlayer
    private lateinit var session: MediaSession
    private val delegate = RecordingLibraryCallback()
    private val callback = RestrictedConnectionCallback("lt.radijas.tv", delegate)

    @Before
    fun setUp() {
        player = FakeRadioPlayer(Looper.getMainLooper())
        session = MediaSession.Builder(
            ApplicationProvider.getApplicationContext<Context>(),
            player
        ).build()
    }

    @After
    fun tearDown() {
        session.release()
        player.release()
    }

    @Test
    fun `our own media controller keeps every player command`() {
        val commands = connect(controllerInfo("lt.radijas.tv")).availablePlayerCommands

        assertTrue(commands.contains(Player.COMMAND_PLAY_PAUSE))
        assertTrue(commands.contains(Player.COMMAND_PREPARE))
        assertTrue(commands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        assertTrue(commands.contains(Player.COMMAND_STOP))
    }

    @Test
    fun `another app cannot play pause prepare seek or replace the item`() {
        val commands = connect(controllerInfo("com.google.android.googlequicksearchbox"))
            .availablePlayerCommands

        assertFalse(commands.contains(Player.COMMAND_PLAY_PAUSE))
        assertFalse(commands.contains(Player.COMMAND_PREPARE))
        assertFalse(commands.contains(Player.COMMAND_STOP))
        assertFalse(commands.contains(Player.COMMAND_SEEK_TO_DEFAULT_POSITION))
        assertFalse(commands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
        assertFalse(commands.contains(Player.COMMAND_SEEK_TO_PREVIOUS))
        assertFalse(commands.contains(Player.COMMAND_SEEK_TO_NEXT))
        assertFalse(commands.contains(Player.COMMAND_SEEK_BACK))
        assertFalse(commands.contains(Player.COMMAND_SEEK_FORWARD))
        assertFalse(commands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        assertFalse(commands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS))
        // Still enough to describe what is playing.
        assertTrue(commands.contains(Player.COMMAND_GET_CURRENT_MEDIA_ITEM))
        assertTrue(commands.contains(Player.COMMAND_GET_TIMELINE))
        assertTrue(commands.contains(Player.COMMAND_GET_METADATA))
    }

    @Test
    fun `the notification controller is restricted even though it is our own package`() {
        val commands = connect(controllerInfo("lt.radijas.tv", notificationController = true))
            .availablePlayerCommands

        assertFalse(commands.contains(Player.COMMAND_PLAY_PAUSE))
        assertFalse(commands.contains(Player.COMMAND_PREPARE))
        assertFalse(commands.contains(Player.COMMAND_SET_MEDIA_ITEM))
    }

    @Test
    fun `library callbacks still reach the service callback`() {
        val items = mutableListOf(MediaItem.fromUri("https://example.com/stream"))

        callback.onAddMediaItems(session, controllerInfo("lt.radijas.tv"), items).get()

        // Kotlin's `by delegate` silently drops Java default methods, so this is the guard that the
        // wrapper really forwards the library behaviour instead of Media3's empty defaults.
        assertNotNull("onAddMediaItems was not forwarded to the delegate", delegate.addedItems)
        assertEquals(items, delegate.addedItems)
    }

    private fun connect(info: MediaSession.ControllerInfo): MediaSession.ConnectionResult =
        callback.onConnect(session, info)

    private class RecordingLibraryCallback : MediaLibraryService.MediaLibrarySession.Callback {
        var addedItems: List<MediaItem>? = null

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            addedItems = mediaItems
            return Futures.immediateFuture(mediaItems)
        }
    }

    private fun controllerInfo(
        packageName: String,
        notificationController: Boolean = false
    ): MediaSession.ControllerInfo {
        val hints = Bundle().apply {
            if (notificationController) {
                putBoolean("androidx.media3.session.MediaNotificationManager", true)
            }
        }
        return MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            packageName,
            0,
            1000,
            1,
            1,
            true,
            hints,
            true
        )
    }
}

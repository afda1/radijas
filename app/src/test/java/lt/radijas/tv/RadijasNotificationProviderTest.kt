package lt.radijas.tv

import android.app.Notification
import android.content.Context
import android.os.Looper
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.test.core.app.ApplicationProvider
import com.google.common.collect.ImmutableList
import lt.radijas.tv.player.RadijasPlaybackService
import lt.radijas.tv.player.RadijasNotificationProvider
import lt.radijas.tv.player.TrackTitleState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Guards the design's only outside-the-app surface: a notification with the station, the song and
 * exactly one action, „Stabdyti". The [MediaNotification.ActionFactory] here throws if the
 * provider ever asks Media3 to build a transport action, so adding play/pause or seek back is a
 * test failure, not a silently shipped regression.
 */
@RunWith(RobolectricTestRunner::class)
class RadijasNotificationProviderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var player: FakeRadioPlayer
    private lateinit var session: MediaSession

    @Before
    fun setUp() {
        TrackTitleState.clear()
        player = FakeRadioPlayer(
            Looper.getMainLooper(),
            MediaItem.Builder()
                .setMediaId("lietus")
                .setMediaMetadata(MediaMetadata.Builder().setTitle("Lietus").build())
                .build()
        )
        session = MediaSession.Builder(context, player).build()
    }

    @After
    fun tearDown() {
        session.release()
        player.release()
        TrackTitleState.clear()
    }

    @Test
    fun `the only action is Stabdyti and it targets the playback service`() {
        val actions = createNotification().actions

        assertEquals("exactly one action", 1, actions.size)
        assertEquals("Stabdyti", actions[0].title.toString())
        assertTrue(
            "the action must start the service, not the activity",
            shadowOf(actions[0].actionIntent).isService
        )
        val started = shadowOf(actions[0].actionIntent).savedIntent
        assertEquals(RadijasPlaybackService::class.java.name, started.component?.className)
        assertEquals(RadijasPlaybackService.ACTION_STOP, started.action)
    }

    @Test
    fun `no Media3 transport action is attached`() {
        val actions = createNotification().actions

        // A Media3 media action is a broadcast carrying a KeyEvent (or a session action intent).
        actions.forEach { action ->
            assertTrue(
                "action ${action.title} is not a plain service intent",
                shadowOf(action.actionIntent).isService
            )
            val actionIntent = shadowOf(action.actionIntent).savedIntent
            val media3Action = actionIntent.action.orEmpty()
            assertTrue(
                "unexpected Media3 action $media3Action",
                !media3Action.startsWith("androidx.media3.session.action") &&
                    media3Action != "android.intent.action.MEDIA_BUTTON"
            )
        }
        // ...and the factory that would create one was never called (it throws).
        assertEquals(1, actions.size)
    }

    @Test
    fun `tapping the notification opens the app without resuming the station`() {
        val open = shadowOf(createNotification().contentIntent).savedIntent

        assertEquals(MainActivity::class.java.name, open.component?.className)
        assertTrue(
            "the activity must be able to tell a notification tap from a launcher start",
            open.getBooleanExtra(MainActivity.EXTRA_FROM_NOTIFICATION, false)
        )
    }

    @Test
    fun `content shows the station and the live track title`() {
        TrackTitleState.update("ATL - X")

        val extras = createNotification().extras
        assertEquals("Lietus", extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
        assertEquals("ATL - X", extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
    }

    @Test
    fun `content has no song line when the station has not sent one`() {
        val extras = createNotification().extras

        assertNull(extras.getCharSequence(Notification.EXTRA_TEXT))
    }

    private fun createNotification(): Notification =
        RadijasNotificationProvider(context)
            .createNotification(session, ImmutableList.of(), noMediaActions(), noopCallback)
            .notification

    /**
     * Media3's own way of attaching play/pause/seek. Throwing keeps that path physically
     * unreachable from the provider under test.
     */
    private fun noMediaActions(): MediaNotification.ActionFactory =
        object : MediaNotification.ActionFactory {
            override fun createMediaAction(
                session: MediaSession,
                icon: IconCompat,
                title: CharSequence,
                command: Int
            ): androidx.core.app.NotificationCompat.Action =
                throw AssertionError("provider must not create a media action")

            override fun createCustomAction(
                session: MediaSession,
                icon: IconCompat,
                title: CharSequence,
                customAction: String,
                extras: android.os.Bundle
            ): androidx.core.app.NotificationCompat.Action =
                throw AssertionError("provider must not create a custom action")

            override fun createCustomActionFromCustomCommandButton(
                session: MediaSession,
                customCommandButton: androidx.media3.session.CommandButton
            ): androidx.core.app.NotificationCompat.Action =
                throw AssertionError("provider must not create a command-button action")

            override fun createMediaActionPendingIntent(
                session: MediaSession,
                command: Int
            ): android.app.PendingIntent =
                throw AssertionError("provider must not create a media action intent")
        }

    private val noopCallback = object : MediaNotification.Provider.Callback {
        override fun onNotificationChanged(notification: MediaNotification) = Unit
    }
}

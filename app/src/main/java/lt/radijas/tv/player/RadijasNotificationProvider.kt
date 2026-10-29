package lt.radijas.tv.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.google.common.collect.ImmutableList
import lt.radijas.tv.MainActivity
import lt.radijas.tv.R

/**
 * The only media surface outside the app: the station name, the current song and one action,
 * „Stabdyti".
 *
 * It is built from scratch rather than derived from `DefaultMediaNotificationProvider`, because
 * that provider attaches `MediaStyle` transport actions (play/pause, seek) and there is no public
 * way to remove them from its result. Here `MediaStyle` carries no media action at all: it only
 * marks the notification as a media notification, so the system draws no transport controls from
 * it (the system media card draws from the session's `PlaybackState`, which has no play/pause
 * either).
 *
 * „Stabdyti" is a [PendingIntent] to the playback service itself, so it also works when the
 * activity is gone: the service stops the player, cancels its reconnect loop and stops itself.
 * Nothing is routed back through a `MediaController`, and the action does not depend on the
 * session being connected.
 */
@OptIn(UnstableApi::class)
class RadijasNotificationProvider(private val context: Context) : MediaNotification.Provider {

    init {
        // Media3 1.11 only creates a channel for its own default provider and its shutdown
        // notification; a custom provider owns its channel. IMPORTANCE_LOW keeps the channel quiet
        // (no sound, no heads-up) and works on Android TV without any permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
                manager?.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply { setShowBadge(false) }
                )
            }
        }
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val stationName = mediaSession.player.mediaMetadata.title?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.app_name)
        // The service and the notification live in this process; the ICY title never travels
        // through the MediaSession (see TrackTitleState).
        val trackTitle = TrackTitleState.title.value

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_radio)
            .setContentTitle(stationName)
            .setContentIntent(openAppIntent())
            .addAction(
                R.drawable.ic_notification_stop,
                context.getString(R.string.notification_stop),
                stopIntent()
            )
            // MediaStyle *without* any transport additions: no media action is registered and
            // `setShowActionsInCompactView` is never called, so the only action stays „Stabdyti".
            // It is needed because the system only treats such a notification as a media
            // notification and exempts it from the POST_NOTIFICATIONS permission, which this app
            // deliberately does not request. The system media card draws its buttons from the
            // session's PlaybackState, which carries no play/pause either.
            .setStyle(MediaStyleNotificationHelper.MediaStyle(mediaSession))
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (!trackTitle.isNullOrBlank()) {
            builder.setContentText(trackTitle)
        }

        return MediaNotification(NOTIFICATION_ID, builder.build())
    }

    /** This provider has no custom actions; the only button is a plain service intent. */
    override fun handleCustomCommand(
        mediaSession: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean = false

    /**
     * Still reported so Media3's own shutdown notification uses the same channel. The channel
     * itself is created in [init].
     */
    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
        MediaNotification.Provider.NotificationChannelInfo(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name)
        )

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN_APP,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_FROM_NOTIFICATION, true),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun stopIntent(): PendingIntent = PendingIntent.getService(
        context,
        REQUEST_STOP,
        Intent(context, RadijasPlaybackService::class.java)
            .setAction(RadijasPlaybackService.ACTION_STOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private companion object {
        const val CHANNEL_ID = "radijas_playback"
        const val NOTIFICATION_ID = 1001
        const val REQUEST_OPEN_APP = 1
        const val REQUEST_STOP = 2
    }
}

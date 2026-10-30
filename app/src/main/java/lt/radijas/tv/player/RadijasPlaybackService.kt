package lt.radijas.tv.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import lt.radijas.tv.BuildConfig
import lt.radijas.tv.MainActivity
import lt.radijas.tv.R
import lt.radijas.tv.data.repository.RadioRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class RadijasPlaybackService : MediaLibraryService() {
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var repository: RadioRepository
    private lateinit var player: ExoPlayer

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val retryDelaysMs = longArrayOf(1_000, 2_000, 4_000, 8_000, 15_000, 30_000)
    private var retryAttempt = 0
    private var retryJob: Job? = null
    private val powerMetadata = PowerMetadataClient()
    private var metadataJob: Job? = null
    private var metadataStationId: String? = null

    // Elapsed-realtime timestamp of the last pause, used to jump back to the live edge on resume.
    private var pausedAtMs = 0L

    private var lastRequestedItem: MediaItem? = null

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val httpDataSourceFactory: DefaultHttpDataSource.Factory by lazy {
        DefaultHttpDataSource.Factory()
            .setUserAgent("Radijas/${BuildConfig.VERSION_NAME} (Android; Icecast/Shoutcast Client)")
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)
            .setAllowCrossProtocolRedirects(true)
    }

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(5_000, 15_000, 500, 1_000)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    // Library/browse behaviour only; the connection-level command policy is applied by
    // RestrictedConnectionCallback, which wraps this callback (see SessionCommandPolicy.kt).
    // Every override added here must also be forwarded there - Kotlin's interface delegation does
    // not forward Java default methods, so an un-forwarded override silently falls back to empty.
    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(R.string.app_name))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            if (parentId != ROOT_ID) {
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }
            val items = repository.stations.map { it.toMediaItem(this@RadijasPlaybackService) }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val station = repository.stations.find { it.id == mediaId }
                ?: return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            return Futures.immediateFuture(LibraryResult.ofItem(station.toMediaItem(this@RadijasPlaybackService), null))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                if (item.localConfiguration != null) {
                    item
                } else {
                    repository.stations.find { it.id == item.mediaId }
                        ?.toMediaItem(this@RadijasPlaybackService)
                        ?: item
                }
            }.toMutableList()

            resolved.firstOrNull()?.let { item ->
                Log.d(TAG, "MediaItem resolved: mediaId=${item.mediaId} uri=${item.localConfiguration?.uri}")
                lastRequestedItem = item
                retryJob?.cancel()
                retryAttempt = 0
            }

            return Futures.immediateFuture(resolved)
        }
    }

    private val resiliencePlayerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncPowerMetadata()
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateName = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN($playbackState)"
            }
            Log.d(TAG, "Player state changed: $stateName (playWhenReady=${player.playWhenReady})")

            // A live stream never really ends: STATE_ENDED means the server dropped the connection.
            if (playbackState == Player.STATE_ENDED && player.playWhenReady) {
                scheduleReconnect()
            }

            if (playbackState == Player.STATE_READY && player.playWhenReady) {
                if (retryAttempt != 0) Log.d(TAG, "Reconnect succeeded - resetting backoff")
                retryAttempt = 0
                retryJob?.cancel()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, if (isPlaying) "Playing" else "Not playing (paused/buffering/stopped)")
            if (!isPlaying) AudioLevelState.clear()
        }

        /**
         * ICY is decoded by the default `MetadataRenderer`; `IcyInfo.title` is the raw
         * `StreamTitle`. The title is published for the hero here because a `MediaSession`
         * never forwards metadata events to controllers (see [TrackTitleState]).
         */
        override fun onMetadata(metadata: Metadata) {
            // These stations publish their current song via their website, not useful ICY data.
            if (player.currentMediaItem?.mediaId in setOf("powerhit", "powerhitgold")) return
            val icy = metadata.getFirstEntryOfType(IcyInfo::class.java) ?: return
            Log.d(TAG, "ICY metadata: title=${icy.title}")
            val previous = TrackTitleState.title.value
            TrackTitleState.update(icy.title)
            if (TrackTitleState.title.value != previous) {
                // Media3 never forwards metadata to controllers, so it also never updates the
                // notification on its own. Nudge it only when the visible text actually changed.
                try {
                    triggerNotificationUpdate()
                } catch (e: Exception) {
                    Log.w(TAG, "Notification title update failed", e)
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Every new stream starts with no song, whichever controller changed the item:
            // carrying the previous title over would show a wrong song while it connects.
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                TrackTitleState.clear()
                AudioLevelState.clear()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) {
                if (retryJob?.isActive == true) {
                    Log.d(TAG, "Reconnect cancelled - playWhenReady=false (reason=$reason)")
                }
                retryJob?.cancel()
                pausedAtMs = SystemClock.elapsedRealtime()
                return
            }
            val pausedFor = SystemClock.elapsedRealtime() - pausedAtMs
            val resumingOldBuffer = pausedAtMs != 0L && pausedFor > LIVE_RESYNC_AFTER_MS &&
                player.playbackState == Player.STATE_READY
            pausedAtMs = 0L
            if (resumingOldBuffer) {
                // After a long pause the buffer is stale and the server has usually dropped the
                // connection, so reopen the stream at the live edge instead of replaying old audio.
                Log.d(TAG, "Resumed after ${pausedFor}ms - reloading live stream")
                // Posted (not immediate) so the player is not re-entered from its own callback.
                serviceScope.launch(Dispatchers.Main) { restartStream() }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(
                TAG,
                "Playback error: errorCode=${error.errorCode} errorCodeName=${error.errorCodeName} " +
                    "message=${error.message} cause=${error.cause} " +
                    "mediaItem=${lastRequestedItem?.localConfiguration?.uri}",
                error
            )
            scheduleReconnect()
        }
    }

    private fun syncPowerMetadata() {
        val stationId = player.currentMediaItem?.mediaId
            ?.takeIf { player.isPlaying && it in setOf("powerhit", "powerhitgold") }
        if (stationId == metadataStationId && (stationId == null || metadataJob?.isActive == true)) return
        metadataJob?.cancel()
        metadataStationId = stationId
        metadataJob = null
        if (stationId == null) return
        metadataJob = serviceScope.launch {
            while (true) {
                val title = powerMetadata.powerTitle(stationId)
                if (player.currentMediaItem?.mediaId == stationId && player.isPlaying) {
                    TrackTitleState.update(title)
                    try {
                        triggerNotificationUpdate()
                    } catch (e: Exception) {
                        Log.w(TAG, "Notification update failed", e)
                    }
                }
                delay(20_000L)
            }
        }
    }

    private fun scheduleReconnect(immediate: Boolean = false) {
        retryJob?.cancel()
        val delayMs = if (immediate) 0L else retryDelaysMs.getOrElse(retryAttempt) { retryDelaysMs.last() }
        retryAttempt++
        Log.d(TAG, "Reconnect scheduled in ${delayMs}ms (attempt=$retryAttempt)")
        retryJob = serviceScope.launch {
            delay(delayMs)
            if (!player.playWhenReady) {
                Log.d(TAG, "Reconnect skipped - no longer playWhenReady")
                return@launch
            }
            restartStream()
        }
    }

    private fun restartStream() {
        val item = player.currentMediaItem ?: lastRequestedItem
        if (item == null) {
            Log.d(TAG, "Restart skipped - nothing to play")
            return
        }
        Log.d(TAG, "Restarting stream: uri=${item.localConfiguration?.uri}")
        try {
            player.stop()
            player.setMediaItem(item)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "Restart failed", e)
        }
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        connectivityManager = cm
        val callback = object : ConnectivityManager.NetworkCallback() {
            // Called on a ConnectivityManager thread; ExoPlayer must only be accessed on the main thread.
            override fun onAvailable(network: Network) {
                serviceScope.launch {
                    if (retryJob?.isActive == true && player.playWhenReady && player.playbackState != Player.STATE_READY) {
                        Log.d(TAG, "Network available again - reconnecting immediately")
                        scheduleReconnect(immediate = true)
                    }
                }
            }
        }
        networkCallback = callback
        try {
            cm.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Must be replaced before the session is built: Media3 updates the notification from the
        // session's own events and would otherwise post its default transport-control notification.
        setMediaNotificationProvider(RadijasNotificationProvider(this))
        repository = RadioRepository(applicationContext)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val audioSessionId = audioManager?.generateAudioSessionId()?.takeIf { it != 0 }
            ?: C.AUDIO_SESSION_ID_UNSET

        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(TeeAudioProcessor(LevelAudioBufferSink())))
                .build()
        }
        player = ExoPlayer.Builder(this, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(httpDataSourceFactory))
            .setLoadControl(loadControl)
            .build()
            .apply {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    setAudioSessionId(audioSessionId)
                }

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                setHandleAudioBecomingNoisy(true)

                // Keeps CPU and Wi-Fi awake while streaming; without it playback stops with the screen off.
                setWakeMode(C.WAKE_MODE_NETWORK)
                addListener(resiliencePlayerListener)
            }

        registerNetworkCallback()

        // Tapping the media notification brings the app back to the front.
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Built here, not as a field: the callback needs `packageName`, and property initializers
        // run in the service constructor before the context is attached.
        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            player,
            RestrictedConnectionCallback(packageName, libraryCallback)
        )
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    /**
     * „Stabdyti" in the notification targets the service directly (see
     * [RadijasNotificationProvider]), so it keeps working even when the activity is gone. This
     * path is deliberately independent of the session: no controller has to be connected.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Log.d(TAG, "Stop requested from notification")
            stopPlayback()
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /** Ends playback and every pending reconnect; the service is stopped by the caller. */
    private fun stopPlayback() {
        retryJob?.cancel()
        retryJob = null
        retryAttempt = 0
        player.playWhenReady = false
        player.stop()
        // Nothing stays queued: with an empty timeline Media3 also retires the notification
        // instead of leaving an idle „playing" card behind while the app is still bound.
        player.clearMediaItems()
        lastRequestedItem = null
        TrackTitleState.clear()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            player.pause()
            stopSelf()
        }
    }

    override fun onDestroy() {
        retryJob?.cancel()
        serviceScope.cancel()
        AudioLevelState.clear()
        networkCallback?.let { cb ->
            try { connectivityManager?.unregisterNetworkCallback(cb) } catch (_: Exception) {}
        }
        player.removeListener(resiliencePlayerListener)
        player.release()
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        super.onDestroy()
    }

    companion object {
        private const val ROOT_ID = "radijas_root"
        private const val TAG = "RadijasPlaybackService"
        private const val LIVE_RESYNC_AFTER_MS = 30_000L

        /** Action of the notification's only button; carries no transport semantics. */
        const val ACTION_STOP = "lt.radijas.tv.STOP"
    }
}

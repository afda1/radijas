package lt.radijas.tv.player

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture

/**
 * Decides what a connected controller may ask the player to do.
 *
 * The design forbids a pause control anywhere: in the app, in the notification and through the
 * system (Assistant, TV media surfaces, the media button receiver). Pausing is the one state the
 * user cannot explain from the screen, so no controller outside this app may reach it.
 *
 * The restriction is applied at connection time in [RestrictedConnectionCallback.onConnect], so a
 * disallowed command is never advertised to the controller in the first place - it is not rejected
 * later, when the user has already pressed something and waits for a result.
 */
@OptIn(UnstableApi::class)
internal object SessionCommandPolicy {

    /**
     * Only this app's own `MediaController` (the one [RadioPlayerManager] drives) needs to change
     * the station and start playback. It connects from this app's package.
     *
     * The media notification's internal controller also comes from this package, but it exists
     * only to render and update the notification. It is deliberately *not* given write commands:
     * Media3 copies that controller's command set into the platform `MediaSession` that the system
     * media controls, Assistant and legacy media buttons talk to, so restricting it is what removes
     * the play/pause action from those surfaces as well (Media3 1.11.0
     * `MediaSessionImpl.onConnectOnHandler` /
     * `MediaSessionLegacyStub.getPlatformConnectionResult`).
     */
    fun allowsFullControl(
        controllerPackage: String,
        appPackage: String,
        isNotificationController: Boolean
    ): Boolean = controllerPackage == appPackage && !isNotificationController

    /**
     * Everything a controller needs to *describe* what is playing and nothing that changes it:
     * `COMMAND_GET_*` only. That keeps the notification, the system media card and Assistant able
     * to read the station, while `COMMAND_PLAY_PAUSE`, `COMMAND_PREPARE`, every `COMMAND_SEEK_*`,
     * `COMMAND_SET_MEDIA_ITEM` and `COMMAND_CHANGE_MEDIA_ITEMS` are absent.
     */
    fun externalControllerCommands(): Player.Commands =
        Player.Commands.Builder()
            .addAllReadOnlyCommands()
            .build()
}

/**
 * The callback the service installs: it forwards the library/browse behaviour untouched and
 * answers every controller connection with the command set from [SessionCommandPolicy].
 *
 * It is a real class rather than a private override inside the service so a test can hand it the
 * same `ControllerInfo` Media3 hands a real controller and assert the commands it would receive.
 * `MediaSession.isMediaNotificationController` singles out the notification's own internal
 * controller, which connects from this package but only renders the notification.
 *
 * The platform session only receives this command set when the notification controller connects, a
 * moment after the service starts. A legacy play in that brief window can start nothing: the
 * service starts with no media item, and an `ACTION_PLAY` / media button cannot set one (`play()`
 * on an empty timeline is a no-op), so there is nothing to un-pause and no pause state to fall into.
 */
@OptIn(UnstableApi::class)
internal class RestrictedConnectionCallback(
    private val appPackage: String,
    private val delegate: MediaLibrarySession.Callback
) : MediaLibrarySession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val isNotificationController = session.isMediaNotificationController(controller)
        val fullControl = SessionCommandPolicy.allowsFullControl(
            controllerPackage = controller.packageName,
            appPackage = appPackage,
            isNotificationController = isNotificationController
        )
        if (fullControl) {
            // Session + library commands and every player command: our own MediaController still
            // has to prepare, play, set items and stop.
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                )
                .setAvailablePlayerCommands(Player.Commands.Builder().addAllCommands().build())
                .build()
        }
        Log.d(
            TAG,
            "Restricted controller ${controller.packageName} (notification=$isNotificationController)"
        )
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            .setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS)
            .setAvailablePlayerCommands(SessionCommandPolicy.externalControllerCommands())
            .build()
    }

    // Kotlin's `MediaLibrarySession.Callback by delegate` does not forward Java default methods -
    // the generated class only overrides what is written here, and every library callback silently
    // fell back to the (empty) interface defaults. So the methods the service's callback overrides
    // are forwarded by hand; when one is added there, it has to be added here too.

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> =
        delegate.onGetLibraryRoot(session, browser, params)

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        delegate.onGetChildren(session, browser, parentId, page, pageSize, params)

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> =
        delegate.onGetItem(session, browser, mediaId)

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> =
        delegate.onAddMediaItems(mediaSession, controller, mediaItems)

    private companion object {
        const val TAG = "RestrictedConnectionCallback"
    }
}

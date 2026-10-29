package lt.radijas.tv.player

import androidx.media3.common.Player

/**
 * Decides what a connected controller may ask the player to do.
 *
 * The design forbids a pause control anywhere: in the app, in the notification and through the
 * system (Assistant, TV media surfaces, the media button receiver). Pausing is the one state the
 * user cannot explain from the screen, so no controller outside this app may reach it.
 *
 * The restriction is applied at connection time in
 * [FogwavePlaybackService.librarySessionCallback]'s `onConnect`, so a disallowed command is never
 * advertised to the controller in the first place - it is not rejected later, when the user has
 * already pressed something and waits for a result.
 */
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

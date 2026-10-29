package lt.radijas.tv

import androidx.media3.common.Player
import lt.radijas.tv.player.SessionCommandPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The design's hard guarantee: nothing outside the app can pause. These assertions are on the
 * command set actually handed to a controller at connection time, not on a UI detail.
 *
 * Robolectric is needed because Media3's `FlagSet` is backed by `android.util.SparseBooleanArray`.
 */
@RunWith(RobolectricTestRunner::class)
class SessionCommandPolicyTest {

    private val external = SessionCommandPolicy.externalControllerCommands()

    @Test
    fun `external controllers cannot play pause prepare seek or replace the item`() {
        val forbidden = intArrayOf(
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_PREPARE,
            Player.COMMAND_STOP,
            Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            Player.COMMAND_SEEK_IN_CURRENT_WINDOW,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS_WINDOW,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT_WINDOW,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_WINDOW,
            Player.COMMAND_SEEK_BACK,
            Player.COMMAND_SEEK_FORWARD,
            Player.COMMAND_SET_MEDIA_ITEM,
            Player.COMMAND_CHANGE_MEDIA_ITEMS,
            Player.COMMAND_SET_SPEED_AND_PITCH,
            Player.COMMAND_SET_SHUFFLE_MODE,
            Player.COMMAND_SET_REPEAT_MODE
        )
        forbidden.forEach { command ->
            assertFalse("command $command must not reach an external controller", external.contains(command))
        }
    }

    @Test
    fun `external controllers keep only what they need to display the station`() {
        assertTrue(external.contains(Player.COMMAND_GET_CURRENT_MEDIA_ITEM))
        assertTrue(external.contains(Player.COMMAND_GET_TIMELINE))
        assertTrue(external.contains(Player.COMMAND_GET_METADATA))
    }

    @Test
    fun `the in app controller keeps full control`() {
        assertTrue(
            SessionCommandPolicy.allowsFullControl(
                controllerPackage = "lt.radijas.tv",
                appPackage = "lt.radijas.tv",
                isNotificationController = false
            )
        )
    }

    @Test
    fun `the notification controller is restricted even though it is our own package`() {
        assertFalse(
            SessionCommandPolicy.allowsFullControl(
                controllerPackage = "lt.radijas.tv",
                appPackage = "lt.radijas.tv",
                isNotificationController = true
            )
        )
    }

    @Test
    fun `assistant system ui and other apps are restricted`() {
        listOf(
            "com.google.android.googlequicksearchbox",
            "com.android.systemui",
            "com.android.shell",
            "lt.radijas.tv.other"
        ).forEach { packageName ->
            assertFalse(
                packageName,
                SessionCommandPolicy.allowsFullControl(
                    controllerPackage = packageName,
                    appPackage = "lt.radijas.tv",
                    isNotificationController = false
                )
            )
        }
    }
}

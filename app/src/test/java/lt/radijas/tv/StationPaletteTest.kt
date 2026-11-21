package lt.radijas.tv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.ui.theme.stationColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The generated palette is the only source of colour now, so this test is the guarantee that no
 * station can ship with unreadable text or an invisible focus ring. It iterates the real
 * catalogue: a station added without regenerating the palette fails here instead of on a TV.
 */
@RunWith(RobolectricTestRunner::class)
class StationPaletteTest {

    private val stations: List<lt.radijas.tv.data.model.RadioStation> =
        RadioRepository(ApplicationProvider.getApplicationContext<Context>()).stations

    @Test
    fun `every catalogue station has a generated palette`() {
        assertEquals("the catalogue must still have eleven stations", 11, stations.size)
        assertEquals(stations.map { it.id }.toSet(), stationColors.keys)
    }

    @Test
    fun `text keeps its contrast on every station`() {
        stations.forEach { station ->
            val c = stationColors.getValue(station.id)
            val pairs = listOf(
                "onSurface on surface" to (c.onSurface to c.surface),
                "track line 74% on surface" to (compose(c.onSurface, c.surface, 0.74f) to c.surface),
                "status line 84% on surface" to (compose(c.onSurface, c.surface, 0.84f) to c.surface),
                "onSurfaceVariant on surfaceVariant" to (c.onSurfaceVariant to c.surfaceVariant),
                "onPrimaryContainer on primaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
                "screen-off focused pill text" to (c.onPrimary to c.primary),
                "screen-off resting pill text" to (
                    c.onSurface to compose(c.onSurface, c.surface, 0.08f)
                    ),
                "screen-off resting pill icon" to (
                    c.primary to compose(c.onSurface, c.surface, 0.08f)
                    ),
                "equalizer primary on surface" to (c.primary to c.surface),

                // The hero glow tints the text and top-right clock/date column.
                "track line 74% over the hero glow" to (
                    compose(c.onSurface, glowOver(c.primaryContainer, c.surface), 0.74f) to
                        glowOver(c.primaryContainer, c.surface)
                    ),
                "status line over the hero glow" to (
                    compose(c.onSurface, glowOver(c.primaryContainer, c.surface), 0.84f) to
                        glowOver(c.primaryContainer, c.surface)
                    ),
                "hero date 74% over peak top-right glow" to (
                    compose(c.onSurface, peakGlow(c.primaryContainer, c.surface), 0.74f) to
                        peakGlow(c.primaryContainer, c.surface)
                    ),
                "hero clock 90% over peak top-right glow" to (
                    compose(c.onSurface, peakGlow(c.primaryContainer, c.surface), 0.90f) to
                        peakGlow(c.primaryContainer, c.surface)
                    ),
            )
            pairs.forEach { (name, pair) ->
                val ratio = contrast(pair.first, pair.second)
                assertTrue("${station.id} $name: $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun `focus ring stands out on every surface it is drawn on`() {
        stations.forEach { station ->
            val c = stationColors.getValue(station.id)
            // 6:1 against the page and against the tile container the ring surrounds.
            listOf(
                "tile container" to (c.primaryContainer to 6.0),
                "page" to (c.surface to 6.0),
            ).forEach { (name, pair) ->
                val ratio = contrast(c.border, pair.first)
                assertTrue("${station.id} border on $name: $ratio", ratio >= pair.second)
            }
        }
    }

    /** Peak glow tint under the top-right hero clock and date: 44 % of primaryContainer. */
    private fun peakGlow(primaryContainer: Long, surface: Long): Long =
        compose(primaryContainer, surface, 0.44f)

    /** Worst-case glow tint where it reaches the text column: 16 % of primaryContainer. */
    private fun glowOver(primaryContainer: Long, surface: Long): Long =
        compose(primaryContainer, surface, 0.16f)

    private fun compose(foreground: Long, background: Long, alpha: Float): Long {
        fun channel(shift: Int): Long {
            val fg = ((foreground shr shift) and 0xFF).toFloat()
            val bg = ((background shr shift) and 0xFF).toFloat()
            return (fg * alpha + bg * (1f - alpha)).toLong()
        }
        return (0xFFL shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    private fun contrast(first: Long, second: Long): Double {
        fun linear(channel: Long): Double {
            val value = channel / 255.0
            return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        fun luminance(color: Long): Double =
            0.2126 * linear((color shr 16) and 0xFF) +
                0.7152 * linear((color shr 8) and 0xFF) +
                0.0722 * linear(color and 0xFF)

        val a = luminance(first)
        val b = luminance(second)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }
}

package lt.radijas.tv

import lt.radijas.tv.ui.theme.OutfitFamily
import lt.radijas.tv.ui.theme.RadijasTypography
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TypographyTest {
    @Test
    fun `all visible text styles use the bundled Outfit family`() {
        listOf(
            RadijasTypography.displayLarge,
            RadijasTypography.headlineSmall,
            RadijasTypography.titleMedium,
            RadijasTypography.labelLarge
        ).forEach { assertEquals(OutfitFamily, it.fontFamily) }
    }
}

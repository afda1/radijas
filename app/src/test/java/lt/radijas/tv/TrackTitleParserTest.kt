package lt.radijas.tv

import lt.radijas.tv.player.parseTrackTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackTitleParserTest {
    @Test fun `keeps artist and title`() = assertEquals("Dua Lipa — Levitating", parseTrackTitle("Dua Lipa — Levitating"))
    @Test fun `trims whitespace`() = assertEquals("ABBA — SOS", parseTrackTitle("  ABBA — SOS  "))
    @Test fun `null stays null`() = assertNull(parseTrackTitle(null))
    @Test fun `blank becomes null`() = assertNull(parseTrackTitle("   "))
    @Test fun `single character becomes null`() = assertNull(parseTrackTitle("x"))
    @Test fun `ad text is kept as-is`() = assertEquals("Reklama: Ledo ritulys", parseTrackTitle("Reklama: Ledo ritulys"))
}

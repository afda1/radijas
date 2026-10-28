package lt.radijas.tv.player

/**
 * Turns the raw ICY `StreamTitle` into the hero's "kas groja" line.
 *
 * No content filtering: ads and DJ talk are shown exactly as the station sends them (§6.6).
 * Only whitespace is trimmed, and values that cannot be a meaningful title (blank, shorter
 * than two characters) become null so the hero keeps an empty line instead of a stray glyph.
 */
internal fun parseTrackTitle(raw: String?): String? =
    raw?.trim()?.takeIf { it.length >= 2 }

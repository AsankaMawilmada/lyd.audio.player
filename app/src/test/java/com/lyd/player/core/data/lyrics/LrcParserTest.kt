package com.lyd.player.core.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parses standard mm colon ss dot xx timestamps in order`() {
        val lrc = """
            [00:01.00]First line
            [00:05.50]Second line
            [01:02.25]Third line
        """.trimIndent()

        val lines = LrcParser.parse(lrc)

        assertEquals(3, lines.size)
        assertEquals(1000L, lines[0].timestampMs)
        assertEquals("First line", lines[0].text)
        assertEquals(5500L, lines[1].timestampMs)
        assertEquals(62250L, lines[2].timestampMs)
    }

    @Test
    fun `sorts lines by timestamp even if input is out of order`() {
        val lrc = """
            [00:10.00]Later
            [00:01.00]Earlier
        """.trimIndent()

        val lines = LrcParser.parse(lrc)

        assertEquals("Earlier", lines[0].text)
        assertEquals("Later", lines[1].text)
    }

    @Test
    fun `supports multiple timestamp tags sharing one line of text`() {
        val lrc = "[00:01.00][00:20.00]Repeated chorus"

        val lines = LrcParser.parse(lrc)

        assertEquals(2, lines.size)
        assertEquals("Repeated chorus", lines[0].text)
        assertEquals("Repeated chorus", lines[1].text)
    }

    @Test
    fun `ignores lines without a timestamp tag`() {
        val lrc = """
            [ar:Some Artist]
            [00:01.00]Actual lyric
        """.trimIndent()

        val lines = LrcParser.parse(lrc)

        assertEquals(1, lines.size)
        assertEquals("Actual lyric", lines[0].text)
    }

    @Test
    fun `activeLineIndex returns the last line at or before the position`() {
        val lines = listOf(
            LyricLine(100L, "a"),
            LyricLine(1000L, "b"),
            LyricLine(5000L, "c"),
        )

        assertEquals(-1, LrcParser.activeLineIndex(lines, 50L))
        assertEquals(0, LrcParser.activeLineIndex(lines, 999L))
        assertEquals(1, LrcParser.activeLineIndex(lines, 4999L))
        assertEquals(2, LrcParser.activeLineIndex(lines, 5000L))
        assertEquals(2, LrcParser.activeLineIndex(lines, 999_999L))
    }
}

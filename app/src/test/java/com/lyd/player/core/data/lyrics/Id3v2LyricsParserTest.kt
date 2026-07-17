package com.lyd.player.core.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.Charset

class Id3v2LyricsParserTest {

    private fun tempMp3(id3Tag: ByteArray): File {
        val file = File.createTempFile("lyrics_test", ".mp3")
        file.deleteOnExit()
        // A handful of trailing bytes stand in for the audio frames our parser never reads.
        file.writeBytes(id3Tag + ByteArray(32) { 0x55 })
        return file
    }

    private fun synchsafe(size: Int): ByteArray = byteArrayOf(
        ((size shr 21) and 0x7F).toByte(),
        ((size shr 14) and 0x7F).toByte(),
        ((size shr 7) and 0x7F).toByte(),
        (size and 0x7F).toByte(),
    )

    private fun bigEndian4(size: Int): ByteArray = byteArrayOf(
        ((size shr 24) and 0xFF).toByte(),
        ((size shr 16) and 0xFF).toByte(),
        ((size shr 8) and 0xFF).toByte(),
        (size and 0xFF).toByte(),
    )

    private fun buildTag(vararg frames: ByteArray): ByteArray {
        val framesBytes = frames.reduce { a, b -> a + b }
        val header = "ID3".toByteArray(Charsets.US_ASCII) +
            byteArrayOf(3, 0, 0) + // v2.3, revision 0, no flags
            synchsafe(framesBytes.size)
        return header + framesBytes
    }

    private fun buildFrame(id: String, content: ByteArray): ByteArray =
        id.toByteArray(Charsets.US_ASCII) + bigEndian4(content.size) + byteArrayOf(0, 0) + content

    private fun usltContent(text: String, encoding: Charset = Charsets.ISO_8859_1): ByteArray =
        byteArrayOf(0) + "eng".toByteArray(Charsets.US_ASCII) + byteArrayOf(0) + text.toByteArray(encoding)

    @Test
    fun `reads plain lyrics from a USLT frame`() {
        val tag = buildTag(buildFrame("USLT", usltContent("Just a plain lyric with no timestamps")))
        val file = tempMp3(tag)

        val result = Id3v2LyricsParser.read(file.path)

        assertTrue(result is LyricsResult.Plain)
        assertEquals("Just a plain lyric with no timestamps", (result as LyricsResult.Plain).text)
    }

    @Test
    fun `treats LRC-formatted text embedded in USLT as synced lyrics`() {
        val lrcText = "[00:01.00]First line\n[00:02.50]Second line"
        val tag = buildTag(buildFrame("USLT", usltContent(lrcText)))
        val file = tempMp3(tag)

        val result = Id3v2LyricsParser.read(file.path)

        assertTrue(result is LyricsResult.Synced)
        val lines = (result as LyricsResult.Synced).lines
        assertEquals(2, lines.size)
        assertEquals(1000L, lines[0].timestampMs)
        assertEquals("First line", lines[0].text)
        assertEquals(2500L, lines[1].timestampMs)
        assertEquals("Second line", lines[1].text)
    }

    @Test
    fun `reads a binary SYLT frame as synced lyrics`() {
        fun syltLine(text: String, timestampMs: Int): ByteArray =
            text.toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0) + bigEndian4(timestampMs)

        val syltContent = byteArrayOf(0) + "eng".toByteArray(Charsets.US_ASCII) +
            byteArrayOf(2) + // timestamp format = milliseconds
            byteArrayOf(1) + // content type = lyrics
            byteArrayOf(0) + // empty content descriptor
            syltLine("Hello", 500) +
            syltLine("World", 1500)

        val tag = buildTag(buildFrame("SYLT", syltContent))
        val file = tempMp3(tag)

        val result = Id3v2LyricsParser.read(file.path)

        assertTrue(result is LyricsResult.Synced)
        val lines = (result as LyricsResult.Synced).lines
        assertEquals(2, lines.size)
        assertEquals(500L, lines[0].timestampMs)
        assertEquals("Hello", lines[0].text)
        assertEquals(1500L, lines[1].timestampMs)
        assertEquals("World", lines[1].text)
    }

    @Test
    fun `falls back to a TXXX LYRICS frame when no USLT or SYLT is present`() {
        val txxxContent = byteArrayOf(0) + "LYRICS".toByteArray(Charsets.US_ASCII) + byteArrayOf(0) +
            "Custom tagger lyrics".toByteArray(Charsets.ISO_8859_1)
        val tag = buildTag(buildFrame("TXXX", txxxContent))
        val file = tempMp3(tag)

        val result = Id3v2LyricsParser.read(file.path)

        assertTrue(result is LyricsResult.Plain)
        assertEquals("Custom tagger lyrics", (result as LyricsResult.Plain).text)
    }

    @Test
    fun `returns NotFound when the file has no ID3v2 tag`() {
        val file = File.createTempFile("no_tag", ".mp3")
        file.deleteOnExit()
        file.writeBytes(ByteArray(32) { 0x00 })

        assertEquals(LyricsResult.NotFound, Id3v2LyricsParser.read(file.path))
    }

    @Test
    fun `returns NotFound when the tag has no lyrics frame`() {
        val tag = buildTag(buildFrame("TIT2", byteArrayOf(0) + "Some Title".toByteArray(Charsets.ISO_8859_1)))
        val file = tempMp3(tag)

        assertEquals(LyricsResult.NotFound, Id3v2LyricsParser.read(file.path))
    }
}

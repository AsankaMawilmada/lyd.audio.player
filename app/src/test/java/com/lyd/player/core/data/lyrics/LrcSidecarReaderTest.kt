package com.lyd.player.core.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LrcSidecarReaderTest {

    @Test
    fun `reads a same-named lrc file as synced lyrics`() {
        val dir = createTempDir()
        val song = File(dir, "Song.mp3").apply { writeBytes(ByteArray(4)) }
        File(dir, "Song.lrc").writeText("[00:01.00]First\n[00:02.00]Second")

        val result = LrcSidecarReader.read(song.path)

        assertTrue(result is LyricsResult.Synced)
        val lines = (result as LyricsResult.Synced).lines
        assertEquals(2, lines.size)
        assertEquals("First", lines[0].text)
    }

    @Test
    fun `matches the lrc file name case-insensitively`() {
        val dir = createTempDir()
        val song = File(dir, "Song.mp3").apply { writeBytes(ByteArray(4)) }
        File(dir, "SONG.LRC").writeText("[00:01.00]Only line")

        val result = LrcSidecarReader.read(song.path)

        assertTrue(result is LyricsResult.Synced)
    }

    @Test
    fun `falls back to plain text when the lrc file has no timestamps`() {
        val dir = createTempDir()
        val song = File(dir, "Song.mp3").apply { writeBytes(ByteArray(4)) }
        File(dir, "Song.lrc").writeText("Just some untimed lyric text")

        val result = LrcSidecarReader.read(song.path)

        assertTrue(result is LyricsResult.Plain)
        assertEquals("Just some untimed lyric text", (result as LyricsResult.Plain).text)
    }

    @Test
    fun `returns NotFound when no sidecar file exists`() {
        val dir = createTempDir()
        val song = File(dir, "Song.mp3").apply { writeBytes(ByteArray(4)) }

        assertEquals(LyricsResult.NotFound, LrcSidecarReader.read(song.path))
    }

    private fun createTempDir(): File = kotlin.io.path.createTempDirectory("lrc_test").toFile()
}

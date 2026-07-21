package au.com.inoaspect.lyd.audio.core.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FlacLyricsParserTest {

    private fun littleEndian4(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte(),
    )

    private fun vorbisCommentBlock(fields: Map<String, String>): ByteArray {
        val vendor = "test vendor".toByteArray(Charsets.UTF_8)
        var body = littleEndian4(vendor.size) + vendor + littleEndian4(fields.size)
        fields.forEach { (key, value) ->
            val entry = "$key=$value".toByteArray(Charsets.UTF_8)
            body += littleEndian4(entry.size) + entry
        }
        return body
    }

    private fun metadataBlock(type: Int, isLast: Boolean, content: ByteArray): ByteArray {
        val flagAndType = (type and 0x7F) or (if (isLast) 0x80 else 0)
        val len = content.size
        val header = byteArrayOf(
            flagAndType.toByte(),
            ((len shr 16) and 0xFF).toByte(),
            ((len shr 8) and 0xFF).toByte(),
            (len and 0xFF).toByte(),
        )
        return header + content
    }

    private fun tempFlac(vararg blocks: ByteArray): File {
        val file = File.createTempFile("lyrics_test", ".flac")
        file.deleteOnExit()
        file.writeBytes("fLaC".toByteArray(Charsets.US_ASCII) + blocks.reduce { a, b -> a + b })
        return file
    }

    @Test
    fun `reads a LYRICS vorbis comment as plain text`() {
        val streamInfo = metadataBlock(type = 0, isLast = false, content = ByteArray(34))
        val comments = metadataBlock(type = 4, isLast = true, content = vorbisCommentBlock(mapOf("LYRICS" to "Just a plain lyric")))
        val file = tempFlac(streamInfo, comments)

        val result = FlacLyricsParser.read(file.path)

        assertTrue(result is LyricsResult.Plain)
        assertEquals("Just a plain lyric", (result as LyricsResult.Plain).text)
    }

    @Test
    fun `treats LRC-formatted LYRICS comment as synced`() {
        val comments = metadataBlock(
            type = 4,
            isLast = true,
            content = vorbisCommentBlock(mapOf("LYRICS" to "[00:03.00]Line one\n[00:07.00]Line two")),
        )
        val file = tempFlac(comments)

        val result = FlacLyricsParser.read(file.path)

        assertTrue(result is LyricsResult.Synced)
        val lines = (result as LyricsResult.Synced).lines
        assertEquals(2, lines.size)
        assertEquals(3000L, lines[0].timestampMs)
        assertEquals(7000L, lines[1].timestampMs)
    }

    @Test
    fun `prefers SYNCEDLYRICS over LYRICS when both are present`() {
        val comments = metadataBlock(
            type = 4,
            isLast = true,
            content = vorbisCommentBlock(
                mapOf(
                    "LYRICS" to "plain fallback",
                    "SYNCEDLYRICS" to "[00:01.00]Preferred line",
                ),
            ),
        )
        val file = tempFlac(comments)

        val result = FlacLyricsParser.read(file.path)

        assertTrue(result is LyricsResult.Synced)
        assertEquals("Preferred line", (result as LyricsResult.Synced).lines[0].text)
    }

    @Test
    fun `returns NotFound when there is no vorbis comment block`() {
        val streamInfo = metadataBlock(type = 0, isLast = true, content = ByteArray(34))
        val file = tempFlac(streamInfo)

        assertEquals(LyricsResult.NotFound, FlacLyricsParser.read(file.path))
    }

    @Test
    fun `returns NotFound for a non-FLAC file`() {
        val file = File.createTempFile("not_flac", ".flac")
        file.deleteOnExit()
        file.writeBytes(ByteArray(16) { 0x00 })

        assertEquals(LyricsResult.NotFound, FlacLyricsParser.read(file.path))
    }
}

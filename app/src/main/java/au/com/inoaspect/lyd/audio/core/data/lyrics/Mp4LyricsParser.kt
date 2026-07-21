package au.com.inoaspect.lyd.audio.core.data.lyrics

import java.io.RandomAccessFile

/** Reads the iTunes-style `©lyr` atom out of an MP4/M4A container's moov/udta/meta/ilst atom tree. */
object Mp4LyricsParser {

    private val LYR_ATOM = byteArrayOf(0xA9.toByte(), 'l'.code.toByte(), 'y'.code.toByte(), 'r'.code.toByte())
    private val MOOV = "moov".toByteArray(Charsets.US_ASCII)
    private val UDTA = "udta".toByteArray(Charsets.US_ASCII)
    private val META = "meta".toByteArray(Charsets.US_ASCII)
    private val ILST = "ilst".toByteArray(Charsets.US_ASCII)
    private val DATA = "data".toByteArray(Charsets.US_ASCII)

    fun read(path: String): LyricsResult {
        RandomAccessFile(path, "r").use { raf ->
            val fileLen = raf.length()
            val moov = findChild(raf, MOOV, 0, fileLen) ?: return LyricsResult.NotFound
            val udta = findChild(raf, UDTA, moov.first, moov.second) ?: return LyricsResult.NotFound
            val meta = findChild(raf, META, udta.first, udta.second) ?: return LyricsResult.NotFound
            // The `meta` box has a 4-byte version/flags field before its children (unlike most boxes).
            val ilst = findChild(raf, ILST, meta.first + 4, meta.second) ?: return LyricsResult.NotFound
            val lyr = findChild(raf, LYR_ATOM, ilst.first, ilst.second) ?: return LyricsResult.NotFound
            val data = findChild(raf, DATA, lyr.first, lyr.second) ?: return LyricsResult.NotFound

            // `data` box: 4-byte type flags, 4-byte locale, then the value bytes (UTF-8 for text tags).
            val textStart = data.first + 8
            val textLen = (data.second - textStart).toInt()
            if (textLen <= 0) return LyricsResult.NotFound
            raf.seek(textStart)
            val buf = ByteArray(textLen)
            raf.readFully(buf)
            return textToLyricsResult(String(buf, Charsets.UTF_8))
        }
    }

    /** Returns the (contentStart, contentEnd) range of the first child atom matching [fourCC] within [rangeStart, rangeEnd). */
    private fun findChild(raf: RandomAccessFile, fourCC: ByteArray, rangeStart: Long, rangeEnd: Long): Pair<Long, Long>? {
        var pos = rangeStart
        val header = ByteArray(8)
        while (pos + 8 <= rangeEnd) {
            raf.seek(pos)
            if (raf.read(header) != 8) return null
            var size = bigEndianLong(header, 0, 4)
            val type = header.copyOfRange(4, 8)
            var headerLen = 8L
            if (size == 1L) {
                val ext = ByteArray(8)
                raf.readFully(ext)
                size = bigEndianLong(ext, 0, 8)
                headerLen = 16L
            } else if (size == 0L) {
                size = rangeEnd - pos
            }
            if (size <= 0) break
            val contentStart = pos + headerLen
            val contentEnd = pos + size
            if (type.contentEquals(fourCC)) return contentStart to contentEnd.coerceAtMost(rangeEnd)
            pos += size
        }
        return null
    }

    private fun bigEndianLong(bytes: ByteArray, offset: Int, len: Int): Long {
        var v = 0L
        for (i in 0 until len) v = (v shl 8) or (bytes[offset + i].toLong() and 0xFF)
        return v
    }
}

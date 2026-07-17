package com.lyd.player.core.data.lyrics

import java.io.RandomAccessFile

/** Reads a `LYRICS`/`SYNCEDLYRICS`/`UNSYNCEDLYRICS` Vorbis comment out of a FLAC file's metadata blocks. */
object FlacLyricsParser {

    fun read(path: String): LyricsResult {
        RandomAccessFile(path, "r").use { raf ->
            val magic = ByteArray(4)
            if (raf.read(magic) != 4 || String(magic, Charsets.US_ASCII) != "fLaC") return LyricsResult.NotFound

            while (true) {
                val blockHeader = ByteArray(4)
                if (raf.read(blockHeader) != 4) break
                val isLast = (blockHeader[0].toInt() and 0x80) != 0
                val blockType = blockHeader[0].toInt() and 0x7F
                val blockLen = ((blockHeader[1].toInt() and 0xFF) shl 16) or
                    ((blockHeader[2].toInt() and 0xFF) shl 8) or (blockHeader[3].toInt() and 0xFF)

                if (blockType == 4) {
                    val content = ByteArray(blockLen)
                    if (raf.read(content) != blockLen) break
                    val comments = parseVorbisComments(content)
                    val text = comments["SYNCEDLYRICS"] ?: comments["LYRICS"] ?: comments["UNSYNCEDLYRICS"]
                    return if (!text.isNullOrBlank()) textToLyricsResult(text) else LyricsResult.NotFound
                } else {
                    raf.skipBytes(blockLen)
                }
                if (isLast) break
            }
        }
        return LyricsResult.NotFound
    }
}

/** Shared by FLAC (raw metadata block) and could be reused for Ogg Vorbis comment payloads. */
internal fun parseVorbisComments(bytes: ByteArray): Map<String, String> {
    var pos = 0
    fun readLEInt(): Int {
        if (pos + 4 > bytes.size) return -1
        val v = (bytes[pos].toInt() and 0xFF) or
            ((bytes[pos + 1].toInt() and 0xFF) shl 8) or
            ((bytes[pos + 2].toInt() and 0xFF) shl 16) or
            ((bytes[pos + 3].toInt() and 0xFF) shl 24)
        pos += 4
        return v
    }

    val vendorLen = readLEInt()
    if (vendorLen < 0 || pos + vendorLen > bytes.size) return emptyMap()
    pos += vendorLen

    val commentCount = readLEInt()
    if (commentCount < 0) return emptyMap()

    val result = mutableMapOf<String, String>()
    repeat(commentCount) {
        val len = readLEInt()
        if (len < 0 || pos + len > bytes.size) return@repeat
        val entry = String(bytes, pos, len, Charsets.UTF_8)
        pos += len
        val eq = entry.indexOf('=')
        if (eq > 0) result[entry.substring(0, eq).uppercase()] = entry.substring(eq + 1)
    }
    return result
}

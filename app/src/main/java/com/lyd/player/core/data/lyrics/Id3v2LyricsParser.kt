package com.lyd.player.core.data.lyrics

import java.io.RandomAccessFile

/**
 * Reads embedded lyrics out of an MP3's ID3v2 tag: prefers a binary-synced `SYLT` frame, then
 * falls back to `USLT` (often itself full LRC text — see [textToLyricsResult]), then a
 * `TXXX:LYRICS`-style custom text frame some taggers use instead.
 */
object Id3v2LyricsParser {

    fun read(path: String): LyricsResult {
        RandomAccessFile(path, "r").use { raf ->
            val header = ByteArray(10)
            if (raf.read(header) != 10) return LyricsResult.NotFound
            if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
                return LyricsResult.NotFound
            }
            val majorVersion = header[3].toInt() and 0xFF
            val flags = header[5].toInt() and 0xFF
            var bytesRemaining = synchsafe(header[6], header[7], header[8], header[9])

            if (flags and 0x40 != 0) {
                val extHeader = ByteArray(4)
                if (raf.read(extHeader) != 4) return LyricsResult.NotFound
                bytesRemaining -= 4
                val toSkip = if (majorVersion >= 4) {
                    (synchsafe(extHeader[0], extHeader[1], extHeader[2], extHeader[3]) - 4).coerceAtLeast(0)
                } else {
                    bigEndianInt(extHeader)
                }
                raf.skipBytes(toSkip)
                bytesRemaining -= toSkip
            }

            val idLen = if (majorVersion == 2) 3 else 4
            val sizeLen = if (majorVersion == 2) 3 else 4
            val frameFlagsLen = if (majorVersion == 2) 0 else 2

            var sylt: List<LyricLine>? = null
            var uslt: String? = null
            var txxxLyrics: String? = null

            while (bytesRemaining > idLen + sizeLen + frameFlagsLen) {
                val idBytes = ByteArray(idLen)
                if (raf.read(idBytes) != idLen) break
                bytesRemaining -= idLen
                if (idBytes[0] == 0.toByte()) break // padding reached

                val sizeBytes = ByteArray(sizeLen)
                if (raf.read(sizeBytes) != sizeLen) break
                bytesRemaining -= sizeLen
                val frameSize = when {
                    majorVersion == 2 ->
                        ((sizeBytes[0].toInt() and 0xFF) shl 16) or ((sizeBytes[1].toInt() and 0xFF) shl 8) or (sizeBytes[2].toInt() and 0xFF)
                    majorVersion >= 4 -> synchsafe(sizeBytes[0], sizeBytes[1], sizeBytes[2], sizeBytes[3])
                    else -> bigEndianInt(sizeBytes)
                }

                if (frameFlagsLen > 0) {
                    raf.skipBytes(frameFlagsLen)
                    bytesRemaining -= frameFlagsLen
                }

                if (frameSize <= 0 || frameSize > bytesRemaining) break
                val content = ByteArray(frameSize)
                val read = raf.read(content)
                bytesRemaining -= frameSize
                if (read != frameSize) break

                when (String(idBytes, Charsets.US_ASCII)) {
                    "SYLT", "SLT" -> parseSylt(content)?.let { sylt = it }
                    "USLT", "ULT" -> parseUslt(content)?.let { uslt = it }
                    "TXXX" -> {
                        val (description, value) = parseTxxx(content) ?: continue
                        if (description.equals("LYRICS", true) ||
                            description.equals("SYNCEDLYRICS", true) ||
                            description.equals("UNSYNCEDLYRICS", true)
                        ) {
                            txxxLyrics = value
                        }
                    }
                }
            }

            return when {
                sylt != null -> LyricsResult.Synced(sylt!!)
                !uslt.isNullOrBlank() -> textToLyricsResult(uslt!!)
                !txxxLyrics.isNullOrBlank() -> textToLyricsResult(txxxLyrics)
                else -> LyricsResult.NotFound
            }
        }
    }

    private fun synchsafe(a: Byte, b: Byte, c: Byte, d: Byte): Int =
        ((a.toInt() and 0x7F) shl 21) or ((b.toInt() and 0x7F) shl 14) or ((c.toInt() and 0x7F) shl 7) or (d.toInt() and 0x7F)

    private fun bigEndianInt(bytes: ByteArray): Int =
        ((bytes[0].toInt() and 0xFF) shl 24) or ((bytes[1].toInt() and 0xFF) shl 16) or
            ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[3].toInt() and 0xFF)

    private fun decodeText(bytes: ByteArray, encodingByte: Int): String {
        val charset = when (encodingByte) {
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        return String(bytes, charset)
    }

    private fun findTerminator(bytes: ByteArray, start: Int, doubleByte: Boolean): Int {
        var i = start
        if (doubleByte) {
            while (i + 1 < bytes.size) {
                if (bytes[i] == 0.toByte() && bytes[i + 1] == 0.toByte()) return i
                i += 2
            }
        } else {
            while (i < bytes.size) {
                if (bytes[i] == 0.toByte()) return i
                i++
            }
        }
        return bytes.size
    }

    private fun parseUslt(content: ByteArray): String? {
        if (content.size < 5) return null
        val encodingByte = content[0].toInt() and 0xFF
        val doubleByte = encodingByte == 1 || encodingByte == 2
        val pos = 4 // encoding byte + 3-byte language code
        val descEnd = findTerminator(content, pos, doubleByte)
        val textStart = (descEnd + if (doubleByte) 2 else 1).coerceAtMost(content.size)
        val text = decodeText(content.copyOfRange(textStart, content.size), encodingByte).trim()
        return text.ifBlank { null }
    }

    private fun parseTxxx(content: ByteArray): Pair<String, String>? {
        if (content.isEmpty()) return null
        val encodingByte = content[0].toInt() and 0xFF
        val doubleByte = encodingByte == 1 || encodingByte == 2
        val descEnd = findTerminator(content, 1, doubleByte)
        val description = decodeText(content.copyOfRange(1, descEnd.coerceAtMost(content.size)), encodingByte).trim()
        val valueStart = (descEnd + if (doubleByte) 2 else 1).coerceAtMost(content.size)
        val value = decodeText(content.copyOfRange(valueStart, content.size), encodingByte).trim()
        return description to value
    }

    private fun parseSylt(content: ByteArray): List<LyricLine>? {
        if (content.size < 6) return null
        val encodingByte = content[0].toInt() and 0xFF
        val timestampFormat = content[4].toInt() and 0xFF // 1 = MPEG frames, 2 = milliseconds
        if (timestampFormat != 2) return null // only ms-based sync is meaningfully seekable
        val doubleByte = encodingByte == 1 || encodingByte == 2
        val descEnd = findTerminator(content, 6, doubleByte)
        var pos = (descEnd + if (doubleByte) 2 else 1).coerceAtMost(content.size)

        val lines = mutableListOf<LyricLine>()
        while (pos < content.size) {
            val textEnd = findTerminator(content, pos, doubleByte)
            if (textEnd >= content.size) break
            val text = decodeText(content.copyOfRange(pos, textEnd), encodingByte).trim()
            val tsPos = textEnd + if (doubleByte) 2 else 1
            if (tsPos + 4 > content.size) break
            val timestampMs = ((content[tsPos].toInt() and 0xFF) shl 24) or
                ((content[tsPos + 1].toInt() and 0xFF) shl 16) or
                ((content[tsPos + 2].toInt() and 0xFF) shl 8) or
                (content[tsPos + 3].toInt() and 0xFF)
            if (text.isNotBlank()) lines += LyricLine(timestampMs.toLong(), text)
            pos = tsPos + 4
        }
        return lines.ifEmpty { null }
    }
}

package com.lyd.player.core.data.lyrics

data class LyricLine(val timestampMs: Long, val text: String)

/** Parses `[mm:ss.xx]lyric text` LRC-format synced lyrics into timestamp-ordered lines. */
object LrcParser {
    private val tagRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parse(syncedLyrics: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        syncedLyrics.lineSequence().forEach { rawLine ->
            val tags = tagRegex.findAll(rawLine).toList()
            if (tags.isEmpty()) return@forEach
            val text = rawLine.substring(tags.last().range.last + 1).trim()
            if (text.isEmpty()) return@forEach
            tags.forEach { match ->
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val fraction = match.groupValues[3]
                val fractionMs = when (fraction.length) {
                    0 -> 0L
                    1 -> fraction.toLong() * 100
                    2 -> fraction.toLong() * 10
                    else -> fraction.toLong()
                }
                val timestampMs = minutes * 60_000 + seconds * 1000 + fractionMs
                lines += LyricLine(timestampMs, text)
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    /** Index of the last line whose timestamp has passed, or -1 if none yet. */
    fun activeLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        var result = -1
        for (i in lines.indices) {
            if (lines[i].timestampMs <= positionMs) result = i else break
        }
        return result
    }
}

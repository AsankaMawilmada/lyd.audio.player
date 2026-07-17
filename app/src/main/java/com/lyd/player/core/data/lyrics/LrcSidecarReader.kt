package com.lyd.player.core.data.lyrics

import java.io.File

/**
 * Reads a sidecar `.lrc` file sitting next to the song — the common local-library convention of
 * naming it identically to the audio file (`Song.mp3` + `Song.lrc`). This is checked before any
 * embedded tag, since a sidecar file is the more deliberate, user-curated source.
 */
object LrcSidecarReader {

    fun read(songPath: String): LyricsResult {
        val songFile = File(songPath)
        val baseName = songFile.nameWithoutExtension
        val dir = songFile.parentFile ?: return LyricsResult.NotFound
        val exactMatch = File(dir, "$baseName.lrc")
        val lrcFile = when {
            exactMatch.isFile -> exactMatch
            else -> dir.listFiles { f -> f.isFile && f.name.equals("$baseName.lrc", ignoreCase = true) }?.firstOrNull()
        } ?: return LyricsResult.NotFound

        return try {
            textToLyricsResult(lrcFile.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            LyricsResult.NotFound
        }
    }
}

package au.com.inoaspect.lyd.audio.core.util

import au.com.inoaspect.lyd.audio.core.data.model.Song
import java.net.URLDecoder

/** Reads and writes the extended M3U format used to interchange playlists with other players. */
object M3uPlaylist {

    fun write(songs: List<Song>): String = buildString {
        appendLine("#EXTM3U")
        for (song in songs) {
            appendLine("#EXTINF:${song.duration / 1000},${song.artist} - ${song.title}")
            appendLine(song.path)
        }
    }

    /** Extracts the song file paths from M3U text, ignoring directives and blank lines. */
    fun parsePaths(text: String): List<String> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { line -> if (line.startsWith("file://")) URLDecoder.decode(line.removePrefix("file://"), "UTF-8") else line }
        .toList()

    fun sanitizeFileName(name: String): String = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "Playlist" }
}

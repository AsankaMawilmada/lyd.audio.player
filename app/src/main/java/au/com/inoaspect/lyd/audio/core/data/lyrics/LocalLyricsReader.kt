package au.com.inoaspect.lyd.audio.core.data.lyrics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads lyrics for a song with no network lookup, in priority order:
 * 1. A sidecar `.lrc` file next to the song (same base filename) — [LrcSidecarReader].
 * 2. Lyrics embedded in the file's own tags — MP3 (ID3v2 USLT/SYLT), FLAC (Vorbis LYRICS
 *    comment), M4A/MP4 (iTunes ©lyr atom).
 * Files with neither simply report [LyricsResult.NotFound] so the UI shows its empty state.
 */
@Singleton
class LocalLyricsReader @Inject constructor() {

    fun read(path: String): LyricsResult = try {
        val sidecarResult = LrcSidecarReader.read(path)
        if (sidecarResult != LyricsResult.NotFound) {
            sidecarResult
        } else {
            when (path.substringAfterLast('.', "").lowercase()) {
                "mp3" -> Id3v2LyricsParser.read(path)
                "flac" -> FlacLyricsParser.read(path)
                "m4a", "mp4", "aac", "m4b" -> Mp4LyricsParser.read(path)
                else -> LyricsResult.NotFound
            }
        }
    } catch (_: Exception) {
        LyricsResult.NotFound
    }
}

package com.lyd.player.core.data.lyrics

import javax.inject.Inject
import javax.inject.Singleton

sealed interface LyricsResult {
    data class Synced(val lines: List<LyricLine>) : LyricsResult
    data class Plain(val text: String) : LyricsResult
    data object NotFound : LyricsResult
}

@Singleton
class LyricsRepository @Inject constructor(
    private val api: LrcLibApi,
) {

    suspend fun fetch(title: String, artist: String, album: String?, durationSeconds: Int): LyricsResult = try {
        val response = api.getLyrics(title, artist, album, durationSeconds)
        val track = response.body()
        track.toResult() ?: fallbackSearch(title, artist)
    } catch (_: Exception) {
        LyricsResult.NotFound
    }

    private suspend fun fallbackSearch(title: String, artist: String): LyricsResult = try {
        val results = api.search(title, artist).body().orEmpty()
        val best = results.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
            ?: results.firstOrNull { !it.plainLyrics.isNullOrBlank() }
        best.toResult() ?: LyricsResult.NotFound
    } catch (_: Exception) {
        LyricsResult.NotFound
    }

    private fun LrcLibTrack?.toResult(): LyricsResult? = when {
        this == null -> null
        !syncedLyrics.isNullOrBlank() -> LyricsResult.Synced(LrcParser.parse(syncedLyrics))
        !plainLyrics.isNullOrBlank() -> LyricsResult.Plain(plainLyrics)
        else -> null
    }
}

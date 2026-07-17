package com.lyd.player.core.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LyricsResult {
    data class Synced(val lines: List<LyricLine>) : LyricsResult
    data class Plain(val text: String) : LyricsResult
    data object NotFound : LyricsResult
}

@Singleton
class LyricsRepository @Inject constructor(
    private val localLyricsReader: LocalLyricsReader,
) {

    /** Reads lyrics for the song at [path] (sidecar .lrc, then embedded tags). File I/O runs off the main thread. */
    suspend fun fetch(path: String): LyricsResult = withContext(Dispatchers.IO) {
        localLyricsReader.read(path)
    }
}

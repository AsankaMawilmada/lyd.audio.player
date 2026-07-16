package com.lyd.player.core.data.repo

import com.lyd.player.core.data.db.RecentPlayDao
import com.lyd.player.core.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentPlaysRepository @Inject constructor(
    private val dao: RecentPlayDao,
    private val libraryRepository: LibraryRepository,
) {

    suspend fun record(songPath: String) = dao.record(songPath, System.currentTimeMillis())

    /** Most-recent-first, silently dropping entries whose file no longer exists in the library. */
    fun observeRecentSongs(): Flow<List<Song>> =
        combine(dao.observeRecentPlays(), libraryRepository.songs) { recent, _ ->
            recent.mapNotNull { libraryRepository.songByPath(it.songPath) }
        }
}

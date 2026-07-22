package au.com.inoaspect.lyd.audio.core.data.repo

import au.com.inoaspect.lyd.audio.core.data.db.RecentPlayDao
import au.com.inoaspect.lyd.audio.core.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    /** One-shot snapshot for callers that can't observe a [Flow] (e.g. Android Auto's media-browser callbacks). */
    suspend fun currentRecentSongs(): List<Song> = observeRecentSongs().first()
}

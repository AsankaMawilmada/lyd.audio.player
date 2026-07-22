package au.com.inoaspect.lyd.audio.core.data.repo

import au.com.inoaspect.lyd.audio.core.data.db.PlaylistDao
import au.com.inoaspect.lyd.audio.core.data.db.PlaylistEntity
import au.com.inoaspect.lyd.audio.core.data.db.PlaylistWithSongs
import au.com.inoaspect.lyd.audio.core.data.model.FAVORITES_PLAYLIST_NAME
import au.com.inoaspect.lyd.audio.core.data.model.Playlist
import au.com.inoaspect.lyd.audio.core.data.model.ResolvedPlaylist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private fun PlaylistWithSongs.toModel(): Playlist = Playlist(
    id = playlist.id,
    name = playlist.name,
    createdAt = playlist.createdAt,
    isFavorites = playlist.isFavorites,
    songPaths = songs.sortedBy { it.position }.map { it.songPath },
)

@Singleton
class PlaylistRepository @Inject constructor(
    private val dao: PlaylistDao,
    private val libraryRepository: LibraryRepository,
) {

    /** Idempotent: creates the permanent Favorites playlist on first run only. */
    suspend fun ensureFavoritesPlaylist(): Long {
        dao.getFavoritesPlaylist()?.let { return it.id }
        return dao.insertPlaylist(
            PlaylistEntity(
                name = FAVORITES_PLAYLIST_NAME,
                createdAt = System.currentTimeMillis(),
                isFavorites = true,
            ),
        )
    }

    fun observePlaylists(): Flow<List<Playlist>> =
        dao.observePlaylistsWithSongs().map { list -> list.map { it.toModel() } }

    fun observePlaylist(id: Long): Flow<Playlist?> =
        dao.observePlaylistWithSongs(id).map { it?.toModel() }

    fun observeResolvedPlaylist(id: Long): Flow<ResolvedPlaylist?> =
        combine(observePlaylist(id), libraryRepository.songs) { playlist, _ ->
            playlist?.let { ResolvedPlaylist(it, libraryRepository.songsByPaths(it.songPaths)) }
        }

    fun observeFavoritePaths(): Flow<Set<String>> =
        dao.observePlaylistsWithSongs().map { list ->
            list.firstOrNull { it.playlist.isFavorites }?.songs?.map { it.songPath }?.toSet().orEmpty()
        }

    /** One-shot snapshots for callers that can't observe a [Flow] (e.g. Android Auto's media-browser callbacks). */
    suspend fun currentPlaylists(): List<Playlist> = observePlaylists().first()
    suspend fun currentFavoritePaths(): Set<String> = observeFavoritePaths().first()

    suspend fun isFavorite(songPath: String): Boolean {
        val favorites = dao.getFavoritesPlaylist() ?: return false
        return dao.getSongsForPlaylist(favorites.id).any { it.songPath == songPath }
    }

    suspend fun toggleFavorite(songPath: String) {
        val favoritesId = ensureFavoritesPlaylist()
        val current = dao.getSongsForPlaylist(favoritesId)
        if (current.any { it.songPath == songPath }) {
            dao.removeSong(favoritesId, songPath)
        } else {
            dao.appendSongsDeduped(favoritesId, listOf(songPath))
        }
    }

    suspend fun createPlaylist(name: String, initialSongPaths: List<String>): Long {
        val id = dao.insertPlaylist(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))
        if (initialSongPaths.isNotEmpty()) dao.appendSongsDeduped(id, initialSongPaths)
        return id
    }

    suspend fun addSongs(playlistId: Long, songPaths: List<String>) =
        dao.appendSongsDeduped(playlistId, songPaths)

    suspend fun removeSongAt(playlistId: Long, songPath: String) =
        dao.removeSong(playlistId, songPath)

    suspend fun reorder(playlistId: Long, orderedSongPaths: List<String>) =
        dao.replaceOrder(playlistId, orderedSongPaths)

    suspend fun rename(playlistId: Long, name: String) = dao.renamePlaylist(playlistId, name)

    /** No-op for the Favorites playlist — the UI must not offer delete for it, but guard here too. */
    suspend fun delete(playlistId: Long) = dao.deletePlaylist(playlistId)
}

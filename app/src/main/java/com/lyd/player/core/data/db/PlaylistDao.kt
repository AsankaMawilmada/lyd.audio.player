package com.lyd.player.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(parentColumn = "id", entityColumn = "playlistId", entity = PlaylistSongEntity::class)
    val songs: List<PlaylistSongEntity>,
)

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY isFavorites DESC, createdAt ASC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY isFavorites DESC, createdAt ASC")
    fun observePlaylistsWithSongs(): Flow<List<PlaylistWithSongs>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observePlaylistWithSongs(id: Long): Flow<PlaylistWithSongs?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observePlaylist(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE isFavorites = 1 LIMIT 1")
    suspend fun getFavoritesPlaylist(): PlaylistEntity?

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id AND isFavorites = 0")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getSongsForPlaylist(playlistId: Long): List<PlaylistSongEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongs(songs: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songPath = :songPath")
    suspend fun removeSong(playlistId: Long, songPath: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Update
    suspend fun updatePlaylistSongs(songs: List<PlaylistSongEntity>)

    /** Appends [songPaths] after the current max position, skipping any already present (dedupe). */
    @Transaction
    suspend fun appendSongsDeduped(playlistId: Long, songPaths: List<String>) {
        val existing = getSongsForPlaylist(playlistId).map { it.songPath }.toSet()
        var nextPosition = getMaxPosition(playlistId) + 1
        val toInsert = songPaths
            .filter { it !in existing }
            .distinct()
            .map { path ->
                PlaylistSongEntity(playlistId, path, nextPosition++)
            }
        if (toInsert.isNotEmpty()) insertPlaylistSongs(toInsert)
    }

    /** Persists a full reorder/removal of a playlist's song list. */
    @Transaction
    suspend fun replaceOrder(playlistId: Long, orderedPaths: List<String>) {
        clearPlaylistSongs(playlistId)
        insertPlaylistSongs(orderedPaths.mapIndexed { index, path -> PlaylistSongEntity(playlistId, path, index) })
    }

    @Delete
    suspend fun deletePlaylistSongEntity(song: PlaylistSongEntity)
}

package com.lyd.player.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val isFavorites: Boolean = false,
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songPath"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songPath: String,
    val position: Int,
)

@Entity(tableName = "recent_plays")
data class RecentPlayEntity(
    @PrimaryKey val songPath: String,
    val playedAt: Long,
)

@Entity(tableName = "art_cache")
data class ArtCacheEntity(
    @PrimaryKey val albumId: Long,
    val filePath: String?,
    val hasArt: Boolean,
)

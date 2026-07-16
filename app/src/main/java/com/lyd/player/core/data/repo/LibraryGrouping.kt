package com.lyd.player.core.data.repo

import com.lyd.player.core.data.model.Album
import com.lyd.player.core.data.model.Artist
import com.lyd.player.core.data.model.Folder
import com.lyd.player.core.data.model.Song
import com.lyd.player.core.data.model.SongSortOrder

fun List<Song>.toAlbums(): List<Album> = groupBy { it.albumId }
    .map { (albumId, songs) ->
        val first = songs.first()
        Album(
            id = albumId,
            name = first.album,
            artist = first.artist,
            artistId = first.artistId,
            songCount = songs.size,
            totalDuration = songs.sumOf { it.duration },
        )
    }
    .sortedBy { it.name.lowercase() }

fun List<Song>.toArtists(): List<Artist> = groupBy { it.artistId }
    .map { (artistId, songs) ->
        Artist(
            id = artistId,
            name = songs.first().artist,
            trackCount = songs.size,
            albumCount = songs.map { it.albumId }.distinct().size,
        )
    }
    .sortedBy { it.name.lowercase() }

fun List<Song>.toFolders(): List<Folder> = groupBy { it.folderPath }
    .map { (path, songs) ->
        Folder(
            path = path,
            name = songs.first().folderName,
            trackCount = songs.size,
            totalDuration = songs.sumOf { it.duration },
        )
    }
    .sortedBy { it.name.lowercase() }

fun List<Song>.search(query: String): List<Song> {
    if (query.isBlank()) return this
    val q = query.trim()
    return filter {
        it.title.contains(q, ignoreCase = true) ||
            it.artist.contains(q, ignoreCase = true) ||
            it.album.contains(q, ignoreCase = true)
    }
}

fun List<Song>.sortedWith(order: SongSortOrder): List<Song> = when (order) {
    SongSortOrder.TITLE -> sortedBy { it.title.lowercase() }
    SongSortOrder.ARTIST -> sortedBy { it.artist.lowercase() }
    SongSortOrder.ALBUM -> sortedBy { it.album.lowercase() }
    SongSortOrder.DATE_ADDED -> sortedByDescending { it.dateAdded }
    SongSortOrder.DURATION -> sortedBy { it.duration }
}

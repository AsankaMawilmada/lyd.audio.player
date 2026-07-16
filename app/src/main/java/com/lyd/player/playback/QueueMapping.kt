package com.lyd.player.playback

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.lyd.player.core.data.model.Song

private const val EXTRA_DURATION_MS = "durationMs"
private const val EXTRA_ALBUM_ID = "albumId"
private const val EXTRA_ARTIST_ID = "artistId"
private const val EXTRA_MEDIA_STORE_ID = "mediaStoreId"

data class QueueItem(
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: Uri?,
    val durationMs: Long,
    val albumId: Long,
    val artistId: Long,
    val mediaStoreId: Long,
) {
    /** Reconstructs a [Song] for reuse in generic song-actions UI (favorite/add-to-playlist/etc). */
    fun toSong(): Song = Song(
        path = path,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        artistId = artistId,
        duration = durationMs,
        mediaStoreId = mediaStoreId,
    )
}

fun Song.toMediaItem(artworkFileUri: Uri?): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(artworkFileUri)
        .setExtras(
            Bundle().apply {
                putLong(EXTRA_DURATION_MS, duration)
                putLong(EXTRA_ALBUM_ID, albumId)
                putLong(EXTRA_ARTIST_ID, artistId)
                putLong(EXTRA_MEDIA_STORE_ID, mediaStoreId)
            },
        )
        .build()
    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
    return MediaItem.Builder()
        .setMediaId(path)
        .setUri(contentUri)
        .setMediaMetadata(metadata)
        .build()
}

fun MediaItem.toQueueItem(): QueueItem {
    val extras = mediaMetadata.extras
    return QueueItem(
        path = mediaId,
        title = mediaMetadata.title?.toString().orEmpty(),
        artist = mediaMetadata.artist?.toString().orEmpty(),
        album = mediaMetadata.albumTitle?.toString().orEmpty(),
        artworkUri = mediaMetadata.artworkUri,
        durationMs = extras?.getLong(EXTRA_DURATION_MS) ?: 0L,
        albumId = extras?.getLong(EXTRA_ALBUM_ID) ?: -1L,
        artistId = extras?.getLong(EXTRA_ARTIST_ID) ?: -1L,
        mediaStoreId = extras?.getLong(EXTRA_MEDIA_STORE_ID) ?: -1L,
    )
}

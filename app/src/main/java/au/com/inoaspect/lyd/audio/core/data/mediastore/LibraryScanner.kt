package au.com.inoaspect.lyd.audio.core.data.mediastore

import android.content.Context
import android.provider.MediaStore
import au.com.inoaspect.lyd.audio.core.data.model.Song
import au.com.inoaspect.lyd.audio.core.data.model.UNKNOWN_ALBUM
import au.com.inoaspect.lyd.audio.core.data.model.UNKNOWN_ARTIST
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Queries the device MediaStore for all local audio tracks. */
@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun scanSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: continue
                val rawArtist = cursor.getString(artistCol)
                val rawAlbum = cursor.getString(albumCol)
                songs += Song(
                    path = path,
                    title = cursor.getString(titleCol) ?: path.substringAfterLast('/'),
                    artist = rawArtist?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: UNKNOWN_ARTIST,
                    album = rawAlbum?.takeIf { it.isNotBlank() } ?: UNKNOWN_ALBUM,
                    albumId = cursor.getLong(albumIdCol),
                    artistId = cursor.getLong(artistIdCol),
                    duration = cursor.getLong(durationCol),
                    sizeBytes = cursor.getLong(sizeCol).takeIf { it > 0 },
                    dateAdded = cursor.getLong(dateAddedCol) * 1000L,
                    mediaStoreId = cursor.getLong(idCol),
                )
            }
        }
        songs
    }
}

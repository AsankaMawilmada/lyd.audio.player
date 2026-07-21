package au.com.inoaspect.lyd.audio.core.data.model

import java.io.File

const val UNKNOWN_ARTIST = "Unknown artist"
const val UNKNOWN_ALBUM = "Unknown album"

/**
 * A single track scanned from the device MediaStore. [path] is the stable unique identity
 * used everywhere a song needs to be referenced (queue, playlists, favorites, recent-plays)
 * since MediaStore row ids can churn across rescans.
 */
data class Song(
    val path: String,
    val title: String,
    val artist: String = UNKNOWN_ARTIST,
    val album: String = UNKNOWN_ALBUM,
    val albumId: Long,
    val artistId: Long,
    val duration: Long,
    val sizeBytes: Long? = null,
    val genre: String? = null,
    val dateAdded: Long = 0L,
    val mediaStoreId: Long = 0L,
) {
    val folderPath: String get() = File(path).parent ?: "/"
    val folderName: String get() = File(folderPath).name.ifBlank { "/" }
}

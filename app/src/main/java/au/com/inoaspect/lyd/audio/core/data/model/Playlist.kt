package au.com.inoaspect.lyd.audio.core.data.model

const val FAVORITES_PLAYLIST_NAME = "Favorites"

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val isFavorites: Boolean,
    val songPaths: List<String>,
) {
    val songCount: Int get() = songPaths.size
}

/** A playlist resolved against the live library — missing files are already filtered out. */
data class ResolvedPlaylist(
    val playlist: Playlist,
    val songs: List<Song>,
)

enum class SongSortOrder {
    TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION
}

enum class RepeatMode {
    OFF, ALL, ONE;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }
}

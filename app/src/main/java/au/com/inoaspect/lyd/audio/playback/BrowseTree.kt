package au.com.inoaspect.lyd.audio.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import au.com.inoaspect.lyd.audio.core.data.model.Album
import au.com.inoaspect.lyd.audio.core.data.model.Artist
import au.com.inoaspect.lyd.audio.core.data.model.Folder
import au.com.inoaspect.lyd.audio.core.data.model.Playlist
import au.com.inoaspect.lyd.audio.core.data.model.Song

/** Node ids for the Android Auto / media-browser content tree (see [BrowseTree]). */
object BrowseIds {
    const val ROOT = "root"
    const val RECENT = "recent"
    const val FAVORITES = "favorites"
    const val PLAYLISTS = "playlists"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val FOLDERS = "folders"
    const val ALL_SONGS = "all_songs"

    private const val PREFIX_ALBUM = "album_"
    private const val PREFIX_ARTIST = "artist_"
    private const val PREFIX_FOLDER = "folder_"
    private const val PREFIX_PLAYLIST = "playlist_"

    fun album(id: Long) = "$PREFIX_ALBUM$id"
    fun artist(id: Long) = "$PREFIX_ARTIST$id"
    fun folder(path: String) = "$PREFIX_FOLDER$path"
    fun playlist(id: Long) = "$PREFIX_PLAYLIST$id"

    fun albumId(nodeId: String): Long? = nodeId.takeIf { it.startsWith(PREFIX_ALBUM) }?.removePrefix(PREFIX_ALBUM)?.toLongOrNull()
    fun artistId(nodeId: String): Long? = nodeId.takeIf { it.startsWith(PREFIX_ARTIST) }?.removePrefix(PREFIX_ARTIST)?.toLongOrNull()
    fun folderPath(nodeId: String): String? = nodeId.takeIf { it.startsWith(PREFIX_FOLDER) }?.removePrefix(PREFIX_FOLDER)
    fun playlistId(nodeId: String): Long? = nodeId.takeIf { it.startsWith(PREFIX_PLAYLIST) }?.removePrefix(PREFIX_PLAYLIST)?.toLongOrNull()
}

/**
 * Builds the Android Auto / media-browser content tree as plain [MediaItem]s, mirroring the
 * phone app's own primary destinations (Home/Library/Playlists) so there's no separate
 * information architecture to maintain. Pure and Android-framework-light (only [MediaItem]/
 * [MediaMetadata]) so it's directly unit-testable without Hilt or a real device.
 *
 * Leaf song nodes reuse the song's file path as [MediaItem.mediaId], identical to how the phone
 * queue keys items ([Song.toMediaItem]) — only the intermediate category/album/artist/folder/
 * playlist nodes need the synthetic ids in [BrowseIds].
 */
object BrowseTree {

    fun rootItem(): MediaItem = categoryNode(BrowseIds.ROOT, "Lyd")

    fun rootChildren(): List<MediaItem> = listOf(
        categoryNode(BrowseIds.RECENT, "Recently Played"),
        categoryNode(BrowseIds.FAVORITES, "Favorites"),
        categoryNode(BrowseIds.PLAYLISTS, "Playlists"),
        categoryNode(BrowseIds.ALBUMS, "Albums"),
        categoryNode(BrowseIds.ARTISTS, "Artists"),
        categoryNode(BrowseIds.FOLDERS, "Folders"),
        categoryNode(BrowseIds.ALL_SONGS, "All Songs"),
    )

    /** Returns null for an unrecognized [nodeId] (distinct from a recognized-but-empty node). */
    fun childrenFor(
        nodeId: String,
        allSongs: List<Song>,
        albums: List<Album>,
        artists: List<Artist>,
        folders: List<Folder>,
        playlists: List<Playlist>,
        recentSongs: List<Song>,
        favoriteSongs: List<Song>,
        artByAlbumId: Map<Long, Uri?> = emptyMap(),
    ): List<MediaItem>? {
        fun songs(list: List<Song>) = list.map { songNode(it, artByAlbumId[it.albumId]) }

        return when {
            nodeId == BrowseIds.ROOT -> rootChildren()
            nodeId == BrowseIds.RECENT -> songs(recentSongs)
            nodeId == BrowseIds.FAVORITES -> songs(favoriteSongs)
            nodeId == BrowseIds.PLAYLISTS -> playlists.map { playlistNode(it) }
            nodeId == BrowseIds.ALBUMS -> albums.map { albumNode(it, artByAlbumId[it.id]) }
            nodeId == BrowseIds.ARTISTS -> artists.map { artistNode(it) }
            nodeId == BrowseIds.FOLDERS -> folders.map { folderNode(it) }
            nodeId == BrowseIds.ALL_SONGS -> songs(allSongs.sortedBy { it.title.lowercase() })
            BrowseIds.albumId(nodeId) != null ->
                songs(allSongs.filter { it.albumId == BrowseIds.albumId(nodeId) }.sortedBy { it.title.lowercase() })
            BrowseIds.artistId(nodeId) != null ->
                songs(allSongs.filter { it.artistId == BrowseIds.artistId(nodeId) }.sortedBy { it.title.lowercase() })
            BrowseIds.folderPath(nodeId) != null ->
                songs(allSongs.filter { it.folderPath == BrowseIds.folderPath(nodeId) }.sortedBy { it.title.lowercase() })
            BrowseIds.playlistId(nodeId) != null -> {
                val playlist = playlists.firstOrNull { it.id == BrowseIds.playlistId(nodeId) } ?: return null
                val byPath = allSongs.associateBy { it.path }
                songs(playlist.songPaths.mapNotNull { byPath[it] })
            }
            else -> null
        }
    }

    fun songNode(song: Song, artUri: Uri? = null): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(artUri)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build()
        return MediaItem.Builder().setMediaId(song.path).setMediaMetadata(metadata).build()
    }

    fun albumNode(album: Album, artUri: Uri? = null): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(album.name)
            .setArtist(album.artist)
            .setArtworkUri(artUri)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
            .build()
        return MediaItem.Builder().setMediaId(BrowseIds.album(album.id)).setMediaMetadata(metadata).build()
    }

    fun artistNode(artist: Artist): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(artist.name)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
            .build()
        return MediaItem.Builder().setMediaId(BrowseIds.artist(artist.id)).setMediaMetadata(metadata).build()
    }

    fun folderNode(folder: Folder): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(folder.name)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            .build()
        return MediaItem.Builder().setMediaId(BrowseIds.folder(folder.path)).setMediaMetadata(metadata).build()
    }

    fun playlistNode(playlist: Playlist): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(playlist.name)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
            .build()
        return MediaItem.Builder().setMediaId(BrowseIds.playlist(playlist.id)).setMediaMetadata(metadata).build()
    }

    private fun categoryNode(id: String, title: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            .build()
        return MediaItem.Builder().setMediaId(id).setMediaMetadata(metadata).build()
    }
}

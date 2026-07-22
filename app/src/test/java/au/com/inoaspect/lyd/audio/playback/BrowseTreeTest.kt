package au.com.inoaspect.lyd.audio.playback

import au.com.inoaspect.lyd.audio.core.data.model.Album
import au.com.inoaspect.lyd.audio.core.data.model.Artist
import au.com.inoaspect.lyd.audio.core.data.model.Folder
import au.com.inoaspect.lyd.audio.core.data.model.Playlist
import au.com.inoaspect.lyd.audio.core.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseTreeTest {

    private val songA1 = Song(path = "/music/albumA/one.mp3", title = "One", artist = "Artist A", album = "Album A", albumId = 1L, artistId = 10L, duration = 1000L, mediaStoreId = 101L)
    private val songA2 = Song(path = "/music/albumA/two.mp3", title = "Two", artist = "Artist A", album = "Album A", albumId = 1L, artistId = 10L, duration = 2000L, mediaStoreId = 102L)
    private val songB1 = Song(path = "/music/albumB/three.mp3", title = "Three", artist = "Artist B", album = "Album B", albumId = 2L, artistId = 20L, duration = 1500L, mediaStoreId = 103L)
    private val allSongs = listOf(songA1, songA2, songB1)

    private val albums = listOf(
        Album(id = 1L, name = "Album A", artist = "Artist A", artistId = 10L, songCount = 2, totalDuration = 3000L),
        Album(id = 2L, name = "Album B", artist = "Artist B", artistId = 20L, songCount = 1, totalDuration = 1500L),
    )
    private val artists = listOf(
        Artist(id = 10L, name = "Artist A", trackCount = 2, albumCount = 1),
        Artist(id = 20L, name = "Artist B", trackCount = 1, albumCount = 1),
    )
    private val folders = listOf(
        Folder(path = "/music/albumA", name = "albumA", trackCount = 2, totalDuration = 3000L),
        Folder(path = "/music/albumB", name = "albumB", trackCount = 1, totalDuration = 1500L),
    )
    private val playlist = Playlist(id = 5L, name = "Road Trip", createdAt = 0L, isFavorites = false, songPaths = listOf(songB1.path, songA1.path))

    private fun childrenFor(nodeId: String) = BrowseTree.childrenFor(
        nodeId = nodeId,
        allSongs = allSongs,
        albums = albums,
        artists = artists,
        folders = folders,
        playlists = listOf(playlist),
        recentSongs = emptyList(),
        favoriteSongs = emptyList(),
    )

    @Test
    fun `root has the seven expected category nodes in order`() {
        val children = BrowseTree.rootChildren()
        assertEquals(
            listOf("Recently Played", "Favorites", "Playlists", "Albums", "Artists", "Folders", "All Songs"),
            children.map { it.mediaMetadata.title.toString() },
        )
        assertTrue(children.all { it.mediaMetadata.isBrowsable == true })
        assertTrue(children.none { it.mediaMetadata.isPlayable == true })
    }

    @Test
    fun `childrenFor root id matches rootChildren`() {
        val result = childrenFor(BrowseIds.ROOT)
        assertEquals(BrowseTree.rootChildren().map { it.mediaId }, result?.map { it.mediaId })
    }

    @Test
    fun `albums category returns one browsable node per album`() {
        val result = childrenFor(BrowseIds.ALBUMS)
        assertEquals(listOf(BrowseIds.album(1L), BrowseIds.album(2L)), result?.map { it.mediaId })
        assertTrue(result!!.all { it.mediaMetadata.isBrowsable == true && it.mediaMetadata.isPlayable == false })
    }

    @Test
    fun `album node children are that album's songs sorted by title`() {
        val result = childrenFor(BrowseIds.album(1L))
        assertEquals(listOf(songA1.path, songA2.path), result?.map { it.mediaId })
        assertTrue(result!!.all { it.mediaMetadata.isPlayable == true && it.mediaMetadata.isBrowsable == false })
    }

    @Test
    fun `artist node children are that artist's songs`() {
        assertEquals(listOf(songB1.path), childrenFor(BrowseIds.artist(20L))?.map { it.mediaId })
    }

    @Test
    fun `folder node children are that folder's songs`() {
        // Use the actual computed folderPath (File.parent normalizes separators per-platform)
        // rather than a hardcoded literal, so this test is correct on Windows and Unix alike.
        assertEquals(listOf(songB1.path), childrenFor(BrowseIds.folder(songB1.folderPath))?.map { it.mediaId })
    }

    @Test
    fun `playlist node children follow the playlist's own song order, not library order`() {
        assertEquals(listOf(songB1.path, songA1.path), childrenFor(BrowseIds.playlist(5L))?.map { it.mediaId })
    }

    @Test
    fun `recently played and favorites reflect whatever song lists are passed in`() {
        val recent = BrowseTree.childrenFor(
            BrowseIds.RECENT, allSongs, albums, artists, folders, listOf(playlist),
            recentSongs = listOf(songB1), favoriteSongs = emptyList(),
        )
        assertEquals(listOf(songB1.path), recent?.map { it.mediaId })

        val favorites = BrowseTree.childrenFor(
            BrowseIds.FAVORITES, allSongs, albums, artists, folders, listOf(playlist),
            recentSongs = emptyList(), favoriteSongs = listOf(songA1, songA2),
        )
        assertEquals(listOf(songA1.path, songA2.path), favorites?.map { it.mediaId })
    }

    @Test
    fun `all songs category returns every song sorted by title`() {
        // "One" < "Three" < "Two"
        assertEquals(listOf(songA1.path, songB1.path, songA2.path), childrenFor(BrowseIds.ALL_SONGS)?.map { it.mediaId })
    }

    @Test
    fun `unrecognized node id returns null, distinct from an empty result`() {
        assertNull(childrenFor("not_a_real_id"))
    }

    @Test
    fun `unknown playlist id returns null`() {
        assertNull(childrenFor(BrowseIds.playlist(999L)))
    }

    @Test
    fun `song node carries the file path as its media id and is playable, not browsable`() {
        val node = BrowseTree.songNode(songA1)
        assertEquals(songA1.path, node.mediaId)
        assertEquals(songA1.title, node.mediaMetadata.title.toString())
        assertTrue(node.mediaMetadata.isPlayable == true)
        assertTrue(node.mediaMetadata.isBrowsable == false)
    }
}

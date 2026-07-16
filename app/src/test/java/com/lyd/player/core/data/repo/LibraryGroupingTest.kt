package com.lyd.player.core.data.repo

import com.lyd.player.core.data.model.Song
import com.lyd.player.core.data.model.SongSortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

private fun song(
    path: String,
    title: String,
    artist: String = "Artist",
    album: String = "Album",
    albumId: Long = 1L,
    artistId: Long = 1L,
    duration: Long = 1000L,
    dateAdded: Long = 0L,
) = Song(
    path = path,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    artistId = artistId,
    duration = duration,
    dateAdded = dateAdded,
)

class LibraryGroupingTest {

    @Test
    fun `search matches title artist or album case-insensitively`() {
        val songs = listOf(
            song("/a", title = "Ethereal Drift", artist = "Solaris Phase", album = "Nightfall"),
            song("/b", title = "Shadow Work", artist = "Kinetica", album = "Motion"),
        )

        assertEquals(1, songs.search("ethereal").size)
        assertEquals(1, songs.search("KINETICA").size)
        assertEquals(1, songs.search("motion").size)
        assertEquals(0, songs.search("nonexistent").size)
        assertEquals(2, songs.search("").size)
    }

    @Test
    fun `sortedWith orders by the requested field`() {
        val songs = listOf(
            song("/a", title = "Bravo", artist = "Zeta", duration = 300, dateAdded = 100),
            song("/b", title = "Alpha", artist = "Yankee", duration = 100, dateAdded = 200),
        )

        assertEquals(listOf("Alpha", "Bravo"), songs.sortedWith(SongSortOrder.TITLE).map { it.title })
        assertEquals(listOf("Alpha", "Bravo"), songs.sortedWith(SongSortOrder.ARTIST).map { it.title })
        assertEquals(listOf("Alpha", "Bravo"), songs.sortedWith(SongSortOrder.DATE_ADDED).map { it.title })
        assertEquals(listOf("Alpha", "Bravo"), songs.sortedWith(SongSortOrder.DURATION).map { it.title })
    }

    @Test
    fun `toAlbums groups by album id and counts songs`() {
        val songs = listOf(
            song("/a", "Song A", albumId = 1L, duration = 100),
            song("/b", "Song B", albumId = 1L, duration = 200),
            song("/c", "Song C", albumId = 2L, duration = 300),
        )

        val albums = songs.toAlbums()

        assertEquals(2, albums.size)
        val albumOne = albums.first { it.id == 1L }
        assertEquals(2, albumOne.songCount)
        assertEquals(300L, albumOne.totalDuration)
    }

    @Test
    fun `toFolders groups by parent directory of the file path`() {
        val songs = listOf(
            song("/music/rock/one.mp3", "One"),
            song("/music/rock/two.mp3", "Two"),
            song("/music/jazz/three.mp3", "Three"),
        )

        val folders = songs.toFolders()

        assertEquals(2, folders.size)
        assertEquals(2, folders.first { it.name == "rock" }.trackCount)
        assertEquals(1, folders.first { it.name == "jazz" }.trackCount)
    }
}

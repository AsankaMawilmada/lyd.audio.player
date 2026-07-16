package com.lyd.player.core.data.repo

import com.lyd.player.core.data.mediastore.LibraryScanner
import com.lyd.player.core.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache of the scanned local library. `null` in [songs] means "never scanned yet"
 * (distinct from an empty result), so UIs can tell "no permission / no scan" from "no music".
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val scanner: LibraryScanner,
) {
    private val _songs = MutableStateFlow<List<Song>?>(null)
    val songs: StateFlow<List<Song>?> = _songs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    suspend fun rescan() {
        _isScanning.value = true
        try {
            _songs.value = scanner.scanSongs()
        } finally {
            _isScanning.value = false
        }
    }

    fun songByPath(path: String): Song? = _songs.value?.firstOrNull { it.path == path }

    fun songsByPaths(paths: List<String>): List<Song> {
        val byPath = _songs.value?.associateBy { it.path } ?: return emptyList()
        return paths.mapNotNull { byPath[it] }
    }

    fun songsForAlbum(albumId: Long): List<Song> = _songs.value?.filter { it.albumId == albumId }.orEmpty()

    fun songsForArtist(artistId: Long): List<Song> = _songs.value?.filter { it.artistId == artistId }.orEmpty()

    fun songsForFolder(folderPath: String): List<Song> = _songs.value?.filter { it.folderPath == folderPath }.orEmpty()
}

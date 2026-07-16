package com.lyd.player.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyd.player.core.data.model.Folder
import com.lyd.player.core.data.model.Song
import com.lyd.player.core.data.repo.LibraryRepository
import com.lyd.player.core.data.repo.PlaylistRepository
import com.lyd.player.core.data.repo.search
import com.lyd.player.core.data.repo.toFolders
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CreatePlaylistMode { SONGS, FOLDERS }

@HiltViewModel
class CreatePlaylistViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    val mode = MutableStateFlow(CreatePlaylistMode.SONGS)
    val name = MutableStateFlow("")
    val songSearchQuery = MutableStateFlow("")
    val selectedSongPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolderPaths = MutableStateFlow<Set<String>>(emptySet())

    private val rawSongs: StateFlow<List<Song>?> = libraryRepository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val filteredSongs: StateFlow<List<Song>> = combine(rawSongs, songSearchQuery) { list, q ->
        (list ?: emptyList()).search(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = rawSongs
        .map { it?.toFolders() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canCreate: StateFlow<Boolean> = combine(name, mode, selectedSongPaths, selectedFolderPaths) { n, m, songPaths, folderPaths ->
        n.isNotBlank() && if (m == CreatePlaylistMode.SONGS) songPaths.isNotEmpty() else folderPaths.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleSong(path: String) {
        selectedSongPaths.update { if (path in it) it - path else it + path }
    }

    fun toggleFolder(path: String) {
        selectedFolderPaths.update { if (path in it) it - path else it + path }
    }

    fun create(onDone: (Long) -> Unit) {
        val trimmedName = name.value.trim()
        if (trimmedName.isBlank()) return
        val paths = if (mode.value == CreatePlaylistMode.SONGS) {
            selectedSongPaths.value.toList()
        } else {
            (rawSongs.value ?: emptyList())
                .filter { it.folderPath in selectedFolderPaths.value }
                .map { it.path }
        }
        if (paths.isEmpty()) return
        viewModelScope.launch {
            val id = playlistRepository.createPlaylist(trimmedName, paths.distinct())
            onDone(id)
        }
    }
}

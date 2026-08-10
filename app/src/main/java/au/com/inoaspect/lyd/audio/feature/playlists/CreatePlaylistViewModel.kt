package au.com.inoaspect.lyd.audio.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.inoaspect.lyd.audio.core.data.model.Folder
import au.com.inoaspect.lyd.audio.core.data.model.Song
import au.com.inoaspect.lyd.audio.core.data.repo.LibraryRepository
import au.com.inoaspect.lyd.audio.core.data.repo.PlaylistRepository
import au.com.inoaspect.lyd.audio.core.data.repo.search
import au.com.inoaspect.lyd.audio.core.data.repo.toFolders
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
    private val attemptedCreate = MutableStateFlow(false)

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

    val nameError: StateFlow<String?> = combine(name, attemptedCreate) { n, attempted ->
        if (attempted && n.isBlank()) "Enter a playlist name" else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectionError: StateFlow<String?> = combine(mode, selectedSongPaths, selectedFolderPaths, attemptedCreate) { m, songPaths, folderPaths, attempted ->
        if (!attempted) return@combine null
        when {
            m == CreatePlaylistMode.SONGS && songPaths.isEmpty() -> "Select at least one song"
            m == CreatePlaylistMode.FOLDERS && folderPaths.isEmpty() -> "Select at least one folder"
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleSong(path: String) {
        selectedSongPaths.update { if (path in it) it - path else it + path }
    }

    fun toggleFolder(path: String) {
        selectedFolderPaths.update { if (path in it) it - path else it + path }
    }

    fun create(onDone: (Long) -> Unit) {
        val trimmedName = name.value.trim()
        val paths = if (mode.value == CreatePlaylistMode.SONGS) {
            selectedSongPaths.value.toList()
        } else {
            (rawSongs.value ?: emptyList())
                .filter { it.folderPath in selectedFolderPaths.value }
                .map { it.path }
        }
        if (trimmedName.isBlank() || paths.isEmpty()) {
            attemptedCreate.value = true
            return
        }
        viewModelScope.launch {
            val id = playlistRepository.createPlaylist(trimmedName, paths.distinct())
            onDone(id)
        }
    }
}

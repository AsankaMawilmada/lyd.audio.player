package com.lyd.player.feature.playlists

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyd.player.core.data.model.ResolvedPlaylist
import com.lyd.player.core.data.model.Song
import com.lyd.player.core.data.repo.LibraryRepository
import com.lyd.player.core.data.repo.PlaylistRepository
import com.lyd.player.core.data.repo.search
import com.lyd.player.core.util.M3uPlaylist
import com.lyd.player.nav.Routes
import com.lyd.player.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val playlistId: Long = checkNotNull(savedStateHandle[Routes.PLAYLIST_ID_ARG])

    val resolved: StateFlow<ResolvedPlaylist?> = playlistRepository.observeResolvedPlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // "Add songs" picker state
    val addSongsQuery = MutableStateFlow("")
    val addSongsSelected = MutableStateFlow<Set<String>>(emptySet())

    val addSongsCandidates: StateFlow<List<Song>> = combine(libraryRepository.songs, addSongsQuery, resolved) { all, query, playlist ->
        val existing = playlist?.playlist?.songPaths?.toSet().orEmpty()
        (all ?: emptyList()).search(query).filter { it.path !in existing }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleAddSong(path: String) {
        addSongsSelected.update { if (path in it) it - path else it + path }
    }

    fun confirmAddSongs(onDone: () -> Unit) {
        val paths = addSongsSelected.value.toList()
        if (paths.isEmpty()) {
            onDone()
            return
        }
        viewModelScope.launch {
            playlistRepository.addSongs(playlistId, paths)
            addSongsSelected.value = emptySet()
            addSongsQuery.value = ""
            onDone()
        }
    }

    fun play(startIndex: Int = 0) {
        val r = resolved.value ?: return
        viewModelScope.launch { playerController.playList(r.songs, startIndex, false, r.playlist.name) }
    }

    fun shuffle() {
        val r = resolved.value ?: return
        viewModelScope.launch { playerController.playList(r.songs, 0, true, r.playlist.name) }
    }

    fun removeSong(path: String) {
        viewModelScope.launch { playlistRepository.removeSongAt(playlistId, path) }
    }

    fun moveSong(from: Int, to: Int) {
        val r = resolved.value ?: return
        val paths = r.playlist.songPaths.toMutableList()
        if (from !in paths.indices || to !in paths.indices) return
        val item = paths.removeAt(from)
        paths.add(to, item)
        viewModelScope.launch { playlistRepository.reorder(playlistId, paths) }
    }

    fun rename(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlistRepository.rename(playlistId, name.trim()) }
    }

    fun delete(onDone: () -> Unit) {
        val r = resolved.value ?: return
        if (r.playlist.isFavorites) return
        viewModelScope.launch {
            playlistRepository.delete(playlistId)
            onDone()
        }
    }

    /** Writes the current playlist as M3U8 to an already-opened SAF destination [uri]. */
    fun exportPlaylist(contentResolver: ContentResolver, uri: Uri, onDone: (success: Boolean) -> Unit) {
        val songs = resolved.value?.songs.orEmpty()
        viewModelScope.launch {
            val success = runCatching {
                contentResolver.openOutputStream(uri)?.use { it.write(M3uPlaylist.write(songs).toByteArray()) }
                    ?: error("Could not open destination for writing")
            }.isSuccess
            onDone(success)
        }
    }
}

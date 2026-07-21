package au.com.inoaspect.lyd.audio.feature.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.inoaspect.lyd.audio.core.data.model.Playlist
import au.com.inoaspect.lyd.audio.core.data.repo.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToExisting(playlistId: Long, songPaths: List<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            playlistRepository.addSongs(playlistId, songPaths)
            onDone()
        }
    }

    fun createAndAdd(name: String, songPaths: List<String>, onDone: () -> Unit) {
        if (name.isBlank() || songPaths.isEmpty()) return
        viewModelScope.launch {
            playlistRepository.createPlaylist(name.trim(), songPaths)
            onDone()
        }
    }
}

package com.lyd.player.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyd.player.core.data.repo.LibraryRepository
import com.lyd.player.core.data.repo.PlaylistRepository
import com.lyd.player.playback.PlaybackUiState
import com.lyd.player.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    val playerController: PlayerController,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackUiState> = playerController.uiState

    fun onPermissionGranted() {
        viewModelScope.launch {
            playlistRepository.ensureFavoritesPlaylist()
            if (libraryRepository.songs.value == null) libraryRepository.rescan()
        }
    }
}

package com.lyd.player.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyd.player.core.data.model.Playlist
import com.lyd.player.core.data.model.Song
import com.lyd.player.core.data.repo.LibraryRepository
import com.lyd.player.core.data.repo.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PlaylistWithResolvedSongs(val playlist: Playlist, val songs: List<Song>)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    playlistRepository: PlaylistRepository,
    libraryRepository: LibraryRepository,
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistWithResolvedSongs>> =
        combine(playlistRepository.observePlaylists(), libraryRepository.songs) { playlists, _ ->
            playlists.map { PlaylistWithResolvedSongs(it, libraryRepository.songsByPaths(it.songPaths)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

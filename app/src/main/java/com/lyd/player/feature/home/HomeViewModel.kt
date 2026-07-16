package com.lyd.player.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyd.player.core.data.model.Song
import com.lyd.player.core.data.repo.LibraryRepository
import com.lyd.player.core.data.repo.RecentPlaysRepository
import com.lyd.player.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val SOURCE_ALL_SONGS = "All songs"
const val SOURCE_RECENTLY_PLAYED = "Recently played"
const val SOURCE_SEARCH_RESULTS = "Search results"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    recentPlaysRepository: RecentPlaysRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    val songs: StateFlow<List<Song>?> = libraryRepository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentSongs: StateFlow<List<Song>> = recentPlaysRepository.observeRecentSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isScanning: StateFlow<Boolean> = libraryRepository.isScanning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun rescan() {
        viewModelScope.launch { libraryRepository.rescan() }
    }

    fun playAllSongs(shuffled: Boolean = false) {
        val list = songs.value.orEmpty()
        if (list.isEmpty()) return
        viewModelScope.launch { playerController.playList(list, 0, shuffled, SOURCE_ALL_SONGS) }
    }

    fun playSong(song: Song) {
        val list = songs.value.orEmpty()
        val index = list.indexOf(song).coerceAtLeast(0)
        viewModelScope.launch { playerController.playList(list, index, false, SOURCE_ALL_SONGS) }
    }

    fun playRecent(song: Song) {
        val list = recentSongs.value
        val index = list.indexOf(song).coerceAtLeast(0)
        viewModelScope.launch { playerController.playList(list, index, false, SOURCE_RECENTLY_PLAYED) }
    }
}

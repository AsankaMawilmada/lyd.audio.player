package au.com.inoaspect.lyd.audio.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.inoaspect.lyd.audio.core.data.model.Song
import au.com.inoaspect.lyd.audio.core.data.repo.LibraryRepository
import au.com.inoaspect.lyd.audio.core.data.repo.search
import au.com.inoaspect.lyd.audio.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val SOURCE_SEARCH = "Search results"

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    val query = MutableStateFlow("")

    private val rawSongs = libraryRepository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val results: StateFlow<List<Song>> = combine(rawSongs, query) { list, q ->
        if (q.isBlank()) emptyList() else (list ?: emptyList()).search(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playResult(song: Song) {
        val list = results.value
        val index = list.indexOf(song).coerceAtLeast(0)
        viewModelScope.launch { playerController.playList(list, index, false, SOURCE_SEARCH) }
    }
}

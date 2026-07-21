package au.com.inoaspect.lyd.audio.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.inoaspect.lyd.audio.core.data.model.Album
import au.com.inoaspect.lyd.audio.core.data.model.Artist
import au.com.inoaspect.lyd.audio.core.data.model.Folder
import au.com.inoaspect.lyd.audio.core.data.model.Song
import au.com.inoaspect.lyd.audio.core.data.model.SongSortOrder
import au.com.inoaspect.lyd.audio.core.data.repo.LibraryRepository
import au.com.inoaspect.lyd.audio.core.data.repo.search
import au.com.inoaspect.lyd.audio.core.data.repo.sortedWith
import au.com.inoaspect.lyd.audio.core.data.repo.toAlbums
import au.com.inoaspect.lyd.audio.core.data.repo.toArtists
import au.com.inoaspect.lyd.audio.core.data.repo.toFolders
import au.com.inoaspect.lyd.audio.nav.Routes
import au.com.inoaspect.lyd.audio.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibrarySection { SONGS, ALBUMS, ARTISTS, FOLDERS }

const val SOURCE_ALL_SONGS_LIBRARY = "All songs"

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialTab = savedStateHandle.get<String>(Routes.LIBRARY_TAB_ARG) ?: "songs"

    val section = MutableStateFlow(
        when (initialTab) {
            "albums" -> LibrarySection.ALBUMS
            "artists" -> LibrarySection.ARTISTS
            "folders" -> LibrarySection.FOLDERS
            else -> LibrarySection.SONGS
        },
    )

    val sortOrder = MutableStateFlow(SongSortOrder.TITLE)
    val filterQuery = MutableStateFlow("")

    val isScanning: StateFlow<Boolean> = libraryRepository.isScanning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val rawSongs: StateFlow<List<Song>?> = libraryRepository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val songs: StateFlow<List<Song>> = combine(rawSongs, sortOrder, filterQuery) { list, order, query ->
        (list ?: emptyList()).search(query).sortedWith(order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = rawSongs
        .map { it?.toAlbums() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = rawSongs
        .map { it?.toArtists() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = rawSongs
        .map { it?.toFolders() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun rescan() {
        viewModelScope.launch { libraryRepository.rescan() }
    }

    fun playAllSongs(shuffled: Boolean = false) {
        val list = songs.value
        if (list.isEmpty()) return
        viewModelScope.launch { playerController.playList(list, 0, shuffled, SOURCE_ALL_SONGS_LIBRARY) }
    }

    fun playSong(song: Song) {
        val list = songs.value
        val index = list.indexOf(song).coerceAtLeast(0)
        viewModelScope.launch { playerController.playList(list, index, false, SOURCE_ALL_SONGS_LIBRARY) }
    }
}

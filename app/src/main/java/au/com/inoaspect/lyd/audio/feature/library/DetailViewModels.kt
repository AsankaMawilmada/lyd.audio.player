package au.com.inoaspect.lyd.audio.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.inoaspect.lyd.audio.core.data.model.Song
import au.com.inoaspect.lyd.audio.core.data.repo.LibraryRepository
import au.com.inoaspect.lyd.audio.nav.Routes
import au.com.inoaspect.lyd.audio.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun playAndShuffle(scope: kotlinx.coroutines.CoroutineScope, controller: PlayerController, songs: List<Song>, source: String, startIndex: Int = 0, shuffled: Boolean = false) {
    if (songs.isEmpty()) return
    scope.launch { controller.playList(songs, startIndex, shuffled, source) }
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val albumId: Long = checkNotNull(savedStateHandle[Routes.ALBUM_ID_ARG])

    val songs: StateFlow<List<Song>> = libraryRepository.songs
        .map { it?.filter { s -> s.albumId == albumId } ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumName: StateFlow<String> = songs.map { it.firstOrNull()?.album ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val artistName: StateFlow<String> = songs.map { it.firstOrNull()?.artist ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun play(startIndex: Int = 0) = playAndShuffle(viewModelScope, playerController, songs.value, albumName.value, startIndex)
    fun shuffle() = playAndShuffle(viewModelScope, playerController, songs.value, albumName.value, 0, true)
}

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId: Long = checkNotNull(savedStateHandle[Routes.ARTIST_ID_ARG])

    val songs: StateFlow<List<Song>> = libraryRepository.songs
        .map { it?.filter { s -> s.artistId == artistId } ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artistName: StateFlow<String> = songs.map { it.firstOrNull()?.artist ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun play(startIndex: Int = 0) = playAndShuffle(viewModelScope, playerController, songs.value, artistName.value, startIndex)
    fun shuffle() = playAndShuffle(viewModelScope, playerController, songs.value, artistName.value, 0, true)
}

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val folderPath: String = Routes.decodeFolderPath(checkNotNull(savedStateHandle[Routes.FOLDER_PATH_ARG]))

    val songs: StateFlow<List<Song>> = libraryRepository.songs
        .map { it?.filter { s -> s.folderPath == folderPath } ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folderName: String = folderPath.substringAfterLast('/').ifBlank { "/" }

    fun play(startIndex: Int = 0) = playAndShuffle(viewModelScope, playerController, songs.value, folderName, startIndex)
    fun shuffle() = playAndShuffle(viewModelScope, playerController, songs.value, folderName, 0, true)
}

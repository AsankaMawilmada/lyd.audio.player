package au.com.inoaspect.lyd.audio.feature.common

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.inoaspect.lyd.audio.core.data.mediastore.DeleteResult
import au.com.inoaspect.lyd.audio.core.data.mediastore.SongDeleter
import au.com.inoaspect.lyd.audio.core.data.model.Song
import au.com.inoaspect.lyd.audio.core.data.repo.LibraryRepository
import au.com.inoaspect.lyd.audio.core.data.repo.PlaylistRepository
import au.com.inoaspect.lyd.audio.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeleteConsentRequest(
    val song: Song,
    val intentSender: IntentSender,
    val retryAfterConsent: Boolean,
)

@HiltViewModel
class SongActionsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val playerController: PlayerController,
    private val libraryRepository: LibraryRepository,
    private val songDeleter: SongDeleter,
) : ViewModel() {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private var observedPath: String? = null

    private val _deleteConsentRequest = MutableSharedFlow<DeleteConsentRequest>(extraBufferCapacity = 1)
    val deleteConsentRequest: SharedFlow<DeleteConsentRequest> = _deleteConsentRequest.asSharedFlow()

    fun observeFavorite(song: Song) {
        if (observedPath == song.path) return
        observedPath = song.path
        viewModelScope.launch { _isFavorite.value = playlistRepository.isFavorite(song.path) }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            playlistRepository.toggleFavorite(song.path)
            _isFavorite.value = playlistRepository.isFavorite(song.path)
        }
    }

    fun playNext(song: Song) {
        viewModelScope.launch { playerController.playNext(song) }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch { playerController.appendToQueue(song) }
    }

    /** Call only after the user has confirmed the destructive action in a confirmation dialog. */
    fun deleteSong(song: Song) {
        viewModelScope.launch {
            when (val result = songDeleter.delete(song)) {
                DeleteResult.Deleted -> onDeleteSucceeded(song)
                is DeleteResult.NeedsConsent ->
                    _deleteConsentRequest.tryEmit(DeleteConsentRequest(song, result.intentSender, result.retryAfterConsent))
                DeleteResult.Failed -> Unit
            }
        }
    }

    /** Call with the result of launching [DeleteConsentRequest.intentSender]. */
    fun onDeleteConsentResult(request: DeleteConsentRequest, granted: Boolean) {
        if (!granted) return
        viewModelScope.launch {
            if (!request.retryAfterConsent || songDeleter.retryAfterConsent(request.song)) {
                onDeleteSucceeded(request.song)
            }
        }
    }

    private suspend fun onDeleteSucceeded(song: Song) {
        playerController.removeFromQueueByPath(song.path)
        libraryRepository.rescan()
    }
}

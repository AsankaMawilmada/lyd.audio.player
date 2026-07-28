package au.com.inoaspect.lyd.audio.feature.nowplaying

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.inoaspect.lyd.audio.core.data.lyrics.LrcParser
import au.com.inoaspect.lyd.audio.core.data.lyrics.LyricsRepository
import au.com.inoaspect.lyd.audio.core.data.lyrics.LyricsResult
import au.com.inoaspect.lyd.audio.core.data.mediastore.DeleteResult
import au.com.inoaspect.lyd.audio.core.data.mediastore.LyricsFileDeleter
import au.com.inoaspect.lyd.audio.core.data.repo.PlaylistRepository
import au.com.inoaspect.lyd.audio.playback.PlaybackUiState
import au.com.inoaspect.lyd.audio.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface LyricsUiState {
    data object Loading : LyricsUiState
    data class Synced(val lines: List<au.com.inoaspect.lyd.audio.core.data.lyrics.LyricLine>) : LyricsUiState
    data class Plain(val text: String) : LyricsUiState
    data object Empty : LyricsUiState
}

data class LyricsDeleteConsentRequest(
    val lrcPath: String,
    val intentSender: IntentSender,
    val retryAfterConsent: Boolean,
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    val playerController: PlayerController,
    private val playlistRepository: PlaylistRepository,
    private val lyricsRepository: LyricsRepository,
    private val lyricsFileDeleter: LyricsFileDeleter,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackUiState> = playerController.uiState

    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Empty)
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    /** The current song's sidecar .lrc path, or null if it has none (nothing to delete). */
    private val _lyricsFilePath = MutableStateFlow<String?>(null)
    val lyricsFilePath: StateFlow<String?> = _lyricsFilePath.asStateFlow()

    private val _lyricsDeleteConsentRequest = MutableSharedFlow<LyricsDeleteConsentRequest>(extraBufferCapacity = 1)
    val lyricsDeleteConsentRequest: SharedFlow<LyricsDeleteConsentRequest> = _lyricsDeleteConsentRequest.asSharedFlow()

    init {
        viewModelScope.launch {
            playbackState.map { it.currentItem?.path }.distinctUntilChanged().collect { path ->
                if (path == null) {
                    _lyricsState.value = LyricsUiState.Empty
                    _lyricsFilePath.value = null
                    _isFavorite.value = false
                } else {
                    _isFavorite.value = playlistRepository.isFavorite(path)
                    fetchLyrics(path)
                }
            }
        }
    }

    private suspend fun fetchLyrics(path: String) {
        _lyricsState.value = LyricsUiState.Loading
        val result = lyricsRepository.fetch(path)
        val filePath = withContext(Dispatchers.IO) { lyricsFileDeleter.lyricsFilePath(path) }
        if (playbackState.value.currentItem?.path != path) return // track changed while fetching
        _lyricsState.value = when (result) {
            is LyricsResult.Synced -> LyricsUiState.Synced(result.lines)
            is LyricsResult.Plain -> LyricsUiState.Plain(result.text)
            LyricsResult.NotFound -> LyricsUiState.Empty
        }
        _lyricsFilePath.value = filePath
    }

    fun toggleFavorite() {
        val path = playbackState.value.currentItem?.path ?: return
        viewModelScope.launch {
            playlistRepository.toggleFavorite(path)
            _isFavorite.value = playlistRepository.isFavorite(path)
        }
    }

    fun activeLyricLineIndex(lines: List<au.com.inoaspect.lyd.audio.core.data.lyrics.LyricLine>): Int =
        LrcParser.activeLineIndex(lines, playbackState.value.positionMs)

    /** Call only after the user has confirmed the destructive action in a confirmation dialog. */
    fun deleteLyricsFile() {
        val path = _lyricsFilePath.value ?: return
        viewModelScope.launch {
            when (val result = lyricsFileDeleter.delete(path)) {
                DeleteResult.Deleted -> onLyricsFileDeleted()
                is DeleteResult.NeedsConsent ->
                    _lyricsDeleteConsentRequest.tryEmit(LyricsDeleteConsentRequest(path, result.intentSender, result.retryAfterConsent))
                DeleteResult.Failed -> Unit
            }
        }
    }

    /** Call with the result of launching [LyricsDeleteConsentRequest.intentSender]. */
    fun onLyricsDeleteConsentResult(request: LyricsDeleteConsentRequest, granted: Boolean) {
        if (!granted) return
        viewModelScope.launch {
            if (!request.retryAfterConsent || lyricsFileDeleter.retryAfterConsent(request.lrcPath)) {
                onLyricsFileDeleted()
            }
        }
    }

    private fun onLyricsFileDeleted() {
        _lyricsFilePath.value = null
        _lyricsState.value = LyricsUiState.Empty
    }
}

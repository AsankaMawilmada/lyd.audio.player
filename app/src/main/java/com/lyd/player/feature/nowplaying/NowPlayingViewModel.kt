package com.lyd.player.feature.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyd.player.core.data.lyrics.LrcParser
import com.lyd.player.core.data.lyrics.LyricsRepository
import com.lyd.player.core.data.lyrics.LyricsResult
import com.lyd.player.core.data.repo.PlaylistRepository
import com.lyd.player.playback.PlaybackUiState
import com.lyd.player.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LyricsUiState {
    data object Loading : LyricsUiState
    data class Synced(val lines: List<com.lyd.player.core.data.lyrics.LyricLine>) : LyricsUiState
    data class Plain(val text: String) : LyricsUiState
    data object Empty : LyricsUiState
}

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    val playerController: PlayerController,
    private val playlistRepository: PlaylistRepository,
    private val lyricsRepository: LyricsRepository,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackUiState> = playerController.uiState

    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Empty)
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    init {
        viewModelScope.launch {
            playbackState.map { it.currentItem?.path }.distinctUntilChanged().collect { path ->
                if (path == null) {
                    _lyricsState.value = LyricsUiState.Empty
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
        if (playbackState.value.currentItem?.path != path) return // track changed while fetching
        _lyricsState.value = when (result) {
            is LyricsResult.Synced -> LyricsUiState.Synced(result.lines)
            is LyricsResult.Plain -> LyricsUiState.Plain(result.text)
            LyricsResult.NotFound -> LyricsUiState.Empty
        }
    }

    fun toggleFavorite() {
        val path = playbackState.value.currentItem?.path ?: return
        viewModelScope.launch {
            playlistRepository.toggleFavorite(path)
            _isFavorite.value = playlistRepository.isFavorite(path)
        }
    }

    fun activeLyricLineIndex(lines: List<com.lyd.player.core.data.lyrics.LyricLine>): Int =
        LrcParser.activeLineIndex(lines, playbackState.value.positionMs)
}

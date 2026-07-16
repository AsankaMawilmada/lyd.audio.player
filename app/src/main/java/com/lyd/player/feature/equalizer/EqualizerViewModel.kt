package com.lyd.player.feature.equalizer

import androidx.lifecycle.ViewModel
import com.lyd.player.playback.EqualizerController
import com.lyd.player.playback.EqualizerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val equalizerController: EqualizerController,
) : ViewModel() {

    val state: StateFlow<EqualizerUiState> = equalizerController.state

    fun setEnabled(enabled: Boolean) = equalizerController.setEnabled(enabled)
    fun setBandLevel(index: Int, level: Int) = equalizerController.setBandLevel(index, level)
    fun applyPreset(name: String) = equalizerController.applyPreset(name)
}

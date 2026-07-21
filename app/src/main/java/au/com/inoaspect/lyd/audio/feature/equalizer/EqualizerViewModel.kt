package au.com.inoaspect.lyd.audio.feature.equalizer

import androidx.lifecycle.ViewModel
import au.com.inoaspect.lyd.audio.playback.EqualizerController
import au.com.inoaspect.lyd.audio.playback.EqualizerUiState
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

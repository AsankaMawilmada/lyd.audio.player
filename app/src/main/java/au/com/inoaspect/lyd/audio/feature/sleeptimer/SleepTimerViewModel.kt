package au.com.inoaspect.lyd.audio.feature.sleeptimer

import androidx.lifecycle.ViewModel
import au.com.inoaspect.lyd.audio.playback.SleepTimerController
import au.com.inoaspect.lyd.audio.playback.SleepTimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SleepTimerViewModel @Inject constructor(
    private val sleepTimerController: SleepTimerController,
) : ViewModel() {

    val state: StateFlow<SleepTimerState> = sleepTimerController.state

    fun startCountdown(minutes: Int) = sleepTimerController.startCountdown(minutes * 60_000L)
    fun startEndOfTrack() = sleepTimerController.startEndOfTrack()
    fun cancel() = sleepTimerController.cancel()
}

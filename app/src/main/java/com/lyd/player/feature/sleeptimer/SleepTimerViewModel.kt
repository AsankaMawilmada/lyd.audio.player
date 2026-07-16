package com.lyd.player.feature.sleeptimer

import androidx.lifecycle.ViewModel
import com.lyd.player.playback.SleepTimerController
import com.lyd.player.playback.SleepTimerState
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

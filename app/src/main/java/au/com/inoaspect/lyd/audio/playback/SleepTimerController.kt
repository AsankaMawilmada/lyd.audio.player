package au.com.inoaspect.lyd.audio.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SleepTimerState {
    data object Off : SleepTimerState
    data class Countdown(val remainingMs: Long) : SleepTimerState
    data object EndOfTrack : SleepTimerState
}

/**
 * Two mutually-exclusive sleep-timer modes. This controller only owns the *timing* state;
 * it emits a fire-and-forget [fireEvents] pulse when playback should be paused, which
 * [PlayerController] observes so this class never needs a direct player reference.
 */
@Singleton
class SleepTimerController @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private val _fireEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fireEvents: SharedFlow<Unit> = _fireEvents.asSharedFlow()

    fun startCountdown(durationMs: Long) {
        tickJob?.cancel()
        val endAt = System.currentTimeMillis() + durationMs
        _state.value = SleepTimerState.Countdown(durationMs)
        tickJob = scope.launch {
            while (isActive) {
                val remaining = endAt - System.currentTimeMillis()
                if (remaining <= 0) {
                    _state.value = SleepTimerState.Off
                    _fireEvents.tryEmit(Unit)
                    break
                }
                _state.value = SleepTimerState.Countdown(remaining)
                delay(500)
            }
        }
    }

    fun startEndOfTrack() {
        tickJob?.cancel()
        _state.value = SleepTimerState.EndOfTrack
    }

    fun cancel() {
        tickJob?.cancel()
        _state.value = SleepTimerState.Off
    }

    /** Called by [PlaybackService] whenever the current media item transitions. */
    fun onTrackChanged() {
        if (_state.value is SleepTimerState.EndOfTrack) {
            _state.value = SleepTimerState.Off
            _fireEvents.tryEmit(Unit)
        }
    }
}

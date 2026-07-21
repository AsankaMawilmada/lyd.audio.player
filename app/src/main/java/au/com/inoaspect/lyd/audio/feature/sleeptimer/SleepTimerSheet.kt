@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package au.com.inoaspect.lyd.audio.feature.sleeptimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.inoaspect.lyd.audio.core.design.LydColors
import au.com.inoaspect.lyd.audio.core.design.LydSpacing
import au.com.inoaspect.lyd.audio.core.design.LydType
import au.com.inoaspect.lyd.audio.core.design.PillButton
import au.com.inoaspect.lyd.audio.core.util.formatDurationMs
import au.com.inoaspect.lyd.audio.playback.SleepTimerState

private val DURATION_OPTIONS_MINUTES = listOf(5, 15, 30, 45, 60, 90)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimerSheet(onDismiss: () -> Unit, viewModel: SleepTimerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = LydColors.SurfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = LydSpacing.lg, vertical = LydSpacing.md)) {
            Text("Sleep timer", style = LydType.headlineLgMobile, color = LydColors.OnSurface)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.md))

            when (val s = state) {
                is SleepTimerState.Countdown -> {
                    Text(
                        "Stopping in ${formatDurationMs(s.remainingMs)}",
                        style = LydType.headlineMdMobile,
                        color = LydColors.Secondary,
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.md))
                    PillButton(text = "Cancel", filled = false) { viewModel.cancel() }
                }
                SleepTimerState.EndOfTrack -> {
                    Text("Stopping at end of track", style = LydType.headlineMdMobile, color = LydColors.Secondary)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.md))
                    PillButton(text = "Cancel", filled = false) { viewModel.cancel() }
                }
                SleepTimerState.Off -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm)) {
                        DURATION_OPTIONS_MINUTES.forEach { minutes ->
                            PillButton(text = "${minutes}m", filled = false) { viewModel.startCountdown(minutes) }
                        }
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.md))
                    PillButton(text = "Stop at end of track") { viewModel.startEndOfTrack() }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = LydSpacing.lg))
        }
    }
}

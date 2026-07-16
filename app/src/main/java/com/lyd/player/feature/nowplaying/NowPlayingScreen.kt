package com.lyd.player.feature.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydShapes
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.LydType
import com.lyd.player.core.design.TopBarIconAction
import com.lyd.player.feature.common.rememberArtFile
import com.lyd.player.feature.sleeptimer.SleepTimerSheet

private enum class NowPlayingPane { PLAYER, LYRICS, QUEUE }

@Composable
fun NowPlayingScreen(
    onCollapse: () -> Unit,
    onOpenEqualizer: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    var pane by remember { mutableStateOf(NowPlayingPane.PLAYER) }
    var overflowOpen by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    val actionsState = com.lyd.player.feature.common.rememberSongActionsState()

    val artFile = playbackState.currentItem?.artworkUri

    Column(Modifier.fillMaxSize().background(LydColors.Background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            TopBarIconAction(Icons.Filled.ExpandMore, "Collapse", onCollapse)
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("LYD", style = LydType.labelSm, color = LydColors.Secondary)
                Text("Now Playing", style = LydType.headlineMdMobile, color = LydColors.OnSurface)
            }
            Box {
                TopBarIconAction(Icons.Filled.MoreVert, "More") { overflowOpen = true }
                DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Equalizer") },
                        leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Equalizer, contentDescription = null) },
                        onClick = { overflowOpen = false; onOpenEqualizer() },
                    )
                    DropdownMenuItem(
                        text = { Text("Sleep timer") },
                        leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Timer, contentDescription = null) },
                        onClick = { overflowOpen = false; showSleepTimer = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            overflowOpen = false
                            playbackState.currentItem?.path?.let { actionsState.addToPlaylistPaths = listOf(it) }
                        },
                    )
                }
            }
        }

        PaneSegmentedControl(pane = pane, onSelect = { pane = it })

        Box(Modifier.fillMaxSize().padding(top = LydSpacing.md)) {
            when (pane) {
                NowPlayingPane.PLAYER -> PlayerPane(
                    state = playbackState,
                    artFile = artFile,
                    isFavorite = isFavorite,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onOpenSongMenu = {
                        playbackState.currentItem?.let { actionsState.menuSong = it.toSong() }
                    },
                    onSeek = viewModel.playerController::seekTo,
                    onTogglePlayPause = viewModel.playerController::togglePlayPause,
                    onNext = viewModel.playerController::next,
                    onPrevious = viewModel.playerController::smartPrevious,
                    onToggleShuffle = viewModel.playerController::toggleShuffle,
                    onCycleRepeat = viewModel.playerController::cycleRepeat,
                )
                NowPlayingPane.LYRICS -> LyricsPane(
                    state = lyricsState,
                    activeLineIndex = (lyricsState as? LyricsUiState.Synced)?.let { viewModel.activeLyricLineIndex(it.lines) } ?: -1,
                    modifier = Modifier.fillMaxSize(),
                )
                NowPlayingPane.QUEUE -> QueuePane(
                    queue = playbackState.queue,
                    currentIndex = playbackState.currentIndex,
                    onJumpTo = viewModel.playerController::seekToIndex,
                    onMove = viewModel.playerController::moveInQueue,
                    onRemove = viewModel.playerController::removeFromQueue,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    com.lyd.player.feature.common.SongActionsHost(actionsState)
    if (showSleepTimer) {
        SleepTimerSheet(onDismiss = { showSleepTimer = false })
    }
}

@Composable
private fun PaneSegmentedControl(pane: NowPlayingPane, onSelect: (NowPlayingPane) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea),
        horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm),
    ) {
        listOf(
            NowPlayingPane.PLAYER to "Player",
            NowPlayingPane.LYRICS to "Lyrics",
            NowPlayingPane.QUEUE to "Queue",
        ).forEach { (value, label) ->
            val selected = value == pane
            Text(
                text = label,
                style = LydType.labelSm,
                color = if (selected) LydColors.OnSurface else LydColors.OnSurfaceVariant,
                modifier = Modifier
                    .clickable { onSelect(value) }
                    .background(if (selected) LydColors.SecondaryContainer else LydColors.SurfaceContainer, LydShapes.full)
                    .padding(horizontal = LydSpacing.md, vertical = LydSpacing.sm),
            )
        }
    }
}

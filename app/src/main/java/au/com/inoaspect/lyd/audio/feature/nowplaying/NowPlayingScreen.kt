package au.com.inoaspect.lyd.audio.feature.nowplaying

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.inoaspect.lyd.audio.core.design.LydColors
import au.com.inoaspect.lyd.audio.core.design.LydShapes
import au.com.inoaspect.lyd.audio.core.design.LydSpacing
import au.com.inoaspect.lyd.audio.core.design.LydType
import au.com.inoaspect.lyd.audio.core.design.PillButton
import au.com.inoaspect.lyd.audio.core.design.TopBarIconAction
import au.com.inoaspect.lyd.audio.feature.common.rememberArtFile
import au.com.inoaspect.lyd.audio.feature.sleeptimer.SleepTimerSheet

private enum class NowPlayingPane { PLAYER, LYRICS, QUEUE }

@Composable
fun NowPlayingScreen(
    onCollapse: () -> Unit,
    onOpenEqualizer: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
    val lyricsFilePath by viewModel.lyricsFilePath.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    var pane by remember { mutableStateOf(NowPlayingPane.PLAYER) }
    var overflowOpen by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var confirmingLyricsDelete by remember { mutableStateOf(false) }
    val actionsState = au.com.inoaspect.lyd.audio.feature.common.rememberSongActionsState()

    val showLyricsTab = lyricsState != LyricsUiState.Empty
    LaunchedEffect(showLyricsTab) {
        if (!showLyricsTab && pane == NowPlayingPane.LYRICS) pane = NowPlayingPane.PLAYER
    }

    var pendingLyricsConsent by remember { mutableStateOf<LyricsDeleteConsentRequest?>(null) }
    val lyricsDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        pendingLyricsConsent?.let { viewModel.onLyricsDeleteConsentResult(it, result.resultCode == Activity.RESULT_OK) }
        pendingLyricsConsent = null
    }
    LaunchedEffect(viewModel) {
        viewModel.lyricsDeleteConsentRequest.collect { request ->
            pendingLyricsConsent = request
            lyricsDeleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        }
    }

    val artFile = playbackState.currentItem?.artworkUri

    Column(Modifier.fillMaxSize().background(LydColors.Background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            TopBarIconAction(Icons.Filled.ExpandMore, "Collapse", onCollapse)
            Text("Now Playing", style = LydType.headlineMdMobile, color = LydColors.OnSurface)
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
                    if (lyricsFilePath != null) {
                        DropdownMenuItem(
                            text = { Text("Delete lyrics file", color = LydColors.Error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = LydColors.Error) },
                            onClick = {
                                overflowOpen = false
                                confirmingLyricsDelete = true
                            },
                        )
                    }
                }
            }
        }

        PaneSegmentedControl(pane = pane, showLyrics = showLyricsTab, onSelect = { pane = it })

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

    au.com.inoaspect.lyd.audio.feature.common.SongActionsHost(actionsState)
    if (showSleepTimer) {
        SleepTimerSheet(onDismiss = { showSleepTimer = false })
    }
    if (confirmingLyricsDelete) {
        AlertDialog(
            onDismissRequest = { confirmingLyricsDelete = false },
            containerColor = LydColors.SurfaceContainer,
            title = { Text("Delete lyrics file?", color = LydColors.OnSurface) },
            text = {
                Text(
                    "The lyrics file for \"${playbackState.currentItem?.title.orEmpty()}\" will be permanently deleted from this device. This can't be undone.",
                    color = LydColors.OnSurfaceVariant,
                )
            },
            confirmButton = {
                PillButton(text = "Delete") {
                    confirmingLyricsDelete = false
                    viewModel.deleteLyricsFile()
                }
            },
            dismissButton = { PillButton(text = "Cancel", filled = false) { confirmingLyricsDelete = false } },
        )
    }
}

@Composable
private fun PaneSegmentedControl(pane: NowPlayingPane, showLyrics: Boolean, onSelect: (NowPlayingPane) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea),
        horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm),
    ) {
        buildList {
            add(NowPlayingPane.PLAYER to "Player")
            if (showLyrics) add(NowPlayingPane.LYRICS to "Lyrics")
            add(NowPlayingPane.QUEUE to "Queue")
        }.forEach { (value, label) ->
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

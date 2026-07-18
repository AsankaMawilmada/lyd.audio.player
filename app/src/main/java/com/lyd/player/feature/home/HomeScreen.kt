package com.lyd.player.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.lyd.player.core.data.model.Song
import com.lyd.player.core.design.ArtThumbnail
import com.lyd.player.core.design.EmptyState
import com.lyd.player.core.design.FullScreenLoading
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydShapes
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.LydType
import com.lyd.player.core.design.PillButton
import com.lyd.player.core.design.RootTitleBar
import com.lyd.player.core.design.SectionHeader
import com.lyd.player.core.design.SongRow
import com.lyd.player.core.design.TopBarIconAction
import com.lyd.player.feature.common.SongActionsHost
import com.lyd.player.feature.common.rememberArtFile
import com.lyd.player.feature.common.rememberSongActionsState

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenLibrary: (String) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsState()
    val recentSongs by viewModel.recentSongs.collectAsState()
    val actionsState = rememberSongActionsState()
    var overflowOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        RootTitleBar(
            title = "Listen",
            actions = {
                TopBarIconAction(Icons.Filled.Search, "Search", onOpenSearch)
                Box {
                    TopBarIconAction(Icons.Filled.MoreVert, "Menu") { overflowOpen = true }
                    androidx.compose.material3.DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Sort songs") },
                            onClick = { overflowOpen = false; onOpenLibrary("songs") },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Equalizer") },
                            onClick = { overflowOpen = false; onOpenEqualizer() },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Sleep timer") },
                            onClick = { overflowOpen = false; onOpenSleepTimer() },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Rescan library") },
                            onClick = { overflowOpen = false; viewModel.rescan() },
                        )
                    }
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = LydSpacing.safeArea, vertical = LydSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LydSpacing.lg),
        ) {
            if (recentSongs.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(LydSpacing.md)) {
                        SectionHeader("Recently Played")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(LydSpacing.lg)) {
                            items(recentSongs, key = { it.path }) { song ->
                                RecentSongCard(song, onClick = { viewModel.playRecent(song) })
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm)) {
                    PillButton(text = "Albums", icon = Icons.Filled.Album, filled = false, onClick = { onOpenLibrary("albums") })
                    PillButton(text = "Artists", icon = Icons.Filled.Person, filled = false, onClick = { onOpenLibrary("artists") })
                    PillButton(text = "Folders", icon = Icons.Filled.Folder, filled = false, onClick = { onOpenLibrary("folders") })
                }
            }

            item {
                SectionHeader(
                    title = "All Songs",
                    actionLabel = "See all",
                    onActionClick = { onOpenLibrary("songs") },
                    trailing = {
                        IconButton(onClick = { viewModel.playAllSongs(shuffled = true) }) {
                            Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle all", tint = LydColors.OnSurfaceVariant)
                        }
                    },
                )
            }

            val currentSongs = songs
            when {
                currentSongs == null -> item { FullScreenLoading(Modifier.fillMaxWidth()) }
                currentSongs.isEmpty() -> item { EmptyState("No songs found on this device yet.") }
                else -> items(currentSongs.take(6), key = { it.path }) { song ->
                    val artFile = rememberArtFile(song.albumId, song.mediaStoreId)
                    SongRow(
                        song = song,
                        artFile = artFile,
                        isActive = false,
                        onClick = { viewModel.playSong(song) },
                        onMenuClick = { actionsState.menuSong = song },
                    )
                }
            }
        }
    }
    SongActionsHost(actionsState)
}

@Composable
private fun RecentSongCard(song: Song, onClick: () -> Unit) {
    val artFile = rememberArtFile(song.albumId, song.mediaStoreId)
    Column(modifier = Modifier.width(160.dp).clickable(onClick = onClick)) {
        ArtThumbnail(
            artFile,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = LydShapes.default,
        )
        Text(song.title, style = LydType.headlineMdMobile, color = LydColors.OnSurface, maxLines = 1)
        Text(song.artist, style = LydType.bodyMd, color = LydColors.OnSurfaceVariant, maxLines = 1)
    }
}

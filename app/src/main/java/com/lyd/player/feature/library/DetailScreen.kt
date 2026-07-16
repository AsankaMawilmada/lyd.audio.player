package com.lyd.player.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.lyd.player.core.data.model.Song
import com.lyd.player.core.design.EmptyState
import com.lyd.player.core.design.GlassTopBar
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.LydType
import com.lyd.player.core.design.PillButton
import com.lyd.player.core.design.SongRow
import com.lyd.player.core.design.TopBarIconAction
import com.lyd.player.core.util.formatTrackCount
import com.lyd.player.feature.common.SongActionsHost
import com.lyd.player.feature.common.rememberArtFile
import com.lyd.player.feature.common.rememberSongActionsState

@Composable
private fun DetailScreenShell(
    title: String,
    subtitle: String,
    songs: List<Song>,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
) {
    val actionsState = rememberSongActionsState()
    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = title,
            navigationIcon = { TopBarIconAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) },
        )
        if (songs.isEmpty()) {
            EmptyState("No songs here.")
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = LydSpacing.safeArea, vertical = LydSpacing.md)) {
                item {
                    Column {
                        Text(subtitle, style = LydType.bodyMd, color = LydColors.OnSurfaceVariant)
                        Text(formatTrackCount(songs.size), style = LydType.bodyMd, color = LydColors.OnSurfaceVariant)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = LydSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm),
                        ) {
                            PillButton(text = "Play", icon = Icons.Filled.PlayArrow, onClick = { onPlay(0) })
                            PillButton(text = "Shuffle", icon = Icons.Filled.Shuffle, filled = false, onClick = onShuffle)
                            PillButton(
                                text = "Add all",
                                icon = Icons.Filled.PlaylistAdd,
                                filled = false,
                                onClick = { actionsState.addToPlaylistPaths = songs.map { it.path } },
                            )
                        }
                    }
                }
                items(songs, key = { it.path }) { song ->
                    val artFile = rememberArtFile(song.albumId, song.mediaStoreId)
                    SongRow(
                        song = song,
                        artFile = artFile,
                        isActive = false,
                        onClick = { onPlay(songs.indexOf(song)) },
                        onMenuClick = { actionsState.menuSong = song },
                    )
                }
            }
        }
    }
    SongActionsHost(actionsState)
}

@Composable
fun AlbumDetailScreen(onBack: () -> Unit, viewModel: AlbumDetailViewModel = hiltViewModel()) {
    val songs by viewModel.songs.collectAsState()
    val albumName by viewModel.albumName.collectAsState()
    val artistName by viewModel.artistName.collectAsState()
    DetailScreenShell(
        title = albumName,
        subtitle = artistName,
        songs = songs,
        onBack = onBack,
        onPlay = viewModel::play,
        onShuffle = viewModel::shuffle,
    )
}

@Composable
fun ArtistDetailScreen(onBack: () -> Unit, viewModel: ArtistDetailViewModel = hiltViewModel()) {
    val songs by viewModel.songs.collectAsState()
    val artistName by viewModel.artistName.collectAsState()
    DetailScreenShell(
        title = artistName,
        subtitle = "Artist",
        songs = songs,
        onBack = onBack,
        onPlay = viewModel::play,
        onShuffle = viewModel::shuffle,
    )
}

@Composable
fun FolderDetailScreen(onBack: () -> Unit, viewModel: FolderDetailViewModel = hiltViewModel()) {
    val songs by viewModel.songs.collectAsState()
    DetailScreenShell(
        title = viewModel.folderName,
        subtitle = "Folder",
        songs = songs,
        onBack = onBack,
        onPlay = viewModel::play,
        onShuffle = viewModel::shuffle,
    )
}

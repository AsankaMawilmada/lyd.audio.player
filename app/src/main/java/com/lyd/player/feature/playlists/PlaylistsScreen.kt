package com.lyd.player.feature.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.lyd.player.core.design.EmptyState
import com.lyd.player.core.design.GlassTopBar
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.PillButton
import com.lyd.player.core.design.PlaylistCard
import com.lyd.player.feature.common.rememberArtFile

@Composable
fun PlaylistsScreen(
    onOpenPlaylist: (Long) -> Unit,
    onCreatePlaylist: () -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsState()

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = "Playlists",
            actions = { PillButton(text = "New", icon = Icons.Filled.Add, onClick = onCreatePlaylist) },
        )
        if (playlists.isEmpty()) {
            EmptyState("No playlists yet — tap New to create one.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(LydSpacing.safeArea),
                horizontalArrangement = Arrangement.spacedBy(LydSpacing.md),
                verticalArrangement = Arrangement.spacedBy(LydSpacing.md),
            ) {
                items(playlists, key = { it.playlist.id }) { entry ->
                    val distinctAlbums = entry.songs.distinctBy { it.albumId }.take(4)
                    val artFiles = distinctAlbums.map { rememberArtFile(it.albumId, it.mediaStoreId) }
                    PlaylistCard(
                        playlist = entry.playlist,
                        artFiles = artFiles,
                        totalDurationMs = entry.songs.sumOf { it.duration },
                        onClick = { onOpenPlaylist(entry.playlist.id) },
                    )
                }
            }
        }
    }
}

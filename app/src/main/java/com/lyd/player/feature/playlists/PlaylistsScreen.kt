package com.lyd.player.feature.playlists

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.lyd.player.core.design.EmptyState
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.PillButton
import com.lyd.player.core.design.PlaylistCard
import com.lyd.player.core.design.RootTitleBar
import com.lyd.player.core.design.TopBarIconAction
import com.lyd.player.feature.common.rememberArtFile

@Composable
fun PlaylistsScreen(
    onOpenPlaylist: (Long) -> Unit,
    onCreatePlaylist: () -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsState()
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.importPlaylist(context.contentResolver, uri) { result ->
            val message = when {
                result == null -> "Import failed"
                result.matchedCount == 0 -> "No matching songs found in your library"
                else -> "Imported ${result.matchedCount} of ${result.totalCount} songs"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        RootTitleBar(
            title = "Playlists",
            actions = {
                PillButton(text = "New", icon = Icons.Filled.Add, onClick = onCreatePlaylist)
                Box {
                    TopBarIconAction(Icons.Filled.MoreVert, "More") { menuOpen = true }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Import playlist") },
                            onClick = { menuOpen = false; importLauncher.launch(arrayOf("*/*")) },
                        )
                    }
                }
            },
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

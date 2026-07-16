@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lyd.player.feature.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lyd.player.core.data.model.Song
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.LydType
import com.lyd.player.core.design.PillButton

@Composable
fun SongActionsSheet(
    song: Song,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    viewModel: SongActionsViewModel = hiltViewModel(),
) {
    LaunchedEffect(song.path) { viewModel.observeFavorite(song) }
    val isFavorite by viewModel.isFavorite.collectAsState()
    var confirmingDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = LydColors.SurfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(bottom = LydSpacing.xl)) {
            Text(
                song.title,
                style = LydType.headlineMdMobile,
                color = LydColors.OnSurface,
                modifier = Modifier.padding(horizontal = LydSpacing.lg, vertical = LydSpacing.sm),
            )
            SheetAction(
                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                tint = if (isFavorite) LydColors.Secondary else LydColors.OnSurface,
            ) {
                viewModel.toggleFavorite(song)
            }
            SheetAction(Icons.Filled.PlaylistAdd, "Add to playlist") { onAddToPlaylist() }
            SheetAction(Icons.Filled.PlaylistPlay, "Play next") { viewModel.playNext(song); onDismiss() }
            SheetAction(Icons.Filled.QueueMusic, "Add to queue") { viewModel.addToQueue(song); onDismiss() }
            SheetAction(Icons.Filled.Delete, "Delete from device", tint = LydColors.Error) {
                confirmingDelete = true
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            containerColor = LydColors.SurfaceContainer,
            title = { Text("Delete song?", color = LydColors.OnSurface) },
            text = {
                Text(
                    "\"${song.title}\" will be permanently deleted from this device. This can't be undone.",
                    color = LydColors.OnSurfaceVariant,
                )
            },
            confirmButton = {
                PillButton(text = "Delete") {
                    confirmingDelete = false
                    viewModel.deleteSong(song)
                    onDismiss()
                }
            },
            dismissButton = { PillButton(text = "Cancel", filled = false) { confirmingDelete = false } },
        )
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = LydColors.OnSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = LydSpacing.lg, vertical = LydSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(LydSpacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(label, style = LydType.bodyLg, color = tint)
    }
}

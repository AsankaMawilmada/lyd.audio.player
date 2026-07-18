@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lyd.player.feature.playlists

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.lyd.player.core.design.EmptyState
import com.lyd.player.core.design.GlassTopBar
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydShapes
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.LydType
import com.lyd.player.core.design.PillButton
import com.lyd.player.core.design.ReorderableColumn
import com.lyd.player.core.design.SongRow
import com.lyd.player.core.design.TopBarIconAction
import com.lyd.player.core.util.M3uPlaylist
import com.lyd.player.core.util.formatTrackCount

@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val resolved by viewModel.resolved.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var addingSongs by remember { mutableStateOf(false) }

    val playlist = resolved?.playlist
    val songs = resolved?.songs.orEmpty()

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.apple.mpegurl")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportPlaylist(context.contentResolver, uri) { success ->
            val message = if (success) "Exported \"${playlist?.name}\"" else "Export failed"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = playlist?.name ?: "",
            navigationIcon = { TopBarIconAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) },
            actions = {
                androidx.compose.foundation.layout.Box {
                    TopBarIconAction(Icons.Filled.MoreVert, "More") { menuOpen = true }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; renaming = true })
                        DropdownMenuItem(text = { Text("Add songs") }, onClick = { menuOpen = false; addingSongs = true })
                        DropdownMenuItem(
                            text = { Text("Export playlist") },
                            onClick = {
                                menuOpen = false
                                val fileName = M3uPlaylist.sanitizeFileName(playlist?.name ?: "Playlist")
                                exportLauncher.launch("$fileName.m3u8")
                            },
                        )
                        if (playlist?.isFavorites == false) {
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; confirmingDelete = true })
                        }
                    }
                }
            },
        )

        if (songs.isEmpty()) {
            EmptyState("This playlist is empty — add some songs.")
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = LydSpacing.safeArea, vertical = LydSpacing.md)) {
                item {
                    Column {
                        Text(formatTrackCount(songs.size), style = LydType.bodyMd, color = LydColors.OnSurfaceVariant)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = LydSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm),
                        ) {
                            PillButton(text = "Play", icon = Icons.Filled.PlayArrow, onClick = { viewModel.play(0) })
                            PillButton(text = "Shuffle", icon = Icons.Filled.Shuffle, filled = false, onClick = viewModel::shuffle)
                        }
                    }
                }
                item {
                    ReorderableColumn(
                        items = songs,
                        key = { it.path },
                        onMove = viewModel::moveSong,
                    ) { song, isDragging, dragHandleModifier ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.DragHandle,
                                contentDescription = "Reorder",
                                tint = LydColors.OnSurfaceVariant,
                                modifier = dragHandleModifier.padding(end = LydSpacing.xs),
                            )
                            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                                val artFile = com.lyd.player.feature.common.rememberArtFile(song.albumId, song.mediaStoreId)
                                SongRow(
                                    song = song,
                                    artFile = artFile,
                                    isActive = isDragging,
                                    onClick = { viewModel.play(songs.indexOf(song)) },
                                    onMenuClick = {},
                                )
                            }
                            IconButton(onClick = { viewModel.removeSong(song.path) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = LydColors.OnSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (renaming && playlist != null) {
        var text by remember { mutableStateOf(playlist.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            confirmButton = { PillButton(text = "Save") { viewModel.rename(text); renaming = false } },
            dismissButton = { PillButton(text = "Cancel", filled = false) { renaming = false } },
            containerColor = LydColors.SurfaceContainer,
            title = { Text("Rename playlist", color = LydColors.OnSurface) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = LydColors.SurfaceContainerHigh,
                        unfocusedContainerColor = LydColors.SurfaceContainerHigh,
                        focusedTextColor = LydColors.OnSurface,
                        unfocusedTextColor = LydColors.OnSurface,
                    ),
                )
            },
        )
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            confirmButton = {
                PillButton(text = "Delete") {
                    confirmingDelete = false
                    viewModel.delete(onDone = onBack)
                }
            },
            dismissButton = { PillButton(text = "Cancel", filled = false) { confirmingDelete = false } },
            containerColor = LydColors.SurfaceContainer,
            title = { Text("Delete playlist?", color = LydColors.OnSurface) },
            text = { Text("This can't be undone.", color = LydColors.OnSurfaceVariant) },
        )
    }

    if (addingSongs) {
        val query by viewModel.addSongsQuery.collectAsState()
        val candidates by viewModel.addSongsCandidates.collectAsState()
        val selected by viewModel.addSongsSelected.collectAsState()
        ModalBottomSheet(onDismissRequest = { addingSongs = false }, containerColor = LydColors.SurfaceContainer) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = LydSpacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Add songs", style = LydType.headlineLgMobile, color = LydColors.OnSurface)
                    PillButton(text = "Add (${selected.size})", icon = Icons.Filled.PersonAdd) {
                        viewModel.confirmAddSongs(onDone = { addingSongs = false })
                    }
                }
                SongMultiSelectList(
                    songs = candidates,
                    selected = selected,
                    searchQuery = query,
                    onSearchChange = { viewModel.addSongsQuery.value = it },
                    onToggle = viewModel::toggleAddSong,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

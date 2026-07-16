package com.lyd.player.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.lyd.player.core.data.model.SongSortOrder
import com.lyd.player.core.design.AlbumCard
import com.lyd.player.core.design.ArtistRow
import com.lyd.player.core.design.EmptyState
import com.lyd.player.core.design.FolderRow
import com.lyd.player.core.design.GlassTopBar
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydShapes
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.LydType
import com.lyd.player.core.design.SongRow
import com.lyd.player.feature.common.SongActionsHost
import com.lyd.player.feature.common.rememberArtFile
import com.lyd.player.feature.common.rememberSongActionsState

@Composable
fun LibraryScreen(
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (Long) -> Unit,
    onOpenFolder: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val section by viewModel.section.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val filterQuery by viewModel.filterQuery.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val actionsState = rememberSongActionsState()
    var sortMenuOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = "Library")
        LibrarySegmentedControl(section = section, onSelect = { viewModel.section.value = it })

        if (section == LibrarySection.SONGS) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.sm),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm),
            ) {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { viewModel.filterQuery.value = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = LydShapes.default,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = LydColors.OnSurfaceVariant) },
                    placeholder = { Text("Filter songs", color = LydColors.OnSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = LydColors.SurfaceContainer,
                        unfocusedContainerColor = LydColors.SurfaceContainer,
                        focusedTextColor = LydColors.OnSurface,
                        unfocusedTextColor = LydColors.OnSurface,
                    ),
                )
                Box {
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = LydColors.OnSurfaceVariant)
                    }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        SongSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label()) },
                                onClick = { viewModel.sortOrder.value = order; sortMenuOpen = false },
                            )
                        }
                    }
                }
                IconButton(onClick = { viewModel.playAllSongs(shuffled = true) }) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle all", tint = LydColors.OnSurfaceVariant)
                }
            }
        }

        when (section) {
            LibrarySection.SONGS -> {
                if (songs.isEmpty()) {
                    EmptyState("No songs match.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = LydSpacing.safeArea, vertical = LydSpacing.sm)) {
                        item {
                            Text(
                                "${songs.size} songs",
                                style = LydType.bodyMd,
                                color = LydColors.OnSurfaceVariant,
                                modifier = Modifier.padding(vertical = LydSpacing.sm),
                            )
                        }
                        items(songs, key = { it.path }) { song ->
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
            LibrarySection.ALBUMS -> {
                if (albums.isEmpty()) {
                    EmptyState("No albums found.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(LydSpacing.safeArea),
                        horizontalArrangement = Arrangement.spacedBy(LydSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(LydSpacing.md),
                    ) {
                        items(albums, key = { it.id }) { album ->
                            val artFile = rememberArtFile(album.id, album.id)
                            AlbumCard(album = album, artFile = artFile, onClick = { onOpenAlbum(album.id) })
                        }
                    }
                }
            }
            LibrarySection.ARTISTS -> {
                if (artists.isEmpty()) {
                    EmptyState("No artists found.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = LydSpacing.safeArea)) {
                        items(artists, key = { it.id }) { artist ->
                            ArtistRow(artist = artist, onClick = { onOpenArtist(artist.id) })
                        }
                    }
                }
            }
            LibrarySection.FOLDERS -> {
                if (folders.isEmpty()) {
                    EmptyState("No folders found.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = LydSpacing.safeArea)) {
                        items(folders, key = { it.path }) { folder ->
                            FolderRow(folder = folder, onClick = { onOpenFolder(folder.path) })
                        }
                    }
                }
            }
        }
    }
    SongActionsHost(actionsState)
}

@Composable
private fun LibrarySegmentedControl(section: LibrarySection, onSelect: (LibrarySection) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm),
    ) {
        LibrarySection.entries.forEach { s ->
            val selected = s == section
            Text(
                text = s.label(),
                style = LydType.labelSm,
                color = if (selected) LydColors.OnSurface else LydColors.OnSurfaceVariant,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable { onSelect(s) }
                    .background(if (selected) LydColors.SecondaryContainer else LydColors.SurfaceContainer, LydShapes.full)
                    .padding(horizontal = LydSpacing.md, vertical = LydSpacing.sm),
            )
        }
    }
}

private fun LibrarySection.label() = when (this) {
    LibrarySection.SONGS -> "Songs"
    LibrarySection.ALBUMS -> "Albums"
    LibrarySection.ARTISTS -> "Artists"
    LibrarySection.FOLDERS -> "Folders"
}

private fun SongSortOrder.label() = when (this) {
    SongSortOrder.TITLE -> "Title"
    SongSortOrder.ARTIST -> "Artist"
    SongSortOrder.ALBUM -> "Album"
    SongSortOrder.DATE_ADDED -> "Date added"
    SongSortOrder.DURATION -> "Duration"
}

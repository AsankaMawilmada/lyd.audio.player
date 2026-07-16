package com.lyd.player.feature.search

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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.lyd.player.core.design.EmptyState
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydShapes
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.LydType
import com.lyd.player.core.design.PillButton
import com.lyd.player.core.design.SongRow
import com.lyd.player.feature.common.SongActionsHost
import com.lyd.player.feature.common.rememberArtFile
import com.lyd.player.feature.common.rememberSongActionsState

@Composable
fun SearchScreen(
    onOpenLibrary: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val actionsState = rememberSongActionsState()

    Column(Modifier.fillMaxSize().padding(top = LydSpacing.xl)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea)) {
            Text("Search", style = LydType.displayMobile, color = LydColors.OnSurface)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.md))
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = LydShapes.full,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = LydColors.OnSurfaceVariant) },
                placeholder = { Text("Search title, artist, or album", color = LydColors.OnSurfaceVariant) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LydColors.SurfaceContainer,
                    unfocusedContainerColor = LydColors.SurfaceContainer,
                    focusedTextColor = LydColors.OnSurface,
                    unfocusedTextColor = LydColors.OnSurface,
                ),
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.lg))

        if (query.isBlank()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea)) {
                Text("Browse", style = LydType.headlineLgMobile, color = LydColors.OnSurface)
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm)) {
                    PillButton("Albums", icon = Icons.Filled.Album, filled = false) { onOpenLibrary("albums") }
                    PillButton("Artists", icon = Icons.Filled.Person, filled = false) { onOpenLibrary("artists") }
                    PillButton("Folders", icon = Icons.Filled.Folder, filled = false) { onOpenLibrary("folders") }
                }
            }
        } else if (results.isEmpty()) {
            EmptyState("No matches for \"$query\".")
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = LydSpacing.safeArea, vertical = LydSpacing.md)) {
                item {
                    Text(
                        "${results.size} results",
                        style = LydType.bodyMd,
                        color = LydColors.OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = LydSpacing.sm),
                    )
                }
                items(results, key = { it.path }) { song ->
                    val artFile = rememberArtFile(song.albumId, song.mediaStoreId)
                    SongRow(
                        song = song,
                        artFile = artFile,
                        isActive = false,
                        onClick = { viewModel.playResult(song) },
                        onMenuClick = { actionsState.menuSong = song },
                    )
                }
            }
        }
    }
    SongActionsHost(actionsState)
}

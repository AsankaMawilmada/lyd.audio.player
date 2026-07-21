package au.com.inoaspect.lyd.audio.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.inoaspect.lyd.audio.core.design.GlassTopBar
import au.com.inoaspect.lyd.audio.core.design.LydColors
import au.com.inoaspect.lyd.audio.core.design.LydShapes
import au.com.inoaspect.lyd.audio.core.design.LydSpacing
import au.com.inoaspect.lyd.audio.core.design.LydType
import au.com.inoaspect.lyd.audio.core.design.PillButton
import au.com.inoaspect.lyd.audio.core.design.TopBarIconAction

@Composable
fun CreatePlaylistScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: CreatePlaylistViewModel = hiltViewModel(),
) {
    val mode by viewModel.mode.collectAsState()
    val name by viewModel.name.collectAsState()
    val songQuery by viewModel.songSearchQuery.collectAsState()
    val songs by viewModel.filteredSongs.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedSongs by viewModel.selectedSongPaths.collectAsState()
    val selectedFolders by viewModel.selectedFolderPaths.collectAsState()
    val canCreate by viewModel.canCreate.collectAsState()

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = "New Playlist",
            navigationIcon = { TopBarIconAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) },
            actions = { PillButton(text = "Create", filled = canCreate) { viewModel.create(onCreated) } },
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.md)) {
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.name.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = LydShapes.default,
                placeholder = { Text("Playlist name", color = LydColors.OnSurfaceVariant) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LydColors.SurfaceContainer,
                    unfocusedContainerColor = LydColors.SurfaceContainer,
                    focusedTextColor = LydColors.OnSurface,
                    unfocusedTextColor = LydColors.OnSurface,
                ),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm)) {
                ModeTab("By songs", mode == CreatePlaylistMode.SONGS) { viewModel.mode.value = CreatePlaylistMode.SONGS }
                ModeTab("By folders", mode == CreatePlaylistMode.FOLDERS) { viewModel.mode.value = CreatePlaylistMode.FOLDERS }
            }
        }

        if (mode == CreatePlaylistMode.SONGS) {
            SongMultiSelectList(
                songs = songs,
                selected = selectedSongs,
                searchQuery = songQuery,
                onSearchChange = { viewModel.songSearchQuery.value = it },
                onToggle = viewModel::toggleSong,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            FolderMultiSelectList(
                folders = folders,
                selected = selectedFolders,
                onToggle = viewModel::toggleFolder,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = LydType.labelSm,
        color = if (selected) LydColors.OnSurface else LydColors.OnSurfaceVariant,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (selected) LydColors.SecondaryContainer else LydColors.SurfaceContainer, LydShapes.full)
            .padding(horizontal = LydSpacing.md, vertical = LydSpacing.sm),
    )
}

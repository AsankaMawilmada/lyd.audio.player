package au.com.inoaspect.lyd.audio.feature.playlists

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import au.com.inoaspect.lyd.audio.core.data.model.Folder
import au.com.inoaspect.lyd.audio.core.data.model.Song
import au.com.inoaspect.lyd.audio.core.design.LydColors
import au.com.inoaspect.lyd.audio.core.design.LydShapes
import au.com.inoaspect.lyd.audio.core.design.LydSpacing
import au.com.inoaspect.lyd.audio.core.design.LydType
import au.com.inoaspect.lyd.audio.core.design.SongRow
import au.com.inoaspect.lyd.audio.core.util.formatTrackCount
import au.com.inoaspect.lyd.audio.feature.common.rememberArtFile

@Composable
fun SongMultiSelectList(
    songs: List<Song>,
    selected: Set<String>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = LydSpacing.sm)) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.sm),
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
        }
        items(songs, key = { it.path }) { song ->
            val artFile = rememberArtFile(song.albumId, song.mediaStoreId)
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = song.path in selected,
                    onCheckedChange = { onToggle(song.path) },
                    colors = CheckboxDefaults.colors(checkedColor = LydColors.Secondary),
                )
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                    SongRow(song = song, artFile = artFile, isActive = false, onClick = { onToggle(song.path) }, onMenuClick = {})
                }
            }
        }
    }
}

@Composable
fun FolderMultiSelectList(
    folders: List<Folder>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = LydSpacing.sm)) {
        items(folders, key = { it.path }) { folder ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(LydSpacing.md),
            ) {
                Checkbox(
                    checked = folder.path in selected,
                    onCheckedChange = { onToggle(folder.path) },
                    colors = CheckboxDefaults.colors(checkedColor = LydColors.Secondary),
                )
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    Text(folder.name, style = LydType.headlineMdMobile, color = LydColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatTrackCount(folder.trackCount), style = LydType.bodyMd, color = LydColors.OnSurfaceVariant)
                }
            }
        }
    }
}

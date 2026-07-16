@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lyd.player.feature.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydShapes
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.LydType
import com.lyd.player.core.design.PillButton

@Composable
fun AddToPlaylistSheet(
    songPaths: List<String>,
    onDismiss: () -> Unit,
    viewModel: AddToPlaylistViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsState()
    var newName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = LydColors.SurfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = LydSpacing.lg, vertical = LydSpacing.sm)) {
            Text("Add to playlist", style = LydType.headlineLgMobile, color = LydColors.OnSurface)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.md))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("New playlist name", style = LydType.bodyMd, color = LydColors.OnSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = LydShapes.default,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = LydColors.SurfaceContainerHigh,
                        unfocusedContainerColor = LydColors.SurfaceContainerHigh,
                        focusedTextColor = LydColors.OnSurface,
                        unfocusedTextColor = LydColors.OnSurface,
                    ),
                )
                PillButton(text = "Create", icon = Icons.Filled.Add) {
                    viewModel.createAndAdd(newName, songPaths, onDone = onDismiss)
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.md))
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.addToExisting(playlist.id, songPaths, onDone = onDismiss) }
                            .padding(vertical = LydSpacing.md),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LydSpacing.md),
                    ) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = LydColors.OnSurfaceVariant)
                        Text(
                            playlist.name,
                            style = LydType.bodyLg,
                            color = LydColors.OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

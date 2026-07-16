package com.lyd.player.feature.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyd.player.core.data.model.RepeatMode
import com.lyd.player.core.design.ArtThumbnail
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydShapes
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.LydType
import com.lyd.player.core.util.formatDurationMs
import com.lyd.player.playback.PlaybackUiState

@Composable
fun PlayerPane(
    state: PlaybackUiState,
    artFile: Any?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenSongMenu: () -> Unit,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.currentItem
    var dragPosition by remember { mutableStateOf<Float?>(null) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Playing from ${state.playingFrom.ifBlank { "Library" }}",
            style = LydType.labelSm,
            color = LydColors.Secondary,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.md))
        ArtThumbnail(
            artFile,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = LydShapes.md,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.xl))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    item?.title ?: "Nothing playing",
                    style = LydType.displayMobile,
                    color = LydColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item?.artist ?: "",
                    style = LydType.bodyLg,
                    color = LydColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) LydColors.Secondary else LydColors.OnSurfaceVariant,
                    )
                }
                IconButton(onClick = onOpenSongMenu) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = LydColors.OnSurfaceVariant)
                }
            }
        }

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.md))
        val duration = state.durationMs.coerceAtLeast(1L).toFloat()
        val sliderValue = dragPosition ?: state.positionMs.toFloat().coerceIn(0f, duration)
        Slider(
            value = sliderValue,
            onValueChange = { dragPosition = it },
            onValueChangeFinished = {
                dragPosition?.let { onSeek(it.toLong()) }
                dragPosition = null
            },
            valueRange = 0f..duration,
            colors = SliderDefaults.colors(
                thumbColor = LydColors.OnSurface,
                activeTrackColor = LydColors.Secondary,
                inactiveTrackColor = LydColors.SurfaceContainerHighest,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDurationMs(sliderValue.toLong()), style = LydType.labelSm, color = LydColors.OnSurfaceVariant)
            Text(formatDurationMs(state.durationMs), style = LydType.labelSm, color = LydColors.OnSurfaceVariant)
        }

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffleEnabled) LydColors.Secondary else LydColors.OnSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LydSpacing.lg)) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = LydColors.OnSurface, modifier = Modifier.size(40.dp))
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(LydColors.SecondaryContainer, LydShapes.full),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onTogglePlayPause) {
                        Icon(
                            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = LydColors.OnSecondaryContainer,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = LydColors.OnSurface, modifier = Modifier.size(40.dp))
                }
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode != RepeatMode.OFF) LydColors.Secondary else LydColors.OnSurfaceVariant,
                )
            }
        }
    }
}

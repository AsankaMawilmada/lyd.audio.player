package com.lyd.player.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MiniPlayer(
    title: String,
    artist: String,
    artFile: Any?,
    isPlaying: Boolean,
    progressFraction: Float,
    onClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(LydShapes.default)
            .background(LydColors.GlassSurface)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = LydSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtThumbnail(artFile, modifier = Modifier.size(40.dp))
            Box(Modifier.weight(1f).padding(horizontal = LydSpacing.md)) {
                androidx.compose.foundation.layout.Column {
                    Text(title, style = LydType.headlineMdMobile, color = LydColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist, style = LydType.bodyMd, color = LydColors.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = LydColors.Secondary,
                    modifier = Modifier.size(32.dp),
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = LydColors.OnSurface)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                .height(2.dp)
                .background(LydColors.Secondary),
        )
    }
}

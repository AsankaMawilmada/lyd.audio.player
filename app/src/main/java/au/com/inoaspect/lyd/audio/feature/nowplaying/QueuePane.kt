package au.com.inoaspect.lyd.audio.feature.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.inoaspect.lyd.audio.core.design.ArtThumbnail
import au.com.inoaspect.lyd.audio.core.design.EmptyState
import au.com.inoaspect.lyd.audio.core.design.LydColors
import au.com.inoaspect.lyd.audio.core.design.LydShapes
import au.com.inoaspect.lyd.audio.core.design.LydSpacing
import au.com.inoaspect.lyd.audio.core.design.LydType
import au.com.inoaspect.lyd.audio.core.design.ReorderableColumn
import au.com.inoaspect.lyd.audio.playback.QueueItem

@Composable
fun QueuePane(
    queue: List<QueueItem>,
    currentIndex: Int,
    onJumpTo: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (queue.isEmpty()) {
        EmptyState("Queue is empty.", modifier = modifier.fillMaxSize())
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = LydSpacing.safeArea, vertical = LydSpacing.md),
    ) {
        item {
            Text(
                "Up next",
                style = LydType.headlineLgMobile,
                color = LydColors.OnSurface,
                modifier = Modifier.padding(bottom = LydSpacing.md),
            )
        }
        item {
            ReorderableColumn(
                items = queue.mapIndexed { index, item -> index to item },
                key = { it.second.path + "_" + it.first },
                onMove = onMove,
            ) { (index, item), isDragging, dragHandleModifier ->
                val isCurrent = index == currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LydShapes.default)
                        .background(if (isCurrent) LydColors.SurfaceContainer else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onJumpTo(index) }
                        .padding(vertical = LydSpacing.sm, horizontal = LydSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = "Reorder",
                        tint = LydColors.OnSurfaceVariant,
                        modifier = dragHandleModifier.padding(end = LydSpacing.xs),
                    )
                    ArtThumbnail(item.artworkUri, modifier = Modifier.size(40.dp))
                    Column(Modifier.weight(1f).padding(horizontal = LydSpacing.md)) {
                        Text(
                            item.title,
                            style = LydType.headlineMdMobile,
                            color = if (isCurrent) LydColors.Secondary else LydColors.OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(item.artist, style = LydType.bodyMd, color = LydColors.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = LydColors.OnSurfaceVariant)
                    }
                }
            }
        }
    }
}

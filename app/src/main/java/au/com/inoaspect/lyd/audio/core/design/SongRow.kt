package au.com.inoaspect.lyd.audio.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import au.com.inoaspect.lyd.audio.core.data.model.Song
import au.com.inoaspect.lyd.audio.core.util.formatDurationMs
import java.io.File

@Composable
fun ArtThumbnail(
    art: Any?,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = LydShapes.sm,
) {
    Box(
        modifier = modifier.clip(shape).background(LydColors.SurfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) {
            AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = LydColors.OnSurfaceVariant)
        }
    }
}

@Composable
fun SongRow(
    song: Song,
    artFile: File?,
    isActive: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingBadge: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LydShapes.default)
            .background(if (isActive) LydColors.SurfaceContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(LydSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            ArtThumbnail(artFile, modifier = Modifier.fillMaxSize())
            if (isActive) {
                Box(
                    Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f), LydShapes.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = LydColors.Secondary, modifier = Modifier.size(20.dp))
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = LydSpacing.md),
        ) {
            Text(
                text = song.title,
                style = LydType.headlineMdMobile,
                color = if (isActive) LydColors.Secondary else LydColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = LydType.bodyMd,
                color = LydColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm)) {
            if (trailingBadge != null) {
                Box(
                    Modifier.background(LydColors.SurfaceContainerHighest, LydShapes.sm).padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(trailingBadge, style = LydType.labelSm, color = LydColors.OnSurface)
                }
            }
            Text(
                text = formatDurationMs(song.duration),
                style = LydType.labelSm,
                color = LydColors.OnSurfaceVariant,
            )
            IconButton(onClick = onMenuClick, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = LydColors.OnSurfaceVariant)
            }
        }
    }
}

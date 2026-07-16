package com.lyd.player.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyd.player.core.data.model.Album
import com.lyd.player.core.data.model.Artist
import com.lyd.player.core.data.model.Folder
import com.lyd.player.core.data.model.Playlist
import com.lyd.player.core.util.formatDurationLong
import com.lyd.player.core.util.formatTrackCount
import java.io.File

@Composable
fun AlbumCard(
    album: Album,
    artFile: File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(160.dp).clickable(onClick = onClick)) {
        ArtThumbnail(artFile, modifier = Modifier.fillMaxWidth().aspectRatio(1f), shape = LydShapes.default)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.sm))
        Text(album.name, style = LydType.headlineMdMobile, color = LydColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(album.artist, style = LydType.bodyMd, color = LydColors.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    artFiles: List<File?>,
    totalDurationMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(LydShapes.default)
                .background(LydColors.SurfaceContainer),
        ) {
            val arts = artFiles.filterNotNull().take(4)
            if (arts.isEmpty()) {
                Icon(
                    Icons.Filled.QueueMusic,
                    contentDescription = null,
                    tint = LydColors.OnSurfaceVariant,
                    modifier = Modifier.size(40.dp).align(Alignment.Center),
                )
            } else if (arts.size == 1) {
                ArtThumbnail(arts[0], modifier = Modifier.fillMaxSize(), shape = LydShapes.default)
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        ArtThumbnail(arts.getOrNull(0), Modifier.weight(1f).fillMaxSize(), shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
                        ArtThumbnail(arts.getOrNull(1), Modifier.weight(1f).fillMaxSize(), shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
                    }
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        ArtThumbnail(arts.getOrNull(2), Modifier.weight(1f).fillMaxSize(), shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
                        ArtThumbnail(arts.getOrNull(3), Modifier.weight(1f).fillMaxSize(), shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
                    }
                }
            }
        }
        Column(Modifier.padding(top = LydSpacing.sm)) {
            Text(playlist.name, style = LydType.headlineMdMobile, color = LydColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatTrackCount(playlist.songCount)} • ${formatDurationLong(totalDurationMs)}",
                style = LydType.bodyMd,
                color = LydColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ArtistRow(artist: Artist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(LydSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LydSpacing.md),
    ) {
        Box(
            Modifier.size(48.dp).clip(LydShapes.full).background(LydColors.SurfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = LydColors.OnSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Text(artist.name, style = LydType.headlineMdMobile, color = LydColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatTrackCount(artist.trackCount), style = LydType.bodyMd, color = LydColors.OnSurfaceVariant)
        }
    }
}

@Composable
fun FolderRow(folder: Folder, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(LydSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LydSpacing.md),
    ) {
        Box(
            Modifier.size(48.dp).clip(LydShapes.sm).background(LydColors.SurfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = LydColors.OnSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Text(folder.name, style = LydType.headlineMdMobile, color = LydColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatTrackCount(folder.trackCount), style = LydType.bodyMd, color = LydColors.OnSurfaceVariant)
        }
    }
}

@Composable
fun AlbumIconFallback(modifier: Modifier = Modifier) {
    Icon(Icons.Filled.Album, contentDescription = null, tint = LydColors.OnSurfaceVariant, modifier = modifier)
}

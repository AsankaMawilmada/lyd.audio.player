package com.lyd.player.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TopBarIconAction(icon: ImageVector, contentDescription: String?, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = contentDescription, tint = LydColors.OnSurface)
    }
}

@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LydColors.GlassBackground)
            .padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.CenterStart) { navigationIcon?.invoke() }
        Text(
            text = title,
            style = LydType.headlineMdMobile,
            color = LydColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm), content = actions)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = LydType.headlineLgMobile, color = LydColors.OnSurface)
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel.uppercase(),
                style = LydType.labelSm,
                color = LydColors.Secondary,
                modifier = Modifier.clickable(onClick = onActionClick),
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = true,
    onClick: () -> Unit,
) {
    val background = if (filled) LydColors.SecondaryContainer else LydColors.SurfaceContainer
    val content = LydColors.OnSurface
    Row(
        modifier = modifier
            .background(background, LydShapes.full)
            .clickable(onClick = onClick)
            .padding(horizontal = LydSpacing.md, vertical = LydSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LydSpacing.xs),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text, style = LydType.labelSm, color = content)
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.MusicNote,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(LydSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LydSpacing.md),
    ) {
        Box(
            Modifier.size(72.dp).background(LydColors.SurfaceContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = LydColors.OnSurfaceVariant, modifier = Modifier.size(32.dp))
        }
        Text(message, style = LydType.bodyMd, color = LydColors.OnSurfaceVariant, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(LydSpacing.xxl), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LydColors.Secondary)
    }
}

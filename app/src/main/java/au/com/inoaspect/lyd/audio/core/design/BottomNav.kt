package au.com.inoaspect.lyd.audio.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun BottomNavBar(items: List<BottomNavItem>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(LydColors.GlassSurface),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .clickable(onClick = item.onClick)
                    .padding(horizontal = LydSpacing.md, vertical = LydSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = if (item.selected) LydColors.Secondary else LydColors.OnSurfaceVariant.copy(alpha = 0.6f),
                )
                Text(
                    item.label,
                    style = LydType.labelSm,
                    color = if (item.selected) LydColors.Secondary else LydColors.OnSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

package au.com.inoaspect.lyd.audio.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Rounding scale from designs/sonic_minimalist/DESIGN.md
object LydShapes {
    val sm = RoundedCornerShape(8.dp)
    val default = RoundedCornerShape(16.dp)
    val md = RoundedCornerShape(24.dp)
    val lg = RoundedCornerShape(32.dp)
    val xl = RoundedCornerShape(48.dp)
    val full = RoundedCornerShape(percent = 50)
}

object LydSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val safeArea = 20.dp
}

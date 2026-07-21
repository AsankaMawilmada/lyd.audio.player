package au.com.inoaspect.lyd.audio.feature.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.inoaspect.lyd.audio.core.design.EmptyState
import au.com.inoaspect.lyd.audio.core.design.GlassTopBar
import au.com.inoaspect.lyd.audio.core.design.LydColors
import au.com.inoaspect.lyd.audio.core.design.LydShapes
import au.com.inoaspect.lyd.audio.core.design.LydSpacing
import au.com.inoaspect.lyd.audio.core.design.LydType
import au.com.inoaspect.lyd.audio.core.design.TopBarIconAction
import au.com.inoaspect.lyd.audio.playback.EqBand
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(onBack: () -> Unit, viewModel: EqualizerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = "Equalizer",
            navigationIcon = { TopBarIconAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) },
        )
        if (!state.supported) {
            EmptyState("This device doesn't support an equalizer effect.", modifier = Modifier.fillMaxSize())
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Enable equalizer", style = LydType.bodyLg, color = LydColors.OnSurface)
                Switch(
                    checked = state.enabled,
                    onCheckedChange = viewModel::setEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = LydColors.Secondary, checkedTrackColor = LydColors.SecondaryContainer),
                )
            }

            val contentAlpha = if (state.enabled) 1f else 0.4f

            FlowRow(
                modifier = Modifier.fillMaxWidth().alpha(contentAlpha).padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(LydSpacing.sm),
            ) {
                state.presets.forEach { preset ->
                    val selected = preset == state.activePreset
                    Text(
                        text = preset,
                        style = LydType.labelSm,
                        color = if (selected) LydColors.OnSurface else LydColors.OnSurfaceVariant,
                        modifier = Modifier
                            .clickable(enabled = state.enabled) { viewModel.applyPreset(preset) }
                            .background(if (selected) LydColors.SecondaryContainer else LydColors.SurfaceContainer, LydShapes.full)
                            .padding(horizontal = LydSpacing.md, vertical = LydSpacing.sm),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .alpha(contentAlpha)
                    .padding(horizontal = LydSpacing.sm, vertical = LydSpacing.xl),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                state.bands.forEach { band ->
                    BandSlider(
                        band = band,
                        enabled = state.enabled,
                        onChange = { viewModel.setBandLevel(band.index, it) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

private val BAND_SLIDER_HEIGHT = 520.dp
private val BAND_SLIDER_THICKNESS = 3.dp
private val BAND_THUMB_RADIUS = 7.dp

@Composable
private fun BandSlider(band: EqBand, enabled: Boolean, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            formatFrequency(band.centerFreqHz),
            style = LydType.labelSm,
            color = LydColors.OnSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.sm))
        VerticalBandBar(
            value = band.levelMilliBel,
            range = band.minMilliBel..band.maxMilliBel,
            enabled = enabled,
            onChange = onChange,
            modifier = Modifier.height(BAND_SLIDER_HEIGHT).fillMaxWidth(),
        )
    }
}

/**
 * A custom-drawn vertical bar/slider. Material3's [androidx.compose.material3.Slider] is
 * authored (and internally sized) for horizontal use only — rotating it 90° to fake a vertical
 * slider leaves it clamped to its own small default footprint regardless of outer size
 * modifiers. Drawing it ourselves gives full, reliable control over how tall and thin it is.
 */
@Composable
private fun VerticalBandBar(
    value: Int,
    range: IntRange,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    val trackColor = LydColors.SurfaceContainerHighest
    val activeColor = LydColors.Secondary
    val thumbColor = LydColors.OnSurface

    Box(
        modifier = modifier
            .onSizeChanged { sizePx = it }
            .pointerInput(enabled, range) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset -> onChange(valueFromOffsetY(offset.y, sizePx.height, range)) },
                    onDrag = { change, _ ->
                        change.consume()
                        onChange(valueFromOffsetY(change.position.y, sizePx.height, range))
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.width(BAND_SLIDER_THICKNESS).fillMaxHeight()) {
            val trackWidthPx = size.width
            val h = size.height
            val span = (range.last - range.first).coerceAtLeast(1)
            val fraction = ((value - range.first).toFloat() / span).coerceIn(0f, 1f)
            val thumbY = h * (1f - fraction)
            val corner = CornerRadius(trackWidthPx / 2)

            drawRoundRect(color = trackColor, size = Size(trackWidthPx, h), cornerRadius = corner)
            drawRoundRect(
                color = if (enabled) activeColor else trackColor,
                topLeft = Offset(0f, thumbY),
                size = Size(trackWidthPx, h - thumbY),
                cornerRadius = corner,
            )
            drawCircle(
                color = if (enabled) thumbColor else trackColor,
                radius = BAND_THUMB_RADIUS.toPx(),
                center = Offset(trackWidthPx / 2, thumbY),
            )
        }
    }
}

private fun valueFromOffsetY(y: Float, heightPx: Int, range: IntRange): Int {
    if (heightPx <= 0) return range.first
    val fraction = (1f - (y / heightPx)).coerceIn(0f, 1f)
    return (range.first + fraction * (range.last - range.first)).roundToInt()
}

private fun formatFrequency(hz: Int): String =
    if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"

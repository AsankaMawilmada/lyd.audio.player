package com.lyd.player.feature.equalizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lyd.player.core.design.EmptyState
import com.lyd.player.core.design.GlassTopBar
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydShapes
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.LydType
import com.lyd.player.core.design.TopBarIconAction
import com.lyd.player.playback.EqBand

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

            LazyRow(
                modifier = Modifier.fillMaxWidth().alpha(contentAlpha).padding(vertical = LydSpacing.sm),
                contentPadding = PaddingValues(horizontal = LydSpacing.safeArea),
                horizontalArrangement = Arrangement.spacedBy(LydSpacing.sm),
            ) {
                items(state.presets) { preset ->
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
                    .fillMaxSize()
                    .alpha(contentAlpha)
                    .padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.xl),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                state.bands.forEach { band ->
                    BandSlider(band = band, enabled = state.enabled, onChange = { viewModel.setBandLevel(band.index, it) })
                }
            }
        }
    }
}

@Composable
private fun BandSlider(band: EqBand, enabled: Boolean, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
        Text(formatFrequency(band.centerFreqHz), style = LydType.labelSm, color = LydColors.OnSurfaceVariant)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.sm))
        Box(modifier = Modifier.height(200.dp).width(48.dp), contentAlignment = Alignment.Center) {
            Slider(
                value = band.levelMilliBel.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = band.minMilliBel.toFloat()..band.maxMilliBel.toFloat(),
                enabled = enabled,
                modifier = Modifier
                    .width(200.dp)
                    .graphicsLayer { rotationZ = -90f },
                colors = SliderDefaults.colors(
                    thumbColor = LydColors.OnSurface,
                    activeTrackColor = LydColors.Secondary,
                    inactiveTrackColor = LydColors.SurfaceContainerHighest,
                ),
            )
        }
    }
}

private fun formatFrequency(hz: Int): String =
    if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"

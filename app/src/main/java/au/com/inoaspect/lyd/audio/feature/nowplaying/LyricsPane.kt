package au.com.inoaspect.lyd.audio.feature.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import au.com.inoaspect.lyd.audio.core.data.lyrics.LyricLine
import au.com.inoaspect.lyd.audio.core.design.EmptyState
import au.com.inoaspect.lyd.audio.core.design.FullScreenLoading
import au.com.inoaspect.lyd.audio.core.design.LydColors
import au.com.inoaspect.lyd.audio.core.design.LydSpacing
import au.com.inoaspect.lyd.audio.core.design.LydType

@Composable
fun LyricsPane(
    state: LyricsUiState,
    activeLineIndex: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            LyricsUiState.Loading -> FullScreenLoading(Modifier.fillMaxSize())
            LyricsUiState.Empty -> EmptyState("No lyrics found for this track.", modifier = Modifier.fillMaxSize())
            is LyricsUiState.Plain -> {
                val scrollState = rememberScrollState()
                Text(
                    text = state.text,
                    style = LydType.bodyLg,
                    color = LydColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(LydSpacing.safeArea),
                )
            }
            is LyricsUiState.Synced -> SyncedLyrics(state.lines, activeLineIndex)
        }
    }
}

@Composable
private fun SyncedLyrics(lines: List<LyricLine>, activeIndex: Int) {
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val target = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = LydSpacing.safeArea, vertical = LydSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LydSpacing.md),
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            Text(
                text = line.text,
                style = if (isActive) LydType.headlineMdMobile else LydType.bodyLg,
                color = if (isActive) LydColors.Secondary else LydColors.OnSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

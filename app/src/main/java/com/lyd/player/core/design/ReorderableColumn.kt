package com.lyd.player.core.design

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex

/**
 * A simple long-press-drag reorderable list. Not virtualized (renders all [items] eagerly),
 * which is fine for the playlist/queue sizes this app deals with, but keeps the drag math
 * (swap-on-half-item-height-crossed) simple and reliable instead of fighting LazyColumn offsets.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, isDragging: Boolean, dragHandleModifier: Modifier) -> Unit,
) {
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

    Column(modifier) {
        items.forEachIndexed { index, item ->
            val isDragging = index == draggingIndex
            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f }
                    .onGloballyPositioned { coordinates -> itemHeights[index] = coordinates.size.height },
            ) {
                val dragHandleModifier = Modifier.pointerInput(key(item)) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { draggingIndex = index },
                        onDragEnd = { draggingIndex = -1; dragOffsetY = 0f },
                        onDragCancel = { draggingIndex = -1; dragOffsetY = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY += dragAmount.y
                            val current = draggingIndex
                            if (current < 0) return@detectDragGesturesAfterLongPress
                            val height = itemHeights[current] ?: return@detectDragGesturesAfterLongPress
                            if (dragOffsetY > height / 2 && current < items.lastIndex) {
                                onMove(current, current + 1)
                                draggingIndex = current + 1
                                dragOffsetY -= height
                            } else if (dragOffsetY < -height / 2 && current > 0) {
                                onMove(current, current - 1)
                                draggingIndex = current - 1
                                dragOffsetY += height
                            }
                        },
                    )
                }
                itemContent(item, isDragging, dragHandleModifier)
            }
        }
    }
}

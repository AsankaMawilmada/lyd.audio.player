package com.lyd.player.core.util

import java.util.Locale
import kotlin.math.roundToLong

/** e.g. 4:22 or 1:04:15 */
fun formatDurationMs(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000.0).roundToLong()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/** e.g. "8h 42m" or "45m" */
fun formatDurationLong(durationMs: Long): String {
    val totalMinutes = durationMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun formatTrackCount(count: Int): String = if (count == 1) "1 Track" else "$count Tracks"

package au.com.inoaspect.lyd.audio.core.data.mediastore

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val RECORDER_FILENAME_REGEX = Regex("^([A-Za-z]{2,8})[_-](\\d{8})[_-](\\d{6})$")
private val TIMESTAMP_PARSER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

// Fixed locale so casing (e.g. "PM" vs "pm") is stable across devices/JDKs regardless of the
// system default locale — this is a readable fallback for a raw filename, not localized content.
private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.US)

/**
 * When a device has no ID3 title tag, MediaStore falls back to the raw filename (e.g. voice
 * recorder output like "MCR_20230815_143022"). Reformats that specific shape into a readable
 * date/time; any other title (including real song titles that merely contain digits) is
 * returned unchanged.
 */
fun humanizeRecorderTitle(rawTitle: String): String {
    val match = RECORDER_FILENAME_REGEX.matchEntire(rawTitle) ?: return rawTitle
    val (prefix, datePart, timePart) = match.destructured
    return try {
        val dateTime = LocalDateTime.parse(datePart + timePart, TIMESTAMP_PARSER)
        "$prefix · ${dateTime.format(DISPLAY_FORMATTER)}"
    } catch (_: DateTimeParseException) {
        rawTitle
    }
}

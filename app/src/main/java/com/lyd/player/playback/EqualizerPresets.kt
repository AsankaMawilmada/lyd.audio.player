package com.lyd.player.playback

const val CUSTOM_PRESET_NAME = "Custom"
const val NORMAL_PRESET_NAME = "Normal"

/**
 * Each preset is a smooth curve of relative gain (-1f full cut .. 1f full boost) sampled at
 * normalized frequency positions from 0 (lowest band) to 1 (highest band). Applying a preset
 * interpolates this curve across however many bands the device equalizer actually reports,
 * then scales by that band's own min/max milliBel range — so it works identically whether the
 * device exposes 5 bands or 10.
 */
private data class CurvePoint(val position: Float, val relativeGain: Float)

private val PRESET_CURVES: Map<String, List<CurvePoint>> = mapOf(
    NORMAL_PRESET_NAME to listOf(
        CurvePoint(0f, 0f), CurvePoint(1f, 0f),
    ),
    "Pop" to listOf(
        CurvePoint(0f, -0.2f), CurvePoint(0.25f, 0.1f), CurvePoint(0.5f, 0.3f),
        CurvePoint(0.75f, 0.15f), CurvePoint(1f, -0.1f),
    ),
    "Rock" to listOf(
        CurvePoint(0f, 0.5f), CurvePoint(0.25f, 0.25f), CurvePoint(0.5f, -0.15f),
        CurvePoint(0.75f, 0.2f), CurvePoint(1f, 0.4f),
    ),
    "Jazz" to listOf(
        CurvePoint(0f, 0.3f), CurvePoint(0.25f, 0.2f), CurvePoint(0.5f, 0.05f),
        CurvePoint(0.75f, 0.2f), CurvePoint(1f, 0.3f),
    ),
    "Classical" to listOf(
        CurvePoint(0f, 0.15f), CurvePoint(0.25f, 0.05f), CurvePoint(0.5f, 0f),
        CurvePoint(0.75f, 0.1f), CurvePoint(1f, 0.3f),
    ),
    "Dance" to listOf(
        CurvePoint(0f, 0.6f), CurvePoint(0.25f, 0.3f), CurvePoint(0.5f, -0.2f),
        CurvePoint(0.75f, 0.15f), CurvePoint(1f, 0.2f),
    ),
    "Bass boost" to listOf(
        CurvePoint(0f, 1f), CurvePoint(0.25f, 0.5f), CurvePoint(0.5f, 0f),
        CurvePoint(0.75f, 0f), CurvePoint(1f, 0f),
    ),
    "Treble boost" to listOf(
        CurvePoint(0f, 0f), CurvePoint(0.25f, 0f), CurvePoint(0.5f, 0f),
        CurvePoint(0.75f, 0.5f), CurvePoint(1f, 1f),
    ),
)

val EQUALIZER_PRESET_NAMES: List<String> = PRESET_CURVES.keys.toList()

/** How much of the device's available headroom a "full" (±1f) curve point should use. */
private const val PRESET_INTENSITY = 0.7f

private fun sampleCurve(curve: List<CurvePoint>, position: Float): Float {
    val sorted = curve.sortedBy { it.position }
    if (position <= sorted.first().position) return sorted.first().relativeGain
    if (position >= sorted.last().position) return sorted.last().relativeGain
    for (i in 0 until sorted.size - 1) {
        val a = sorted[i]
        val b = sorted[i + 1]
        if (position in a.position..b.position) {
            val t = if (b.position == a.position) 0f else (position - a.position) / (b.position - a.position)
            return a.relativeGain + (b.relativeGain - a.relativeGain) * t
        }
    }
    return 0f
}

/**
 * Computes the target milliBel level for each band for [presetName], clamped to that band's
 * own [minMilliBel]/[maxMilliBel] range.
 */
fun computePresetLevels(
    presetName: String,
    bandCount: Int,
    minMilliBel: (bandIndex: Int) -> Int,
    maxMilliBel: (bandIndex: Int) -> Int,
): List<Int> {
    val curve = PRESET_CURVES[presetName] ?: PRESET_CURVES.getValue(NORMAL_PRESET_NAME)
    return (0 until bandCount).map { index ->
        val position = if (bandCount <= 1) 0f else index / (bandCount - 1).toFloat()
        val relativeGain = sampleCurve(curve, position) * PRESET_INTENSITY
        val min = minMilliBel(index)
        val max = maxMilliBel(index)
        val raw = if (relativeGain >= 0) relativeGain * max else -relativeGain * min
        raw.toInt().coerceIn(min, max)
    }
}

package com.lyd.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerPresetsTest {

    private val min: (Int) -> Int = { -1500 }
    private val max: (Int) -> Int = { 1500 }

    @Test
    fun `balanced preset applies zero gain to every band`() {
        val levels = computePresetLevels(BALANCED_PRESET_NAME, bandCount = 9, minMilliBel = min, maxMilliBel = max)
        assertTrue(levels.all { it == 0 })
    }

    @Test
    fun `bass boost favors the lowest band over the highest`() {
        val levels = computePresetLevels("Bass Boost", bandCount = 9, minMilliBel = min, maxMilliBel = max)
        assertTrue(levels.first() > levels.last())
    }

    @Test
    fun `treble boost favors the highest band over the lowest`() {
        val levels = computePresetLevels("Treble Boost", bandCount = 9, minMilliBel = min, maxMilliBel = max)
        assertTrue(levels.last() > levels.first())
    }

    @Test
    fun `dynamic dips the middle band relative to the outer bands`() {
        val levels = computePresetLevels("Dynamic", bandCount = 9, minMilliBel = min, maxMilliBel = max)
        val middle = levels[4]
        assertTrue(middle < levels.first())
        assertTrue(middle < levels.last())
    }

    @Test
    fun `levels are always clamped within the device reported range`() {
        val narrowMin: (Int) -> Int = { -100 }
        val narrowMax: (Int) -> Int = { 100 }
        val levels = computePresetLevels("Bass Boost", bandCount = 9, minMilliBel = narrowMin, maxMilliBel = narrowMax)
        assertTrue(levels.all { it in -100..100 })
    }

    @Test
    fun `works for a single band without dividing by zero`() {
        val levels = computePresetLevels("Smooth", bandCount = 1, minMilliBel = min, maxMilliBel = max)
        assertEquals(1, levels.size)
    }

    @Test
    fun `unknown preset name falls back to balanced (all zero)`() {
        val levels = computePresetLevels("Not a real preset", bandCount = 9, minMilliBel = min, maxMilliBel = max)
        assertTrue(levels.all { it == 0 })
    }
}

package com.lyd.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerPresetsTest {

    private val min: (Int) -> Int = { -1500 }
    private val max: (Int) -> Int = { 1500 }

    @Test
    fun `normal preset applies zero gain to every band`() {
        val levels = computePresetLevels(NORMAL_PRESET_NAME, bandCount = 5, minMilliBel = min, maxMilliBel = max)
        assertTrue(levels.all { it == 0 })
    }

    @Test
    fun `bass boost favors the lowest band over the highest`() {
        val levels = computePresetLevels("Bass boost", bandCount = 5, minMilliBel = min, maxMilliBel = max)
        assertTrue(levels.first() > levels.last())
    }

    @Test
    fun `treble boost favors the highest band over the lowest`() {
        val levels = computePresetLevels("Treble boost", bandCount = 5, minMilliBel = min, maxMilliBel = max)
        assertTrue(levels.last() > levels.first())
    }

    @Test
    fun `levels are always clamped within the device reported range`() {
        val narrowMin: (Int) -> Int = { -100 }
        val narrowMax: (Int) -> Int = { 100 }
        val levels = computePresetLevels("Bass boost", bandCount = 5, minMilliBel = narrowMin, maxMilliBel = narrowMax)
        assertTrue(levels.all { it in -100..100 })
    }

    @Test
    fun `works for a single band without dividing by zero`() {
        val levels = computePresetLevels("Rock", bandCount = 1, minMilliBel = min, maxMilliBel = max)
        assertEquals(1, levels.size)
    }

    @Test
    fun `unknown preset name falls back to normal (all zero)`() {
        val levels = computePresetLevels("Not a real preset", bandCount = 5, minMilliBel = min, maxMilliBel = max)
        assertTrue(levels.all { it == 0 })
    }
}

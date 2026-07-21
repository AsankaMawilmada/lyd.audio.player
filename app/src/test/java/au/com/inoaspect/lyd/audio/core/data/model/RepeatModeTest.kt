package au.com.inoaspect.lyd.audio.core.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatModeTest {

    @Test
    fun `cycles Off to All to One and back to Off`() {
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next())
        assertEquals(RepeatMode.OFF, RepeatMode.ONE.next())
    }
}

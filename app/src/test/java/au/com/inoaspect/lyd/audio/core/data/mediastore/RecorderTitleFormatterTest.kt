package au.com.inoaspect.lyd.audio.core.data.mediastore

import org.junit.Assert.assertEquals
import org.junit.Test

class RecorderTitleFormatterTest {

    @Test
    fun `recorder-style filename becomes a readable date`() {
        val result = humanizeRecorderTitle("MCR_20230815_143022")
        assertEquals("MCR · Aug 15, 2023, 2:30 PM", result)
    }

    @Test
    fun `hyphen separators are also recognized`() {
        val result = humanizeRecorderTitle("REC-20240101-090000")
        assertEquals("REC · Jan 1, 2024, 9:00 AM", result)
    }

    @Test
    fun `real song titles are left untouched`() {
        assertEquals("Bohemian Rhapsody", humanizeRecorderTitle("Bohemian Rhapsody"))
    }

    @Test
    fun `titles that merely contain digits are left untouched`() {
        assertEquals("Track 09", humanizeRecorderTitle("Track 09"))
    }

    @Test
    fun `matching shape with an invalid date falls back to the raw title`() {
        val raw = "MCR_20231399_999999"
        assertEquals(raw, humanizeRecorderTitle(raw))
    }

    @Test
    fun `blank title is left untouched`() {
        assertEquals("", humanizeRecorderTitle(""))
    }
}

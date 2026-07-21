package au.com.inoaspect.lyd.audio.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import au.com.inoaspect.lyd.audio.core.data.model.Song

/** Shared per-screen state for the song overflow menu + add-to-playlist sheet combo. */
class SongActionsState {
    var menuSong by mutableStateOf<Song?>(null)
    var addToPlaylistPaths by mutableStateOf<List<String>?>(null)
    var addToPlaylistLabel by mutableStateOf<String?>(null)
}

@Composable
fun rememberSongActionsState(): SongActionsState = remember { SongActionsState() }

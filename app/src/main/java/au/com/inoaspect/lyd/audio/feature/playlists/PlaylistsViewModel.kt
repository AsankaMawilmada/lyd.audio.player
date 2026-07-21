package au.com.inoaspect.lyd.audio.feature.playlists

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.inoaspect.lyd.audio.core.data.model.Playlist
import au.com.inoaspect.lyd.audio.core.data.model.Song
import au.com.inoaspect.lyd.audio.core.data.repo.LibraryRepository
import au.com.inoaspect.lyd.audio.core.data.repo.PlaylistRepository
import au.com.inoaspect.lyd.audio.core.util.M3uPlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistWithResolvedSongs(val playlist: Playlist, val songs: List<Song>)

/** [matchedCount] of [totalCount] paths in the M3U file matched a song in the local library. */
data class ImportResult(val matchedCount: Int, val totalCount: Int)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistWithResolvedSongs>> =
        combine(playlistRepository.observePlaylists(), libraryRepository.songs) { playlists, _ ->
            playlists.map { PlaylistWithResolvedSongs(it, libraryRepository.songsByPaths(it.songPaths)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Reads an M3U file from SAF [uri], matches its paths against the local library, and creates a playlist. */
    fun importPlaylist(contentResolver: ContentResolver, uri: Uri, onDone: (ImportResult?) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val text = contentResolver.openInputStream(uri)?.use { it.reader().readText() } ?: error("Could not open file")
                val paths = M3uPlaylist.parsePaths(text)
                val matched = libraryRepository.songsByPaths(paths)
                val name = displayName(contentResolver, uri)?.substringBeforeLast('.') ?: "Imported playlist"
                playlistRepository.createPlaylist(name, matched.map { it.path })
                ImportResult(matched.size, paths.size)
            }.getOrNull()
            onDone(result)
        }
    }

    private fun displayName(contentResolver: ContentResolver, uri: Uri): String? =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}

package au.com.inoaspect.lyd.audio.playback

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import au.com.inoaspect.lyd.audio.core.data.art.ArtworkRepository
import au.com.inoaspect.lyd.audio.core.data.model.Song
import au.com.inoaspect.lyd.audio.core.data.repo.LibraryRepository
import au.com.inoaspect.lyd.audio.core.data.repo.PlaylistRepository
import au.com.inoaspect.lyd.audio.core.data.repo.RecentPlaysRepository
import au.com.inoaspect.lyd.audio.core.data.repo.search
import au.com.inoaspect.lyd.audio.core.data.repo.toAlbums
import au.com.inoaspect.lyd.audio.core.data.repo.toArtists
import au.com.inoaspect.lyd.audio.core.data.repo.toFolders
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hosts the single [ExoPlayer] instance and its [MediaLibrarySession] for background/lock-screen
 * playback *and* Android Auto / media-browser clients. Runs in the app's main process (no
 * android:process split) so the Hilt singletons here (equalizer, sleep timer, recent-plays,
 * library) are shared directly with [PlayerController].
 */
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var recentPlaysRepository: RecentPlaysRepository
    @Inject lateinit var sleepTimerController: SleepTimerController
    @Inject lateinit var equalizerController: EqualizerController
    @Inject lateinit var libraryRepository: LibraryRepository
    @Inject lateinit var playlistRepository: PlaylistRepository
    @Inject lateinit var artworkRepository: ArtworkRepository

    private var mediaSession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player = ExoPlayer.Builder(this)
            // handleAudioFocus=true makes ExoPlayer request focus on play() and react to
            // losing it — pausing when another app (YouTube, a call, another player) starts
            // its own playback, ducking for transient interruptions, and resuming afterward
            // where appropriate — instead of two audio streams playing over each other.
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            // Pause rather than keep playing into a now-disconnected output (e.g. headphones
            // unplugged), matching standard Android media-app behavior.
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(playerListener)
        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback()).build()
        // NOT player.audioSessionId here: right after build() the audio sink hasn't been
        // configured with a real format yet, so this is still 0 (unset) and attaching the
        // Equalizer to it is a no-op on most devices. The real session id only becomes
        // available once playback actually starts, via onAudioSessionIdChanged below.

        // Android Auto can bind this service in a freshly started process before MainActivity
        // (and therefore AppViewModel's own bootstrap) ever runs — make sure the library/
        // Favorites exist independent of whether the phone UI has been opened yet.
        serviceScope.launch {
            playlistRepository.ensureFavoritesPlaylist()
            if (libraryRepository.songs.value == null) libraryRepository.rescan()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onDestroy() {
        equalizerController.release()
        serviceScope.cancel()
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val path = mediaItem?.mediaId ?: return
            serviceScope.launch { recentPlaysRepository.record(path) }
            sleepTimerController.onTrackChanged()
        }

        // Fires once the audio sink assigns its first real session id, and again if the device
        // ever has to allocate a new one (e.g. a track with a different sample rate/channel
        // config forces the AudioTrack to be recreated) — re-attaching keeps the Equalizer
        // pointed at whatever session is actually producing sound.
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            equalizerController.attach(audioSessionId)
        }
    }

    /**
     * Resolves Android Auto's (and any other media-browser client's) content tree, search, and
     * playback requests against the exact same repositories the phone UI reads from — see
     * [BrowseTree] for how the tree itself is shaped.
     */
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(BrowseTree.rootItem(), params))

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = serviceScope.future {
            val song = libraryRepository.songByPath(mediaId)
            if (song != null) {
                LibraryResult.ofItem(BrowseTree.songNode(song, artUriFor(song.albumId, song.mediaStoreId)), null)
            } else {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            }
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = serviceScope.future {
            val children = buildChildren(parentId)
            if (children == null) {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            } else {
                val paged = children.drop(page * pageSize).take(pageSize)
                LibraryResult.ofItemList(ImmutableList.copyOf(paged), params)
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = serviceScope.future {
            val results = libraryRepository.songs.value.orEmpty().search(query)
            session.notifySearchResultChanged(browser, query, results.size, params)
            LibraryResult.ofVoid(params)
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = serviceScope.future {
            val songs = libraryRepository.songs.value.orEmpty().search(query)
            val art = artMapFor(songs)
            val nodes = songs.map { BrowseTree.songNode(it, art[it.albumId]) }.drop(page * pageSize).take(pageSize)
            LibraryResult.ofItemList(ImmutableList.copyOf(nodes), params)
        }

        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = serviceScope.future {
            val resolved = mediaItems.mapNotNull { libraryRepository.songByPath(it.mediaId) }
            val (finalSongs, finalStartIndex) = if (resolved.size == 1) {
                // A single resolved song (voice command, or "play" on a lone search result) —
                // queue its whole album for context, matching the phone's "Playing from Album"
                // behavior so next/previous do something sensible in the car.
                val song = resolved[0]
                val albumSongs = libraryRepository.songsForAlbum(song.albumId).sortedBy { it.title.lowercase() }
                val index = albumSongs.indexOfFirst { it.path == song.path }.coerceAtLeast(0)
                albumSongs to index
            } else {
                resolved to startIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))
            }
            val art = artMapFor(finalSongs)
            val items = finalSongs.map { it.toMediaItem(art[it.albumId]) }
            MediaSession.MediaItemsWithStartPosition(items, finalStartIndex, startPositionMs)
        }

        private suspend fun buildChildren(nodeId: String): List<MediaItem>? {
            val allSongs = libraryRepository.songs.value.orEmpty()
            return BrowseTree.childrenFor(
                nodeId = nodeId,
                allSongs = allSongs,
                albums = allSongs.toAlbums(),
                artists = allSongs.toArtists(),
                folders = allSongs.toFolders(),
                playlists = playlistRepository.currentPlaylists(),
                recentSongs = recentPlaysRepository.currentRecentSongs(),
                favoriteSongs = libraryRepository.songsByPaths(playlistRepository.currentFavoritePaths().toList()),
                artByAlbumId = artMapFor(allSongs),
            )
        }

        private suspend fun artUriFor(albumId: Long, representativeMediaStoreId: Long): Uri? =
            artworkRepository.getArtFile(albumId, representativeMediaStoreId)?.let(Uri::fromFile)

        private suspend fun artMapFor(songs: List<Song>): Map<Long, Uri?> =
            songs.distinctBy { it.albumId }.associate { it.albumId to artUriFor(it.albumId, it.mediaStoreId) }
    }
}

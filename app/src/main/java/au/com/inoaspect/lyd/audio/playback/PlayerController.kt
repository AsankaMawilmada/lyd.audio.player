package au.com.inoaspect.lyd.audio.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import au.com.inoaspect.lyd.audio.core.data.art.ArtworkRepository
import au.com.inoaspect.lyd.audio.core.data.model.RepeatMode
import au.com.inoaspect.lyd.audio.core.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackUiState(
    val currentItem: QueueItem? = null,
    val queue: List<QueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playingFrom: String = "",
) {
    val hasMedia: Boolean get() = currentItem != null
}

/**
 * App-facing wrapper around a [MediaController] bound to [PlaybackService]'s session. This is
 * the single entry point the whole UI layer uses for transport/queue operations — it never
 * touches [androidx.media3.exoplayer.ExoPlayer] directly.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artworkRepository: ArtworkRepository,
    private val sleepTimerController: SleepTimerController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controllerDeferred = CompletableDeferred<MediaController>()
    private var controller: MediaController? = null

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val mediaController = MediaController.Builder(context, sessionToken)
                .buildAsync()
                .awaitFuture(ContextCompat.getMainExecutor(context))
            controller = mediaController
            mediaController.addListener(playerListener)
            controllerDeferred.complete(mediaController)
            syncStateFromController()
            startPositionTicker()
        }
        scope.launch {
            sleepTimerController.fireEvents.collect { controller?.pause() }
        }
    }

    private fun startPositionTicker() {
        scope.launch {
            while (isActive) {
                controller?.let { c ->
                    _uiState.update {
                        it.copy(positionMs = c.currentPosition, durationMs = c.duration.coerceAtLeast(0))
                    }
                }
                delay(500)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            // Rebuilding the queue walks every item over the MediaController's binder connection —
            // only do that when the timeline actually changed, not on every play/pause/position event.
            val rebuildQueue = events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)
            syncStateFromController(rebuildQueue)
        }
    }

    private fun syncStateFromController(rebuildQueue: Boolean = true) {
        val c = controller ?: return
        val currentIndex = c.currentMediaItemIndex
        _uiState.update { state ->
            val queue = if (rebuildQueue) {
                (0 until c.mediaItemCount).map { c.getMediaItemAt(it).toQueueItem() }
            } else {
                state.queue
            }
            state.copy(
                currentItem = queue.getOrNull(currentIndex),
                queue = queue,
                currentIndex = currentIndex,
                isPlaying = c.isPlaying,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                durationMs = c.duration.coerceAtLeast(0),
                positionMs = c.currentPosition,
                shuffleEnabled = c.shuffleModeEnabled,
                repeatMode = when (c.repeatMode) {
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    else -> RepeatMode.OFF
                },
            )
        }
    }

    suspend fun playList(songs: List<Song>, startIndex: Int = 0, shuffled: Boolean = false, source: String) {
        if (songs.isEmpty()) return
        val c = awaitController()
        val items = buildMediaItems(songs)
        _uiState.update { it.copy(playingFrom = source) }
        c.shuffleModeEnabled = shuffled
        c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    suspend fun playSong(song: Song, source: String) = playList(listOf(song), 0, false, source)

    suspend fun appendToQueue(song: Song) {
        val c = awaitController()
        c.addMediaItem(buildMediaItem(song))
    }

    suspend fun playNext(song: Song) {
        val c = awaitController()
        val insertAt = (c.currentMediaItemIndex + 1).coerceIn(0, c.mediaItemCount)
        c.addMediaItem(insertAt, buildMediaItem(song))
    }

    fun removeFromQueue(index: Int) {
        controller?.removeMediaItem(index)
    }

    /** Used after a song is deleted from disk, so a copy of it doesn't linger in the live queue. */
    fun removeFromQueueByPath(path: String) {
        val index = _uiState.value.queue.indexOfFirst { it.path == path }
        if (index >= 0) controller?.removeMediaItem(index)
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        controller?.moveMediaItem(fromIndex, toIndex)
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun seekToIndex(index: Int) {
        controller?.seekTo(index, 0L)
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNext()
    }

    /** ExoPlayer's default seekToPrevious() already restarts the track if >3s elapsed, else
     *  skips to the previous item — exactly the spec's "smart previous" behavior. */
    fun smartPrevious() {
        controller?.seekToPrevious()
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let { c ->
            c.repeatMode = when (c.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    private suspend fun awaitController(): MediaController = controllerDeferred.await()

    private suspend fun buildMediaItems(songs: List<Song>): List<MediaItem> {
        val artByAlbum = songs.distinctBy { it.albumId }.associate { song ->
            song.albumId to artworkRepository.getArtFile(song.albumId, song.mediaStoreId)
        }
        return songs.map { song -> song.toMediaItem(artByAlbum[song.albumId]?.let(Uri::fromFile)) }
    }

    private suspend fun buildMediaItem(song: Song): MediaItem {
        val art = artworkRepository.getArtFile(song.albumId, song.mediaStoreId)
        return song.toMediaItem(art?.let(Uri::fromFile))
    }
}

package com.lyd.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lyd.player.core.data.repo.RecentPlaysRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hosts the single [ExoPlayer] instance and its [MediaSession] for background/lock-screen
 * playback. Runs in the app's main process (no android:process split) so the Hilt singletons
 * here (equalizer, sleep timer, recent-plays) are shared directly with [PlayerController].
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var recentPlaysRepository: RecentPlaysRepository
    @Inject lateinit var sleepTimerController: SleepTimerController
    @Inject lateinit var equalizerController: EqualizerController

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        player.addListener(playerListener)
        mediaSession = MediaSession.Builder(this, player).build()
        // NOT player.audioSessionId here: right after build() the audio sink hasn't been
        // configured with a real format yet, so this is still 0 (unset) and attaching the
        // Equalizer to it is a no-op on most devices. The real session id only becomes
        // available once playback actually starts, via onAudioSessionIdChanged below.
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

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
}

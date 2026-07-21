package au.com.inoaspect.lyd.audio.playback

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EqBand(
    val index: Int,
    val centerFreqHz: Int,
    val minMilliBel: Int,
    val maxMilliBel: Int,
    val levelMilliBel: Int,
)

data class EqualizerUiState(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val bands: List<EqBand> = emptyList(),
    val presets: List<String> = EQUALIZER_PRESET_NAMES,
    val activePreset: String = BALANCED_PRESET_NAME,
)

/**
 * Wraps the device-native [Equalizer] audio effect attached to the current playback session.
 * Band count/frequencies/range all come from whatever the device reports — nothing here is
 * hardcoded to a specific layout, so every band the UI shows is a real, independent one (no two
 * sliders ever share the same underlying gain). Settings persist for the lifetime of the app
 * process (they are re-applied on every [attach], e.g. after the playback service is recreated)
 * but are not required to survive a full process death.
 */
@Singleton
class EqualizerController @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var pendingEnabled = true
    private var pendingPreset = BALANCED_PRESET_NAME
    private var pendingCustomLevels: List<Int>? = null

    private val _state = MutableStateFlow(EqualizerUiState())
    val state: StateFlow<EqualizerUiState> = _state.asStateFlow()

    fun attach(audioSessionId: Int) {
        release()
        val eq = try {
            Equalizer(0, audioSessionId)
        } catch (_: Exception) {
            _state.value = EqualizerUiState(supported = false)
            return
        }
        equalizer = eq
        eq.enabled = pendingEnabled

        val bands = (0 until eq.numberOfBands).map { i ->
            val range = eq.getBandLevelRange()
            EqBand(
                index = i,
                centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000,
                minMilliBel = range[0].toInt(),
                maxMilliBel = range[1].toInt(),
                levelMilliBel = eq.getBandLevel(i.toShort()).toInt(),
            )
        }
        _state.value = EqualizerUiState(
            supported = true,
            enabled = pendingEnabled,
            bands = bands,
            activePreset = pendingPreset,
        )

        val customLevels = pendingCustomLevels
        if (pendingPreset == CUSTOM_PRESET_NAME && customLevels != null && customLevels.size == bands.size) {
            customLevels.forEachIndexed { index, level -> eq.setBandLevel(index.toShort(), level.toShort()) }
            syncBandsFromEffect()
        } else {
            applyPreset(pendingPreset)
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }

    fun setEnabled(enabled: Boolean) {
        pendingEnabled = enabled
        equalizer?.enabled = enabled
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun setBandLevel(bandIndex: Int, levelMilliBel: Int) {
        val eq = equalizer ?: return
        val band = _state.value.bands.getOrNull(bandIndex) ?: return
        val clamped = levelMilliBel.coerceIn(band.minMilliBel, band.maxMilliBel)
        eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
        pendingPreset = CUSTOM_PRESET_NAME
        pendingCustomLevels = _state.value.bands.map { if (it.index == bandIndex) clamped else it.levelMilliBel }
        syncBandsFromEffect()
    }

    fun applyPreset(name: String) {
        val eq = equalizer ?: return
        val bands = _state.value.bands
        if (bands.isEmpty()) return
        val levels = computePresetLevels(
            presetName = name,
            bandCount = bands.size,
            minMilliBel = { bands[it].minMilliBel },
            maxMilliBel = { bands[it].maxMilliBel },
        )
        levels.forEachIndexed { index, level -> eq.setBandLevel(index.toShort(), level.toShort()) }
        pendingPreset = name
        pendingCustomLevels = if (name == CUSTOM_PRESET_NAME) levels else null
        syncBandsFromEffect(activePreset = name)
    }

    private fun syncBandsFromEffect(activePreset: String = pendingPreset) {
        val eq = equalizer ?: return
        val bands = _state.value.bands.map { band ->
            band.copy(levelMilliBel = eq.getBandLevel(band.index.toShort()).toInt())
        }
        _state.value = _state.value.copy(bands = bands, activePreset = activePreset)
    }
}

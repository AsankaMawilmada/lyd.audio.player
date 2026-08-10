package au.com.inoaspect.lyd.audio.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "equalizer"
private const val KEY_ENABLED = "enabled"
private const val KEY_PRESET = "preset"
private const val KEY_CUSTOM_LEVELS = "custom_levels"

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
 * sliders ever share the same underlying gain). Settings are persisted to [SharedPreferences] so
 * they survive process death and are re-applied on every [attach] (e.g. after the playback
 * service is recreated).
 */
@Singleton
class EqualizerController @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var pendingEnabled = prefs.getBoolean(KEY_ENABLED, true)
    private var pendingPreset = prefs.getString(KEY_PRESET, BALANCED_PRESET_NAME) ?: BALANCED_PRESET_NAME
    private var pendingCustomLevels: List<Int>? = prefs.getString(KEY_CUSTOM_LEVELS, null)
        ?.split(',')
        ?.mapNotNull { it.toIntOrNull() }
        ?.takeIf { it.isNotEmpty() }

    private val _state = MutableStateFlow(EqualizerUiState())
    val state: StateFlow<EqualizerUiState> = _state.asStateFlow()

    /** Active preset name for display elsewhere (e.g. a Home-tab badge), or null if there's nothing worth flagging. */
    private val _badgeText = MutableStateFlow(computeBadgeText())
    val badgeText: StateFlow<String?> = _badgeText.asStateFlow()

    private fun computeBadgeText(): String? =
        if (pendingEnabled && pendingPreset != BALANCED_PRESET_NAME) pendingPreset else null

    private fun persistAndRefreshBadge() {
        prefs.edit {
            putBoolean(KEY_ENABLED, pendingEnabled)
            putString(KEY_PRESET, pendingPreset)
            putString(KEY_CUSTOM_LEVELS, pendingCustomLevels?.joinToString(","))
        }
        _badgeText.value = computeBadgeText()
    }

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
        persistAndRefreshBadge()
    }

    fun setBandLevel(bandIndex: Int, levelMilliBel: Int) {
        val eq = equalizer ?: return
        val band = _state.value.bands.getOrNull(bandIndex) ?: return
        val clamped = levelMilliBel.coerceIn(band.minMilliBel, band.maxMilliBel)
        eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
        pendingPreset = CUSTOM_PRESET_NAME
        pendingCustomLevels = _state.value.bands.map { if (it.index == bandIndex) clamped else it.levelMilliBel }
        syncBandsFromEffect()
        persistAndRefreshBadge()
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
        persistAndRefreshBadge()
    }

    private fun syncBandsFromEffect(activePreset: String = pendingPreset) {
        val eq = equalizer ?: return
        val bands = _state.value.bands.map { band ->
            band.copy(levelMilliBel = eq.getBandLevel(band.index.toShort()).toInt())
        }
        _state.value = _state.value.copy(bands = bands, activePreset = activePreset)
    }
}

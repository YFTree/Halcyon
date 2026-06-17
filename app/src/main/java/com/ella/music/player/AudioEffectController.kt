package com.ella.music.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Full snapshot of the user's audio-effect configuration, persisted in settings and applied to
 * whichever audio session is currently playing.
 *
 * [eqBandLevelsMb] holds the per-band gain in millibels (1 dB = 100 mB). It is stored explicitly
 * (presets are resolved to concrete band levels in the UI), so the engine never has to call
 * Equalizer.usePreset() at playback time — that keeps a future swap to a custom 10-band DSP a
 * drop-in change behind [AudioEffectController].
 */
data class AudioEffectSettings(
    val eqEnabled: Boolean = false,
    val eqPreset: Int = PRESET_CUSTOM,
    val eqBandLevelsMb: List<Int> = emptyList(),
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 0,
    val reverbPreset: Int = REVERB_PRESET_OFF
) {
    companion object {
        const val PRESET_CUSTOM = -1
        const val STRENGTH_MAX = 1000
        const val REVERB_PRESET_OFF = 0
        const val REVERB_PRESET_STUDIO = 10
        const val REVERB_PRESET_SMALL_ROOM = 1
        const val REVERB_PRESET_MEDIUM_ROOM = 2
        const val REVERB_PRESET_LARGE_ROOM = 3
        const val REVERB_PRESET_HALL = 4
        const val REVERB_PRESET_CHURCH = 5
        const val REVERB_PRESET_PLATE = 6
    }
}

data class ReverbAuxEffectInfo(
    val effectId: Int,
    val sendLevel: Float
)

/**
 * Static capabilities of the device's audio-effect hardware for the bound session, published so
 * the settings UI can render the right number of bands at the right frequencies without itself
 * touching AudioEffect APIs.
 */
data class EqualizerCapabilities(
    val supported: Boolean,
    val bandCount: Int,
    val centerFreqsHz: List<Int>,
    val displayBandCount: Int = FIXED_EQ_BAND_COUNT,
    val displayCenterFreqsHz: List<Int> = FIXED_EQ_CENTER_FREQS_HZ,
    val minLevelMb: Int,
    val maxLevelMb: Int,
    val presetNames: List<String>,
    /** presetIndex -> per-band levels in millibels, used by the UI when a preset is selected. */
    val presetBandLevelsMb: List<List<Int>>,
    val bassBoostSupported: Boolean,
    val virtualizerSupported: Boolean,
    val reverbSupported: Boolean,
    /** Whether the device exposes a *variable* strength control (vs. plain on/off). */
    val bassBoostStrengthAdjustable: Boolean,
    val virtualizerStrengthAdjustable: Boolean
) {
    companion object {
        val Unsupported = EqualizerCapabilities(
            supported = false,
            bandCount = 0,
            centerFreqsHz = emptyList(),
            displayBandCount = FIXED_EQ_BAND_COUNT,
            displayCenterFreqsHz = FIXED_EQ_CENTER_FREQS_HZ,
            minLevelMb = -1500,
            maxLevelMb = 1500,
            presetNames = emptyList(),
            presetBandLevelsMb = emptyList(),
            bassBoostSupported = false,
            virtualizerSupported = false,
            reverbSupported = false,
            bassBoostStrengthAdjustable = false,
            virtualizerStrengthAdjustable = false
        )
    }
}

/** Process-global publisher for the bound session's effect capabilities (like [PlaybackAudioSession]). */
object AudioEffectState {
    private val _capabilities = MutableStateFlow<EqualizerCapabilities?>(null)
    val capabilities: StateFlow<EqualizerCapabilities?> = _capabilities.asStateFlow()

    internal fun publish(capabilities: EqualizerCapabilities?) {
        _capabilities.value = capabilities
    }
}

const val FIXED_EQ_BAND_COUNT = 10
val FIXED_EQ_CENTER_FREQS_HZ = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

/**
 * Owns the [Equalizer], [BassBoost], [Virtualizer], and [PresetReverb] effects for playback.
 * Created and driven by [PlaybackService] so effects stay alive for the whole playback
 * lifetime (independent of any UI). The settings UI communicates only through persisted
 * [AudioEffectSettings] and reads [AudioEffectState] for rendering.
 */
class AudioEffectController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var boundSessionId: Int = -1
    private var lastSettings: AudioEffectSettings = AudioEffectSettings()

    /** Attach effects to [sessionId], publish its capabilities, and re-apply the last settings. */
    fun bind(sessionId: Int) {
        if (sessionId <= 0) return
        if (sessionId == boundSessionId && equalizer != null) return
        release()
        boundSessionId = sessionId

        equalizer = runCatching { Equalizer(0, sessionId) }
            .onFailure { Log.w(TAG, "Equalizer unavailable on session $sessionId", it) }
            .getOrNull()
        bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()
        virtualizer = runCatching { Virtualizer(0, sessionId) }.getOrNull()
        presetReverb = runCatching { PresetReverb(0, 0) }
            .onFailure { Log.w(TAG, "PresetReverb unavailable", it) }
            .getOrNull()

        AudioEffectState.publish(captureCapabilities())
        apply(lastSettings)
    }

    /** Persist [settings] as the active configuration and push it onto the live effects. */
    fun apply(settings: AudioEffectSettings) {
        lastSettings = settings
        applyEqualizer(settings)
        applyBassBoost(settings)
        applyVirtualizer(settings)
        applyPresetReverb(settings)
    }

    fun reverbAuxEffectInfo(): ReverbAuxEffectInfo? {
        val effect = presetReverb ?: return null
        val config = reverbPresetConfig(lastSettings.reverbPreset) ?: return null
        val effectId = runCatching { effect.id }.getOrDefault(0)
        return effectId.takeIf { it > 0 }?.let { ReverbAuxEffectInfo(it, config.sendLevel) }
    }

    private fun applyEqualizer(settings: AudioEffectSettings) {
        val eq = equalizer ?: return
        runCatching {
            val bandCount = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val min = range[0].toInt()
            val max = range[1].toInt()
            for (band in 0 until bandCount) {
                val freqHz = runCatching { eq.getCenterFreq(band.toShort()) / 1000 }.getOrDefault(0)
                val profileBand = nearestDisplayBandIndex(freqHz)
                val levelMb = settings.eqBandLevelsMb.getOrElse(profileBand) { 0 }.coerceIn(min, max)
                runCatching { eq.setBandLevel(band.toShort(), levelMb.toShort()) }
            }
            eq.enabled = settings.eqEnabled
        }
    }

    private fun applyBassBoost(settings: AudioEffectSettings) {
        val effect = bassBoost ?: return
        runCatching {
            // Enable before setting strength: some devices ignore setStrength() while disabled.
            effect.enabled = settings.bassBoostEnabled
            if (settings.bassBoostEnabled && effect.strengthSupported) {
                effect.setStrength(settings.bassBoostStrength.coerceIn(0, AudioEffectSettings.STRENGTH_MAX).toShort())
            }
        }
    }

    private fun applyVirtualizer(settings: AudioEffectSettings) {
        val effect = virtualizer ?: return
        runCatching {
            effect.enabled = settings.virtualizerEnabled
            if (settings.virtualizerEnabled && effect.strengthSupported) {
                effect.setStrength(settings.virtualizerStrength.coerceIn(0, AudioEffectSettings.STRENGTH_MAX).toShort())
            }
        }
    }

    private fun applyPresetReverb(settings: AudioEffectSettings) {
        val effect = presetReverb ?: return
        val config = reverbPresetConfig(settings.reverbPreset)
        runCatching {
            if (config == null) {
                effect.enabled = false
            } else {
                effect.preset = config.androidPreset.toShort()
                effect.enabled = true
            }
        }
    }

    private fun captureCapabilities(): EqualizerCapabilities {
        val eq = equalizer ?: return EqualizerCapabilities.Unsupported.copy(
            bassBoostSupported = bassBoost != null,
            virtualizerSupported = virtualizer != null,
            reverbSupported = presetReverb != null,
            bassBoostStrengthAdjustable = runCatching { bassBoost?.strengthSupported == true }.getOrDefault(false),
            virtualizerStrengthAdjustable = runCatching { virtualizer?.strengthSupported == true }.getOrDefault(false)
        )
        return runCatching {
            val bandCount = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            val centerFreqs = (0 until bandCount).map { band ->
                // Equalizer reports center frequency in milliHertz.
                (eq.getCenterFreq(band.toShort()) / 1000)
            }
            // Scan presets while the equalizer is still disabled (no audible glitch) so the UI can
            // resolve a preset selection to concrete band levels.
            val presetCount = eq.numberOfPresets.toInt()
            val presetNames = ArrayList<String>(presetCount)
            val presetLevels = ArrayList<List<Int>>(presetCount)
            for (preset in 0 until presetCount) {
                presetNames += runCatching { eq.getPresetName(preset.toShort()) }.getOrDefault("Preset $preset")
                val levels = runCatching {
                    eq.usePreset(preset.toShort())
                    (0 until bandCount).map { band -> eq.getBandLevel(band.toShort()).toInt() }
                }.getOrDefault(List(bandCount) { 0 })
                presetLevels += levels
            }
            EqualizerCapabilities(
                supported = true,
                bandCount = bandCount,
                centerFreqsHz = centerFreqs,
                displayBandCount = FIXED_EQ_BAND_COUNT,
                displayCenterFreqsHz = FIXED_EQ_CENTER_FREQS_HZ,
                minLevelMb = minMb,
                maxLevelMb = maxMb,
                presetNames = presetNames,
                presetBandLevelsMb = presetLevels,
                bassBoostSupported = bassBoost != null,
                virtualizerSupported = virtualizer != null,
                reverbSupported = presetReverb != null,
                bassBoostStrengthAdjustable = runCatching { bassBoost?.strengthSupported == true }.getOrDefault(false),
                virtualizerStrengthAdjustable = runCatching { virtualizer?.strengthSupported == true }.getOrDefault(false)
            )
        }.getOrElse { EqualizerCapabilities.Unsupported }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { presetReverb?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        boundSessionId = -1
    }

    private companion object {
        const val TAG = "AudioEffectController"
    }
}

private data class ReverbPresetConfig(
    val androidPreset: Int,
    val sendLevel: Float
)

private fun reverbPresetConfig(preset: Int): ReverbPresetConfig? =
    when (preset) {
        AudioEffectSettings.REVERB_PRESET_STUDIO ->
            ReverbPresetConfig(PresetReverb.PRESET_SMALLROOM.toInt(), 0.18f)
        AudioEffectSettings.REVERB_PRESET_SMALL_ROOM ->
            ReverbPresetConfig(PresetReverb.PRESET_SMALLROOM.toInt(), 0.35f)
        AudioEffectSettings.REVERB_PRESET_MEDIUM_ROOM ->
            ReverbPresetConfig(PresetReverb.PRESET_MEDIUMROOM.toInt(), 0.42f)
        AudioEffectSettings.REVERB_PRESET_LARGE_ROOM ->
            ReverbPresetConfig(PresetReverb.PRESET_LARGEROOM.toInt(), 0.48f)
        AudioEffectSettings.REVERB_PRESET_HALL ->
            ReverbPresetConfig(PresetReverb.PRESET_MEDIUMHALL.toInt(), 0.56f)
        AudioEffectSettings.REVERB_PRESET_CHURCH ->
            ReverbPresetConfig(PresetReverb.PRESET_LARGEHALL.toInt(), 0.72f)
        AudioEffectSettings.REVERB_PRESET_PLATE ->
            ReverbPresetConfig(PresetReverb.PRESET_PLATE.toInt(), 0.46f)
        else -> null
    }

private fun nearestDisplayBandIndex(freqHz: Int): Int {
    if (freqHz <= 0) return 0
    var bestIndex = 0
    var bestDistance = Float.MAX_VALUE
    FIXED_EQ_CENTER_FREQS_HZ.forEachIndexed { index, center ->
        val distance = kotlin.math.abs(kotlin.math.ln(freqHz.toFloat() / center.toFloat()))
        if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    return bestIndex
}

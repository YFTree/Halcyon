package com.ella.music.ui.player

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import com.ella.music.R

/**
 * 打开系统/厂商均衡器。优先使用标准 [AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL]，
 * 这样均衡器能拿到本播放器的 audioSession 做针对性处理。若设备未注册该 action（部分
 * MIUI/魅族 ROM 的均衡器没接标准协议），则按已知厂商 Activity 兜底：
 *
 * - com.android.musicfx.ActivityMusic（AOSP MusicFX 通用入口）
 * - com.miui.misound.HeadsetSettingsActivity（MIUI 音效/听感）
 * - com.android.musicfx.SquareSoundActivity（魅族魔改的 MusicFX）
 *
 * 全部失败时再探测系统所有注册了 audio-effect 控制面板 action 的 Activity。
 */
internal fun openSystemEqualizer(context: Context, audioSessionId: Int) {
    if (audioSessionId <= 0 || audioSessionId == AudioEffect.ERROR_BAD_VALUE) {
        Toast.makeText(context, context.getString(R.string.player_no_audio_session), Toast.LENGTH_SHORT).show()
        return
    }

    val standardIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    // 1. 优先尝试标准 AudioEffect intent（若系统有注册该 action 的均衡器，启动它最自然，
    //    且能拿到 audioSession 做会话级处理）。
    val pm = context.packageManager
    val standardResolves = runCatching {
        pm.queryIntentActivities(standardIntent, 0)
    }.getOrDefault(emptyList())
    if (standardResolves.isNotEmpty()) {
        if (launchActivity(context, standardIntent)) return
    }

    // 2. 厂商已知 Activity 兜底（某些 ROM 的均衡器未注册标准 action，只能直接 setComponent）
    val vendorCandidates = listOf(
        // AOSP MusicFX 通用入口（部分 ROM 该 Activity 存在但未 export 到标准 action）
        ComponentName("com.android.musicfx", "com.android.musicfx.ActivityMusic"),
        // 魅族魔改的 MusicFX（SquareSound，Flyme 的音效设置）
        ComponentName("com.android.musicfx", "com.android.musicfx.SquareSoundActivity"),
        // MIUI 听感 / 音效设置
        ComponentName("com.miui.misound", "com.miui.misound.HeadsetSettingsActivity")
    )
    for (component in vendorCandidates) {
        val intent = Intent().apply {
            this.component = component
            // 兜底厂商入口无法接 audioSession，但音频会话本就由系统全局均衡器统一接管，
            // 这里只负责把用户带到均衡器设置页。
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchActivity(context, intent)) return
    }

    // 3. 最终探测：queryIntentActivities 找到的所有候选都试一遍
    //    （覆盖厂商 list 没覆盖到的 ROM）。
    for (resolveInfo in standardResolves) {
        val intent = Intent(standardIntent).apply {
            component = ComponentName(
                resolveInfo.activityInfo.packageName,
                resolveInfo.activityInfo.name
            )
        }
        if (launchActivity(context, intent)) return
    }

    Toast.makeText(context, context.getString(R.string.player_no_equalizer), Toast.LENGTH_SHORT).show()
}

private fun launchActivity(context: Context, intent: Intent): Boolean {
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        // 部分 ROM 的均衡器 Activity export=false，直接跳会抛 SecurityException
        false
    } catch (_: Exception) {
        false
    }
}

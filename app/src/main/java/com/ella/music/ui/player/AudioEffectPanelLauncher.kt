package com.ella.music.ui.player

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import com.ella.music.R

internal fun openSystemEqualizer(context: Context, audioSessionId: Int) {
    if (audioSessionId <= 0 || audioSessionId == AudioEffect.ERROR_BAD_VALUE) {
        Toast.makeText(context, context.getString(R.string.player_no_audio_session), Toast.LENGTH_SHORT).show()
        return
    }

    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    runCatching { context.startActivity(intent) }
        .onFailure { error ->
            if (error is ActivityNotFoundException) {
                Toast.makeText(context, context.getString(R.string.player_no_equalizer), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    context,
                    error.localizedMessage ?: context.getString(R.string.player_no_equalizer),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
}

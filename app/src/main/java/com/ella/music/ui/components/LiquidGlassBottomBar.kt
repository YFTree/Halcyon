package com.ella.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LiquidGlassBottomBar(
    backdrop: com.kyant.backdrop.Backdrop,
    isBlurEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val isLight = MiuixTheme.colorScheme.background.luminance() > 0.5f
    val containerColor = if (isBlurEnabled) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.35f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(28.dp) },
                effects = {
                    if (isBlurEnabled) {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(20f.dp.toPx(), 20f.dp.toPx())
                    }
                },
                highlight = {
                    Highlight.Default.copy(alpha = if (isBlurEnabled) 0.6f else 0f)
                },
                shadow = {
                    Shadow.Default.copy(
                        color = Color.Black.copy(alpha = if (isLight) 0.08f else 0.2f)
                    )
                },
                onDrawSurface = { drawRect(containerColor) }
            )
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun RowScope.LiquidGlassBottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    backdrop: com.kyant.backdrop.Backdrop,
    isBlurEnabled: Boolean = true,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pressProgress = remember { Animatable(0f) }
    var isPressed by remember { mutableStateOf(false) }
    val isLight = MiuixTheme.colorScheme.background.luminance() > 0.5f

    Column(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    scope.launch {
                        pressProgress.animateTo(
                            1f,
                            animationSpec = spring(stiffness = 400f, dampingRatio = 0.7f)
                        )
                    }
                    waitForUpOrCancellation()
                    isPressed = false
                    scope.launch {
                        pressProgress.animateTo(
                            0f,
                            animationSpec = spring(stiffness = 300f, dampingRatio = 0.6f)
                        )
                    }
                    onClick()
                }
            }
            .graphicsLayer {
                val scale = lerp(1f, 1.08f, pressProgress.value)
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isBlurEnabled) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(16.dp) },
                    effects = {
                        val p = pressProgress.value
                        if (p > 0.01f) {
                            vibrancy()
                            blur(4f.dp.toPx() * p)
                            lens(12f.dp.toPx() * p, 16f.dp.toPx() * p)
                        }
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = pressProgress.value * 0.8f)
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = 6f.dp * pressProgress.value,
                            alpha = pressProgress.value * 0.3f
                        )
                    },
                    onDrawSurface = {
                        val p = pressProgress.value
                        drawRect(
                            color = if (isLight) Color.Black.copy(0.06f) else Color.White.copy(0.06f),
                            alpha = p
                        )
                    }
                ) else Modifier
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.graphicsLayer { alpha = if (selected) 1f else 0.6f }) {
            icon()
        }
        Box(modifier = Modifier.graphicsLayer { alpha = if (selected) 1f else 0.5f }) {
            label()
        }
    }
}

private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

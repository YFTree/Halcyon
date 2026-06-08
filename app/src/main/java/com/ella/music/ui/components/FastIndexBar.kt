package com.ella.music.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FastIndexBar(
    letters: List<String>,
    onLetterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val indexLetters = remember(letters) { letters.toFastIndexLetters() }
    var heightPx by remember { mutableStateOf(1) }
    var contentHeightPx by remember { mutableStateOf(1) }
    var lastSelectedLetter by remember { mutableStateOf<String?>(null) }
    var lastDispatchTimeMs by remember { mutableStateOf(0L) }

    fun selectAt(y: Float, force: Boolean = false) {
        if (indexLetters.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        if (!force && now - lastDispatchTimeMs < 80L) return
        val contentTop = ((heightPx - contentHeightPx) / 2f).coerceAtLeast(0f)
        val localY = (y - contentTop).coerceIn(0f, contentHeightPx.toFloat() - 1f)
        val index = floor((localY / contentHeightPx) * indexLetters.size)
            .toInt()
            .coerceIn(0, indexLetters.lastIndex)
        val letter = indexLetters[index]
        if (letter != lastSelectedLetter) {
            lastSelectedLetter = letter
            lastDispatchTimeMs = now
            onLetterClick(letter)
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(indexLetters, heightPx, contentHeightPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    selectAt(down.position.y, force = true)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.changedToUpIgnoreConsumed()) break
                        if (change.pressed) {
                            selectAt(change.position.y)
                            change.consume()
                        }
                    }
                    lastSelectedLetter = null
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.onSizeChanged { contentHeightPx = it.height.coerceAtLeast(1) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            indexLetters.forEach { letter ->
                Text(
                    text = letter,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable {
                            lastSelectedLetter = letter
                            lastDispatchTimeMs = SystemClock.uptimeMillis()
                            onLetterClick(letter)
                        }
                        .padding(horizontal = 8.dp, vertical = 1.dp)
                )
            }
        }
    }
}

fun List<String>.toFastIndexLetters(): List<String> =
    distinct().sortedWith(compareBy<String> { if (it == "#") "ZZZ" else it })

@Composable
fun LazyListScrollIndicator(
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    val info by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val total = layoutInfo.totalItemsCount
            val visible = layoutInfo.visibleItemsInfo.size
            val first = state.firstVisibleItemIndex
            Triple(first, visible, total)
        }
    }
    ScrollIndicator(
        scrollInProgress = state.isScrollInProgress,
        firstVisibleIndex = info.first,
        visibleCount = info.second,
        totalCount = info.third,
        modifier = modifier,
        onDragToIndex = { index ->
            state.scrollToItem(index)
        }
    )
}

@Composable
fun LazyGridScrollIndicator(
    state: LazyGridState,
    modifier: Modifier = Modifier
) {
    val info by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val total = layoutInfo.totalItemsCount
            val visible = layoutInfo.visibleItemsInfo.size
            val first = state.firstVisibleItemIndex
            Triple(first, visible, total)
        }
    }
    ScrollIndicator(
        scrollInProgress = state.isScrollInProgress,
        firstVisibleIndex = info.first,
        visibleCount = info.second,
        totalCount = info.third,
        modifier = modifier,
        onDragToIndex = { index ->
            state.scrollToItem(index)
        }
    )
}

@Composable
private fun ScrollIndicator(
    scrollInProgress: Boolean,
    firstVisibleIndex: Int,
    visibleCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
    onDragToIndex: (suspend (Int) -> Unit)? = null
) {
    if (totalCount <= 0 || visibleCount <= 0 || totalCount <= visibleCount) return
    val visibleFraction = (visibleCount.toFloat() / totalCount.toFloat()).coerceIn(0.08f, 1f)
    val maxFirst = max(1, totalCount - visibleCount)
    val offsetFraction = (firstVisibleIndex.toFloat() / maxFirst.toFloat()).coerceIn(0f, 1f)
    var trackHeightPx by remember(totalCount, visibleCount) { mutableStateOf(1) }
    var visible by remember(totalCount, visibleCount) { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val thumbAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, label = "scrollThumbAlpha")
    val currentOnDragToIndex by rememberUpdatedState(onDragToIndex)

    LaunchedEffect(firstVisibleIndex, scrollInProgress, dragging) {
        if (scrollInProgress || dragging) {
            visible = true
            return@LaunchedEffect
        }
        if (visible) {
            delay(SCROLL_THUMB_IDLE_HIDE_MS)
            if (!dragging) visible = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .padding(horizontal = 4.dp, vertical = 28.dp)
            .onSizeChanged { trackHeightPx = it.height.coerceAtLeast(1) }
            .pointerInput(totalCount, visibleCount, maxFirst, trackHeightPx) {
                if (currentOnDragToIndex == null) return@pointerInput
                coroutineScope {
                    val targetIndices = Channel<Int>(Channel.CONFLATED)
                    val scrollWorker = launch {
                        for (targetIndex in targetIndices) {
                            currentOnDragToIndex?.invoke(targetIndex)
                        }
                    }
                    try {
                        awaitEachGesture {
                            var lastIndex = -1
                            var lastDispatchTimeMs = 0L

                            fun calculateIndex(y: Float): Int =
                                ((y.coerceIn(0f, trackHeightPx.toFloat()) / trackHeightPx.toFloat()) * maxFirst)
                                    .roundToInt()
                                    .coerceIn(0, maxFirst)

                            fun dispatch(y: Float, force: Boolean = false) {
                                val targetIndex = calculateIndex(y)
                                val now = SystemClock.uptimeMillis()
                                if (!force && targetIndex == lastIndex) return
                                if (!force && now - lastDispatchTimeMs < SCROLL_THUMB_DRAG_THROTTLE_MS) return
                                lastIndex = targetIndex
                                lastDispatchTimeMs = now
                                targetIndices.trySend(targetIndex)
                            }

                            val down = awaitFirstDown(requireUnconsumed = false)
                            dragging = true
                            visible = true
                            down.consume()
                            dispatch(down.position.y, force = true)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.changedToUpIgnoreConsumed()) break
                                if (change.pressed) {
                                    change.consume()
                                    dispatch(change.position.y)
                                }
                            }
                            dragging = false
                        }
                    } finally {
                        dragging = false
                        targetIndices.close()
                        scrollWorker.cancel()
                    }
                }
            }
    ) {
        val thumbHeight = maxHeight * visibleFraction
        val thumbOffset = (maxHeight - thumbHeight) * offsetFraction
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = thumbOffset)
                .height(thumbHeight.coerceAtLeast(24.dp))
                .width(4.dp)
                .alpha(thumbAlpha)
                .clip(RoundedCornerShape(999.dp))
                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.48f))
        )
    }
}

private const val SCROLL_THUMB_DRAG_THROTTLE_MS = 24L
private const val SCROLL_THUMB_IDLE_HIDE_MS = 3_000L

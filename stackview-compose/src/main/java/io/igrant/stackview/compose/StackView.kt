package io.igrant.stackview.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import io.igrant.stackview.StackConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A wallet-style stacked card view for Jetpack Compose.
 *
 * One card is **presented** at the top; the rest collapse into a stack below, each
 * showing only a [StackConfig.collapsedPeekHeight] peek strip. Tapping a stacked card
 * promotes it to the top with an animation; pulling down at the top fans the stack out
 * with a rubber-band stretch that snaps back on release; the stack scrolls when it
 * overflows the viewport.
 *
 * This is the Compose counterpart of the View `StackLayoutManager` and shares the same
 * [StackConfig] (from `io.igrant:stackview-core`), so behavior matches across both.
 *
 * @param items backing data; one card is composed per item via [cardContent].
 * @param state hoisted [StackViewState]; defaults to a remembered instance.
 * @param config layout/animation tuning. Distances are in **pixels** (see [StackConfig]).
 * @param onPresentedCardClick invoked when the already-presented card is tapped again.
 * @param cardContent renders a single card given its index and item.
 */
@Composable
fun <T> StackView(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: StackViewState = rememberStackViewState(),
    config: StackConfig = StackConfig(),
    onPresentedCardClick: (index: Int) -> Unit = {},
    cardContent: @Composable (index: Int, item: T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var snapJob by remember { mutableStateOf<Job?>(null) }

    // Drive the present / reflow animation whenever the presented card changes.
    // transition goes 0 -> 1, interpolating from the previous arrangement to the new one.
    val transition = remember { Animatable(1f) }
    LaunchedEffect(state.presentedIndex, config.animationDuration) {
        transition.snapTo(0f)
        transition.animateTo(
            targetValue = 1f,
            animationSpec = tween(config.animationDuration.toInt(), easing = FastOutSlowInEasing)
        )
    }

    val dragState = rememberDraggableState { delta ->
        snapJob?.cancel()
        // draggable delta is +down; the View dy convention is +content-up, so invert.
        state.onDrag(-delta, config)
    }

    Layout(
        modifier = modifier
            .clipToBounds()
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = {
                    if (state.stretch > 0f) {
                        snapJob = scope.launch { state.snapBack(config) }
                    }
                }
            ),
        content = {
            items.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (index == state.presentedIndex) {
                            onPresentedCardClick(index)
                        } else {
                            snapJob?.cancel()
                            state.present(index)
                        }
                    }
                ) {
                    cardContent(index, item)
                }
            }
        }
    ) { measurables, constraints ->
        val count = measurables.size
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val viewportHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else 0

        if (count == 0) {
            return@Layout layout(width, viewportHeight) {}
        }

        // Every card is measured at full width and its natural height. The cards share a
        // design so heights match; the tallest drives the peek grid (as presentedHeight
        // does in the View LayoutManager).
        val childConstraints = Constraints(
            minWidth = width,
            maxWidth = width,
            minHeight = 0,
            maxHeight = Constraints.Infinity
        )
        val placeables = measurables.map { it.measure(childConstraints) }
        val cardHeight = placeables.maxOf { it.height }

        // maxScroll — mirrors StackLayoutManager.computeMaxScroll().
        val stackCount = count - 1
        state.maxScrollOffset = if (stackCount <= 0) {
            0
        } else {
            val totalContentHeight = cardHeight + config.stackTopMargin +
                (stackCount - 1) * config.collapsedPeekHeight + cardHeight
            (totalContentHeight - viewportHeight).coerceAtLeast(0)
        }
        // scrollOffset is re-clamped against maxScrollOffset in StackViewState.onDrag, so
        // there's no need to write observable state here (which would force a relayout).
        val clampedScroll = state.scrollOffset.coerceIn(0f, state.maxScrollOffset.toFloat())

        val p0 = state.prevPresentedIndex.coerceIn(0, count - 1)
        val p1 = state.presentedIndex.coerceIn(0, count - 1)
        val t = transition.value
        val scroll = clampedScroll
        val stretch = state.stretch

        // Painter's algorithm: draw back-to-front by z (presented on top), matching the
        // `cards.sortBy { it.zOrder }` loop in doLayout.
        val drawOrder = (0 until count).sortedBy { i -> zRank(i, p1, count) }

        layout(width, viewportHeight) {
            for (i in drawOrder) {
                val from = slotY(i, p0, cardHeight, config)
                val to = slotY(i, p1, cardHeight, config)
                var y = (from + (to - from) * t) - scroll
                if (i != p1) {
                    y += stretch * stackRank(i, p1)
                }
                placeables[i].placeRelative(0, y.roundToInt())
            }
        }
    }
}

/** Number of non-presented cards that sit before [i] in the stack. */
private fun stackRank(i: Int, presented: Int): Int = if (i < presented) i else i - 1

/** Target top of card [i] for a given [presented] card, before scroll/stretch, in px. */
private fun slotY(i: Int, presented: Int, cardHeight: Int, config: StackConfig): Float {
    if (i == presented) return 0f
    val rank = stackRank(i, presented)
    return (cardHeight + config.stackTopMargin + rank * config.collapsedPeekHeight).toFloat()
}

/** Paint order key: presented card on top, stack cards ordered by their rank. */
private fun zRank(i: Int, presented: Int, count: Int): Int =
    if (i == presented) count + 1 else stackRank(i, presented)

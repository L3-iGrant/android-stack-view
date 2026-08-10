package io.igrant.stackview.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
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

    // The post-gesture animation: a fling, or the snap-back from a pull-down stretch. Held so
    // the next drag or tap can cancel it and take over mid-flight.
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val decay = rememberSplineBasedDecay<Float>()

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
        settleJob?.cancel()
        // draggable delta is +down; the View dy convention is +content-up, so invert.
        state.onDrag(-delta, config)
    }

    Layout(
        modifier = modifier
            .clipToBounds()
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    settleJob = scope.launch {
                        if (state.stretch > 0f) {
                            state.snapBack(config)
                        } else {
                            // Velocity is +down like the drag delta, so invert it too.
                            state.fling(-velocity, decay, config)
                        }
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
                            settleJob?.cancel()
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

        // maxScroll — all stacking math lives in StackGeometry (unit-tested). It depends only
        // on the measured sizes, so it belongs here rather than in the placement block below.
        state.maxScrollOffset = StackGeometry.maxScrollOffset(count, cardHeight, viewportHeight, config)

        layout(width, viewportHeight) {
            // Scroll / stretch / animation state is read *inside* the placement block on
            // purpose. Compose scopes invalidation to where a snapshot value is read, so a
            // drag frame only re-runs this lambda — the measure pass above, and with it every
            // card's measure, is skipped entirely. Hoisting any of these reads out of here
            // puts a full re-measure of the whole stack back on every frame of a drag.
            val p0 = state.prevPresentedIndex.coerceIn(0, count - 1)
            val p1 = state.presentedIndex.coerceIn(0, count - 1)
            val t = transition.value
            val scroll = state.scrollOffset.coerceIn(0f, state.maxScrollOffset.toFloat())
            val stretch = state.stretch

            for (i in 0 until count) {
                // Interpolate between the previous and current arrangement for the present
                // animation, then apply scroll and (stack cards only) the pull-down stretch.
                val from = StackGeometry.slotTop(i, p0, cardHeight, config)
                val to = StackGeometry.slotTop(i, p1, cardHeight, config)
                var y = (from + (to - from) * t) - scroll
                if (i != p1) {
                    y += stretch * StackGeometry.stackRank(i, p1)
                }
                val top = y.roundToInt()

                // Cards scrolled clear of the viewport are left unplaced, so they aren't drawn
                // and aren't hit-tested. Per-frame cost then tracks how many cards are visible
                // rather than how many the wallet holds.
                if (viewportHeight > 0 &&
                    (top >= viewportHeight || top + placeables[i].height <= 0)
                ) {
                    continue
                }

                // Painter's algorithm via zIndex (presented on top, stack cards by rank so each
                // covers the peek strip above it). Letting Compose order the draw avoids sorting
                // — and allocating a sorted index list — on every frame of a drag.
                //
                // ...WithLayer gives each card its own render node, so moving it re-runs a layer
                // translation instead of re-recording the card's draw commands.
                placeables[i].placeRelativeWithLayer(
                    x = 0,
                    y = top,
                    zIndex = StackGeometry.zOrder(i, p1, count).toFloat()
                )
            }
        }
    }
}

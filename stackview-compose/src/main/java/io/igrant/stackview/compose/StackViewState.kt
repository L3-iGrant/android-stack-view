package io.igrant.stackview.compose

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import io.igrant.stackview.StackConfig
import kotlin.math.abs

/**
 * Hoisted state for [StackView]. Holds which card is presented plus the transient
 * scroll / stretch offsets that drive the wallet-stack interactions.
 *
 * Create with [rememberStackViewState] so the presented index survives configuration
 * changes and process death.
 */
@Stable
class StackViewState internal constructor(initialPresentedIndex: Int) {

    /** Index of the currently presented (top) card. */
    var presentedIndex: Int by mutableIntStateOf(initialPresentedIndex)
        internal set

    /** The card presented before the current one — the "from" of the reflow animation. */
    internal var prevPresentedIndex: Int by mutableIntStateOf(initialPresentedIndex)

    /** Vertical scroll of the whole stack, in pixels (0..[maxScrollOffset]). */
    internal var scrollOffset: Float by mutableFloatStateOf(0f)

    /** Pull-down fan-out amount, in pixels. Applied per stacked card by its rank. */
    internal var stretch: Float by mutableFloatStateOf(0f)

    /** Recomputed by [StackView] on every layout pass; read by the drag handler. */
    internal var maxScrollOffset: Int = 0

    /**
     * Present the card at [index] with the reflow animation. No-op / callback handling
     * for tapping the already-presented card lives in [StackView].
     */
    fun present(index: Int) {
        if (index == presentedIndex) return
        prevPresentedIndex = presentedIndex
        presentedIndex = index
        scrollOffset = 0f
        stretch = 0f
    }

    /**
     * Reset the stack back to the first card. Call after items are added or removed so
     * the selection doesn't go stale. Mirrors `StackLayoutManager.refresh()`.
     */
    fun refresh() {
        prevPresentedIndex = 0
        presentedIndex = 0
        scrollOffset = 0f
        stretch = 0f
    }

    /**
     * Consume a vertical drag of [dy] pixels (View `scrollVerticallyBy` convention:
     * positive = content scrolls up). Returns the amount actually consumed.
     *
     * A single delta can cross the boundary between the rubber-band stretch and the scroll —
     * that is exactly what happens when the user reverses direction near the top. The
     * remainder is carried across the boundary rather than dropped, so the stack keeps
     * tracking the finger through a direction change instead of stalling for a frame.
     */
    internal fun onDrag(dy: Float, config: StackConfig): Float {
        val resistance = config.stretchResistance
        var remaining = dy

        // Pushing back into an existing stretch unwinds it first; anything left over falls
        // through to the scroll below.
        if (stretch > 0f && remaining > 0f) {
            // Finger distance that would take the current stretch back to zero.
            val toUnwind = if (resistance > 0f) stretch / resistance else remaining
            val used = minOf(remaining, toUnwind)
            stretch = (stretch - used * resistance).coerceAtLeast(0f)
            remaining -= used
            if (remaining <= 0f) return dy
        }

        // Pulling down while already stretched, or with the stack at the very top: rubber-band.
        if (remaining < 0f && (stretch > 0f || scrollOffset == 0f)) {
            stretch = (stretch + abs(remaining) * resistance)
                .coerceAtMost(config.maxStretchDistance.toFloat())
            return dy
        }

        val scrolled = scrollBy(remaining)
        val leftover = remaining - scrolled

        // Scrolled past the top mid-delta: spend the rest on the stretch.
        if (leftover < 0f) {
            stretch = (stretch + abs(leftover) * resistance)
                .coerceAtMost(config.maxStretchDistance.toFloat())
            return dy
        }
        return dy - leftover
    }

    /**
     * Move the scroll by [dy], clamped to the scrollable range. Returns the amount actually
     * applied — less than [dy] means an edge was reached. Never rubber-bands.
     */
    private fun scrollBy(dy: Float): Float {
        val old = scrollOffset
        scrollOffset = (scrollOffset + dy).coerceIn(0f, maxScrollOffset.toFloat().coerceAtLeast(0f))
        return scrollOffset - old
    }

    /**
     * Carry the stack on after the user lifts their finger, decaying from [initialVelocity]
     * (px/s, positive = content scrolls up) with the platform's standard scroll physics.
     *
     * Without this the stack stops dead the instant the finger leaves the glass, which reads
     * as the list dragging against you — the momentum every other Android list has is missing,
     * not the frame rate. Cancel the job to hand control back to a new gesture.
     */
    internal suspend fun fling(
        initialVelocity: Float,
        decay: DecayAnimationSpec<Float>,
        config: StackConfig,
    ) {
        if (abs(initialVelocity) < MinFlingVelocity) return
        if (maxScrollOffset <= 0) return

        var lastValue = 0f
        AnimationState(initialValue = 0f, initialVelocity = initialVelocity)
            .animateDecay(decay) {
                val delta = value - lastValue
                lastValue = value
                val consumed = scrollBy(delta)
                // Ran into the top or bottom: stop rather than let the decay keep spending
                // velocity against a clamped offset. The fling does not rubber-band — the
                // stretch is a deliberate pull-down gesture, not something a flick triggers.
                if (abs(consumed) + FlingEdgeEpsilon < abs(delta)) cancelAnimation()
            }
        // A fling can only ever land somewhere scrollable, so there is nothing to snap back.
    }

    /** Animate the stretch back to zero when the user releases. */
    internal suspend fun snapBack(config: StackConfig) {
        val start = stretch
        if (start <= 0f) return
        animate(
            initialValue = start,
            targetValue = 0f,
            animationSpec = tween(config.snapBackDuration.toInt())
        ) { value, _ ->
            stretch = value
        }
    }

    companion object {
        /** Below this (px/s) a lift is a slow drag ending, not a flick worth carrying on. */
        private const val MinFlingVelocity = 50f

        /** Rounding slack when deciding whether a fling step hit an edge. */
        private const val FlingEdgeEpsilon = 0.5f

        /** Persists only the presented index; transient offsets reset on restore. */
        val Saver: Saver<StackViewState, Int> = Saver(
            save = { it.presentedIndex },
            restore = { StackViewState(it) }
        )
    }
}

/**
 * Remember a [StackViewState]. The presented index is retained across recomposition,
 * configuration changes and process death.
 */
@Composable
fun rememberStackViewState(initialPresentedIndex: Int = 0): StackViewState =
    rememberSaveable(saver = StackViewState.Saver) {
        StackViewState(initialPresentedIndex)
    }

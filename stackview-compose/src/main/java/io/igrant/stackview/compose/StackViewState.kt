package io.igrant.stackview.compose

import androidx.compose.animation.core.animate
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
     */
    internal fun onDrag(dy: Float, config: StackConfig): Float {
        // Already stretched: pulling further grows it, pushing back shrinks it.
        if (stretch > 0f) {
            stretch = if (dy < 0) {
                (stretch + abs(dy) * config.stretchResistance)
                    .coerceAtMost(config.maxStretchDistance.toFloat())
            } else {
                (stretch - dy * config.stretchResistance).coerceAtLeast(0f)
            }
            return dy
        }

        // At the very top and pulling down: start stretching (rubber-band).
        if (dy < 0 && scrollOffset == 0f) {
            stretch += abs(dy) * config.stretchResistance
            return dy
        }

        val old = scrollOffset
        scrollOffset = (scrollOffset + dy).coerceIn(0f, maxScrollOffset.toFloat().coerceAtLeast(0f))
        return scrollOffset - old
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

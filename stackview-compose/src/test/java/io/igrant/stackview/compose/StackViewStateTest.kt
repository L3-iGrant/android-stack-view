package io.igrant.stackview.compose

import io.igrant.stackview.StackConfig
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [StackViewState] — the drag/present/refresh state transitions that back
 * [StackView]. These exercise the non-Compose logic directly (no snapshot animation involved).
 *
 * Drag convention matches the View `scrollVerticallyBy`: positive `dy` scrolls content up,
 * negative `dy` pulls down.
 */
class StackViewStateTest {

    // stretchResistance 0.5 → half the pull distance becomes stretch.
    private val config = StackConfig(stretchResistance = 0.5f, maxStretchDistance = 800)

    private fun state(initial: Int = 0) = StackViewState(initialPresentedIndex = initial)

    // --- onDrag: stretch ---

    @Test
    fun onDrag_pullDownAtTop_startsStretchingWithResistance() {
        val s = state()
        s.onDrag(dy = -100f, config = config) // pull down 100 → 50 stretch
        assertEquals(50f, s.stretch, 0.001f)
        assertEquals(0f, s.scrollOffset, 0.001f)
    }

    @Test
    fun onDrag_continuedPull_growsStretch() {
        val s = state()
        s.onDrag(-100f, config) // 50
        s.onDrag(-100f, config) // 50 + 50 = 100
        assertEquals(100f, s.stretch, 0.001f)
    }

    @Test
    fun onDrag_stretchIsClampedToMax() {
        val s = state()
        val clamped = StackConfig(stretchResistance = 0.5f, maxStretchDistance = 120)
        s.onDrag(-100f, clamped)   // 50
        s.onDrag(-100000f, clamped) // would be huge → clamped to 120
        assertEquals(120f, s.stretch, 0.001f)
    }

    @Test
    fun onDrag_pushingBack_shrinksStretchAndStopsAtZero() {
        val s = state()
        s.onDrag(-200f, config) // stretch 100
        s.onDrag(100f, config)  // 100 - 50 = 50
        assertEquals(50f, s.stretch, 0.001f)
        s.onDrag(1000f, config) // 50 - 500 → clamped to 0
        assertEquals(0f, s.stretch, 0.001f)
    }

    // --- onDrag: scroll ---

    @Test
    fun onDrag_whenScrollable_scrollsAndClampsToMax() {
        val s = state()
        s.maxScrollOffset = 500
        assertEquals(100f, s.onDrag(100f, config), 0.001f) // consumes 100
        assertEquals(100f, s.scrollOffset, 0.001f)

        val consumed = s.onDrag(1000f, config) // only 400 left to max
        assertEquals(400f, consumed, 0.001f)
        assertEquals(500f, s.scrollOffset, 0.001f)
    }

    @Test
    fun onDrag_scrollingBackUp_beforeTop_doesNotStretch() {
        val s = state()
        s.maxScrollOffset = 500
        s.onDrag(300f, config) // scrollOffset 300
        s.onDrag(-100f, config) // still scrolled (200), not stretching
        assertEquals(200f, s.scrollOffset, 0.001f)
        assertEquals(0f, s.stretch, 0.001f)
    }

    @Test
    fun onDrag_pullingDownOnlyStretchesOnceScrollReachesTop() {
        val s = state()
        s.maxScrollOffset = 500
        s.onDrag(100f, config)   // scroll to 100
        s.onDrag(-100f, config)  // back to top (0), no stretch yet
        assertEquals(0f, s.scrollOffset, 0.001f)
        assertEquals(0f, s.stretch, 0.001f)
        s.onDrag(-100f, config)  // now at top → stretch 50
        assertEquals(50f, s.stretch, 0.001f)
    }

    // --- present / refresh ---

    @Test
    fun present_updatesIndicesAndResetsOffsets() {
        val s = state(initial = 1)
        s.maxScrollOffset = 500
        s.onDrag(200f, config)  // some scroll
        s.onDrag(-1000f, config) // and some stretch

        s.present(4)
        assertEquals(4, s.presentedIndex)
        assertEquals(1, s.prevPresentedIndex) // remembers where we came from (for the animation)
        assertEquals(0f, s.scrollOffset, 0.001f)
        assertEquals(0f, s.stretch, 0.001f)
    }

    @Test
    fun present_sameIndex_isNoOp() {
        val s = state(initial = 2)
        s.maxScrollOffset = 500
        s.onDrag(150f, config) // scrollOffset 150
        s.present(2)           // no change
        assertEquals(2, s.presentedIndex)
        assertEquals(150f, s.scrollOffset, 0.001f)
    }

    @Test
    fun refresh_returnsToFirstCardAndClearsOffsets() {
        val s = state(initial = 0)
        s.present(5)
        s.maxScrollOffset = 500
        s.onDrag(200f, config)
        s.onDrag(-500f, config)

        s.refresh()
        assertEquals(0, s.presentedIndex)
        assertEquals(0, s.prevPresentedIndex)
        assertEquals(0f, s.scrollOffset, 0.001f)
        assertEquals(0f, s.stretch, 0.001f)
    }
}

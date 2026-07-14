package io.igrant.stackview.compose

import io.igrant.stackview.StackConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [StackGeometry] — the pure stacking math shared by [StackView]'s layout.
 * Uses round numbers (card 200px, peek 50px, top-margin 10px) so expected offsets are obvious.
 */
class StackGeometryTest {

    private val config = StackConfig(collapsedPeekHeight = 50, stackTopMargin = 10)
    private val cardHeight = 200

    // --- stackRank ---

    @Test
    fun stackRank_cardsBeforePresented_keepTheirIndex() {
        // presented = 3 → cards 0,1,2 rank as 0,1,2
        assertEquals(0, StackGeometry.stackRank(index = 0, presentedIndex = 3))
        assertEquals(1, StackGeometry.stackRank(index = 1, presentedIndex = 3))
        assertEquals(2, StackGeometry.stackRank(index = 2, presentedIndex = 3))
    }

    @Test
    fun stackRank_cardsAfterPresented_shiftUpByOne() {
        // presented = 0 → cards 1,2,3 rank as 0,1,2 (the presented card is pulled out)
        assertEquals(0, StackGeometry.stackRank(index = 1, presentedIndex = 0))
        assertEquals(1, StackGeometry.stackRank(index = 2, presentedIndex = 0))
        assertEquals(2, StackGeometry.stackRank(index = 3, presentedIndex = 0))
    }

    @Test
    fun stackRank_spanningThePresentedCard_isContiguous() {
        // presented = 2 → ranks: 0→0, 1→1, 3→2, 4→3 (no gap where the presented card was)
        assertEquals(0, StackGeometry.stackRank(index = 0, presentedIndex = 2))
        assertEquals(1, StackGeometry.stackRank(index = 1, presentedIndex = 2))
        assertEquals(2, StackGeometry.stackRank(index = 3, presentedIndex = 2))
        assertEquals(3, StackGeometry.stackRank(index = 4, presentedIndex = 2))
    }

    // --- slotTop ---

    @Test
    fun slotTop_presentedCard_isAtTop() {
        assertEquals(0, StackGeometry.slotTop(index = 2, presentedIndex = 2, cardHeight, config))
    }

    @Test
    fun slotTop_firstStackCard_sitsBelowPresentedByCardHeightPlusMargin() {
        // rank 0 → cardHeight(200) + margin(10) + 0*peek = 210
        assertEquals(210, StackGeometry.slotTop(index = 1, presentedIndex = 0, cardHeight, config))
    }

    @Test
    fun slotTop_laterStackCards_addOnePeekPerRank() {
        // rank 1 → 210 + 50 = 260 ; rank 2 → 310
        assertEquals(260, StackGeometry.slotTop(index = 2, presentedIndex = 0, cardHeight, config))
        assertEquals(310, StackGeometry.slotTop(index = 3, presentedIndex = 0, cardHeight, config))
    }

    @Test
    fun slotTop_isIndependentOfWhichCardIsPresented_forEqualRanks() {
        // The rank-1 stack card is at the same offset regardless of which card is presented.
        val whenPresented0 = StackGeometry.slotTop(index = 2, presentedIndex = 0, cardHeight, config)
        val whenPresented4 = StackGeometry.slotTop(index = 1, presentedIndex = 4, cardHeight, config)
        assertEquals(whenPresented0, whenPresented4)
    }

    // --- zOrder ---

    @Test
    fun zOrder_presentedCard_isDrawnOnTop() {
        val count = 4
        val presented = 1
        val presentedZ = StackGeometry.zOrder(presented, presented, count)
        for (i in 0 until count) {
            if (i == presented) continue
            assertTrue(
                "presented card must outrank every stack card",
                presentedZ > StackGeometry.zOrder(i, presented, count)
            )
        }
    }

    @Test
    fun zOrder_stackCards_increaseWithRank_soLowerCardsCoverHigherOnes() {
        // presented = 0, count = 4 → stack cards 1,2,3 get z 0,1,2
        assertEquals(0, StackGeometry.zOrder(1, presentedIndex = 0, count = 4))
        assertEquals(1, StackGeometry.zOrder(2, presentedIndex = 0, count = 4))
        assertEquals(2, StackGeometry.zOrder(3, presentedIndex = 0, count = 4))
    }

    // --- maxScrollOffset ---

    @Test
    fun maxScrollOffset_zeroOrOneCard_isZero() {
        assertEquals(0, StackGeometry.maxScrollOffset(count = 0, cardHeight, viewportHeight = 800, config))
        assertEquals(0, StackGeometry.maxScrollOffset(count = 1, cardHeight, viewportHeight = 800, config))
    }

    @Test
    fun maxScrollOffset_contentShorterThanViewport_isZero() {
        // count=5 → total = 200 + 10 + 3*50 + 200 = 560; viewport 600 → no scroll
        assertEquals(0, StackGeometry.maxScrollOffset(count = 5, cardHeight, viewportHeight = 600, config))
    }

    @Test
    fun maxScrollOffset_contentTallerThanViewport_isTheOverflow() {
        // count=5 → total 560; viewport 400 → overflow 160
        assertEquals(160, StackGeometry.maxScrollOffset(count = 5, cardHeight, viewportHeight = 400, config))
    }
}

package io.igrant.stackview.compose

import io.igrant.stackview.StackConfig

/**
 * Pure layout math for the Compose [StackView] — no Compose or Android types, so it can be
 * unit-tested in isolation. All positions are in **pixels**.
 *
 * The model matches the View `StackLayoutManager`:
 * - the **presented** card sits at the top, at offset `0`;
 * - every other card is a **stack card**, placed [StackConfig.stackTopMargin] below the
 *   presented card's bottom and then [StackConfig.collapsedPeekHeight] below the previous
 *   stack card, so only a peek strip of each shows;
 * - cards are drawn back-to-front (see [zOrder]) so the peek strips stay visible and the
 *   presented card covers everything above the stack.
 *
 * "Rank" is a stack card's 0-based position among the non-presented cards, in item order.
 */
internal object StackGeometry {

    /**
     * The stack rank of card [index] given the [presentedIndex] — i.e. how many non-presented
     * cards precede it. Not meaningful for the presented card itself (callers guard that case).
     *
     * Because the presented card is pulled out of the sequence, every card after it shifts up
     * by one rank: cards before the presented one keep their index as rank, cards after it use
     * `index - 1`.
     */
    fun stackRank(index: Int, presentedIndex: Int): Int =
        if (index < presentedIndex) index else index - 1

    /**
     * Top offset (px) of card [index] for the given [presentedIndex], before scroll/stretch
     * are applied. The presented card is at `0`; each stack card is [cardHeight] +
     * [StackConfig.stackTopMargin] below the top, plus its rank × [StackConfig.collapsedPeekHeight].
     *
     * @param cardHeight measured height of a card (all cards share the peek grid height).
     */
    fun slotTop(index: Int, presentedIndex: Int, cardHeight: Int, config: StackConfig): Int {
        if (index == presentedIndex) return 0
        val rank = stackRank(index, presentedIndex)
        return cardHeight + config.stackTopMargin + rank * config.collapsedPeekHeight
    }

    /**
     * Paint-order key (higher = drawn later = on top). The presented card gets the highest
     * value so it sits above the stack; stack cards are ordered by rank so each covers the
     * peek strip of the one above it. Sort ascending and place in that order.
     */
    fun zOrder(index: Int, presentedIndex: Int, count: Int): Int =
        if (index == presentedIndex) count + 1 else stackRank(index, presentedIndex)

    /**
     * Maximum vertical scroll (px) so the last card can reach the bottom of the viewport.
     * Mirrors `StackLayoutManager.computeMaxScroll()`. Returns `0` when the stack fits (0 or 1
     * item, or content shorter than the viewport).
     *
     * @param count total number of cards.
     * @param cardHeight measured card height.
     * @param viewportHeight height of the visible area (px).
     */
    fun maxScrollOffset(count: Int, cardHeight: Int, viewportHeight: Int, config: StackConfig): Int {
        val stackCount = count - 1
        if (stackCount <= 0) return 0
        val totalContentHeight = cardHeight + config.stackTopMargin +
            (stackCount - 1) * config.collapsedPeekHeight + cardHeight
        return (totalContentHeight - viewportHeight).coerceAtLeast(0)
    }
}

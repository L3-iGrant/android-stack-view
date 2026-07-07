package io.igrant.stackview

/**
 * Configuration for the StackView SDK.
 *
 * Shared by both implementations — the View [StackLayoutManager] and the Compose
 * `StackView` composable — so behavior stays identical across the two. All distances
 * are expressed in **pixels**.
 *
 * @param collapsedPeekHeight The visible strip height (in pixels) for each collapsed card.
 *                            Maps to iOS `minimalDistanceBetweenCollapsedCardViews`.
 * @param stackTopMargin Space (in pixels) between the presented card's bottom and the stack.
 * @param animationDuration Duration in ms for present/dismiss animation (iOS: 350ms).
 * @param stretchResistance How much of the pull distance translates to stretch (0.0–1.0).
 *                          Lower = more resistance. Default 0.5 for a rubber-band feel.
 * @param maxStretchDistance Maximum stretch distance in pixels. Caps the fan-out.
 * @param snapBackDuration Duration in ms for the snap-back animation when the user releases.
 */
data class StackConfig(
    val collapsedPeekHeight: Int = 120,
    val stackTopMargin: Int = 0,
    val animationDuration: Long = 350L,
    val stretchResistance: Float = 0.5f,
    val maxStretchDistance: Int = 800,
    val snapBackDuration: Long = 600L
)

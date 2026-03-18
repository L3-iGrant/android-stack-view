package io.igrant.stackview

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class StackLayoutManager(
    private val config: StackConfig = StackConfig()
) : RecyclerView.LayoutManager() {

    /** Callback when the already-presented card is tapped again. */
    var onPresentedCardClicked: ((position: Int) -> Unit)? = null

    var presentedPosition: Int = 0
        private set

    private var animator: ValueAnimator? = null
    private var snapBackAnimator: ValueAnimator? = null

    private var scrollOffset = 0
    private var maxScrollOffset = 0

    private var stretchDistance: Float = 0f
    private var isUserTouching = false
    private var touchListenerAttached = false

    private var presentedHeight = 0

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun canScrollVertically(): Boolean = true

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        attachTouchListener(view)
    }

    private fun attachTouchListener(recyclerView: RecyclerView) {
        if (touchListenerAttached) return
        touchListenerAttached = true

        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isUserTouching = true
                        // Don't cancel snap-back — let it finish
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isUserTouching = false
                        if (stretchDistance > 0f) {
                            animateSnapBack(rv)
                        }
                    }
                }
                return false
            }
        })
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (itemCount == 0) return 0
        if (animator?.isRunning == true) return 0

        // Block all scroll input while snap-back animation is running
        if (snapBackAnimator?.isRunning == true) return 0

        if (stretchDistance > 0f) {
            if (dy < 0) {
                stretchDistance += abs(dy.toFloat()) * config.stretchResistance
                stretchDistance = min(stretchDistance, config.maxStretchDistance.toFloat())
                doLayout(recycler)
                return dy
            } else {
                stretchDistance -= dy.toFloat() * config.stretchResistance
                if (stretchDistance <= 0f) stretchDistance = 0f
                doLayout(recycler)
                return dy
            }
        }

        if (dy < 0 && scrollOffset == 0) {
            stretchDistance += abs(dy.toFloat()) * config.stretchResistance
            doLayout(recycler)
            return dy
        }

        val oldOffset = scrollOffset
        scrollOffset = (scrollOffset + dy).coerceIn(0, max(0, maxScrollOffset))
        val consumed = scrollOffset - oldOffset

        if (consumed != 0) {
            doLayout(recycler)
        }

        return consumed
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            return
        }

        presentedPosition = presentedPosition.coerceIn(0, itemCount - 1)
        if (animator?.isRunning == true) return

        presentedHeight = measureChildHeight(presentedPosition, recycler)
        computeMaxScroll()
        scrollOffset = scrollOffset.coerceIn(0, max(0, maxScrollOffset))
        doLayout(recycler)
    }

    private fun computeMaxScroll() {
        val stackCount = itemCount - 1
        if (stackCount <= 0) {
            maxScrollOffset = 0
            return
        }
        // Total content: presented card + margin + (n-1) peeks + last card full height
        // Last card should be fully visible when scrolled to the bottom
        val lastCardHeight = presentedHeight // approximate with same height
        val totalContentHeight = presentedHeight + config.stackTopMargin +
                (stackCount - 1) * config.collapsedPeekHeight + lastCardHeight
        maxScrollOffset = max(0, totalContentHeight - height)
    }

    /**
     * Core layout. Everything scrolls together by scrollOffset.
     * Presented card at top, stack cards below with peekHeight spacing.
     * Stretch fans out stack cards when pulling down at top.
     */
    private fun doLayout(recycler: RecyclerView.Recycler) {
        detachAndScrapAttachedViews(recycler)

        // Presented card scrolls with content
        val presentedTop = -scrollOffset
        val stackTop = presentedTop + presentedHeight + config.stackTopMargin

        data class CardLayout(val adapterPos: Int, val top: Int, val zOrder: Float)

        val cards = mutableListOf<CardLayout>()

        // Presented card
        cards.add(CardLayout(presentedPosition, presentedTop, (itemCount + 1).toFloat()))

        // Stack cards
        var stackIdx = 0
        for (i in 0 until itemCount) {
            if (i == presentedPosition) continue
            val baseY = stackTop + stackIdx * config.collapsedPeekHeight
            val stretchOffset = (stretchDistance * stackIdx).toInt()
            cards.add(CardLayout(i, baseY + stretchOffset, stackIdx.toFloat()))
            stackIdx++
        }

        // Sort by z so lower cards are added first
        cards.sortBy { it.zOrder }

        for (card in cards) {
            // Skip if fully below screen
            if (card.top > height) continue
            // Skip if fully above screen (use presentedHeight as estimate)
            if (card.top + presentedHeight < 0) continue

            val view = recycler.getViewForPosition(card.adapterPos)
            addView(view)
            measureChildWithMargins(view, 0, 0)

            val mh = getDecoratedMeasuredHeight(view)
            val mw = getDecoratedMeasuredWidth(view)
            val left = paddingLeft

            layoutDecoratedWithMargins(view, left, card.top, left + mw, card.top + mh)
            view.translationZ = card.zOrder
        }
    }

    private fun animateSnapBack(recyclerView: RecyclerView) {
        val recycler = recyclerView.recycler
        val startStretch = stretchDistance

        snapBackAnimator?.cancel()
        snapBackAnimator = ValueAnimator.ofFloat(startStretch, 0f).apply {
            duration = config.snapBackDuration
            addUpdateListener { anim ->
                stretchDistance = anim.animatedValue as Float
                doLayout(recycler)
            }
            start()
        }
    }

    private fun measureChildHeight(position: Int, recycler: RecyclerView.Recycler): Int {
        val view = recycler.getViewForPosition(position)
        addView(view)
        measureChildWithMargins(view, 0, 0)
        val h = getDecoratedMeasuredHeight(view)
        detachView(view)
        recycler.recycleView(view)
        return h
    }

    fun presentCard(position: Int, recyclerView: RecyclerView) {
        if (position < 0 || position >= itemCount) return
        if (position == presentedPosition) {
            onPresentedCardClicked?.invoke(position)
            return
        }

        stretchDistance = 0f
        scrollOffset = 0
        snapBackAnimator?.cancel()

        val recycler = recyclerView.recycler

        // Capture old positions
        val oldStackTop = presentedHeight + config.stackTopMargin
        val oldCards = IntArray(itemCount)
        oldCards[presentedPosition] = 0
        var idx = 0
        for (i in 0 until itemCount) {
            if (i == presentedPosition) continue
            oldCards[i] = oldStackTop + idx * config.collapsedPeekHeight
            idx++
        }

        // Update
        presentedPosition = position
        presentedHeight = measureChildHeight(presentedPosition, recycler)
        computeMaxScroll()

        // Capture new positions
        val newStackTop = presentedHeight + config.stackTopMargin
        val newCards = IntArray(itemCount)
        newCards[presentedPosition] = 0
        idx = 0
        for (i in 0 until itemCount) {
            if (i == presentedPosition) continue
            newCards[i] = newStackTop + idx * config.collapsedPeekHeight
            idx++
        }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = config.animationDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val progress = anim.animatedValue as Float
                detachAndScrapAttachedViews(recycler)

                data class AnimCard(val adapterPos: Int, val top: Int, val zOrder: Float)
                val cards = mutableListOf<AnimCard>()

                var sIdx = 0
                for (i in 0 until itemCount) {
                    val fromY = oldCards[i]
                    val toY = newCards[i]
                    val y = (fromY + (toY - fromY) * progress).toInt()
                    val z = if (i == presentedPosition) {
                        (itemCount + 1).toFloat()
                    } else {
                        sIdx.toFloat().also { sIdx++ }
                    }
                    cards.add(AnimCard(i, y, z))
                }

                cards.sortBy { it.zOrder }

                for (card in cards) {
                    if (card.top > height) continue

                    val view = recycler.getViewForPosition(card.adapterPos)
                    addView(view)
                    measureChildWithMargins(view, 0, 0)
                    val mh = getDecoratedMeasuredHeight(view)
                    val mw = getDecoratedMeasuredWidth(view)
                    layoutDecoratedWithMargins(view, paddingLeft, card.top, paddingLeft + mw, card.top + mh)
                    view.translationZ = card.zOrder
                }
            }
            start()
        }
    }

    /**
     * Resets the stack to show the 0th item as presented.
     * Call this after adding/removing items to avoid stale selection.
     */
    fun refresh(recyclerView: RecyclerView) {
        animator?.cancel()
        snapBackAnimator?.cancel()
        stretchDistance = 0f
        scrollOffset = 0
        presentedPosition = 0
        requestLayout()
    }

    private val RecyclerView.recycler: RecyclerView.Recycler
        get() {
            val field = RecyclerView::class.java.getDeclaredField("mRecycler")
            field.isAccessible = true
            return field.get(this) as RecyclerView.Recycler
        }
}

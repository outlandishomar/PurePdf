package com.example.purepdf

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A custom fast-scroller overlay for [RecyclerView].
 *
 * Features:
 *  - Thick, rounded red thumb that is easy to grab on touch screens.
 *  - Touch-drag scrolling: dragging the thumb scrolls the RecyclerView proportionally.
 *  - Auto-hide: the thumb fades out after [HIDE_DELAY_MS] of inactivity and fades back in
 *    when the user scrolls or touches.
 */
class FastScrollerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        /** Width of the thumb in dp. */
        private const val THUMB_WIDTH_DP = 8f

        /** Minimum height of the thumb in dp. */
        private const val THUMB_MIN_HEIGHT_DP = 48f

        /** Corner radius of the thumb in dp. */
        private const val THUMB_CORNER_DP = 4f

        /** Right margin of the thumb in dp. */
        private const val THUMB_MARGIN_END_DP = 4f

        /** Milliseconds before the thumb auto-hides. */
        private const val HIDE_DELAY_MS = 1500L

        /** Duration of the fade animation. */
        private const val FADE_DURATION_MS = 300L
    }

    // ── Dimensions (px) ─────────────────────────────────────
    private val thumbWidthPx   = dpToPx(THUMB_WIDTH_DP)
    private val thumbMinHeight = dpToPx(THUMB_MIN_HEIGHT_DP)
    private val thumbCorner    = dpToPx(THUMB_CORNER_DP)
    private val thumbMarginEnd = dpToPx(THUMB_MARGIN_END_DP)

    // ── Drawing ─────────────────────────────────────────────
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.brand_red)
        style = Paint.Style.FILL
    }
    private val thumbRect = RectF()

    // ── Scroll state ────────────────────────────────────────
    private var recyclerView: RecyclerView? = null
    private var thumbOffsetY = 0f    // top of the thumb (px)
    private var thumbHeight  = 0f    // current thumb height (px)
    private var isDragging   = false

    // ── Auto-hide ───────────────────────────────────────────
    private var fadeAnimator: ValueAnimator? = null
    private val hideRunnable = Runnable { animateAlpha(0f) }

    // ── RecyclerView scroll listener ────────────────────────
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            if (!isDragging) updateThumbPosition()
            showThenAutoHide()
        }
    }

    // ═════════════════════════════════════════════════════════
    //  Public API
    // ═════════════════════════════════════════════════════════

    /** Attach to a [RecyclerView]. Call this once the RV has an adapter. */
    fun attachTo(rv: RecyclerView) {
        recyclerView?.removeOnScrollListener(scrollListener)
        recyclerView = rv
        rv.addOnScrollListener(scrollListener)
        alpha = 0f // start hidden
        post { updateThumbPosition(); showThenAutoHide() }
    }

    // ═════════════════════════════════════════════════════════
    //  Drawing
    // ═════════════════════════════════════════════════════════

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (thumbHeight <= 0f) return
        val left = width - thumbWidthPx - thumbMarginEnd
        thumbRect.set(left, thumbOffsetY, left + thumbWidthPx, thumbOffsetY + thumbHeight)
        canvas.drawRoundRect(thumbRect, thumbCorner, thumbCorner, thumbPaint)
    }

    // ═════════════════════════════════════════════════════════
    //  Touch handling
    // ═════════════════════════════════════════════════════════

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rv = recyclerView ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Only start drag if touch is near the thumb region (right 48dp of screen).
                if (event.x < width - dpToPx(48f)) return false
                isDragging = true
                showThenAutoHide()
                
                // Immediately snap thumb to touch position
                updateThumbFromTouch(event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    showThenAutoHide()
                    // Update thumb visually only – DO NOT scroll RecyclerView (deferred scroll)
                    updateThumbFromTouch(event.y)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    showThenAutoHide()
                    
                    // On release, calculate the fraction based on where the thumb landed
                    // tracking space = height - thumbHeight
                    // offset fraction = thumbOffsetY / tracking space
                    val trackSpace = (height - thumbHeight).coerceAtLeast(1f)
                    val fraction = (thumbOffsetY / trackSpace).coerceIn(0f, 1f)
                    
                    scrollToFraction(fraction, rv)
                    return true
                }
            }
        }
        return false
    }

    // ═════════════════════════════════════════════════════════
    //  Internal helpers
    // ═════════════════════════════════════════════════════════

    /** Visually moves the thumb to track the user's touch Y, without scrolling the RV. */
    private fun updateThumbFromTouch(touchY: Float) {
        // Center the thumb vertically on the drag point
        val top = touchY - (thumbHeight / 2f)
        
        // Clamp it within the track bounds
        val maxOffset = height - thumbHeight
        thumbOffsetY = top.coerceIn(0f, maxOffset)
        
        invalidate()
    }

    /** Scroll the RecyclerView so that [fraction] (0‥1) of the content is above the viewport. */
    private fun scrollToFraction(fraction: Float, rv: RecyclerView) {
        val lm = rv.layoutManager as? LinearLayoutManager ?: return
        val itemCount = rv.adapter?.itemCount ?: return
        if (itemCount == 0) return

        val clamped = fraction.coerceIn(0f, 1f)
        val targetPos = (clamped * (itemCount - 1)).toInt()
        lm.scrollToPositionWithOffset(targetPos, 0)
        updateThumbPosition()
    }

    /** Read the RecyclerView's scroll state and reposition the thumb. */
    private fun updateThumbPosition() {
        val rv = recyclerView ?: return
        val range  = rv.computeVerticalScrollRange()
        val extent = rv.computeVerticalScrollExtent()
        val offset = rv.computeVerticalScrollOffset()

        if (range <= extent) {
            thumbHeight = 0f           // content fits — no scrollbar needed
            visibility = GONE
            invalidate()
            return
        }
        visibility = VISIBLE

        val trackHeight = height.toFloat()
        thumbHeight = (extent.toFloat() / range * trackHeight).coerceAtLeast(thumbMinHeight)
        val scrollableTrack = trackHeight - thumbHeight
        val scrollFraction  = offset.toFloat() / (range - extent)
        thumbOffsetY = scrollFraction * scrollableTrack

        invalidate()
    }

    // ── Auto-hide animation ─────────────────────────────────

    private fun showThenAutoHide() {
        animateAlpha(1f)
        handler?.removeCallbacks(hideRunnable)
        if (!isDragging) handler?.postDelayed(hideRunnable, HIDE_DELAY_MS)
    }

    private fun animateAlpha(target: Float) {
        if (alpha == target) return
        fadeAnimator?.cancel()
        fadeAnimator = ValueAnimator.ofFloat(alpha, target).apply {
            duration = FADE_DURATION_MS
            addUpdateListener { alpha = it.animatedValue as Float }
            start()
        }
    }

    // ── Utility ─────────────────────────────────────────────

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}

package com.shrey.reelize.overlay

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.shrey.reelize.Prefs
import kotlin.math.abs

/**
 * The floating counter shown over Instagram and YouTube while their short-video feed is open.
 *
 * Drawn as an accessibility overlay, which the service may add without the separate
 * "display over other apps" permission. It can be dragged and snaps to the nearest side.
 * Tapping it shows today's time in the feed for a few seconds. Its look - colour, opacity,
 * picture and starting position - comes from the Bubble settings, read each time it appears.
 */
class CounterBubble(private val service: AccessibilityService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private val prefs = Prefs(service)
    private val density = service.resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop

    private var count = 0
    private var millis = 0L
    private var expanded = false
    private var attached = false

    private val pill = GradientDrawable().apply { cornerRadius = 100 * density }
    private var photo: Drawable? = null

    private val view = TextView(service).apply {
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.BOLD)
        textSize = 16f
        minWidth = dp(44)
        minHeight = dp(44)
        setPadding(dp(12), dp(6), dp(12), dp(6))
        elevation = 6 * density
    }

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    private val collapse = Runnable { expanded = false; render() }

    init {
        attachDrag()
    }

    fun show() {
        if (attached || !prefs.bubbleEnabled) return
        photo = if (prefs.bubbleImage == BubbleImage.PHOTO) {
            BubblePhoto.load(service)?.let { bmp ->
                RoundedBitmapDrawableFactory.create(service.resources, bmp).apply { isCircular = true }
            }
        } else null
        render()
        val screen = wm.currentWindowMetrics.bounds
        val savedX = prefs.bubbleX
        val savedY = prefs.bubbleY
        if (prefs.rememberDrag && savedX >= 0 && savedY >= 0) {
            params.x = savedX
            params.y = savedY
        } else {
            val pos = prefs.bubblePosition
            params.x = if (pos.xFraction < 0.5f) dp(4) else screen.width() - dp(BUBBLE_DP) - dp(4)
            params.y = (screen.height() * pos.yFraction).toInt()
        }
        runCatching { wm.addView(view, params) }.onSuccess { attached = true }
    }

    fun hide() {
        if (!attached) return
        view.removeCallbacks(collapse)
        expanded = false
        runCatching { wm.removeView(view) }
        attached = false
    }

    fun update(todayCount: Int, todayMillis: Long) {
        val grew = todayCount > count
        count = todayCount
        millis = todayMillis
        render()
        if (grew && attached) {
            view.animate().scaleX(1.18f).scaleY(1.18f).setDuration(110).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }.start()
        }
    }

    private fun render() {
        val (bg, fg) = bubbleColors(count, prefs.colorByCount, prefs.bubbleColor)
        val image = prefs.bubbleImage
        val photo = photo
        view.alpha = prefs.bubbleOpacity
        if (photo != null && !expanded) {
            // The photo fills a circle; the count sits on it with a shadow so it stays legible.
            view.background = photo
            view.setTextColor(Color.WHITE)
            view.setShadowLayer(6f, 0f, 1f, Color.BLACK)
            view.width = dp(BUBBLE_DP)
            view.height = dp(BUBBLE_DP)
            view.text = count.toString()
        } else {
            pill.setColor(bg)
            view.background = pill
            view.setTextColor(fg)
            view.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            // Undo the fixed size the photo circle sets.
            view.minWidth = dp(44)
            view.minHeight = dp(44)
            view.maxWidth = Int.MAX_VALUE
            view.maxHeight = Int.MAX_VALUE
            val prefix = image.emoji?.let { "$it " }.orEmpty()
            view.text = if (expanded) "$prefix$count reels · ${minutes(millis)} today" else "$prefix$count"
        }
        if (attached) runCatching { wm.updateViewLayout(view, params) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDrag() {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        view.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = params.x; startY = params.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) dragging = true
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        wm.updateViewLayout(view, params)
                    }
                }
                MotionEvent.ACTION_UP -> if (dragging) snapToEdge() else toggle()
            }
            true
        }
    }

    private fun toggle() {
        expanded = !expanded
        render()
        view.removeCallbacks(collapse)
        if (expanded) view.postDelayed(collapse, 3_000)
    }

    private fun snapToEdge() {
        val screenW = wm.currentWindowMetrics.bounds.width()
        val center = params.x + view.width / 2
        params.x = if (center < screenW / 2) dp(4) else screenW - view.width - dp(4)
        wm.updateViewLayout(view, params)
        if (prefs.rememberDrag) {
            prefs.bubbleX = params.x
            prefs.bubbleY = params.y
        }
    }

    private fun dp(v: Int) = (v * density).toInt()

    private fun minutes(ms: Long): String {
        val m = ms / 60_000
        return if (m < 60) "${m}m" else "${m / 60}h ${m % 60}m"
    }

    private companion object {
        const val BUBBLE_DP = 56
    }
}

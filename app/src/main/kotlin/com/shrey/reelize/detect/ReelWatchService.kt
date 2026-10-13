package com.shrey.reelize.detect

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.shrey.reelize.data.Stats
import com.shrey.reelize.overlay.CounterBubble
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

/**
 * Watches Instagram and YouTube through the accessibility API, works out when their
 * short-video feed is on screen, and counts reels with [ReelCounter].
 *
 * Only the two apps' view structure is inspected - view IDs and the pager's scroll events.
 * No text, captions or content is read, and nothing leaves the device.
 */
class ReelWatchService : AccessibilityService() {

    private val main = Handler(Looper.getMainLooper())
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val counter = ReelCounter()
    private lateinit var bubble: CounterBubble

    private var lastScanAt = 0L
    private var scanQueued = false
    private val screenBounds = Rect()

    private val scan = Runnable {
        scanQueued = false
        lastScanAt = now()
        scanScreen()
    }

    private val tick = object : Runnable {
        override fun run() {
            apply(counter.onTick(wallNow()))
            if (counter.current != null) main.postDelayed(this, TICK_MS)
        }
    }

    private val leftCheck = Runnable {
        val pkg = rootInActiveWindow?.packageName
        if (ReelApp.forPackage(pkg) == null) {
            if (counter.current != null) DetectionLog.add("left feed: foreground is $pkg")
            apply(counter.onLeft(wallNow()))
        }
    }

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            apply(counter.onLeft(wallNow()))
        }
    }

    override fun onServiceConnected() {
        Stats.init(this)
        bubble = CounterBubble(this)
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
        DetectionLog.add("service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName ?: return
        if (pkg == packageName) return
        val app = ReelApp.forPackage(pkg)
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (app == null) {
                    // Keyboards and system overlays also fire this; confirm what is really in front.
                    if (counter.current != null) {
                        main.removeCallbacks(leftCheck)
                        main.postDelayed(leftCheck, LEFT_CHECK_MS)
                    }
                } else {
                    main.removeCallbacks(leftCheck)
                    queueScan(immediate = true)
                    logScreenIds(app)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> if (app != null) queueScan(immediate = false)
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> if (app != null) onScroll(app, event)
        }
    }

    private fun onScroll(app: ReelApp, event: AccessibilityEvent) {
        if (counter.current != app) {
            queueScan(immediate = true)
            return
        }
        val source = event.source ?: return
        val bounds = Rect().also { source.getBoundsInScreen(it) }
        val screenH = screenHeight()
        // The feed's pager fills the screen; comment sheets and carousels do not.
        if (bounds.height() < screenH * PAGER_MIN_HEIGHT) return
        val dx = event.scrollDeltaX.takeIf { it != UNSET }
        val dy = event.scrollDeltaY.takeIf { it != UNSET }
        if (dx != null && dy != null && abs(dx) > abs(dy)) return
        DetectionLog.add(
            "scroll ${app.label} id=${source.viewIdResourceName?.substringAfter(":id/")} " +
                "idx=${event.fromIndex}..${event.toIndex} dy=$dy h=${bounds.height()}",
        )
        apply(counter.onPagerScroll(event.fromIndex, event.toIndex, dy, bounds.height(), wallNow()))
        ensureTicking()
    }

    private fun queueScan(immediate: Boolean) {
        if (scanQueued) return
        scanQueued = true
        val wait = if (immediate) 0L else (SCAN_MIN_GAP_MS - (now() - lastScanAt)).coerceAtLeast(0L)
        main.postDelayed(scan, wait)
    }

    private fun scanScreen() {
        val root = rootInActiveWindow ?: return
        val app = ReelApp.forPackage(root.packageName)
        if (app == null) {
            apply(counter.onLeft(wallNow()))
            return
        }
        val onFeed = findMarker(root, app) != null
        apply(counter.onSurface(if (onFeed) app else null, wallNow()))
        if (onFeed) ensureTicking()
    }

    private fun findMarker(root: AccessibilityNodeInfo, app: ReelApp): String? {
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            val id = node.viewIdResourceName
            if (id != null && app.matches(id) && fillsScreen(node)) return id
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return null
    }

    /**
     * Apps keep a hidden feed in the view tree after the user switches tab (YouTube keeps the
     * Shorts page under Home), so a marker only counts when it is actually showing and large.
     */
    private fun fillsScreen(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        return node.isVisibleToUser && bounds.height() >= screenHeight() * PAGER_MIN_HEIGHT
    }

    private fun logScreenIds(app: ReelApp) {
        val root = rootInActiveWindow ?: return
        val ids = linkedSetOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES && ids.size < 40) {
            val node = queue.removeFirst()
            visited++
            node.viewIdResourceName?.substringAfter(":id/")?.let(ids::add)
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        DetectionLog.add("screen ${app.label}: ${ids.joinToString(", ")}")
    }

    private fun apply(effects: List<ReelCounter.Effect>) {
        for (effect in effects) when (effect) {
            is ReelCounter.Effect.Entered -> {
                DetectionLog.add("entered ${effect.app.label} ${effect.app.feedName}")
                bubble.show()
                ensureTicking()
            }
            is ReelCounter.Effect.Exited -> {
                DetectionLog.add("exited ${effect.app.label} ${effect.app.feedName}")
                bubble.hide()
            }
            is ReelCounter.Effect.Counted -> io.launch {
                Stats.addReel(effect.app, effect.at)
                val today = Stats.reelDays(LocalDate.now(), LocalDate.now()).first()
                DetectionLog.add("counted ${effect.app.label} -> ${today.totalReels} today")
                main.post { bubble.update(today.totalReels, today.totalMillis) }
            }
            is ReelCounter.Effect.Watched -> io.launch {
                Stats.addWatched(effect.app, effect.startMs, effect.endMs)
            }
        }
    }

    private fun ensureTicking() {
        main.removeCallbacks(tick)
        main.postDelayed(tick, TICK_MS)
    }

    private fun screenHeight(): Int {
        if (screenBounds.isEmpty) {
            val wm = getSystemService(android.view.WindowManager::class.java)
            screenBounds.set(wm.currentWindowMetrics.bounds)
        }
        return screenBounds.height()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        screenBounds.setEmpty()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        apply(counter.onLeft(wallNow()))
        runCatching { unregisterReceiver(screenOff) }
        main.removeCallbacksAndMessages(null)
        if (::bubble.isInitialized) bubble.hide()
        // io is left running so the final writes above still land.
        super.onDestroy()
    }

    private fun now() = SystemClock.uptimeMillis()
    private fun wallNow() = System.currentTimeMillis()

    private companion object {
        const val UNSET = -1
        const val TICK_MS = 400L
        const val SCAN_MIN_GAP_MS = 600L
        const val LEFT_CHECK_MS = 300L
        const val MAX_NODES = 800
        const val PAGER_MIN_HEIGHT = 0.6f
    }
}

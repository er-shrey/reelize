package com.shrey.reelize.detect

import kotlin.math.abs

/**
 * Turns raw accessibility signals into "a reel was watched" and "this much time was spent".
 *
 * Pure logic with an injected clock so it can be unit tested; [ReelWatchService] feeds it.
 *
 * Counting has two modes, chosen per session by what the app's pager reports:
 * - **Index mode.** A RecyclerView pager (YouTube Shorts) reports the visible item index.
 *   A reel counts the first time a higher index settles, so swiping back to a reel already
 *   seen does not count it twice.
 * - **Burst mode.** Pagers that report no index are counted per swipe: scroll events are
 *   grouped into a burst, and a burst that ends moving forward and whose forward movement
 *   adds up to a meaningful fraction of the pager's height is one reel.
 *   YouTube's Shorts pager also reports a page-sized reset jump (about -height) somewhere in
 *   each swipe - first on a short flick, last on a long drag - so net movement nets to zero
 *   and the reset can't be trusted for direction. Direction therefore comes from the last
 *   ordinary-sized movement, skipping reset jumps. A drag that snaps back ends moving
 *   backward and is not counted.
 *
 * The reel on screen when the user enters the feed counts too.
 */
class ReelCounter(
    private val settleMs: Long = 450,
    private val exitGraceMs: Long = 1_500,
    private val flushEveryMs: Long = 15_000,
    private val minSwipeFraction: Float = 0.25f,
) {
    sealed interface Effect {
        data class Entered(val app: ReelApp) : Effect
        data class Exited(val app: ReelApp) : Effect
        data class Counted(val app: ReelApp, val at: Long) : Effect
        /** Time spent in the feed, [startMs] until [endMs]. Emitted in slices as it accrues. */
        data class Watched(val app: ReelApp, val startMs: Long, val endMs: Long) : Effect
    }

    var current: ReelApp? = null
        private set

    private var accruedFrom = 0L
    private var absentSince = -1L

    private var maxIndex = -1
    private var indexMode = false

    private var burstLastAt = -1L
    private var burstForward = 0
    private var burstLastSign = 0
    private var burstHasDelta = false
    private var burstPagerHeight = 0

    /**
     * Called after each look at the screen. [app] is the app whose feed is visible, or null
     * when no feed marker was found. A single miss does not end the session, because tree
     * snapshots taken mid-transition can briefly lack the markers.
     */
    fun onSurface(app: ReelApp?, now: Long): List<Effect> {
        val out = mutableListOf<Effect>()
        if (app == null) {
            if (current != null && absentSince < 0) absentSince = now
            out += onTick(now)
            return out
        }
        absentSince = -1
        if (current != app) {
            current?.let { out += exit(it, now) }
            current = app
            accruedFrom = now
            maxIndex = -1
            indexMode = false
            resetBurst()
            out += Effect.Entered(app)
            out += Effect.Counted(app, now)
        }
        return out
    }

    /** The foreground switched to another app entirely: leave without a grace period. */
    fun onLeft(now: Long): List<Effect> = current?.let { exit(it, now) } ?: emptyList()

    /**
     * A vertical scroll from the full-screen pager.
     *
     * @param fromIndex first visible item, or -1 when the pager does not report one
     * @param toIndex last visible item, or -1
     * @param deltaY pixels moved by this event, or null when not reported
     */
    fun onPagerScroll(fromIndex: Int, toIndex: Int, deltaY: Int?, pagerHeight: Int, now: Long): List<Effect> {
        val app = current ?: return emptyList()
        absentSince = -1
        if (fromIndex >= 0) {
            indexMode = true
            resetBurst()
            // Mid-swipe two items are visible; only a settled single item counts.
            if (fromIndex == toIndex && fromIndex > maxIndex) {
                val first = maxIndex < 0
                maxIndex = fromIndex
                // The entry reel was already counted on entering.
                if (!first) return listOf(Effect.Counted(app, now))
            }
            return emptyList()
        }
        if (indexMode) return emptyList()
        burstLastAt = now
        burstPagerHeight = pagerHeight
        if (deltaY != null) {
            burstHasDelta = true
            val isReset = pagerHeight > 0 && abs(deltaY) >= pagerHeight * RESET_FRACTION
            if (!isReset && deltaY != 0) burstLastSign = if (deltaY > 0) 1 else -1
            if (deltaY > 0) burstForward += deltaY
        }
        return emptyList()
    }

    /** Called periodically while a feed is open: settles swipes, ends absent sessions, flushes time. */
    fun onTick(now: Long): List<Effect> {
        val app = current ?: return emptyList()
        val out = mutableListOf<Effect>()
        if (burstLastAt >= 0 && now - burstLastAt >= settleMs) {
            if (isForwardSwipe()) out += Effect.Counted(app, burstLastAt)
            resetBurst()
        }
        if (absentSince >= 0 && now - absentSince >= exitGraceMs) {
            out += exit(app, absentSince)
            return out
        }
        if (now - accruedFrom >= flushEveryMs) {
            out += Effect.Watched(app, accruedFrom, now)
            accruedFrom = now
        }
        return out
    }

    private fun isForwardSwipe(): Boolean {
        if (!burstHasDelta) return true
        val needed = (burstPagerHeight * minSwipeFraction).toInt().coerceAtLeast(1)
        return burstLastSign > 0 && burstForward >= needed
    }

    private fun exit(app: ReelApp, at: Long): List<Effect> {
        val out = mutableListOf<Effect>()
        if (burstLastAt >= 0 && isForwardSwipe()) out += Effect.Counted(app, burstLastAt)
        resetBurst()
        val end = maxOf(at, accruedFrom)
        if (end > accruedFrom) out += Effect.Watched(app, accruedFrom, end)
        out += Effect.Exited(app)
        current = null
        absentSince = -1
        return out
    }

    private fun resetBurst() {
        burstLastAt = -1
        burstForward = 0
        burstLastSign = 0
        burstHasDelta = false
    }

    private companion object {
        /** A single event moving at least this much of the pager is a page reset, not a drag. */
        const val RESET_FRACTION = 0.45f
    }
}

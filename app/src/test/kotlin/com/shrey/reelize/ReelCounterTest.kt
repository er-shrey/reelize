package com.shrey.reelize

import com.google.common.truth.Truth.assertThat
import com.shrey.reelize.detect.ReelApp
import com.shrey.reelize.detect.ReelCounter
import com.shrey.reelize.detect.ReelCounter.Effect
import org.junit.Test

class ReelCounterTest {
    private val yt = ReelApp.YOUTUBE
    private val ig = ReelApp.INSTAGRAM
    private val h = 2000

    private fun List<Effect>.counted() = filterIsInstance<Effect.Counted>().size

    @Test
    fun `entering the feed counts the reel on screen`() {
        val c = ReelCounter()
        val fx = c.onSurface(yt, 0)
        assertThat(fx.first()).isEqualTo(Effect.Entered(yt))
        assertThat(fx.counted()).isEqualTo(1)
        assertThat(c.onSurface(yt, 100).counted()).isEqualTo(0)
    }

    @Test
    fun `index mode counts each new settled index once`() {
        val c = ReelCounter()
        c.onSurface(yt, 0)
        var n = 0
        n += c.onPagerScroll(0, 0, null, h, 10).counted() // entry reel, already counted
        n += c.onPagerScroll(0, 1, null, h, 20).counted() // mid swipe
        n += c.onPagerScroll(1, 1, null, h, 30).counted()
        n += c.onPagerScroll(1, 1, null, h, 40).counted() // repeat event
        n += c.onPagerScroll(0, 0, null, h, 50).counted() // swiped back
        n += c.onPagerScroll(1, 1, null, h, 60).counted() // forward again to a seen reel
        n += c.onPagerScroll(2, 2, null, h, 70).counted()
        assertThat(n).isEqualTo(2)
    }

    @Test
    fun `burst mode counts a forward swipe once it settles`() {
        val c = ReelCounter(settleMs = 400)
        c.onSurface(ig, 0)
        c.onPagerScroll(-1, -1, 300, h, 100)
        c.onPagerScroll(-1, -1, 900, h, 150)
        assertThat(c.onTick(300).counted()).isEqualTo(0)
        assertThat(c.onTick(600).counted()).isEqualTo(1)
        assertThat(c.onTick(1200).counted()).isEqualTo(0)
    }

    @Test
    fun `burst mode ignores backward swipes and small drags`() {
        val c = ReelCounter(settleMs = 400)
        c.onSurface(ig, 0)
        c.onPagerScroll(-1, -1, -1500, h, 100)
        assertThat(c.onTick(600).counted()).isEqualTo(0)
        c.onPagerScroll(-1, -1, 100, h, 1000)
        assertThat(c.onTick(1500).counted()).isEqualTo(0)
    }

    @Test
    fun `burst mode counts a page that reports forward movement then a reset`() {
        // Captured from YouTube Shorts on API 37: one swipe, pager height 2093.
        val c = ReelCounter(settleMs = 400)
        c.onSurface(yt, 0)
        listOf(784, 635, 507, -1926).forEachIndexed { i, dy -> c.onPagerScroll(-1, -1, dy, 2093, 100L + i * 30) }
        assertThat(c.onTick(800).counted()).isEqualTo(1)
    }

    @Test
    fun `burst mode counts a short flick whose reset jump comes first`() {
        // Captured from YouTube Shorts: a short quick flick that still turned the page.
        val c = ReelCounter(settleMs = 400)
        c.onSurface(yt, 0)
        listOf(-779, 756, 23).forEachIndexed { i, dy -> c.onPagerScroll(-1, -1, dy, 2093, 100L + i * 30) }
        assertThat(c.onTick(800).counted()).isEqualTo(1)
    }

    @Test
    fun `burst mode counts a slow drag with the reset in the middle`() {
        val c = ReelCounter(settleMs = 400)
        c.onSurface(yt, 0)
        listOf(78, 78, 78, 78, -1289, 930, 47).forEachIndexed { i, dy -> c.onPagerScroll(-1, -1, dy, 2093, 100L + i * 30) }
        assertThat(c.onTick(900).counted()).isEqualTo(1)
    }

    @Test
    fun `burst mode ignores a drag that snaps back`() {
        val c = ReelCounter(settleMs = 400)
        c.onSurface(yt, 0)
        listOf(120, 150, 90, -200, -160).forEachIndexed { i, dy -> c.onPagerScroll(-1, -1, dy, 2093, 100L + i * 30) }
        assertThat(c.onTick(900).counted()).isEqualTo(0)
    }

    @Test
    fun `burst without delta info counts each swipe`() {
        val c = ReelCounter(settleMs = 400)
        c.onSurface(ig, 0)
        c.onPagerScroll(-1, -1, null, h, 100)
        c.onPagerScroll(-1, -1, null, h, 120)
        assertThat(c.onTick(600).counted()).isEqualTo(1)
    }

    @Test
    fun `a brief miss of the markers does not end the session`() {
        val c = ReelCounter(exitGraceMs = 1500)
        c.onSurface(yt, 0)
        c.onSurface(null, 1000)
        val back = c.onSurface(yt, 1500)
        assertThat(back.counted()).isEqualTo(0)
        assertThat(c.current).isEqualTo(yt)
    }

    @Test
    fun `markers gone past the grace period ends the session and reports time`() {
        val c = ReelCounter(exitGraceMs = 1500, flushEveryMs = 60_000)
        c.onSurface(yt, 0)
        c.onSurface(null, 10_000)
        val fx = c.onTick(11_600)
        assertThat(fx).contains(Effect.Watched(yt, 0, 10_000))
        assertThat(fx.last()).isEqualTo(Effect.Exited(yt))
        assertThat(c.current).isNull()
    }

    @Test
    fun `leaving the app ends the session at once and time is flushed in slices`() {
        val c = ReelCounter(flushEveryMs = 15_000)
        c.onSurface(yt, 0)
        assertThat(c.onTick(15_000)).contains(Effect.Watched(yt, 0, 15_000))
        val fx = c.onLeft(20_000)
        assertThat(fx).containsExactly(Effect.Watched(yt, 15_000, 20_000), Effect.Exited(yt)).inOrder()
    }

    @Test
    fun `switching from one feed to the other counts both entries`() {
        val c = ReelCounter()
        c.onSurface(ig, 0)
        val fx = c.onSurface(yt, 5_000)
        assertThat(fx).contains(Effect.Exited(ig))
        assertThat(fx).contains(Effect.Counted(yt, 5_000))
    }
}

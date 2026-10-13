package com.shrey.reelize

import com.google.common.truth.Truth.assertThat
import com.shrey.reelize.data.splitByDay
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DaySplitTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(s: String) = LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `a session inside one day stays on that day`() {
        val parts = splitByDay(at("2026-10-13T10:00:00"), at("2026-10-13T10:05:00"), zone)
        assertThat(parts).containsExactly(LocalDate.parse("2026-10-13") to 300_000L)
    }

    @Test
    fun `a session past midnight is split between the two days`() {
        val parts = splitByDay(at("2026-10-13T23:58:00"), at("2026-10-14T00:03:00"), zone)
        assertThat(parts).containsExactly(
            LocalDate.parse("2026-10-13") to 120_000L,
            LocalDate.parse("2026-10-14") to 180_000L,
        ).inOrder()
    }

    @Test
    fun `an empty or reversed range yields nothing`() {
        assertThat(splitByDay(5, 5, zone)).isEmpty()
        assertThat(splitByDay(10, 5, zone)).isEmpty()
    }
}

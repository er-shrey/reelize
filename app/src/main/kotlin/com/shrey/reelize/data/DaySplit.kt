package com.shrey.reelize.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Splits [startMs, endMs) at local midnights, so a session that runs past 12:00 AM is
 * credited to each day for the part that fell in it. This is what makes the daily counter
 * reset at midnight without any scheduled job.
 */
fun splitByDay(startMs: Long, endMs: Long, zone: ZoneId = ZoneId.systemDefault()): List<Pair<LocalDate, Long>> {
    if (endMs <= startMs) return emptyList()
    val out = mutableListOf<Pair<LocalDate, Long>>()
    var cursor = startMs
    while (cursor < endMs) {
        val day = dayOf(cursor, zone)
        val nextMidnight = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val sliceEnd = minOf(endMs, nextMidnight)
        out += day to (sliceEnd - cursor)
        cursor = sliceEnd
    }
    return out
}

fun dayOf(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

fun LocalDate.startMs(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

package com.shrey.reelize.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun duration(ms: Long): String {
    val m = ms / 60_000
    return when {
        m < 1 -> if (ms > 0) "<1m" else "0m"
        m < 60 -> "${m}m"
        else -> "${m / 60}h ${m % 60}m"
    }
}

private val longDate = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

fun dayTitle(day: LocalDate, today: LocalDate = LocalDate.now()): String = when (day) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> day.format(longDate)
}

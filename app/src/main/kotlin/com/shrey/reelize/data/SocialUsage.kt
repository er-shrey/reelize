package com.shrey.reelize.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.LocalDate

/** Apps counted as "social media" in the daily total. Messaging apps are left out on purpose. */
val SOCIAL_APPS: Map<String, String> = linkedMapOf(
    "com.instagram.android" to "Instagram",
    "com.google.android.youtube" to "YouTube",
    "com.facebook.katana" to "Facebook",
    "com.facebook.lite" to "Facebook Lite",
    "com.snapchat.android" to "Snapchat",
    "com.zhiliaoapp.musically" to "TikTok",
    "com.ss.android.ugc.trill" to "TikTok",
    "com.twitter.android" to "X",
    "com.instagram.barcelona" to "Threads",
    "com.reddit.frontpage" to "Reddit",
    "com.pinterest" to "Pinterest",
    "in.mohalla.sharechat" to "ShareChat",
    "com.eterno.shortvideos" to "Josh",
    "in.mohalla.video" to "Moj",
)

object SocialUsage {

    fun hasAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Foreground time per social app on [day]. Finished days come from the saved copy when
     * there is one; otherwise they are computed from usage events and saved, because Android
     * drops those events after about a week.
     */
    fun forDay(context: Context, day: LocalDate, today: LocalDate = LocalDate.now()): Map<String, Long> {
        if (day.isBefore(today)) Stats.savedSocial(day)?.let { return it }
        if (!hasAccess(context)) return emptyMap()
        val start = day.startMs()
        val end = minOf(day.plusDays(1).startMs(), System.currentTimeMillis())
        if (end <= start) return emptyMap()
        val computed = compute(context, start, end)
        if (day.isBefore(today)) Stats.saveSocial(day, computed)
        return computed
    }

    private fun compute(context: Context, start: Long, end: Long): Map<String, Long> {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val events = usm.queryEvents(start, end)
        val openSince = mutableMapOf<String, Long>()
        val total = mutableMapOf<String, Long>()
        val e = UsageEvents.Event()

        fun close(pkg: String, at: Long) {
            val since = openSince.remove(pkg) ?: return
            if (at > since) total[pkg] = (total[pkg] ?: 0L) + (at - since)
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    if (e.packageName in SOCIAL_APPS) openSince.putIfAbsent(e.packageName, e.timeStamp)
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED ->
                    close(e.packageName, e.timeStamp)
                UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.DEVICE_SHUTDOWN ->
                    openSince.keys.toList().forEach { close(it, e.timeStamp) }
            }
        }
        openSince.keys.toList().forEach { close(it, end) }
        return total.filterValues { it >= 1_000 }
    }
}

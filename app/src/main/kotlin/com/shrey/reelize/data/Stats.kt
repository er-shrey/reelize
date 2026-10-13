package com.shrey.reelize.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.shrey.reelize.detect.ReelApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

data class ReelDay(
    val day: LocalDate,
    val reels: Map<ReelApp, Int>,
    val millis: Map<ReelApp, Long>,
) {
    val totalReels: Int get() = reels.values.sum()
    val totalMillis: Long get() = millis.values.sum()
}

/**
 * The single store both the detection service and the dashboard use (same process).
 * Calls block on disk, so callers run them off the main thread.
 */
object Stats {
    private lateinit var db: StatsDb

    private val _version = MutableStateFlow(0L)

    /** Bumped on every write, so screens know to reload. */
    val version: StateFlow<Long> = _version

    fun init(context: Context) {
        if (!::db.isInitialized) db = StatsDb(context.applicationContext)
    }

    @Synchronized
    fun addReel(app: ReelApp, at: Long) {
        bump(dayOf(at), app, reels = 1, millis = 0)
        _version.value++
    }

    @Synchronized
    fun addWatched(app: ReelApp, startMs: Long, endMs: Long) {
        splitByDay(startMs, endMs).forEach { (day, ms) -> bump(day, app, reels = 0, millis = ms) }
        _version.value++
    }

    fun reelsOn(day: LocalDate): Int = db.readableDatabase.rawQuery(
        "SELECT COALESCE(SUM(reels), 0) FROM reel_day WHERE day = ?",
        arrayOf(day.toString()),
    ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun reelDays(from: LocalDate, to: LocalDate): List<ReelDay> {
        val reels = mutableMapOf<LocalDate, MutableMap<ReelApp, Int>>()
        val millis = mutableMapOf<LocalDate, MutableMap<ReelApp, Long>>()
        db.readableDatabase.rawQuery(
            "SELECT day, app, reels, millis FROM reel_day WHERE day BETWEEN ? AND ?",
            arrayOf(from.toString(), to.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                val day = LocalDate.parse(c.getString(0))
                val app = ReelApp.entries.firstOrNull { it.name == c.getString(1) } ?: continue
                reels.getOrPut(day) { mutableMapOf() }[app] = c.getInt(2)
                millis.getOrPut(day) { mutableMapOf() }[app] = c.getLong(3)
            }
        }
        return generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }
            .map { ReelDay(it, reels[it].orEmpty(), millis[it].orEmpty()) }
            .toList()
    }

    /** Saved social time for a finished day, or null if it was never saved. */
    fun savedSocial(day: LocalDate): Map<String, Long>? {
        val r = db.readableDatabase
        val done = r.rawQuery("SELECT 1 FROM social_done WHERE day = ?", arrayOf(day.toString()))
            .use { it.moveToFirst() }
        if (!done) return null
        return r.rawQuery("SELECT pkg, millis FROM social_day WHERE day = ?", arrayOf(day.toString()))
            .use { c -> buildMap { while (c.moveToNext()) put(c.getString(0), c.getLong(1)) } }
    }

    @Synchronized
    fun saveSocial(day: LocalDate, byPackage: Map<String, Long>) {
        val w = db.writableDatabase
        w.beginTransaction()
        try {
            w.delete("social_day", "day = ?", arrayOf(day.toString()))
            byPackage.forEach { (pkg, ms) ->
                w.insert("social_day", null, ContentValues().apply {
                    put("day", day.toString()); put("pkg", pkg); put("millis", ms)
                })
            }
            w.insertWithOnConflict("social_done", null, ContentValues().apply { put("day", day.toString()) },
                SQLiteDatabase.CONFLICT_IGNORE)
            w.setTransactionSuccessful()
        } finally {
            w.endTransaction()
        }
    }

    private fun bump(day: LocalDate, app: ReelApp, reels: Int, millis: Long) {
        db.writableDatabase.execSQL(
            """INSERT INTO reel_day (day, app, reels, millis) VALUES (?, ?, ?, ?)
               ON CONFLICT(day, app) DO UPDATE SET reels = reels + excluded.reels, millis = millis + excluded.millis""",
            arrayOf<Any>(day.toString(), app.name, reels, millis),
        )
    }
}

package com.shrey.reelize.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Two small tables, one row per day per app:
 * - `reel_day`: reels counted and time spent in the short-video feed.
 * - `social_day`: total foreground time per social app, saved once a day is over. Android
 *   keeps raw usage events for only about a week, so finished days are kept here to keep
 *   history beyond that.
 */
class StatsDb(context: Context) : SQLiteOpenHelper(context, "reelize.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE reel_day (
                day TEXT NOT NULL, app TEXT NOT NULL,
                reels INTEGER NOT NULL DEFAULT 0, millis INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (day, app))""",
        )
        db.execSQL(
            """CREATE TABLE social_day (
                day TEXT NOT NULL, pkg TEXT NOT NULL, millis INTEGER NOT NULL,
                PRIMARY KEY (day, pkg))""",
        )
        db.execSQL("CREATE TABLE social_done (day TEXT PRIMARY KEY NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}

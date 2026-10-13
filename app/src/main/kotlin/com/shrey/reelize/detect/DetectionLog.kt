package com.shrey.reelize.detect

import android.util.Log
import com.shrey.reelize.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * An in-memory record of what the detector saw, shown on the Detection log screen.
 *
 * Instagram and YouTube rename their internal views with updates, so when counting goes
 * wrong this log - in particular the view IDs captured on each screen - is what shows which
 * marker in [ReelApp] needs changing. Nothing is written to disk or leaves the device.
 */
object DetectionLog {
    private const val MAX = 300
    private val time = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    fun add(message: String) {
        if (BuildConfig.DEBUG) Log.d("Reelize", message)
        val line = "${LocalTime.now().format(time)}  $message"
        synchronized(this) { _lines.value = (_lines.value + line).takeLast(MAX) }
    }

    fun clear() {
        synchronized(this) { _lines.value = emptyList() }
    }
}

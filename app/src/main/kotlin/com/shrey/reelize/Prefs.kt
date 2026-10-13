package com.shrey.reelize

import android.content.Context
import androidx.core.content.edit
import com.shrey.reelize.overlay.BubbleImage
import com.shrey.reelize.overlay.BubblePosition

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("reelize", Context.MODE_PRIVATE)

    var bubbleEnabled: Boolean
        get() = sp.getBoolean("bubble_enabled", true)
        set(v) = sp.edit { putBoolean("bubble_enabled", v) }

    /** True: green, amber, red as the count climbs. False: always [bubbleColor]. */
    var colorByCount: Boolean
        get() = sp.getBoolean("bubble_color_by_count", true)
        set(v) = sp.edit { putBoolean("bubble_color_by_count", v) }

    var bubbleColor: Int
        get() = sp.getInt("bubble_color", 0xFF6A3FD0.toInt())
        set(v) = sp.edit { putInt("bubble_color", v) }

    /** 0.3 to 1.0. */
    var bubbleOpacity: Float
        get() = sp.getFloat("bubble_opacity", 0.92f)
        set(v) = sp.edit { putFloat("bubble_opacity", v.coerceIn(0.3f, 1f)) }

    var bubbleImage: BubbleImage
        get() = BubbleImage.entries.firstOrNull { it.name == sp.getString("bubble_image", null) } ?: BubbleImage.NONE
        set(v) = sp.edit { putString("bubble_image", v.name) }

    var bubblePosition: BubblePosition
        get() = BubblePosition.entries.firstOrNull { it.name == sp.getString("bubble_position", null) }
            ?: BubblePosition.MIDDLE_RIGHT
        set(v) = sp.edit { putString("bubble_position", v.name) }

    /** Keep the bubble where it was last dragged; otherwise it returns to [bubblePosition] each time. */
    var rememberDrag: Boolean
        get() = sp.getBoolean("bubble_remember_drag", true)
        set(v) = sp.edit { putBoolean("bubble_remember_drag", v) }

    var bubbleX: Int
        get() = sp.getInt("bubble_x", -1)
        set(v) = sp.edit { putInt("bubble_x", v) }

    var bubbleY: Int
        get() = sp.getInt("bubble_y", -1)
        set(v) = sp.edit { putInt("bubble_y", v) }

    fun clearDraggedPosition() = sp.edit { remove("bubble_x"); remove("bubble_y") }
}

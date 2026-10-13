package com.shrey.reelize.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.scale
import java.io.File
import kotlin.math.min

/** Pictures that can sit in the bubble next to the count. Emoji, so they need no assets. */
enum class BubbleImage(val emoji: String?, val label: String) {
    NONE(null, "None"),
    FLOWER("🌸", "Flower"),
    SUNFLOWER("🌻", "Sunflower"),
    LEAF("🍀", "Clover"),
    STAR("⭐", "Star"),
    HOURGLASS("⏳", "Hourglass"),
    EYES("👀", "Eyes"),
    FIRE("🔥", "Fire"),
    BRAIN("🧠", "Brain"),
    TURTLE("🐢", "Turtle"),
    COFFEE("☕", "Coffee"),
    PHOTO(null, "Your photo"),
}

/** Where the bubble first appears, as fractions of the screen. */
enum class BubblePosition(val label: String, val xFraction: Float, val yFraction: Float) {
    TOP_LEFT("Top left", 0f, 0.12f),
    TOP_RIGHT("Top right", 1f, 0.12f),
    MIDDLE_LEFT("Middle left", 0f, 0.42f),
    MIDDLE_RIGHT("Middle right", 1f, 0.42f),
    BOTTOM_LEFT("Bottom left", 0f, 0.72f),
    BOTTOM_RIGHT("Bottom right", 1f, 0.72f),
}

val BUBBLE_SWATCHES: List<Int> = listOf(
    0xFF6A3FD0, 0xFF1E88E5, 0xFF00897B, 0xFF2E7D32, 0xFFF9A825,
    0xFFEF6C00, 0xFFD9485F, 0xFFC62828, 0xFFAD1457, 0xFF37474F, 0xFF000000, 0xFFFFFFFF,
).map { it.toInt() }

const val AMBER_AT = 25
const val RED_AT = 50

/** Background and text colour for a given count. */
fun bubbleColors(count: Int, colorByCount: Boolean, baseColor: Int): Pair<Int, Int> {
    val bg = when {
        !colorByCount -> baseColor
        count >= RED_AT -> 0xFFC62828.toInt()
        count >= AMBER_AT -> 0xFFF9A825.toInt()
        else -> 0xFF2E7D32.toInt()
    }
    val fg = if (ColorUtils.calculateLuminance(bg) > 0.5) 0xFF1C1B20.toInt() else Color.WHITE
    return bg to fg
}

/** The user's own picture, stored privately as a small square PNG. */
object BubblePhoto {
    private const val SIZE = 256

    fun file(context: Context) = File(context.filesDir, "bubble_photo.png")

    fun load(context: Context): Bitmap? = file(context).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    /** Center-crops [uri] to a square and saves it. Returns false if it could not be read. */
    fun save(context: Context, uri: Uri): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return false
        val side = min(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val small = square.scale(SIZE, SIZE)
        file(context).outputStream().use { small.compress(Bitmap.CompressFormat.PNG, 100, it) }
        true
    }.getOrDefault(false)
}

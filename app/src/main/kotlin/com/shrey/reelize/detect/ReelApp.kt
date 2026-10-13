package com.shrey.reelize.detect

/**
 * The apps whose short-video feeds Reelize counts.
 *
 * [markers] are substrings of view resource names of the full-screen short-video player. They are internal to each app and change with app updates, so they
 * are kept here, in one place, and checked against what the detection log shows on device.
 */
enum class ReelApp(val packageName: String, val label: String, val feedName: String, val markers: List<String>) {
    INSTAGRAM(
        packageName = "com.instagram.android",
        label = "Instagram",
        feedName = "Reels",
        markers = listOf("clips_viewer", "clips_video_container", "clips_single_media"),
    ),
    YOUTUBE(
        packageName = "com.google.android.youtube",
        label = "YouTube",
        feedName = "Shorts",
        markers = listOf("reel_recycler", "reel_player_page_container", "reel_watch_player"),
    ),
    ;

    fun matches(viewId: String): Boolean = markers.any { viewId.contains(it) }

    companion object {
        fun forPackage(pkg: CharSequence?): ReelApp? = entries.firstOrNull { it.packageName == pkg }
    }
}

package com.shrey.reelize.ui

import android.os.Build

/**
 * How to stop a given phone from force-stopping Reelize. A force-stop switches the
 * accessibility service off, and only the user can switch it back on, so prevention matters.
 *
 * Several brands force-stop every app in Recents on "Clear all" unless the app is locked
 * there, and some also kill background apps through their own battery managers.
 */
data class KeepRunningTips(
    val brand: String,
    val steps: List<String>,
    /** The brand's own background-launch screen, when one exists; it may be missing on some versions. */
    val extraScreen: ExtraScreen? = null,
)

data class ExtraScreen(val label: String, val packageName: String, val className: String)

fun keepRunningTips(manufacturer: String = Build.MANUFACTURER): KeepRunningTips =
    when (manufacturer.lowercase()) {
        "xiaomi", "redmi", "poco" -> KeepRunningTips(
            brand = "Xiaomi",
            steps = listOf(
                "Lock Reelize in Recents: open Recents, long-press the Reelize card and tap the lock. \"Clear all\" then leaves it alone.",
                "Turn on Autostart for Reelize.",
                "Settings → Apps → Reelize → Battery saver → No restrictions.",
            ),
            extraScreen = ExtraScreen(
                label = "Autostart",
                packageName = "com.miui.securitycenter",
                className = "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
        )
        "oppo", "realme", "oneplus" -> KeepRunningTips(
            brand = manufacturer.replaceFirstChar { it.uppercase() },
            steps = listOf(
                "Lock Reelize in Recents: open Recents, tap ⋮ on the Reelize card (or long-press it) and choose Lock.",
                "Settings → Apps → Reelize → Battery usage → turn on Allow background activity and Allow auto launch.",
            ),
        )
        "vivo", "iqoo" -> KeepRunningTips(
            brand = "Vivo",
            steps = listOf(
                "Lock Reelize in Recents: open Recents and pull the Reelize card down until it locks.",
                "Settings → Battery → Background power consumption management → Reelize → Allow.",
            ),
        )
        "samsung" -> KeepRunningTips(
            brand = "Samsung",
            steps = listOf(
                "Settings → Apps → Reelize → Battery → Unrestricted.",
                "Settings → Battery → Background usage limits → Never sleeping apps → add Reelize.",
                "In Recents, tap the Reelize icon above its card and choose Keep open.",
            ),
        )
        "huawei", "honor" -> KeepRunningTips(
            brand = manufacturer.replaceFirstChar { it.uppercase() },
            steps = listOf(
                "Lock Reelize in Recents: open Recents and pull the Reelize card down until it locks.",
                "Settings → Battery → App launch → Reelize → Manage manually, with all three switches on.",
            ),
        )
        else -> KeepRunningTips(
            brand = manufacturer.replaceFirstChar { it.uppercase() },
            steps = listOf(
                "Settings → Apps → Reelize → App battery usage → Unrestricted.",
                "Don't use Force stop on Reelize; that switches the counter off.",
            ),
        )
    }

package com.shrey.reelize.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.shrey.reelize.detect.ReelWatchService

/** Settings shortcuts. All are opened from the dashboard, so they stack on Reelize's task and Back returns to it. */
object Access {
    private fun counterComponent(context: Context) = ComponentName(context, ReelWatchService::class.java)

    fun counterOn(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val me = counterComponent(context)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun openAccessibility(context: Context) =
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openUsageAccess(context: Context) =
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

    /** App info, where the "Allow restricted settings" menu and the battery options live. */
    fun openAppInfo(context: Context) = context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            ,
    )

    fun batteryUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /** Shows Android's "Let app always run in background?" dialog. */
    @SuppressLint("BatteryLife")
    fun requestBatteryUnrestricted(context: Context) {
        val ask = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.fromParts("package", context.packageName, null))
        if (!tryStart(context, ask)) openAppInfo(context)
    }

    fun tryStart(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent) }.isSuccess
}

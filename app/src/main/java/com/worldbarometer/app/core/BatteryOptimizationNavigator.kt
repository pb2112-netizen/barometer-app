package com.worldbarometer.app.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

object BatteryOptimizationNavigator {

    fun open(context: Context) {
        val launchContext = context.findActivityOrSelf()
        val packageUri = Uri.parse("package:${context.packageName}")

        val candidates = listOf(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = packageUri },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = packageUri },
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        )

        for (intent in candidates) {
            try {
                if (intent.resolveActivity(launchContext.packageManager) == null) continue
                if (launchContext !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                launchContext.startActivity(intent)
                Log.d(TAG, "opened battery settings via ${intent.action}")
                return
            } catch (e: Exception) {
                Log.w(TAG, "failed ${intent.action}", e)
            }
        }
        Log.e(TAG, "no battery settings intent could be opened")
    }

    private fun Context.findActivityOrSelf(): Context {
        var ctx: Context = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return applicationContext
    }

    private const val TAG = "WB-Widget"
}

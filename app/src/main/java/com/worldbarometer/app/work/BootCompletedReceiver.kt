package com.worldbarometer.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Po restarcie telefonu WM nie zawsze przeżywa harmonogram — planuj periodic od nowa (WB-045). */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "BOOT_COMPLETED → schedulePeriodic")
        RefreshScheduler.schedulePeriodic(context.applicationContext)
    }

    companion object {
        private const val TAG = "WB-Widget"
    }
}

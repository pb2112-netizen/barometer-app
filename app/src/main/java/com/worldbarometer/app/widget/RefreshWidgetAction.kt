package com.worldbarometer.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.worldbarometer.app.work.RefreshScheduler

/**
 * Tap na widgecie = jednorazowe odświeżenie (SPEC_MVP §3) przez WorkManager (one-off).
 * Worker po pobraniu danych sam odświeży kafelek (updateAll).
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        RefreshScheduler.requestOneOff(context)
    }
}

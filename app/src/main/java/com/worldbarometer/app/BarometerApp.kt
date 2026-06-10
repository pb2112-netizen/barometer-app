package com.worldbarometer.app

import android.app.Application
import com.worldbarometer.app.di.ServiceLocator
import com.worldbarometer.app.work.Notifier
import com.worldbarometer.app.work.RefreshScheduler

class BarometerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        Notifier(this).ensureChannel()
        RefreshScheduler.schedulePeriodic(this)
    }
}

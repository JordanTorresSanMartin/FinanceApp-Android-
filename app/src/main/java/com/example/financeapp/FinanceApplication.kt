package com.example.financeapp

import android.app.Application
import com.example.financeapp.notifications.DueAlerts
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DueAlerts.schedule(this) // idempotente (KEEP)
    }
}

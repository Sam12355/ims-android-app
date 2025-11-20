package com.stocknexus.android

import android.app.Application
import com.stocknexus.service.NotificationPollingReceiver

class StockNexusApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Start periodic notification polling every 1 minute using AlarmManager
        NotificationPollingReceiver.schedulePolling(this)
    }
}
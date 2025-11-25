package com.ims.android

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.ims.android.service.NotificationPollingReceiver
import com.ims.android.data.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StockNexusApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Get FCM token and send to backend
        getFCMToken()
        
        // Polling disabled - using FCM push notifications only
        // NotificationPollingReceiver.schedulePolling(this)
    }
    
    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("StockNexus", "🔑 FCM Token: $token")
                
                // Send token to backend
                applicationScope.launch {
                    try {
                        val apiClient = ApiClient.getInstance(this@StockNexusApplication)
                        val result = apiClient.updateFCMToken(token)
                        
                        if (result.isSuccess) {
                            Log.d("StockNexus", "✅ FCM token successfully sent to backend")
                        } else {
                            Log.e("StockNexus", "❌ Failed to send FCM token to backend: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        Log.e("StockNexus", "❌ Error sending FCM token to backend", e)
                    }
                }
            } else {
                Log.e("StockNexus", "Failed to get FCM token", task.exception)
            }
        }
    }
}
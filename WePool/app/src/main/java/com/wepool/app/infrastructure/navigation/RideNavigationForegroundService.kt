package com.wepool.app.infrastructure.navigation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import androidx.core.os.HandlerCompat
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.wepool.app.R
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.*
import android.app.usage.UsageStatsManager
import android.os.Looper
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.remote.RideNavigationStarter

class RideNavigationForegroundService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_NAVIGATION"
        const val ACTION_STOP = "ACTION_STOP_NAVIGATION"
        const val EXTRA_RIDE_ID = "rideId"
        const val NOTIF_ID = 999
        const val CHANNEL_ID = "ride_navigation_channel"
    }

    private var rideId: String? = null
    private var navigationManager: RideNavigationManager? = null

    private lateinit var checkRunnable: Runnable
    private val handler = HandlerCompat.createAsync(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        checkRunnable = Runnable {
            CoroutineScope(Dispatchers.IO).launch {
                val mapsClosed = isGoogleMapsCompletelyClosed(this@RideNavigationForegroundService)
                val reachedDestination = navigationManager?.hasReachedFinalDestination() == true

                if (mapsClosed || reachedDestination) {
                    Log.d("RideNavService", "✅ סיום ניווט: mapsClosed=$mapsClosed, reachedDestination=$reachedDestination")
                    stopSelf()
                } else {
                    HandlerCompat.postDelayed(handler, checkRunnable, null, 5000L)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                rideId = intent.getStringExtra(EXTRA_RIDE_ID)
                if (!rideId.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val manager = RideNavigationManager(this@RideNavigationForegroundService, rideId!!)
                        val initialized = withContext(Dispatchers.IO) { manager.initialize() }
                        if (initialized) {
                            navigationManager = manager
                            createNotificationChannel()
                            startForeground(NOTIF_ID, buildNotification("🚗 התחלת ניווט ליעד הבא"))
                            startNavigationToCurrentStop()
                            handler.post(checkRunnable)
                        } else {
                            stopSelf()
                        }
                    }
                }
            }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WePool - ניווט פעיל")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ride Navigation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "התראות עבור ניווט פעיל של נהגים"
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startNavigationToCurrentStop() {
        val stop: LocationData = navigationManager?.getCurrentStop() ?: return
        RideNavigationStarter.startNavigationToLocation(this, stop)
    }

    private fun isGoogleMapsCompletelyClosed(context: Context): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 5000

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            beginTime,
            endTime
        ) ?: return true

        val maps = stats.find { it.packageName == "com.google.android.apps.maps" }
        return maps == null || maps.totalTimeInForeground == 0L
    }
}

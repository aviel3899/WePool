package com.wepool.app.infrastructure.navigation

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object RideNavigationServiceController {

    fun startRideNavigation(context: Context, rideId: String) {
        val intent = Intent(context, RideNavigationForegroundService::class.java).apply {
            action = RideNavigationForegroundService.ACTION_START
            putExtra(RideNavigationForegroundService.EXTRA_RIDE_ID, rideId)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopRideNavigation(context: Context) {
        val intent = Intent(context, RideNavigationForegroundService::class.java).apply {
            action = RideNavigationForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}

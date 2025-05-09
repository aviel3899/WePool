package com.wepool.app.infrastructure.navigation

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.wepool.app.data.model.ride.Ride

object RideNavigationServiceController {
    fun startRideNavigation(context: Context, rideId: String) {
        val intent = Intent(context, RideNavigationForegroundService::class.java).apply {
            action = RideNavigationForegroundService.ACTION_START
            putExtra("rideId", rideId)
        }
        ContextCompat.startForegroundService(context, intent)
    }
}


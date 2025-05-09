package com.wepool.app.infrastructure.navigation

import android.content.Context
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.remote.RideNavigationStarter

class RideNavigationManager(
    private val context: Context,
    private val rideId: String
) {
    private var stops: List<LocationData> = emptyList()
    private var currentIndex = 1 // מתחילים בתחנה הראשונה (0 = נקודת התחלה)

    suspend fun initialize(): Boolean {
        val ride = RepositoryProvider.provideRideRepository().getRide(rideId)
        return if (ride != null) {
            stops = listOf(ride.startLocation) + ride.pickupStops.map { it.location } + ride.destination
            true
        } else {
            false
        }
    }

    fun getCurrentStop(): LocationData? =
        if (currentIndex < stops.size) stops[currentIndex] else null

    fun moveToNextStop() {
        if (currentIndex < stops.size - 1) {
            currentIndex++
        }
    }

    fun hasReachedFinalDestination(): Boolean {
        return currentIndex >= stops.lastIndex
    }

}


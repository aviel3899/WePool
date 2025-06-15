package com.wepool.app.infrastructure.navigation

import android.content.Context
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.infrastructure.RepositoryProvider

class RideNavigationManager(
    private val context: Context,
    private val rideId: String
) {
    private var stops: List<LocationData> = emptyList()
    private var pickupStops: List<PickupStop> = emptyList()
    private var currentIndex = 1 // 0 = origin, 1 = התחנה הראשונה

    suspend fun initialize(): Boolean {
        val ride = RepositoryProvider.provideRideRepository().getRide(rideId)
        return if (ride != null) {
            pickupStops = ride.pickupStops
            stops = listOf(ride.startLocation) + ride.pickupStops.map { it.location } + ride.destination
            true
        } else {
            false
        }
    }

    fun getCurrentStop(): LocationData? = stops.getOrNull(currentIndex)

    fun getCurrentPassengerId(): String? {
        if (currentIndex in 1 until stops.size - 1) {
            return pickupStops.getOrNull(currentIndex - 1)?.passengerId
        }
        return null
    }

    fun moveToNextStop() {
        if (currentIndex < stops.lastIndex) {
            currentIndex++
        }
    }

    fun hasReachedFinalDestination(): Boolean = currentIndex >= stops.lastIndex

    fun getAllStops(): Triple<LocationData, List<LocationData>, LocationData> {
        val origin = stops.firstOrNull() ?: return Triple(LocationData(), emptyList(), LocationData())
        val destination = stops.lastOrNull() ?: origin
        val waypoints = stops.subList(1, stops.size - 1)
        return Triple(origin, waypoints, destination)
    }

    fun isCurrentStopPickup(): Boolean {
        return pickupStops.getOrNull(currentIndex - 1)?.pickupTime != null &&
                pickupStops.getOrNull(currentIndex - 1)?.dropoffTime == null
    }

    fun isCurrentStopDropoff(): Boolean {
        return pickupStops.getOrNull(currentIndex - 1)?.dropoffTime != null
    }

    fun getRideId(): String = rideId
}

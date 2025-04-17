package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.logic.DepartureCalculationResult
import com.google.firebase.firestore.GeoPoint

interface IRideRepository {
    suspend fun createRide(driverId: String, ride: Ride)
    suspend fun getRide(driverId: String, rideId: String): Ride?
    suspend fun getRidesForDriver(driverId: String): List<Ride>
    suspend fun updateRide(driverId: String, ride: Ride)
    suspend fun deleteRide(driverId: String, rideId: String)
    suspend fun updateAvailableSeats(driverId: String, rideId: String, seats: Int)
    suspend fun updateMaxDetourMinutes(driverId: String, rideId: String, maxDetour: Int)
    suspend fun updatePreferredArrivalTime(driverId: String, rideId: String, time: String)
    suspend fun updateDestination(driverId: String, rideId: String, destination: GeoPoint)
    suspend fun calculateRideDepartureTime(
        ride: Ride,
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): DepartureCalculationResult
}

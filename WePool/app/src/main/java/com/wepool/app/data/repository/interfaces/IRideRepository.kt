package com.wepool.app.data.repository.interfaces

import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.logic.DepartureCalculationResult
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.enums.RideDirection

interface IRideRepository {
    suspend fun createRide(ride: Ride)
    suspend fun getRide(rideId: String): Ride?
    suspend fun getAllRides(): List<Ride>
    suspend fun getRidesByDriver(driverId: String): List<Ride>
    suspend fun getAvailableRidesForPassenger(
        companyId: String,
        direction: String,
        preferredArrivalTime: String
    ): List<Ride>

    suspend fun updateRide(ride: Ride)
    suspend fun deleteRide(rideId: String)
    suspend fun updateAvailableSeats(rideId: String, seats: Int)
    suspend fun updateMaxDetourMinutes(rideId: String, maxDetour: Int)
    suspend fun updatePreferredArrivalTime(rideId: String, time: String)
    suspend fun updateDestination(rideId: String, destination: GeoPoint)
    suspend fun updateDepartureTime(rideId: String, departureTime: String)
    suspend fun updateRideDate(rideId: String, date: String)
    suspend fun updateEncodedPolyline(rideId: String, encodedPolyline: String)

    suspend fun addPassengerToRide(rideId: String, passengerId: String): Boolean

    suspend fun calculateRideDepartureTime(
        ride: Ride,
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): DepartureCalculationResult

    suspend fun planRideFromUserInput(
        driverId: String,
        companyId: String,
        startAddress: String,
        destinationAddress: String,
        preferredArrivalTime: String,
        date: String,
        direction: RideDirection,
        availableSeats: Int,
        notes: String
    ): Boolean
}


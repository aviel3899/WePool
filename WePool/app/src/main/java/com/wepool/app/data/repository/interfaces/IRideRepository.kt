package com.wepool.app.data.repository.interfaces

import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.logic.DepartureCalculationResult
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.data.model.enums.ride.RideDirection


interface IRideRepository {
    suspend fun createRide(ride: Ride)
    suspend fun getRide(rideId: String): Ride?
    suspend fun getAllRides(): List<Ride>
    suspend fun getRidesByDriver(driverId: String): List<Ride>
    suspend fun getRidesByCompanyAndDirection(companyCode: String, direction: RideDirection): List<Ride>
    fun getPickupTimeForPassenger(ride: Ride, passengerId: String): String?
    fun getDropoffTimeForPassenger(ride: Ride, passengerId: String):String?
    suspend fun getPastRidesAsDriver(uid: String): List<Ride>
    suspend fun getPastRidesAsPassenger(uid: String): List<Ride>

    suspend fun updateRide(ride: Ride)
    suspend fun deleteRide(rideId: String)
    suspend fun updateAvailableSeats(rideId: String, seats: Int)
    suspend fun updateMaxDetourMinutes(rideId: String, maxDetour: Int)
    suspend fun updateArrivalTime(rideId: String, time: String)
    suspend fun updateDestination(rideId: String, destination: GeoPoint)
    suspend fun updateDepartureTime(rideId: String, departureTime: String)
    suspend fun updateRideDate(rideId: String, date: String)
    suspend fun updateEncodedPolyline(rideId: String, encodedPolyline: String)
    suspend fun updateRideIsActive(rideId: String, isActive: Boolean)

    suspend fun deactivateExpiredRides()

    suspend fun approvePassengerRequest(
        candidate: RideCandidate,
        requestId: String,
        passengerId: String
    ): Boolean
    suspend fun declineRideRequest(
        rideId: String,
        requestId: String
    ): Boolean
    suspend fun cancelRideRequest(
        rideId: String,
        requestId: String,
    ): Boolean
    suspend fun removePassengerFromRide(rideId: String, passengerId: String, rideCanceledForOnePassenger: Boolean = false)

    suspend fun calculateRideDepartureTime(
        ride: Ride,
        origin: GeoPoint,
        destination: GeoPoint,
        timeReference: String,
        date: String
    ): DepartureCalculationResult
    suspend fun planRideFromUserInput(
        driverId: String,
        companyCode: String,
        startAddress: LocationData,
        destinationAddress: LocationData,
        arrivalTime: String = "",
        departureTime: String = "",
        date: String,
        direction: RideDirection,
        availableSeats: Int,
        occupiedSeats: Int,
        maxDetourMinutes: Int,
        notes: String
    ): Boolean
    suspend fun adjustTimeAccordingToDirection(time: String, minutes: Int, direction: RideDirection):String
    suspend fun calculateTimeDifferenceInMinutes(startTime: String, endTime: String): Int
}
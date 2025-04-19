package com.wepool.app.data.repository.interfaces

import com.google.firebase.firestore.GeoPoint
import com.google.android.gms.maps.model.LatLng
import com.wepool.app.data.model.logic.DepartureCalculationResult
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.logic.RouteMatcher
import com.wepool.app.data.remote.IGoogleMapsService

interface IRideRepository {
    suspend fun createRide(ride: Ride)
    suspend fun getRide(rideId: String): Ride?
    suspend fun getAllRides(): List<Ride>
    suspend fun getRidesByDriver(driverId: String): List<Ride>

    suspend fun updateRide(ride: Ride)
    suspend fun deleteRide(rideId: String)
    suspend fun updateAvailableSeats(rideId: String, seats: Int)
    suspend fun updateMaxDetourMinutes(rideId: String, maxDetour: Int)
    suspend fun updatePreferredArrivalTime(rideId: String, time: String)
    suspend fun updateDestination(rideId: String, destination: GeoPoint)
    suspend fun updateDepartureTime(rideId: String, departureTime: String)
    suspend fun updateRideDate(rideId: String, date: String)
    suspend fun updateEncodedPolyline(rideId: String, encodedPolyline: String)

    suspend fun getRidesByCompanyAndDirection(companyId: String, direction: RideDirection): List<Ride>
    /*suspend fun getAvailableRidesForPassenger(
        companyId: String,
        direction: String,
        passengerArrivalTime: String,
        pickupPoint: LatLng,
        passengerId: String,
        mapsService: IGoogleMapsService,
        routeMatcher: RouteMatcher
    ): List<Ride>*/
    suspend fun addPassengerToRide(
        rideId: String,
        passengerId: String,
        pickupLocation: GeoPoint
    ): Boolean
    suspend fun approvePassengerRequest(
        candidate: RideCandidate,
        requestId: String,
        passengerId: String
    ): Boolean
    suspend fun declineAndDeleteRideRequest(
        rideId: String,
        requestId: String
    ): Boolean
    suspend fun cancelAndDeleteRideRequest(
        rideId: String,
        requestId: String,
    ): Boolean
    suspend fun removePassengerFromRide(rideId: String, passengerId: String)

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
        occupiedSeats: Int,
        maxDetourMinutes: Int,
        notes: String
    ): Boolean

    suspend fun subtractMinutesFromTime(time: String, minutes: Int): String
}


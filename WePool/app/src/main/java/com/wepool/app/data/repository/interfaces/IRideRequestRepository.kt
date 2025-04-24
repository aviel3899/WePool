package com.wepool.app.data.repository.interfaces

import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.data.model.enums.RequestStatus

interface IRideRequestRepository {
    suspend fun sendRequest(
        rideId: String,
        passengerId: String,
        pickupLocation: GeoPoint
    ): Boolean
    suspend fun updateRequestStatus(rideId: String, requestId: String, newStatus: RequestStatus): Boolean
    suspend fun getRequestsForRide(rideId: String): List<RideRequest>
    suspend fun getRequestsByPassenger(passengerId: String): List<RideRequest>
    suspend fun getRequestsByDriver(driverId: String): List<RideRequest>
    suspend fun deleteRequest(rideId: String, requestId: String)
}
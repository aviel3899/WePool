package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.data.model.enums.RequestStatus
import com.wepool.app.data.model.logic.DetourEvaluationResult

interface IRideRequestRepository {
    suspend fun sendRequest(
        rideId: String,
        passengerId: String,
        pickupLocation: LocationData,
        detourEvaluationResult: DetourEvaluationResult,
        notes: String = ""
    ): Boolean
    suspend fun updateRequestStatus(rideId: String, requestId: String, newStatus: RequestStatus): Boolean
    suspend fun updateDetourEvaluationResult(
        rideId: String,
        requestId: String,
        newDetour: DetourEvaluationResult
    )
    suspend fun getRequestsForRide(rideId: String): List<RideRequest>
    suspend fun getPendingRequestsByRide(rideId: String): List<RideRequest>
    suspend fun getRequestsByPassenger(passengerId: String): List<RideRequest>
    suspend fun getRequestsByDriver(driverId: String): List<RideRequest>
    suspend fun getPendingRequestsByDriver(driverId: String): List<RideRequest>
    suspend fun getPendingRequestsByPassenger(passengerId: String): List<RideRequest>
    suspend fun deleteRequest(rideId: String, requestId: String)
}
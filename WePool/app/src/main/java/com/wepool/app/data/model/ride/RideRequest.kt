package com.wepool.app.data.model.ride

import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.enums.RequestStatus
import com.wepool.app.data.model.logic.DetourEvaluationResult

data class RideRequest(
    val requestId: String = "",
    val rideId: String = "",
    val passengerId: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val pickupLocation: LocationData = LocationData(),
    val detourEvaluationResult: DetourEvaluationResult = DetourEvaluationResult(),
    val notes: String = ""
)
package com.wepool.app.data.model.ride

import com.wepool.app.data.model.logic.DetourEvaluationResult
import com.wepool.app.data.model.ride.Ride

data class RideCandidate(
    val ride: Ride,
    val detourEvaluationResult: DetourEvaluationResult
)
package com.wepool.app.data.model.logic

import com.wepool.app.data.model.ride.PickupStop

data class DetourEvaluationResult(
    val isAllowed: Boolean = false,
    val pickupLocation: PickupStop? = null,
    val encodedPolyline: String? = null,
    val addedDetourMinutes: Int = 0,
    val updatedReferenceTime: String? = null, //TO_WORK - departure time, TO_HOME - arrival time
)
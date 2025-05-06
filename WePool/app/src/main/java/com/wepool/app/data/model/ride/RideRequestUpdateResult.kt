package com.wepool.app.data.model.ride

data class RideRequestUpdateResult(
    val hasUpdates: Boolean,
    val requests: List<RideRequest>
)

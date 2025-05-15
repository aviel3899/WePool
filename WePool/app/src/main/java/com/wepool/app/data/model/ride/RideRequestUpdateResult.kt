package com.wepool.app.data.model.ride

data class RideRequestUpdateResult(
    val hasUpdates: Boolean,
    val newPendingRequestForDriver: List<RideRequest>,
    val newAcceptedRequestsAsPassenger: List<RideRequest>,
    val newDeclinedRequestsAsPassenger: List<RideRequest>,
    val totallRequests: List<RideRequest>
)

package com.wepool.app.data.model.logic

import com.google.firebase.firestore.GeoPoint

data class DetourEvaluationResult(
    val isAllowed: Boolean,
    val pickupLocation: GeoPoint? = null,
    val encodedPolyline: String? = null,
    val addedDetourMinutes: Int = 0,
    val updatedDepartureTime: String? = null
)

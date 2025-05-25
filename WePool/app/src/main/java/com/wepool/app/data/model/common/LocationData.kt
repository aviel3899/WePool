package com.wepool.app.data.model.common

import com.google.firebase.firestore.GeoPoint
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class LocationData(
    var name: String = "",
    @Contextual val geoPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val placeId: String = "",
    var note: String = ""
)
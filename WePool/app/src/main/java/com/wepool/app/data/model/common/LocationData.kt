package com.wepool.app.data.model.common

import com.google.firebase.firestore.GeoPoint

data class LocationData(
    var name: String = "",
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val placeId: String = ""
)
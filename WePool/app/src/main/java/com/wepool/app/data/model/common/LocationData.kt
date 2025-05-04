package com.wepool.app.data.model.common

import com.google.firebase.firestore.GeoPoint

data class LocationData(
    var name: String = "",                      // תיאור של המיקום
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0), // מיקום גאוגרפי
    val placeId: String = ""                     // מזהה ייחודי של המקום (מ-Google)
)
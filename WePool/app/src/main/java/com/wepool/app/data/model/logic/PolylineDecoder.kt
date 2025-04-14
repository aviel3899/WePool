package com.wepool.app.data.model.logic

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil

object PolylineDecoder {

    /**
     * מפענח מחרוזת Polyline מקודדת לרשימת נקודות LatLng
     */
    fun decode(encodedPolyline: String): List<LatLng> {
        return PolyUtil.decode(encodedPolyline)
    }

    /**
     * בודק אם מיקום מסוים (currentLocation) נמצא במרחק סביר מהמסלול
     */
    fun isOffRoute(
        currentLocation: LatLng,
        route: List<LatLng>,
        maxAllowedDeviationMeters: Double
    ): Boolean {
        val closestDistance = route.minOf { point ->
            com.google.maps.android.SphericalUtil.computeDistanceBetween(currentLocation, point)
        }
        return closestDistance > maxAllowedDeviationMeters
    }
}

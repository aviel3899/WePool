package com.wepool.app.data.remote

import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.logic.DurationAndRoute

interface IGoogleMapsService {

    /**
     * מחשב את משך זמן הנסיעה בדקות מ־origin ל־destination,
     * בהתבסס על arrivalTime בפורמט "HH:mm"
     */
    suspend fun getDurationAndRouteFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): DurationAndRoute
    suspend fun getCoordinatesFromAddress(address: String): LocationData?
    suspend fun getAddressSuggestions(input: String): List<String>
}
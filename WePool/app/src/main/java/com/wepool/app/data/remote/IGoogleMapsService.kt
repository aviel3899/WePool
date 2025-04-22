package com.wepool.app.data.remote

import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.logic.DurationAndRoute
import com.wepool.app.data.model.enums.RideDirection

interface IGoogleMapsService {

    suspend fun getDurationAndRouteFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        timeReference: String,
        date: String,
        direction: RideDirection
    ): DurationAndRoute
    suspend fun getDurationAndRouteWithWaypoints(
        origin: GeoPoint,
        waypoints: List<GeoPoint>,
        destination: GeoPoint,
        timeReference: String,
        date: String,
        direction: RideDirection
    ): DurationAndRoute
    suspend fun getCoordinatesFromAddress(address: String): LocationData?
    suspend fun getAddressSuggestions(input: String): List<String>
}
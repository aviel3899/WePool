package com.wepool.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class DirectionsResponse(
    val routes: List<Route>
)

@Serializable
data class Route(
    val overview_polyline: OverviewPolyline,
    val legs: List<Leg>
)

@Serializable
data class OverviewPolyline(
    val points: String
)

@Serializable
data class Leg(
    val duration: Duration
)

@Serializable
data class Duration(
    val value: Int // משך זמן ב־שניות
)
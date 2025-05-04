package com.wepool.app.data.remote

import android.util.Log
import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.remote.DirectionsResponse
import com.wepool.app.data.model.logic.DurationAndRoute
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.ride.PickupStop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.time.*
import java.time.format.DateTimeFormatter

class GoogleMapsService(
    private val apiKey: String // מוזן מתוך BuildConfig.MAPS_API_KEY
) : IGoogleMapsService {

    private val client = OkHttpClient()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    private companion object {
        const val GEOCODING_URL = "https://maps.googleapis.com/maps/api/geocode/json"
        const val AUTOCOMPLETE_URL = "https://maps.googleapis.com/maps/api/place/autocomplete/json"
        const val DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json"
    }

    override suspend fun getDurationAndRouteFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        timeReference: String,
        date: String,
        direction: RideDirection
    ): DurationAndRoute = withContext(Dispatchers.IO) {;

        val url = buildGoogleDirectionsUrl(
            origin = origin,
            destination = destination,
            timeReference = timeReference,
            date = date,
            apiKey = apiKey,
            rideDirection = direction
        )
        val response = fetchDirections(url)
        val route = extractRoute(response)
        val durationSeconds = getDurationSeconds(route, withWaypoints = false)

        return@withContext DurationAndRoute(
            durationMinutes = durationSeconds / 60,
            encodedPolyline = route.overview_polyline.points
        )
    }

    override suspend fun getDurationAndRouteWithWaypoints(
        origin: GeoPoint,
        //waypoints: List<GeoPoint>,
        waypoints: List<PickupStop>, //חדש
        destination: GeoPoint,
        timeReference: String,
        date: String,
        direction: RideDirection,
        passengerStop: PickupStop?
    ): DurationAndRoute = withContext(Dispatchers.IO) {

        val waypointGeoPoints = waypoints.map { it.location.geoPoint }

        val url = buildGoogleDirectionsUrl(
            origin = origin,
            destination = destination,
            timeReference = timeReference,
            date = date,
            apiKey = apiKey,
            //waypoints = waypoints,
            waypoints = waypointGeoPoints, //חדש
            rideDirection = direction,
        )
        val response = fetchDirections(url)
        val route = extractRoute(response)

        val legs = route.legs //חדש

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val baseTime = LocalTime.parse(timeReference, formatter)

        var accumulatedSeconds = 0L
        val pickupTimes = mutableMapOf<String, String>()
        val dropoffTimes = mutableMapOf<String, String>()

        for ((index, leg) in legs.withIndex()) {
            val pickupStop = waypoints.getOrNull(index)

            accumulatedSeconds += leg.duration.value.toLong()

            // pickupTime
            if (pickupStop != null && !pickupTimes.containsKey(pickupStop.passengerId)) {
                val pickupTime = baseTime.plusSeconds(accumulatedSeconds - leg.duration.value.toLong())
                    .format(formatter)
                pickupTimes[pickupStop.passengerId] = pickupTime
                Log.d(
                    "GoogleMapsService",
                    "🟢 זמן איסוף ל-${pickupStop.passengerId} (Index $index): $pickupTime"
                )
            }

            // dropoffTime
            if (pickupStop != null && !dropoffTimes.containsKey(pickupStop.passengerId)) {
                val dropoffTime = baseTime.plusSeconds(accumulatedSeconds).format(formatter)
                dropoffTimes[pickupStop.passengerId] = dropoffTime
                Log.d(
                    "GoogleMapsService",
                    "🔵 זמן הורדה ל-${pickupStop.passengerId} (Index $index): $dropoffTime"
                    )
                }
        }

        val durationSeconds = getDurationSeconds(route, withWaypoints = true)

        return@withContext DurationAndRoute(
            durationMinutes = durationSeconds / 60,
            encodedPolyline = route.overview_polyline.points,
            pickupTimes = pickupTimes,
            dropoffTimes = dropoffTimes
        )
    }

    // ממיר כתובת טקסטואלית ל־LocationData (קואורדינטות, כתובת מילוילת ומזהה מיוחד) באמצעות Google Geocoding API
    override suspend fun getCoordinatesFromAddress(address: String): LocationData? {
        val url = buildUrl(GEOCODING_URL, mapOf(
            "address" to address,
            "key" to apiKey
        ))

        val json = getJsonFromUrl(url) ?: return null
        val status = json.optString("status")
        if (status != "OK") {
            Log.e("Geocoding", "❌ סטטוס שגוי: $status")
            return null
        }

        val result = json.optJSONArray("results")?.optJSONObject(0) ?: return null
        val location = result
            .optJSONObject("geometry")
            ?.optJSONObject("location") ?: return null

        val lat = location.optDouble("lat", 0.0)
        val lng = location.optDouble("lng", 0.0)
        val formattedAddress = result.optString("formatted_address", address)
        val placeId = result.optString("place_id", "")

        return LocationData(
            name = formattedAddress,
            geoPoint = GeoPoint(lat, lng),
            placeId = placeId
        )
    }

    // מחזיר רשימת כתובות מוצעות לפי google autocomplete
    override suspend fun getAddressSuggestions(input: String): List<String> = withContext(Dispatchers.IO) {
        val url = buildUrl(AUTOCOMPLETE_URL, mapOf(
            "input" to input,
            "types" to "address",
            "language" to "iw",
            "components" to "country:il",
            "key" to apiKey
        ))

        val json = getJsonFromUrl(url) ?: return@withContext emptyList()
        val status = json.optString("status")
        if (status != "OK") {
            Log.w("Autocomplete", "⚠ סטטוס לא תקין: $status")
            return@withContext emptyList()
        }

        val predictions = json.optJSONArray("predictions") ?: return@withContext emptyList()

        return@withContext (0 until predictions.length()).mapNotNull { i ->
            predictions.optJSONObject(i)?.optString("description")
        }
    }

    // מבצע בקשת HTTP ומחזיר את התוצאה כ־JSONObject.
    private suspend fun getJsonFromUrl(url: String): JSONObject? = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            JSONObject(body)
        } catch (e: Exception) {
            Log.e("GoogleMapsService", "❌ שגיאה בבקשת HTTP: ${e.message}", e)
            null
        }
    }

    // בונה כתובת URL מלאה עם פרמטרים מקודדים, לפי בסיס ו־Map של פרמטרים.
    private fun buildUrl(base: String, params: Map<String, String>): String {
        val encodedParams = params.map { (key, value) ->
            val encodedValue = URLEncoder.encode(value, "UTF-8")
            "$key=$encodedValue"
        }.joinToString("&")
        return "$base?$encodedParams"
    }

    private fun buildGoogleDirectionsUrl(
        origin: GeoPoint,
        destination: GeoPoint,
        timeReference: String,
        date: String,
        apiKey: String,
        waypoints: List<GeoPoint>? = null,
        rideDirection: RideDirection
    ): String {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destinationStr = "${destination.latitude},${destination.longitude}"
        val epochSeconds = convertDateAndTimeToEpoch(timeReference, date)

        val params = mutableMapOf(
            "origin" to originStr,
            "destination" to destinationStr,
            "mode" to "driving",
            "key" to apiKey
        )

        if (rideDirection == RideDirection.TO_WORK) {
            params["arrival_time"] = epochSeconds.toString()
        } else {
            params["departure_time"] = epochSeconds.toString()
        }

        if (!waypoints.isNullOrEmpty()) {
            val waypointStr = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
            params["waypoints"] = waypointStr
        }

        return buildUrl(DIRECTIONS_URL, params)
    }

    // ממיר מחרוזת זמן בפורמט "HH:mm" ל־Epoch Time (שניות)
    private fun convertDateAndTimeToEpoch(time: String, date: String): Long {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
        val localDateTime = LocalDateTime.parse("$date $time", formatter)
        return localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond()
    }

    private suspend fun fetchDirections(url: String): DirectionsResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response from Google API")

        return@withContext jsonParser.decodeFromString<DirectionsResponse>(body)
    }

    private fun extractRoute(response: DirectionsResponse): Route {
        return response.routes.firstOrNull() ?: throw Exception("No route found")
    }

    private fun getDurationSeconds(route: Route, withWaypoints: Boolean): Int {
        return if (withWaypoints) {
            route.legs.sumOf { it.duration.value }
        } else {
            route.legs.firstOrNull()?.duration?.value
                ?: throw Exception("Duration not found")
            }
        }

}
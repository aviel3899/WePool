package com.wepool.app.data.remote

import android.util.Log
import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.remote.DirectionsResponse
import com.wepool.app.data.model.logic.DurationAndRoute
import com.wepool.app.data.model.common.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.time.*

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

    /**
     * מחשב את משך זמן הנסיעה (בדקות) בין origin ל-destination
     * לפי זמן הגעה רצוי (arrivalTime בפורמט HH:mm)
     */
    override suspend fun getDurationAndRouteFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): DurationAndRoute = withContext(Dispatchers.IO) {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destinationStr = "${destination.latitude},${destination.longitude}"
        val arrivalEpochSeconds = convertTimeToEpoch(arrivalTime)

        val url = buildUrl(DIRECTIONS_URL, mapOf(
            "origin" to originStr,
            "destination" to destinationStr,
            "arrival_time" to arrivalEpochSeconds.toString(),
            "mode" to "driving",
            "key" to apiKey
        ))

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response from Google API")

        val directions = jsonParser.decodeFromString<DirectionsResponse>(body)
        val route = directions.routes.firstOrNull()
            ?: throw Exception("No route found")

        val durationSeconds = route.legs.firstOrNull()?.duration?.value
            ?: throw Exception("Duration not found")

        return@withContext DurationAndRoute(
            durationMinutes = durationSeconds / 60,
            encodedPolyline = route.overview_polyline.points
        )
    }

    override suspend fun getDurationAndRouteWithWaypoints(
        origin: GeoPoint,
        waypoints: List<GeoPoint>,
        destination: GeoPoint,
        arrivalTime: String
    ): DurationAndRoute = withContext(Dispatchers.IO) {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destinationStr = "${destination.latitude},${destination.longitude}"
        val arrivalEpochSeconds = convertTimeToEpoch(arrivalTime)

        // נבנה את מחרוזת נקודות הביניים
        val waypointStr = waypoints.joinToString("|") {
            "${it.latitude},${it.longitude}"
        }

        val url = buildUrl(DIRECTIONS_URL, mapOf(
            "origin" to originStr,
            "destination" to destinationStr,
            "waypoints" to waypointStr,
            "arrival_time" to arrivalEpochSeconds.toString(),
            "mode" to "driving",
            "key" to apiKey
        ))

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response from Google API")

        val directions = jsonParser.decodeFromString<DirectionsResponse>(body)
        val route = directions.routes.firstOrNull()
            ?: throw Exception("No route found")

        val durationSeconds = route.legs.sumOf { it.duration.value }

        return@withContext DurationAndRoute(
            durationMinutes = durationSeconds / 60,
            encodedPolyline = route.overview_polyline.points
        )
    }

    /**
     * ממיר מחרוזת זמן בפורמט "HH:mm" ל־Epoch Time (שניות)
     */
    private fun convertTimeToEpoch(time: String): Long {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        val localTime = LocalTime.parse(time, formatter)
        val today = LocalDate.now()
        val dateTime = LocalDateTime.of(today, localTime)
        val zone = ZoneId.systemDefault()
        return dateTime.atZone(zone).toEpochSecond()
    }

    /**
     * ממיר כתובת טקסטואלית ל־LocationData (קואורדינטות, כתובת מילוילת ומזהה מיוחד) באמצעות Google Geocoding API.
     */
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

    /**
     * מחזירה רשימת כתובות מוצעות לפי קלט טקסטואלי.
     */
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
            Log.w("Autocomplete", "⚠️ סטטוס לא תקין: $status")
            return@withContext emptyList()
        }

        val predictions = json.optJSONArray("predictions") ?: return@withContext emptyList()

        return@withContext (0 until predictions.length()).mapNotNull { i ->
            predictions.optJSONObject(i)?.optString("description")
        }
    }

    /**
     * מבצע בקשת HTTP ומחזיר את התוצאה כ־JSONObject.
     */
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

    /**
     * בונה כתובת URL מלאה עם פרמטרים מקודדים, לפי בסיס ו־Map של פרמטרים.
     */
    private fun buildUrl(base: String, params: Map<String, String>): String {
        val encodedParams = params.map { (key, value) ->
            val encodedValue = URLEncoder.encode(value, "UTF-8")
            "$key=$encodedValue"
        }.joinToString("&")
        return "$base?$encodedParams"
    }

}



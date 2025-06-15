package com.wepool.app.data.remote

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.wepool.app.data.model.common.LocationData
import androidx.core.net.toUri

object RideNavigationStarter {

    fun startNavigationWithWaypoints(
        context: Context,
        origin: LocationData,
        stops: List<LocationData>,
        destination: LocationData
    ) {
        val baseUri = StringBuilder().apply {
            append("https://www.google.com/maps/dir/?api=1")
            append("&origin=${origin.geoPoint.latitude},${origin.geoPoint.longitude}")
            append("&destination=${destination.geoPoint.latitude},${destination.geoPoint.longitude}")
            append("&travelmode=driving")
        }

        if (stops.isNotEmpty()) {
            val waypointsParam = stops.joinToString("|") {
                "${it.geoPoint.latitude},${it.geoPoint.longitude}"
            }
            baseUri.append("&waypoints=$waypointsParam")
        }

        val uri: Uri = baseUri.toString().toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.i(
                    "RideNavigationStarter",
                    "✅ Google Maps נפתח עם ${if (stops.isNotEmpty()) "עצירות במסלול" else "ללא עצירות"}"
                )
            } else {
                Log.e("RideNavigationStarter", "❌ Google Maps לא מותקן או לא נמצא במכשיר")
            }
        } catch (e: ActivityNotFoundException) {
            Log.e("RideNavigationStarter", "❌ לא נמצאה אפליקציה מתאימה: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("RideNavigationStarter", "❌ שגיאה כללית בפתיחת ניווט: ${e.message}", e)
        }
    }
}

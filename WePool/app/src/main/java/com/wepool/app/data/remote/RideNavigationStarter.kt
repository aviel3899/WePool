package com.wepool.app.data.remote

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.wepool.app.data.model.common.LocationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object RideNavigationStarter {
    fun startNavigationToLocation(context: Context, location: LocationData) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            val uri = Uri.parse("google.navigation:q=${location.geoPoint.latitude},${location.geoPoint.longitude}&mode=d")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                val resolved = intent.resolveActivity(context.packageManager)
                if (resolved != null) {
                    context.startActivity(intent)
                    Log.d("Navigation", "✅ Google Maps נפתח ל-${location.name}")
                } else {
                    Log.e("Navigation", "❌ לא נמצא אפליקציה מתאימה לפתיחת Intent ל-${location.name}")
                }
            } catch (e: ActivityNotFoundException) {
                Log.e("Navigation", "❌ לא ניתן להפעיל את Google Maps: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("Navigation", "❌ שגיאה כללית בפתיחת ניווט: ${e.message}", e)
            }
        }
    }
}

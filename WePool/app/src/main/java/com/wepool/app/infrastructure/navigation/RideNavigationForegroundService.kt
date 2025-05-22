package com.wepool.app.infrastructure.navigation

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.os.HandlerCompat
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.remote.RideNavigationStarter
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.notifications.NotificationHelper
import kotlinx.coroutines.*
import com.wepool.app.notifications.NotificationService

class RideNavigationForegroundService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_NAVIGATION"
        const val ACTION_STOP = "ACTION_STOP_NAVIGATION"
        const val EXTRA_RIDE_ID = "rideId"
        const val NOTIF_ID = 999
        private const val ARRIVAL_DISTANCE_METERS = 40
        private const val APPROACHING_DISTANCE_METERS = 200
    }

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private var lastKnownLocation: Location? = null

    private var navigationManager: RideNavigationManager? = null
    private lateinit var checkRunnable: Runnable
    private val handler = HandlerCompat.createAsync(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val notifiedApproaching = mutableSetOf<String>() // 🔔 מי שכבר קיבל התראה "הנהג בדרך"
    private val notifiedNextInLine = mutableSetOf<String>()   // 🔔 מי שכבר קיבל "אתה הבא בתור"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val rideId = intent.getStringExtra(EXTRA_RIDE_ID) ?: return START_NOT_STICKY
                Log.i("RideNavService", "▶️ התחלת ניווט ל-rideId=$rideId")

                serviceScope.launch {
                    val manager = RideNavigationManager(this@RideNavigationForegroundService, rideId)
                    val initialized = withContext(Dispatchers.IO) { manager.initialize() }

                    if (initialized) {
                        navigationManager = manager

                        NotificationService.notifyPassengersRideStarted(rideId)

                        val (origin, waypoints, destination) = manager.getAllStops()
                        RideNavigationStarter.startNavigationWithWaypoints(
                            context = this@RideNavigationForegroundService,
                            origin = origin,
                            stops = waypoints,
                            destination = destination
                        )

                        startForeground(
                            NOTIF_ID,
                            NotificationHelper.buildNavigationNotification(
                                context = this@RideNavigationForegroundService,
                                contentText = "🚗 ניווט התחיל",
                                rideId = rideId,
                                screen = "rideStarted"
                            )
                        )
                        handler.post(checkRunnable)
                    } else {
                        Log.e("RideNavService", "❌ נכשל בטעינת מסלול, השירות נעצר")
                        stopSelf()
                    }
                }
            }

            ACTION_STOP -> {
                Log.i("RideNavService", "🛑 קיבלנו בקשה לעצור ניווט")
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lastKnownLocation = location
            }
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                5f,
                locationListener
            )
        } catch (e: SecurityException) {
            Log.e("RideNavService", "⚠️ אין הרשאת מיקום", e)
            stopSelf()
        }

        checkRunnable = Runnable {
            serviceScope.launch(Dispatchers.IO) {
                val manager = navigationManager ?: return@launch
                val currentStop = manager.getCurrentStop()
                val currentPassengerId = manager.getCurrentPassengerId()

                if (currentStop != null && currentPassengerId != null && lastKnownLocation != null) {
                    val stopLocation = Location("").apply {
                        latitude = currentStop.geoPoint.latitude
                        longitude = currentStop.geoPoint.longitude
                    }

                    val distance = lastKnownLocation!!.distanceTo(stopLocation)

                    // 🔔 שליחת התראה כשהנהג מתקרב ל-500 מטר
                    if (distance < APPROACHING_DISTANCE_METERS && !notifiedApproaching.contains(currentPassengerId)) {
                        Log.i("RideNavService", "🔔 הנהג מתקרב לנוסע $currentPassengerId (מרחק $distance מטר)")
                        NotificationService.sendNotificationToPassengerWhenDriverArriving(
                            passengerId = currentPassengerId,
                            isPickup = true,
                            rideId = manager.getRideId()
                        )
                        notifiedApproaching.add(currentPassengerId)
                    }

                    // ✅ בדיקת הגעה בפועל
                    val arrived = distance < ARRIVAL_DISTANCE_METERS && lastKnownLocation!!.accuracy <= 50f
                    if (arrived) {
                        Log.i("RideNavService", "✅ הגעת לתחנה ${currentStop.name}")

                        RepositoryProvider.providePassengerRepository()
                            .getPassenger(currentPassengerId)
                            ?.user?.name
                            ?.let { name ->
                                if (manager.isCurrentStopPickup()) {
                                    NotificationService.sendNotificationToPassengerWhenDriverArriving(
                                        passengerId = currentPassengerId,
                                        isPickup = true,
                                        rideId = manager.getRideId()
                                    )
                                } else {
                                    Log.w("RideNavService", "⚠️ לא ניתן לזהות סוג תחנה עבור $name")
                                }
                            }

                        manager.moveToNextStop()

                        // 🆕 שליחת התראה לנוסע הבא בתור
                        val nextPassengerId = manager.getCurrentPassengerId()
                        if (nextPassengerId != null && !notifiedNextInLine.contains(nextPassengerId)) {
                            Log.i("RideNavService", "🔔 שולח התראה לנוסע הבא בתור: $nextPassengerId")
                            NotificationService.sendNotificationToPassengerWhenDriverArriving(
                                passengerId = nextPassengerId,
                                isPickup = true,
                                rideId = manager.getRideId()
                            )
                            notifiedNextInLine.add(nextPassengerId)
                        }

                        if (manager.hasReachedFinalDestination()) {
                            Log.i("RideNavService", "🏁 הגעת ליעד הסופי, עצירת השירות")
                            notifyNavigationEnded()
                            stopSelf()
                            return@launch
                        }
                    }
                }

                handler.postDelayed(checkRunnable, 5000L)
            }
        }

    }


    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        serviceScope.cancel()

        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: SecurityException) {
            Log.e("RideNavService", "⚠️ כשל בהסרת LocationListener", e)
        }

        Log.i("RideNavService", "🛑 שירות הניווט נעצר")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasReachedLocation(current: Location, target: LocationData): Boolean {
        if (current.accuracy > 30f) { // לדוגמה, מתחת ל־30 מטר דיוק בלבד
            Log.w("RideNavService", "⚠️ מיקום נוכחי לא מדויק מספיק: ${current.accuracy}m")
            return false
        }

        val targetLocation = Location("").apply {
            latitude = target.geoPoint.latitude
            longitude = target.geoPoint.longitude
        }

        val distance = current.distanceTo(targetLocation)
        Log.d("RideNavService", "📍 Distance to stop: $distance m")

        return distance < ARRIVAL_DISTANCE_METERS
    }


    private fun notifyNavigationEnded() {
        val intent = Intent("com.wepool.app.NAVIGATION_ENDED").apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
}
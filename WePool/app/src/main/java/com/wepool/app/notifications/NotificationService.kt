package com.wepool.app.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.wepool.app.data.model.ride.Ride
import kotlinx.coroutines.tasks.await

object NotificationService {

    private const val TAG = "NotificationService"

    fun notifyDriver(
        driverId: String,
        rideId: String,
        title: String,
        body: String,
        screen: String
    ) {
        if (driverId.isBlank()) {
            Log.e(TAG, "❌ driverId ריק או לא חוקי - לא נשלחה התראה")
            return
        }

        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(driverId).get()
            .addOnSuccessListener { snapshot ->
                val token = snapshot.getString("fcmToken")
                if (token.isNullOrBlank()) {
                    Log.w(TAG, "⚠️ לא נמצא FCM token לנהג $driverId")
                    return@addOnSuccessListener
                }

                Log.d(TAG, "📦 שולח התראה לנהג $driverId עם token: $token")

                sendNotificationToTokens(
                    tokens = listOf(token),
                    rideId = rideId,
                    title = title,
                    body = body,
                    screen = screen
                )
            }
            .addOnFailureListener {
                Log.e(TAG, "❌ שגיאה בשליפת הנהג $driverId", it)
            }
    }

    fun notifyPassengers(
        passengerIds: List<String>,
        rideId: String,
        title: String,
        body: String,
        screen: String
    ) {
        if (passengerIds.isEmpty()) {
            Log.w(TAG, "⚠️ רשימת נוסעים ריקה - לא נשלחה התראה")
            return
        }

        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users")
            .whereIn(FieldPath.documentId(), passengerIds)
            .get()
            .addOnSuccessListener { snapshot ->
                val tokens = snapshot.documents.mapNotNull { doc ->
                    doc.getString("fcmToken")
                }

                if (tokens.isEmpty()) {
                    Log.w(TAG, "⚠️ לא נמצאו טוקנים תקפים לנוסעים")
                    return@addOnSuccessListener
                }

                Log.d(TAG, "📦 שולח התראה ל-${tokens.size} נוסעים: $tokens")

                sendNotificationToTokens(
                    tokens = tokens,
                    rideId = rideId,
                    title = title,
                    body = body,
                    screen = screen
                )
            }
            .addOnFailureListener {
                Log.e(TAG, "❌ שגיאה בשליפת מסמכי נוסעים", it)
            }
    }

    fun notifyPassengersRideStarted(rideId: String) {
        if (rideId.isBlank()) {
            Log.e(TAG, "❌ rideId ריק או לא חוקי - לא נשלחה התראה")
            return
        }

        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("rides").document(rideId).get()
            .addOnSuccessListener { rideDoc ->
                if (!rideDoc.exists()) {
                    Log.e(TAG, "❌ לא נמצאה נסיעה עם rideId: $rideId")
                    return@addOnSuccessListener
                }

                val rawPickupStops = rideDoc.get("pickupStops")
                val pickupStops = if (rawPickupStops is List<*>) {
                    rawPickupStops.filterIsInstance<Map<String, Any>>()
                } else {
                    emptyList()
                }

                val passengerIds = pickupStops.mapNotNull { it["passengerId"] as? String }

                if (passengerIds.isEmpty()) {
                    Log.w(TAG, "⚠️ אין נוסעים לשלוח להם התראה")
                    return@addOnSuccessListener
                }

                notifyPassengers(
                    passengerIds = passengerIds,
                    rideId = rideId,
                    title = "🚗 הנהג בדרך",
                    body = "הנסיעה שלך מתחילה עכשיו!",
                    screen = "rideStarted"
                )
            }
            .addOnFailureListener {
                Log.e(TAG, "❌ שגיאה בשליפת rideId", it)
            }
    }

    suspend fun sendNotificationToPassengerWhenDriverArriving(
        passengerId: String,
        isPickup: Boolean,
        rideId: String
    ) {
        try {
            val userSnapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(passengerId)
                .get()
                .await()

            val name = userSnapshot.getString("name") ?: "הנוסע"

            val title = if (isPickup) "🚗 הנהג בדרך אליך!" else "🛬 הורדת נוסע"
            val body = if (isPickup) "הנהג התחיל בנסיעה לעברך, $name" else "הגעת לנקודת ההורדה של $name"

            val screen = if (isPickup) "pickup" else "dropoff"

            fetchTokensAndSendNotifications(
                userIds = listOf(passengerId),
                rideId = rideId,
                title = title,
                body = body,
                screen = screen
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ שגיאה כללית בשליחת FCM לנוסע $passengerId", e)
        }
    }

    fun notifyRideCancelled(rideId: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("rides").document(rideId).get()
            .addOnSuccessListener { rideDoc ->
                if (!rideDoc.exists()) {
                    Log.e(TAG, "❌ Ride $rideId לא נמצאה")
                    return@addOnSuccessListener
                }

                val ride = rideDoc.toObject(Ride::class.java)
                if (ride == null) {
                    Log.e(TAG, "❌ שגיאה בהמרת המסמך ל-Ride")
                    return@addOnSuccessListener
                }

                val passengerIds = ride.passengers
                if (passengerIds.isEmpty()) {
                    Log.w(TAG, "⚠️ אין נוסעים לשלוח להם התראה על ביטול")
                    return@addOnSuccessListener
                }

                val title = "🚫 הנסיעה בוטלה"
                val body = "הנסיעה שתוכננה בוטלה. מצטערים על חוסר הנוחות."

                val screen = "rideCancelled"

                fetchTokensAndSendNotifications(
                    userIds = passengerIds,
                    rideId = rideId,
                    title = title,
                    body = body,
                    screen = screen
                )
            }
            .addOnFailureListener {
                Log.e(TAG, "❌ שגיאה בשליפת מסמך נסיעה $rideId", it)
            }
    }

    private fun fetchTokensAndSendNotifications(
        userIds: List<String>,
        rideId: String,
        title: String,
        body: String,
        screen: String
    ) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users")
            .whereIn(FieldPath.documentId(), userIds)
            .get()
            .addOnSuccessListener { snapshot ->
                val validTokens = snapshot.documents
                    .mapNotNull { document -> document.getString("fcmToken") }

                if (validTokens.isEmpty()) {
                    Log.w(TAG, "⚠️ לא נמצאו FCM tokens תקפים לנוסעים")
                    return@addOnSuccessListener
                }

                Log.d(TAG, "📦 נמצאו ${validTokens.size} tokens: $validTokens")

                sendNotificationToTokens(validTokens, rideId, title, body, screen)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "❌ שגיאה בשליפת מסמכי נוסעים", error)
            }
    }

    private fun sendNotificationToTokens(
        tokens: List<String>,
        rideId: String,
        title: String,
        body: String,
        screen: String
    ) {
        val functions = FirebaseFunctions.getInstance()

        val payload = hashMapOf(
            "tokens" to ArrayList(tokens),
            "title" to title,
            "body" to body,
            "rideId" to rideId,
            "screen" to screen
        )
        Log.d(TAG, "📤 Payload: $payload")

        functions
            .getHttpsCallable("sendNotificationToTokens")
            .call(payload)
            .addOnSuccessListener {
                Log.d(TAG, "✅ ההתראות נשלחו בהצלחה")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "❌ שגיאה בשליחת ההתראות", error)
            }
    }
}

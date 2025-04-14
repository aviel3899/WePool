package com.wepool.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.Driver
//import com.wepool.app.data.model.users.PassengerRideInfo
import com.wepool.app.data.remote.IGoogleMapsService
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DriverRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val mapsService: IGoogleMapsService
) {

    // יוצרת או מעדכנת את נתוני הנהג במסמך info
    suspend fun saveDriver(driver: Driver) {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .set(driver)
            .await()
    }

    // מחזירה את אובייקט הנהג הנוכחי
    suspend fun getDriver(): Driver? {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .get()
            .await()
        return snapshot.toObject(Driver::class.java)
    }

    // מוחקת את מסמך הנהג
    suspend fun deleteDriver() {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .delete()
            .await()
    }

    // עדכון כללי - תשתית לעדכון שדות נפרדים
    private suspend fun updateDriverFields(updates: Map<String, Any?>) {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .update(updates)
            .await()
    }


    // פונקציות לעדכון שדה אחד בודד
    suspend fun updateAvailableSeats(seats: Int) {
        updateDriverFields(mapOf("availableSeats" to seats))
    }

    suspend fun updateVehicleDetails(vehicleDetails: String) {
        updateDriverFields(mapOf("vehicleDetails" to vehicleDetails))
    }

    suspend fun updateMaxDetourMinutes(maxDetour: Int) {
        updateDriverFields(mapOf("maxDetourMinutes" to maxDetour))
    }

    suspend fun updatePreferredArrivalTime(time: String) {
        updateDriverFields(mapOf("preferredArrivalTime" to time))
    }

    suspend fun updateCalculatedDepartureTime(time: String) {
        updateDriverFields(mapOf("calculatedDepartureTime" to time))
    }

    suspend fun updateDirection(direction: String) {
        updateDriverFields(mapOf("direction" to direction))
    }

    suspend fun updateDestination(destination: GeoPoint) {
        updateDriverFields(mapOf("destination" to destination))
    }

    suspend fun updateActiveRideId(rideId: String?) {
        updateDriverFields(mapOf("activeRideId" to rideId))
    }

    // מחזיר את השעה הנוכחית בפורמט טקסטואלי
    private fun getCurrentTimestamp(): String {
        return LocalTime.now().toString()
    }

    // חישוב זמן יציאה מתוך זמן הגעה
    suspend fun calculateDepartureTimeFromArrival(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): String {
        val durationMinutes = getDurationFromGoogleApi(origin, destination, arrivalTime)
        val totalMinutesBefore = durationMinutes + 10
        return subtractMinutesFromTime(arrivalTime, totalMinutesBefore)
    }

    private suspend fun getDurationFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): Int {
        return mapsService.getDurationFromGoogleApi(origin, destination, arrivalTime)
    }

    private fun subtractMinutesFromTime(time: String, minutes: Int): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm") // הגדרת פורמט זמן
        val localTime = LocalTime.parse(time, formatter) // הופך את המחרוזת של time לאובייקט מסוג localTime
        val adjustedTime = localTime.minusMinutes(minutes.toLong()) // מפחית את כמות הדקות מהשעה המקורית
        return adjustedTime.format(formatter) // מחזיר את השעה החדשה כמחרוזת בפורמט "HH:mm"
    }

    // עדכון נסיעה קיימת עם נוסע חדש תוך בדיקות
    /*suspend fun updateRideWithPassenger(rideId: String, passengerInfo: PassengerRideInfo): Boolean {
        val rideRef = firestore.collection("rides").document(rideId)

        return firestore.runTransaction { tx ->
            val snapshot = tx.get(rideRef)
            val ride = snapshot.toObject(Ride::class.java) ?: return@runTransaction false

            if (ride.availableSeats <= ride.passengers.size) return@runTransaction false
            if (passengerInfo.detourMinutes > ride.maxDetourMinutes) return@runTransaction false

            val updatedPassengers = ride.passengers + passengerInfo
            val updatedRide = ride.copy(passengers = updatedPassengers)

            tx.set(rideRef, updatedRide)
            true
        }.await()
    }*/
}

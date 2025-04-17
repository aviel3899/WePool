package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.logic.DepartureCalculationResult
import com.wepool.app.data.model.logic.DurationAndRoute
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.remote.IGoogleMapsService
import com.wepool.app.data.repository.interfaces.IRideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class RideRepository(
    private val firestore: FirebaseFirestore,
    private val mapsService: IGoogleMapsService
) : IRideRepository {

    private val rideCollection = firestore.collection("rides")

    override suspend fun createRide(ride: Ride) {
        rideCollection.document(ride.rideId).set(ride).await()
    }

    // מחזיר נסיעה מסוימת לפי rideId
    override suspend fun getRide(rideId: String): Ride? = withContext(Dispatchers.IO) {
        val snapshot = rideCollection.document(rideId).get().await()
        return@withContext snapshot.toObject(Ride::class.java)
    }

    override suspend fun getAllRides(): List<Ride> = withContext(Dispatchers.IO) {
        val snapshot = rideCollection.get().await()  // שליפת כל המסמכים מתוך האוסף "rides"
        return@withContext snapshot.documents.mapNotNull { it.toObject(Ride::class.java) }  // המרת המסמכים למודל Ride
    }

    // מחזיר את כל הנסיעות של נהג
    override suspend fun getRidesByDriver(driverId: String): List<Ride> = withContext(Dispatchers.IO) {
        val snapshot = rideCollection
            .whereEqualTo("driverId", driverId)
            .get()
            .await()
        return@withContext snapshot.toObjects(Ride::class.java)
    }

    // מחזיר את כל הנסיעות האפשריות לנוסע
    override suspend fun getAvailableRidesForPassenger(
        companyId: String,
        direction: String,
        preferredArrivalTime: String
    ): List<Ride> = withContext(Dispatchers.IO) {
        val snapshot = rideCollection
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("direction", direction)
            .whereEqualTo("preferredArrivalTime", preferredArrivalTime)
            .get()
            .await()
        return@withContext snapshot.toObjects(Ride::class.java)
    }

    override suspend fun addPassengerToRide(rideId: String, passengerId: String): Boolean = withContext(Dispatchers.IO) {
        val docRef = rideCollection.document(rideId)
        firestore.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            val ride = snapshot.toObject(Ride::class.java) ?: return@runTransaction false

            if (ride.passengers.contains(passengerId)) return@runTransaction true
            if (ride.passengers.size >= ride.availableSeats) return@runTransaction false

            val updatedPassengers = ride.passengers + passengerId
            val updatedRide = ride.copy(passengers = updatedPassengers)
            tx.set(docRef, updatedRide)
            return@runTransaction true
        }.await()
    }

    // מעדכן נסיעה קיימת
    override suspend fun updateRide(ride: Ride)  {
        rideCollection.document(ride.rideId).set(ride).await()
    }

    // מוחק נסיעה
    override suspend fun deleteRide(rideId: String)  {
        rideCollection.document(rideId).delete().await()
    }

    override suspend fun updateAvailableSeats(rideId: String, seats: Int) {
        firestore.collection("rides")
            .document(rideId)
            .update("availableSeats", seats)
            .await()
    }

    override suspend fun updateMaxDetourMinutes(rideId: String, maxDetour: Int) {
        firestore.collection("rides")
            .document(rideId)
            .update("maxDetourMinutes", maxDetour)
            .await()
    }

    override suspend fun updatePreferredArrivalTime(rideId: String, time: String) {
        firestore.collection("rides")
            .document(rideId)
            .update("preferredArrivalTime", time)
            .await()
    }

    override suspend fun updateDestination(rideId: String, destination: GeoPoint) {
        firestore.collection("rides")
            .document(rideId)
            .update("destination", destination)
            .await()
    }

    override suspend fun updateDepartureTime(rideId: String, departureTime: String) {
        firestore.collection("rides")
            .document(rideId)
            .update("departureTime", departureTime)
            .await()
    }

    override suspend fun updateRideDate(rideId: String, date: String) {
        firestore.collection("rides")
            .document(rideId)
            .update("date", date)
            .await()
    }

    override suspend fun updateEncodedPolyline(rideId: String, encodedPolyline: String) {
        firestore.collection("rides")
            .document(rideId)
            .update("encodedPolyline", encodedPolyline)
            .await()
    }

    override suspend fun planRideFromUserInput(
        driverId: String,
        companyId: String,
        startAddress: String,
        destinationAddress: String,
        preferredArrivalTime: String,
        date: String,
        direction: RideDirection,
        availableSeats: Int,
        notes: String
    ): Boolean = withContext(Dispatchers.IO) {
        val existingRides = getRidesByDriver(driverId)
        val alreadyExists = existingRides.any {
            it.direction == direction &&
                    it.preferredArrivalTime == preferredArrivalTime &&
                    it.date == date
        }

        if (alreadyExists) {
            Log.d("RideLogic", "⚠️ נסיעה כזו כבר קיימת — לא נוצרת חדשה")
            return@withContext false
        }

        val startLocation = mapsService.getCoordinatesFromAddress(startAddress)?.geoPoint
        val destination = mapsService.getCoordinatesFromAddress(destinationAddress)?.geoPoint

        if (startLocation == null || destination == null) {
            Log.e("RideLogic", "❌ כתובת לא תקינה - התחלה: $startAddress, יעד: $destinationAddress")
            return@withContext false
        }

        val rideId = firestore.collection("rides").document().id

        val baseRide = Ride(
            rideId = rideId,
            driverId = driverId,
            companyId = companyId,
            startLocation = startLocation,
            destination = destination,
            direction = direction,
            preferredArrivalTime = preferredArrivalTime,
            date = date,
            availableSeats = availableSeats,
            passengers = emptyList(),
            maxDetourMinutes = 10,
            encodedPolyline = "",
            notes = notes
        )

        val result = try {
            calculateRideDepartureTime(
                ride = baseRide,
                origin = startLocation,
                destination = destination,
                arrivalTime = preferredArrivalTime
            )
        } catch (e: Exception) {
            Log.e("RideLogic", "❌ שגיאה בחישוב זמן היציאה: ${e.message}", e)
            return@withContext false
        }

        val finalizedRide = baseRide.copy(
            departureTime = result.departureTime,
            encodedPolyline = result.encodedPolyline
        )

        createRide(finalizedRide)

        Log.d("RideLogic", "✅ נסיעה נוצרה בהצלחה: ${finalizedRide.rideId}")
        return@withContext true
    }


    override suspend fun calculateRideDepartureTime(
        ride: Ride,
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): DepartureCalculationResult {
        val result = getDurationAndRouteFromGoogleApi(origin, destination, arrivalTime)
        val totalMinutesBefore = result.durationMinutes + ride.maxDetourMinutes
        val departureTime = subtractMinutesFromTime(arrivalTime, totalMinutesBefore)
        return DepartureCalculationResult(departureTime, result.encodedPolyline)
    }

    private suspend fun getDurationAndRouteFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): DurationAndRoute {
        return mapsService.getDurationAndRouteFromGoogleApi(origin, destination, arrivalTime)
    }

    private fun subtractMinutesFromTime(time: String, minutes: Int): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm") // הגדרת פורמט זמן
        val localTime = LocalTime.parse(time, formatter) // הופך את המחרוזת של time לאובייקט מסוג localTime
        val adjustedTime = localTime.minusMinutes(minutes.toLong()) // מפחית את כמות הדקות מהשעה המקורית
        return adjustedTime.format(formatter) // מחזיר את השעה החדשה כמחרוזת בפורמט "HH:mm"
    }
}

package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.android.gms.maps.model.LatLng
import com.wepool.app.data.model.enums.RequestStatus
import com.wepool.app.data.model.logic.DepartureCalculationResult
import com.wepool.app.data.model.logic.DurationAndRoute
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.remote.IGoogleMapsService
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.wepool.app.data.repository.interfaces.IRideRequestRepository
import com.wepool.app.data.repository.interfaces.IPassengerRepository
import com.wepool.app.data.model.logic.RouteMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class RideRepository(
    private val firestore: FirebaseFirestore,
    private val mapsService: IGoogleMapsService,
    private val rideRequestRepository: IRideRequestRepository
) : IRideRepository {

    private val rideCollection = firestore.collection("rides")

    //private val maxArrivalTimeDifferenceMinutes = 30L
   // private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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

    // בודק אם הנוסע מגיע אחרי הנהג או באותו זמן, ולא יותר מ־X דקות אחריו
   /* private fun isArrivalTimeValid(driverTime: String, passengerTime: String): Boolean {
        val driver = LocalTime.parse(driverTime, timeFormatter)
        val passenger = LocalTime.parse(passengerTime, timeFormatter)

        return !passenger.isBefore(driver) &&
                passenger.isBefore(driver.plusMinutes(maxArrivalTimeDifferenceMinutes))
    }*/

    override suspend fun getRidesByCompanyAndDirection(companyId: String, direction: RideDirection): List<Ride> {
        return rideCollection
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("direction", direction)
            .get()
            .await()
            .toObjects(Ride::class.java)
    }

    // מחזיר את כל הנסיעות האפשריות לנוסע
   /* override suspend fun getAvailableRidesForPassenger(
        companyId: String,
        direction: String,
        passengerArrivalTime: String,
        pickupPoint: LatLng,
        passengerId: String,
        mapsService: IGoogleMapsService,
        routeMatcher: RouteMatcher
    ): List<Ride> = withContext(Dispatchers.IO) {

        val allRides = rideCollection
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("direction", direction)
            .get()
            .await()
            .toObjects(Ride::class.java)

        val filtered = allRides.filter { ride ->
            val timeOK = isArrivalTimeValid(ride.preferredArrivalTime!!, passengerArrivalTime)
            val seatOK = ride.occupiedSeats < ride.availableSeats
            val notAlreadyJoined = !ride.passengers.contains(passengerId)

            val detourOK = routeMatcher.isPickupWithinDriverDetour(
                encodedPolyline = ride.encodedPolyline,
                pickupPoint = pickupPoint,
                maxAllowedDetourMinutes = ride.maxDetourMinutes.toDouble(),
                arrivalTime = ride.preferredArrivalTime,
                mapsService = mapsService
            )

            if (!timeOK) Log.d("RideFilter", "❌ זמן לא מתאים (rideId=${ride.rideId})")
            if (!seatOK) Log.d("RideFilter", "❌ אין מקום פנוי (rideId=${ride.rideId})")
            if (!notAlreadyJoined) Log.d("RideFilter", "❌ הנוסע כבר חלק מהנסיעה (rideId=${ride.rideId})")
            if (!detourOK) Log.d("RideFilter", "❌ סטייה לא חוקית (rideId=${ride.rideId})")

            timeOK && seatOK && detourOK && notAlreadyJoined
        }

        Log.d("RideFilter", "✅ נמצאו ${filtered.size} נסיעות מתאימות לנוסע")
        return@withContext filtered
    }*/

    override suspend fun addPassengerToRide(
        rideId: String,
        passengerId: String,
        pickupLocation: GeoPoint
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val success = rideRequestRepository.sendRequest(rideId, passengerId, pickupLocation)
            if (success) {
                Log.d("RideRequest", "📤 בקשת הצטרפות נשלחה (rideId: $rideId, passengerId: $passengerId)")
            } else {
                Log.w("RideRequest", "⚠️ הבקשה לא נשלחה")
            }
            return@withContext success
        } catch (e: Exception) {
            Log.e("RideRequest", "❌ שגיאה בשליחת בקשה לנסיעה: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun approvePassengerRequest(
        candidate: RideCandidate,
        requestId: String,
        passengerId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val ride = candidate.ride
        val detour = candidate.detourEvaluationResult

        val rideRef = rideCollection.document(ride.rideId)
        val requestRef = rideRef.collection("requests").document(requestId)

        try {
            val success = firestore.runTransaction { tx ->
                val rideSnapshot = tx.get(rideRef)
                val currentRide =
                    rideSnapshot.toObject(Ride::class.java) ?: return@runTransaction false

                val requestSnapshot = tx.get(requestRef)
                val request =
                    requestSnapshot.toObject(RideRequest::class.java) ?: return@runTransaction false

                val pickupLocation = request.pickupLocation ?: return@runTransaction false

                val updatedRide = currentRide.copy(
                    passengers = currentRide.passengers + passengerId,
                    occupiedSeats = currentRide.occupiedSeats + 1,
                    currentDetourMinutes = currentRide.currentDetourMinutes + detour.addedDetourMinutes,
                    encodedPolyline = detour.encodedPolyline ?: currentRide.encodedPolyline,
                    pickupStops = currentRide.pickupStops + pickupLocation,
                    departureTime = detour.updatedDepartureTime ?: currentRide.departureTime
                )

                tx.set(rideRef, updatedRide)

                Log.d(
                    "RideJoin",
                    "✅ הנוסע נוסף לנסיעה (rideId=${ride.rideId}, passengerId=$passengerId)"
                )
                true
            }.await()

            if (success) {
                rideRequestRepository.updateRequestStatus(
                    ride.rideId,
                    requestId,
                    RequestStatus.ACCEPTED
                )
                Log.d("RideRequest", "📥 סטטוס הבקשה עודכן ל-ACCEPTED (requestId=$requestId)")
            }

            return@withContext success

        } catch (e: Exception) {
            Log.e("RideJoin", "❌ שגיאה באישור הבקשה: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun declineAndDeleteRideRequest(
        rideId: String,
        requestId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            //  עדכון סטטוס הבקשה ל-DECLINED
            val statusUpdated = rideRequestRepository.updateRequestStatus(
                rideId = rideId,
                requestId = requestId,
                newStatus = RequestStatus.DECLINED
            )

            if (!statusUpdated) {
                Log.w("RideRequest", "⚠️ לא ניתן לעדכן סטטוס ל-DECLINED (requestId=$requestId)")
                return@withContext false
            }

            //  מחיקת הבקשה ע"י הפונקציה מ-RideRequestRepository
            rideRequestRepository.deleteRequest(rideId, requestId)

            Log.d("RideRequest", "🗑️ בקשה נדחתה ונמחקה בהצלחה (requestId=$requestId)")
            return@withContext true

        } catch (e: Exception) {
            Log.e("RideRequest", "❌ שגיאה בדחיית/מחיקת בקשה: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun cancelAndDeleteRideRequest(
        rideId: String,
        requestId: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            //  עדכון סטטוס ל-CANCELLED
            val statusUpdated = rideRequestRepository.updateRequestStatus(
                rideId = rideId,
                requestId = requestId,
                newStatus = RequestStatus.CANCELLED
            )

            if (!statusUpdated) {
                Log.w("RideRequest", "⚠️ לא ניתן לעדכן סטטוס ל-CANCELLED (requestId=$requestId)")
                return@withContext false
            }

            //  מחיקת הבקשה
            rideRequestRepository.deleteRequest(rideId, requestId)

            Log.d("RideRequest", "🚫 הבקשה בוטלה ונמחקה בהצלחה (requestId=$requestId)")
            return@withContext true

        } catch (e: Exception) {
            Log.e("RideRequest", "❌ שגיאה בביטול/מחיקת בקשה: ${e.message}", e)
            return@withContext false
        }
    }


    override suspend fun removePassengerFromRide(
        rideId: String,
        passengerId: String
    ): Unit = withContext(Dispatchers.IO) {
        val docRef = rideCollection.document(rideId)

        val originalRide = firestore.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            val ride = snapshot.toObject(Ride::class.java)

            if (ride == null || !ride.passengers.contains(passengerId)) {
                Log.w("RideLeave", "⚠️ הנוסע $passengerId לא נמצא בנסיעה (rideId: $rideId)")
                return@runTransaction null
            }

            return@runTransaction ride
        }.await()

        if (originalRide == null) return@withContext

        try {
            val updatedRide = buildRideAfterPassengerRemoved(originalRide, passengerId)
            rideCollection.document(rideId).set(updatedRide).await()

            deletePassengerRequestsForRide(rideId, passengerId)

            Log.d("RideLeave", "✅ הנוסע $passengerId הוסר והמסלול עודכן (rideId=$rideId)")
        } catch (e: Exception) {
            Log.e("RideLeave", "❌ שגיאה בהסרת נוסע: ${e.message}", e)
        }
    }

    private suspend fun buildRideAfterPassengerRemoved(
        ride: Ride,
        passengerId: String
    ): Ride {
        val updatedPassengers = ride.passengers - passengerId
        val updatedOccupiedSeats = maxOf(ride.occupiedSeats - 1, 0)

        val pickupToRemove = getPickupLocationFromRequest(ride.rideId, passengerId)
        val updatedPickupStops = ride.pickupStops.filterNot { it == pickupToRemove }

        val route = if (updatedPickupStops.isNotEmpty()) {
            mapsService.getDurationAndRouteWithWaypoints(
                origin = ride.startLocation,
                waypoints = updatedPickupStops,
                destination = ride.destination,
                arrivalTime = ride.preferredArrivalTime ?: ""
            )
        } else {
            mapsService.getDurationAndRouteFromGoogleApi(
                origin = ride.startLocation,
                destination = ride.destination,
                arrivalTime = ride.preferredArrivalTime ?: ""
            )
        }

        val updatedDeparture = subtractMinutesFromTime(
            time = ride.preferredArrivalTime ?: "",
            minutes = route.durationMinutes
        )

        return ride.copy(
            passengers = updatedPassengers,
            occupiedSeats = updatedOccupiedSeats,
            pickupStops = updatedPickupStops,
            encodedPolyline = route.encodedPolyline,
            currentDetourMinutes = route.durationMinutes,
            departureTime = updatedDeparture
        )
    }

    private suspend fun getPickupLocationFromRequest(
        rideId: String,
        passengerId: String
    ): GeoPoint? {
        return try {
            val snapshot = rideCollection.document(rideId)
                .collection("requests")
                .whereEqualTo("passengerId", passengerId)
                .get()
                .await()

            snapshot.documents.firstOrNull()
                ?.toObject(RideRequest::class.java)
                ?.pickupLocation
        } catch (e: Exception) {
            Log.e("RideLeave", "❌ שגיאה בשליפת נקודת איסוף: ${e.message}", e)
            null
        }
    }

    private suspend fun deletePassengerRequestsForRide(rideId: String, passengerId: String) {
        try {
            val requests = rideRequestRepository.getRequestsByPassenger(passengerId)
            val relevant = requests.filter { it.rideId == rideId }

            for (request in relevant) {
                rideRequestRepository.deleteRequest(rideId, request.requestId)
                Log.d("RideLeave", "🗑️ בקשה ${request.requestId} נמחקה")
            }

            Log.d("RideLeave", "✅ כל בקשות ההצטרפות של $passengerId לנסיעה $rideId נמחקו")
        } catch (e: Exception) {
            Log.e("RideLeave", "❌ שגיאה במחיקת בקשות נוסע: ${e.message}", e)
        }
    }

    // מעדכן נסיעה קיימת
    override suspend fun updateRide(ride: Ride)  {
        rideCollection.document(ride.rideId).set(ride).await()
    }

    // מוחק נסיעה
    override suspend fun deleteRide(rideId: String) = withContext(Dispatchers.IO) {
        try {
            val rideSnapshot = rideCollection.document(rideId).get().await()
            val ride = rideSnapshot.toObject(Ride::class.java)

            if (ride == null) {
                Log.w("RideDelete", "⚠️ לא נמצאה נסיעה למחיקה (rideId=$rideId)")
                return@withContext
            }

            if (ride.passengers.isNotEmpty()) {
                Log.d("RideDelete", "🔁 התחלת הסרת ${ride.passengers.size} נוסעים מהנסיעה")

                for (passengerId in ride.passengers) {
                    removePassengerFromRide(rideId, passengerId)
                }

                Log.d("RideDelete", "✅ כל הנוסעים הוסרו מהנסיעה (rideId=$rideId)")
            }

            rideCollection.document(rideId).delete().await()
            Log.d("RideDelete", "🗑️ הנסיעה נמחקה בהצלחה (rideId=$rideId)")

        } catch (e: Exception) {
            Log.e("RideDelete", "❌ שגיאה במחיקת נסיעה: ${e.message}", e)
        }
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
        occupiedSeats: Int,
        maxDetourMinutes: Int,
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

        // הפחתת 10 דקות מזמן ההגעה שהמשתמש הגדיר
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val adjustedArrivalTime = LocalTime.parse(preferredArrivalTime, formatter).minusMinutes(maxDetourMinutes.toLong())
        val adjustedArrivalTimeStr = adjustedArrivalTime.format(formatter)

        val rideId = firestore.collection("rides").document().id

        val baseRide = Ride(
            rideId = rideId,
            driverId = driverId,
            companyId = companyId,
            startLocation = startLocation,
            destination = destination,
            direction = direction,
            preferredArrivalTime = adjustedArrivalTimeStr,
            date = date,
            availableSeats = availableSeats,
            occupiedSeats = occupiedSeats,
            passengers = emptyList(),
            pickupStops = emptyList(),
            maxDetourMinutes = maxDetourMinutes,
            currentDetourMinutes = 0,
            encodedPolyline = "",
            notes = notes
        )

        val result = try {
            calculateRideDepartureTime(
                ride = baseRide,
                origin = startLocation,
                destination = destination,
                arrivalTime = adjustedArrivalTimeStr
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
        //val totalMinutesBefore = result.durationMinutes + ride.maxDetourMinutes
        //val departureTime = subtractMinutesFromTime(arrivalTime, totalMinutesBefore)
        val departureTime = subtractMinutesFromTime(arrivalTime, result.durationMinutes)
        return DepartureCalculationResult(departureTime, result.encodedPolyline)
    }

    private suspend fun getDurationAndRouteFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): DurationAndRoute {
        return mapsService.getDurationAndRouteFromGoogleApi(origin, destination, arrivalTime)
    }

    override suspend fun subtractMinutesFromTime(time: String, minutes: Int): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm") // הגדרת פורמט זמן
        val localTime = LocalTime.parse(time, formatter) // הופך את המחרוזת של time לאובייקט מסוג localTime
        val adjustedTime = localTime.minusMinutes(minutes.toLong()) // מפחית את כמות הדקות מהשעה המקורית
        return adjustedTime.format(formatter) // מחזיר את השעה החדשה כמחרוזת בפורמט "HH:mm"
    }
}

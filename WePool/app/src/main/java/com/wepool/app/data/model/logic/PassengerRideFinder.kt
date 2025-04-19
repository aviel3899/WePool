package com.wepool.app.data.model.logic

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.data.remote.IGoogleMapsService
import com.wepool.app.data.repository.interfaces.IRideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.Duration

class PassengerRideFinder(
    private val rideRepository: IRideRepository,
    private val mapsService: IGoogleMapsService,
    private val routeMatcher: RouteMatcher
) {

    private val maxArrivalTimeDifferenceMinutes = 30L
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /*suspend fun getAvailableRidesForPassenger(
        companyId: String,
        direction: RideDirection,
        passengerArrivalTime: String,
        passengerDate: String,
        pickupPoint: LatLng,
        passengerId: String
    ): List<Ride> = withContext(Dispatchers.IO) {

        val allRides = rideRepository.getRidesByCompanyAndDirection(companyId, direction)

        val filtered = allRides.filter { ride ->
            val dateOK = ride.date == passengerDate
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

            if (!dateOK) Log.d("RideFilter", "❌ תאריך לא מתאים (rideId=${ride.rideId})")
            if (!timeOK) Log.d("RideFilter", "❌ זמן לא מתאים (rideId=${ride.rideId})")
            if (!seatOK) Log.d("RideFilter", "❌ אין מקום פנוי (rideId=${ride.rideId})")
            if (!notAlreadyJoined) Log.d("RideFilter", "❌ הנוסע כבר חלק מהנסיעה (rideId=${ride.rideId})")
            if (!detourOK) Log.d("RideFilter", "❌ סטייה לא חוקית (rideId=${ride.rideId})")

            dateOK && timeOK && seatOK && detourOK && notAlreadyJoined
        }

        Log.d("RideFilter", "✅ נמצאו ${filtered.size} נסיעות מתאימות לנוסע")
        return@withContext filtered
    }*/

    suspend fun getAvailableRidesForPassenger(
        companyId: String,
        direction: RideDirection,
        passengerArrivalTime: String,
        passengerDate: String,
        pickupPoint: LatLng,
        passengerId: String,
        rideRepository: IRideRepository,
    ): List<RideCandidate> = withContext(Dispatchers.IO) {

        val allRides = rideRepository.getRidesByCompanyAndDirection(companyId, direction)

        val candidates = allRides.mapNotNull { ride ->
            val dateOK = ride.date == passengerDate
            val timeOK = isArrivalTimeValid(ride.preferredArrivalTime!!, passengerArrivalTime)
            val seatOK = ride.occupiedSeats < ride.availableSeats
            val notAlreadyJoined = !ride.passengers.contains(passengerId)

            val currentRouteTimeMinutes = try {
                val departure = LocalTime.parse(ride.departureTime, timeFormatter)
                val arrival = LocalTime.parse(ride.preferredArrivalTime, timeFormatter)
                Duration.between(departure, arrival).toMinutes().toInt()
            } catch (e: Exception) {
                Log.e("RideFilter", "❌ שגיאה בחישוב זמן מסלול: ${e.message}")
                return@mapNotNull null
            }

            val evaluation = routeMatcher.evaluatePickupDetour(
                encodedPolyline = ride.encodedPolyline,
                pickupPoint = pickupPoint,
                maxAllowedDetourMinutes = ride.maxDetourMinutes,
                currentDetourMinutes = ride.currentDetourMinutes,
                currentRouteTimeMinutes = currentRouteTimeMinutes,
                arrivalTime = ride.preferredArrivalTime,
                mapsService = mapsService,
                startLocation = ride.startLocation,
                destination = ride.destination,
                currentPickupStops = ride.pickupStops,
                rideRepository = rideRepository
            )

            val detourOK = evaluation.isAllowed

            if (!dateOK || !timeOK || !seatOK || !notAlreadyJoined || !detourOK) {
                return@mapNotNull null
            }

            return@mapNotNull RideCandidate(
                ride = ride,
                detourEvaluationResult = evaluation
            )
        }

        Log.d("RideFilter", "✅ נמצאו ${candidates.size} נסיעות מתאימות לנוסע")
        return@withContext candidates
    }

    // בודק אם הנוסע מגיע אחרי הנהג או באותו זמן, ולא יותר מ־X דקות אחריו
    private fun isArrivalTimeValid(driverTime: String, passengerTime: String): Boolean {
        val driver = LocalTime.parse(driverTime, timeFormatter)
        val passenger = LocalTime.parse(passengerTime, timeFormatter)

        return !passenger.isBefore(driver) &&
                passenger.isBefore(driver.plusMinutes(maxArrivalTimeDifferenceMinutes))
    }
}
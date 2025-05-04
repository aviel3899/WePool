package com.wepool.app.data.model.logic

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.ride.PickupStop
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
    private val maxDepartureTimeDifferenceMinutes = 15L
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun getAvailableRidesForPassenger(
        companyId: String,
        direction: RideDirection,
        passengerArrivalTime: String = "",
        passengerDepartureTime: String = "",
        passengerDate: String,
        //pickupPoint: LatLng,
        pickupPoint: PickupStop,
        //passengerId: String, // לא צריך כי אני מעביר בתוך PickupStop את passengerId
        rideRepository: IRideRepository,
    ): List<RideCandidate> = withContext(Dispatchers.IO) {

        val allRides = rideRepository.getRidesByCompanyAndDirection(companyId, direction)

        val candidates = allRides.mapNotNull { ride ->
            val dateOK = ride.date == passengerDate
            //val timeOK = isArrivalTimeValid(ride.arrivalTime!!, passengerArrivalTime)
            val timeOK: Boolean
            if(direction == RideDirection.TO_WORK){
                timeOK = isRideTimeValid(ride.arrivalTime!!, ride.maxDetourMinutes, passengerArrivalTime, direction)
            }
            else{
                timeOK = isRideTimeValid(ride.departureTime!!, ride.maxDetourMinutes, passengerDepartureTime, direction)
            }
            val seatOK = ride.occupiedSeats < ride.availableSeats
            //val notAlreadyJoined = !ride.passengers.contains(passengerId)
            val notAlreadyJoined = !ride.passengers.contains(pickupPoint.passengerId)

            val currentRouteTimeMinutes = try {
                val departure = LocalTime.parse(ride.departureTime, timeFormatter)
                val arrival = LocalTime.parse(ride.arrivalTime, timeFormatter)
                Duration.between(departure, arrival).toMinutes().toInt()
            } catch (e: Exception) {
                Log.e("RideFilter", "❌ שגיאה בחישוב זמן מסלול: ${e.message}")
                return@mapNotNull null
            }

            val timeReference = if(ride.direction == RideDirection.TO_WORK){
                ride.arrivalTime!!
            }
            else{
                ride.departureTime!!
            }

            val evaluation = routeMatcher.evaluatePickupDetour(
                encodedPolyline = ride.encodedPolyline,
                pickupPoint = pickupPoint,
                maxAllowedDetourMinutes = ride.maxDetourMinutes,
                currentDetourMinutes = ride.currentDetourMinutes,
                currentRouteTimeMinutes = currentRouteTimeMinutes,
                timeReference = timeReference,
                date = ride.date,
                mapsService = mapsService,
                startLocation = ride.startLocation,
                destination = ride.destination,
                currentPickupStops = ride.pickupStops,
                rideRepository = rideRepository,
                rideDirection = direction
            )

            val detourOK = evaluation.isAllowed

            if (!dateOK || !timeOK || !seatOK || !notAlreadyJoined || !detourOK) {
                Log.d("RideFilter", """ ❌ נסיעה לא מתאימה:
        - תאריך תואם? $dateOK
        - זמן תואם? $timeOK
        - יש מקומות פנויים? $seatOK
        - נוסע עדיין לא הצטרף? $notAlreadyJoined
        - סטייה מותרת? $detourOK
        """.trimIndent()
                )
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

    private fun isRideTimeValid(
        driverTime: String,
        detourMinutes: Int,
        passengerTime: String,
        direction: RideDirection
    ): Boolean {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        var driver = LocalTime.parse(driverTime, formatter)
        driver = driver.plusMinutes(detourMinutes.toLong())
        val passenger = LocalTime.parse(passengerTime, formatter)

        return if (direction == RideDirection.TO_WORK) { //זמן ההגעה של הנוסע לא אחרי זמן ההגעה של הנהג ולא X דקות לםניו
            !passenger.isAfter(driver.plusMinutes(maxArrivalTimeDifferenceMinutes)) &&
                    !passenger.isBefore(driver)
        } else { //זמן היציאה של הנוסע לא אחרי זמן היציאה של הנהג ולא X דקות לפניו
            !passenger.isBefore(driver.minusMinutes(maxDepartureTimeDifferenceMinutes)) &&
                    !passenger.isAfter(driver)
            }
        }
}
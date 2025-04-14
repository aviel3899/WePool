package com.wepool.app.data.repository.interfaces

import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.Driver
//import com.wepool.app.data.model.users.PassengerRideInfo

interface IDriverRepository {

    // שליפת פרטי הנהג לפי UID
    suspend fun getDriverData(uid: String): Driver?

    // שמירת כל פרטי הנהג למסמך info
    suspend fun saveDriverData(driver: Driver)

    // מחיקת מסמך הנהג (driverData/info)
    suspend fun deleteDriver(uid: String)

    // יצירת נסיעה חדשה על בסיס פרטי הנהג
    suspend fun createRideForDriver(driver: Driver): String

    // שליפת נסיעה עתידית קרובה של הנהג
    suspend fun getUpcomingRide(driverUid: String): Ride?

    // שליפת היסטוריית נסיעות של הנהג
    suspend fun getRideHistory(driverUid: String): List<Ride>

    // חישוב זמן יציאה לפי יעד, מוצא, זמן הגעה, כולל הפחתת 10 דקות
    suspend fun calculateDepartureTimeFromArrival(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): String

    // קריאה ל-Google API לחישוב זמן נסיעה בדקות
    suspend fun getDurationFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): Int

    // עדכון רשימת נוסעים לנסיעה תוך בדיקה של סטייה וזמינות כיסאות
    /*suspend fun updateRideWithPassenger(
        rideId: String,
        passengerInfo: PassengerRideInfo
    ): Boolean*/

    // פונקציות לעדכון שדות ספציפיים בפרטי הנהג
    suspend fun updateAvailableSeats(seats: Int)
    suspend fun updateVehicleDetails(vehicleDetails: String)
    suspend fun updateMaxDetourMinutes(maxDetour: Int)
    suspend fun updatePreferredArrivalTime(time: String)
    suspend fun updateCalculatedDepartureTime(time: String)
    suspend fun updateDirection(direction: String)
    suspend fun updateDestination(destination: GeoPoint)
    suspend fun updateActiveRideId(rideId: String?)
}


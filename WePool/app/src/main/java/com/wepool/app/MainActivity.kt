package com.wepool.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.wepool.app.ui.theme.WePoolTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.wepool.app.BuildConfig
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.enums.UserRole
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.users.Driver
import com.wepool.app.data.model.users.Passenger
import com.wepool.app.data.model.logic.PolylineDecoder
import com.wepool.app.data.model.logic.RouteMatcher
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.wepool.app.data.repository.interfaces.IUserRepository
import com.wepool.app.data.repository.interfaces.IDriverRepository
import com.wepool.app.data.repository.interfaces.IPassengerRepository
import com.wepool.app.data.repository.interfaces.IRideRequestRepository
import com.wepool.app.data.model.logic.PassengerRideFinder
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.data.remote.GoogleMapsService
import com.google.android.gms.maps.model.LatLng
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wepool.app.ui.screens.LoginScreen
import com.wepool.app.ui.screens.SignUpScreen
import com.wepool.app.ui.screens.RoleSelectionScreen
import com.wepool.app.ui.screens.IntermediateScreen
import com.wepool.app.ui.screens.RideHistoryMenuScreen
import com.wepool.app.ui.screens.UpdateDetailsScreen
import com.wepool.app.ui.screens.CreateRideDirectionScreen
import com.wepool.app.ui.screens.DriverCarDetailsScreen
import com.wepool.app.ui.screens.HomeboundRideCreationScreen
import com.wepool.app.ui.screens.WorkboundRideCreationScreen

class MainActivity : ComponentActivity() {

    init {
        RepositoryProvider.initialize(BuildConfig.MAPS_API_KEY)
    }

    private val authRepository = RepositoryProvider.provideAuthRepository()
    private val userRepository: IUserRepository = RepositoryProvider.provideUserRepository()
    private val driverRepository: IDriverRepository = RepositoryProvider.provideDriverRepository()
    private val passengerRepository: IPassengerRepository =
        RepositoryProvider.providePassengerRepository()
    private val rideRepository: IRideRepository = RepositoryProvider.provideRideRepository()
    private val rideRequestRepository: IRideRequestRepository =
        RepositoryProvider.provideRideRequestRepository()
    private val mapsService = RepositoryProvider.mapsService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = "test@wepool.com"
        val password = "test1234"

        // 📝 ניסיון הרשמה
        /*lifecycleScope.launch {
            val user = User(
                uid = "",
                name = "Test Driver",
                email = email,
                phoneNumber = "050-7137453",
                companyId = "company123",
                isBanned = false,
                roles = listOf(UserRole.DRIVER.name)
            )

            val signUpResult = authRepository.signUpWithEmailAndPassword(
                email = user.email,
                password = password,
                user = user,
                userRepository = userRepository
            )

            signUpResult.onSuccess { uid ->
                Log.d("MainActivity", "🎉 הרשמה הצליחה | UID: $uid")
            }.onFailure { error ->
                Log.e("MainActivity", "❌ שגיאה בהרשמה: ${error.message}")
            }
        }*/

        // 🔐 ניסיון התחברות
        /*lifecycleScope.launch {
            val loginResult = authRepository.loginWithEmailAndPassword(
                email = email,
                password = password
            )

            loginResult.onSuccess { uid ->
                Log.d("MainActivity", "✅ התחברות הצליחה | UID: $uid")

                //deleteSingleRide()
                val user = userRepository.getUser(uid)!!
                val driver = createTestDriver(user)
                driverRepository.saveDriver(driver)
                val driverId = driver.user.uid
                val passenger = createTestPassenger(user)
                passengerRepository.savePassengerData(uid, passenger)
                //createTestRideToWork(driverId)
                //createTestRideToHome(driverId)
                //rideRepository.deleteRide("zYqTfv7rwLtJSWPfGSuQ")

                val passengerId = passenger.user.uid
                //testPassengerJoinFlow(passengerId)
                //rideRequestRepository.deleteRequest("2Zsm2OvpUAoOWn4IptUP", "xpbsbeN5iB0IfHloMjvJ" )
                //rideRepository.removePassengerFromRide("ISvOBxjfk173GTj27j81", passengerId)

            }.onFailure { error ->
                Log.e("MainActivity", "❌ שגיאה בהתחברות: ${error.message}")
            }
        }*/

        // UI
        enableEdgeToEdge()
        setContent {
            WePoolTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(navController = navController)
                        }
                        composable("signup") {
                            SignUpScreen(navController = navController)
                        }
                        composable("roleSelection/{uid}") { backStackEntry ->
                            val uid =
                                backStackEntry.arguments?.getString("uid") ?: return@composable
                            RoleSelectionScreen(navController = navController, uid = uid)
                        }
                        composable("intermediate/{uid}") { backStackEntry ->
                            val uid =
                                backStackEntry.arguments?.getString("uid") ?: return@composable
                            IntermediateScreen(navController = navController, uid = uid)
                        }
                        composable("rideHistoryMenu/{uid}") { backStackEntry ->
                            val uid =
                                backStackEntry.arguments?.getString("uid") ?: return@composable
                            RideHistoryMenuScreen(navController = navController, uid = uid)
                        }

                        composable("rideHistoryDriver/{uid}") {
                            Text("📘 Ride History as a Driver (Placeholder)")
                        }

                        composable("rideHistoryPassenger/{uid}") {
                            Text("📕 Ride History as a Passenger (Placeholder)")
                        }

                        composable("rideHistoryCombined/{uid}") {
                            Text("📗 Ride History Combined (Placeholder)")
                        }
                        composable("updateDetails/{uid}") { backStackEntry ->
                            val uid =
                                backStackEntry.arguments?.getString("uid") ?: return@composable
                            UpdateDetailsScreen(navController = navController, uid = uid)
                        }
                        composable("createRideDirection/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            CreateRideDirectionScreen(navController = navController, uid = uid)
                        }
                        composable("homeboundRide/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            HomeboundRideCreationScreen(navController = navController, uid = uid)
                        }
                        composable("workboundRide/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            WorkboundRideCreationScreen(navController = navController, uid = uid)
                        }
                        composable("driverCarDetails/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            DriverCarDetailsScreen(uid = uid, navController = navController)
                            }
                    }
                }
            }
        }
    }


    private fun createTestUser(uid: String): User {
        return User(
            uid = uid,
            name = "Test Driver",
            email = "test@wepool.com",
            phoneNumber = "050-7137453",
            companyCode = "company123",
            isBanned = false,
            roles = listOf(UserRole.DRIVER.name)
        )
    }

    private fun createTestDriver(user: User): Driver {
        return Driver(
            user = user,
            vehicleDetails = "Toyota Corolla 2022",
            activeRideId = ""
        )
    }

    private fun createTestPassenger(user: User): Passenger {
        return Passenger(
            user = user,
        )
    }

    private fun createTestRideToWork(uid: String) {
        lifecycleScope.launch {
            val success = rideRepository.planRideFromUserInput(
                driverId = uid,
                companyId = "company123",
                startAddress = "נחום 20 נתניה",
                destinationAddress = "הנביאים 34 כפר סבא",
                arrivalTime = "09:00",
                date = "31-05-2025",
                direction = RideDirection.TO_WORK,
                availableSeats = 3,
                occupiedSeats  = 0,
                maxDetourMinutes= 10,
                notes = "נסיעה לבדיקה לכיוון העבודה"
            )
            if (success) {
                Log.d("TestRide", "✅ נסיעת בדיקה נוצרה בהצלחה")
            } else {
                Log.w("TestRide", "❌ הנסיעה לא נוצרה")
            }
        }
    }

    private fun createTestRideToHome(uid: String) {
        lifecycleScope.launch {
            val success = rideRepository.planRideFromUserInput(
                driverId = uid,
                companyId = "company123",
                startAddress = "הנביאים 34 כפר סבא",
                destinationAddress = "נחום 20 נתניה",
                departureTime = "09:00",
                date = "31-05-2025",
                direction = RideDirection.TO_HOME,
                availableSeats = 3,
                occupiedSeats  = 0,
                maxDetourMinutes= 10,
                notes = "נסיעה לבדיקה לכיוון הביתה"
            )
            if (success) {
                Log.d("TestRide", "✅ נסיעת בדיקה נוצרה בהצלחה")
            } else {
                Log.w("TestRide", "❌ הנסיעה לא נוצרה")
            }
        }
    }

    private fun handleAddressToLocationData(address: String) {
        lifecycleScope.launch {
            val locationData = mapsService.getCoordinatesFromAddress(address)

            if (locationData != null) {
                Log.d("WePoolLocation", "📍 כתובת: ${locationData.name}")
                Log.d("WePoolLocation", "📌 קואורדינטות: ${locationData.geoPoint.latitude}, ${locationData.geoPoint.longitude}")
                Log.d("WePoolLocation", "🆔 placeId: ${locationData.placeId}")

                // ❗ לשימוש עתידי – שמירה ב־Firestore
                // val locationRepo = RepositoryProvider.provideLocationDataRepository()
                // locationRepo.addLocationToUser(user.uid, locationData)
            } else {
                Log.e("WePoolLocation", "❌ לא נמצאה כתובת מתאימה: $address")
            }
        }
    }

    private fun logAddressSuggestions(addressInput: String) {
        lifecycleScope.launch {
            val suggestions = mapsService.getAddressSuggestions(addressInput)

            if (suggestions.isNotEmpty()) {
                Log.d("WePoolAutocomplete", "📝 נמצאו ${suggestions.size} הצעות לכתובת '$addressInput'")
                suggestions.forEachIndexed { index, suggestion ->
                    Log.d("WePoolAutocomplete", "🔹 ${index + 1}. $suggestion")
                }
            } else {
                Log.w("WePoolAutocomplete", "⚠️ לא נמצאו הצעות לכתובת '$addressInput'")
            }
        }
    }

    private fun testPassengerJoinFlow(passengerId: String) {
        lifecycleScope.launch {
            try {
                Log.d("TestFlow", "🚀 התחלת תהליך הצטרפות נוסע לנסיעה (passengerId=$passengerId)")

                // 1. שליפת נתוני הנוסע
                val passengerData = passengerRepository.getPassenger(passengerId)
                if (passengerData == null) {
                    Log.w("TestFlow", "❌ לא נמצאו נתונים עבור הנוסע")
                    return@launch
                }

                Log.d("TestFlow", "📦 נתוני הנוסע נשלפו בהצלחה")

                // 2. המרת כתובת ל־LocationData
                //val locationData = mapsService.getCoordinatesFromAddress("הנביאים 1 נתניה")
                val locationData = mapsService.getCoordinatesFromAddress("המחקר 3 נתניה")
                if (locationData == null) {
                    Log.w("TestFlow", "❌ לא ניתן להמיר את הכתובת לקואורדינטות")
                    return@launch
                }

                val pickupPoint = LatLng(locationData.geoPoint.latitude, locationData.geoPoint.longitude)
                val arrivalTime = "09:00"
                val departureTime = "09:00"
                val date = "31-05-2025"

                val passengerRideFinder = PassengerRideFinder(
                    rideRepository = rideRepository,
                    mapsService = mapsService,
                    routeMatcher = RouteMatcher
                )

                // 4. שליפת מועמדי נסיעה מתאימים
                val rideCandidates = passengerRideFinder.getAvailableRidesForPassenger(
                    companyId = "company123",
                    direction = RideDirection.TO_HOME,
                    //direction = RideDirection.TO_WORK,
                    passengerArrivalTime = arrivalTime,
                    passengerDepartureTime = departureTime,
                    passengerDate = date,
                    pickupPoint = pickupPoint,
                    passengerId = passengerId,
                    rideRepository = rideRepository
                )

                if (rideCandidates.isEmpty()) {
                    Log.d("TestFlow", "❌ לא נמצאו נסיעות זמינות לנוסע")
                    return@launch
                }

                val selectedCandidate = rideCandidates.first()
                val selectedRide = selectedCandidate.ride
                Log.d("TestFlow", "🚌 נבחרה נסיעה: rideId=${selectedRide.rideId}")

                // 5. שליחת בקשת הצטרפות
                val requestSent = rideRepository.addPassengerToRide(
                    rideId = selectedRide.rideId,
                    passengerId = passengerId,
                    pickupLocation = locationData.geoPoint
                )

                if (!requestSent) {
                    Log.w("TestFlow", "⚠️ שליחת הבקשה נכשלה")
                    return@launch
                }

                Log.d("TestFlow", "📤 בקשת הצטרפות נשלחה בהצלחה")

                // 6. שליפת הבקשה ואישור
                val requests = rideRequestRepository.getRequestsByPassenger(passengerId)
                val matchingRequest = requests.firstOrNull { it.rideId == selectedRide.rideId }

                if (matchingRequest == null) {
                    Log.w("TestFlow", "⚠️ לא נמצאה בקשה תואמת לאישור")
                    return@launch
                }

                Log.d("TestFlow", "📨 הבקשה נשלפה: requestId=${matchingRequest.requestId}")

                val approved = rideRepository.approvePassengerRequest(
                    candidate = selectedCandidate,
                    requestId = matchingRequest.requestId,
                    passengerId = passengerId
                )

                if (approved) {
                    Log.d("TestFlow", "✅ הבקשה אושרה והנוסע נוסף לנסיעה (rideId=${selectedRide.rideId})")
                } else {
                    Log.w("TestFlow", "❌ הבקשה לא אושרה (אין מקום?)")
                }

            } catch (e: Exception) {
                Log.e("TestFlow", "❌ שגיאה חריגה בתהליך ההצטרפות", e)
            }
        }
    }



    @Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WePoolTheme {
        Greeting("Android")
    }
}
}
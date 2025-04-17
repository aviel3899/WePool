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
import com.wepool.app.data.model.logic.PolylineDecoder
import com.wepool.app.data.model.logic.RouteMatcher
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.repository.UserRepository
import com.wepool.app.data.repository.interfaces.IUserRepository
import com.wepool.app.data.repository.DriverRepository
import com.wepool.app.data.repository.interfaces.IDriverRepository
import com.wepool.app.data.repository.RideRepository
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.data.remote.GoogleMapsService
import com.google.android.gms.maps.model.LatLng
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wepool.app.ui.screens.LoginScreen
import com.wepool.app.ui.screens.SignUpScreen
import com.wepool.app.ui.screens.RoleSelectionScreen


class MainActivity : ComponentActivity() {

    init {
        RepositoryProvider.initialize(BuildConfig.MAPS_API_KEY)
    }

    private val authRepository = RepositoryProvider.provideAuthRepository()
    private val userRepository: IUserRepository = RepositoryProvider.provideUserRepository()
    private val driverRepository: IDriverRepository = RepositoryProvider.provideDriverRepository()
    private val rideRepository: IRideRepository = RepositoryProvider.provideRideRepository()
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
        lifecycleScope.launch {
            val loginResult = authRepository.loginWithEmailAndPassword(
                email = email,
                password = password
            )

            loginResult.onSuccess { uid ->
                Log.d("MainActivity", "✅ התחברות הצליחה | UID: $uid")

                //deleteSingleRide()
                createTestRide(uid)

            }.onFailure { error ->
                Log.e("MainActivity", "❌ שגיאה בהתחברות: ${error.message}")
            }
        }

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
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            RoleSelectionScreen(navController = navController, uid = uid)
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
            companyId = "company123",
            isBanned = false,
            roles = listOf(UserRole.DRIVER.name)
        )
    }

    private fun createTestDriver(user: User): Driver {
        return Driver(
            user = user,
            vehicleDetails = "Toyota Corolla 2022",
            activeRideId = ""
            //destination = GeoPoint(32.1798, 34.9133) // יעד לדוגמה: הנביאים 34 כפר סבא
        )
    }

    private fun createTestRide(uid: String) {
        lifecycleScope.launch {
            val success = rideRepository.planRideFromUserInput(
                driverId = uid,
                companyId = "company123",
                startAddress = "נחום 20 נתניה",
                destinationAddress = "הנביאים 34 כפר סבא",
                preferredArrivalTime = "09:00",
                date = "17-04-2025",
                direction = RideDirection.TO_WORK,
                availableSeats = 3,
                notes = "נסיעה לבדיקה"
            )
        }
    }

    private fun deleteSingleRide() {
        lifecycleScope.launch {
            try {
                val rides = rideRepository.getAllRides() // או פונקציה מקבילה שמחזירה את כל הנסיעות
                if (rides.isNotEmpty()) {
                    val rideToDelete = rides.first()
                    rideRepository.deleteRide(rideToDelete.rideId)
                    Log.d("RideManagement", "✅ הנסיעה היחידה נמחקה בהצלחה (rideId: ${rideToDelete.rideId})")
                } else {
                    Log.d("RideManagement", "ℹ️ לא נמצאו נסיעות למחיקה")
                }
            } catch (e: Exception) {
                Log.e("RideManagement", "❌ שגיאה במחיקת נסיעה: ${e.message}", e)
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
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
import com.wepool.app.data.model.users.Driver
import com.wepool.app.data.model.logic.PolylineDecoder
import com.wepool.app.data.model.logic.RouteMatcher
import com.wepool.app.data.repository.UserRepository
import com.wepool.app.data.repository.interfaces.IUserRepository
import com.wepool.app.data.repository.DriverRepository
import com.wepool.app.data.repository.interfaces.IDriverRepository
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.data.remote.GoogleMapsService
import com.google.android.gms.maps.model.LatLng


class MainActivity : ComponentActivity() {

    init {
        RepositoryProvider.initialize(BuildConfig.MAPS_API_KEY)
    }

    private val authRepository = RepositoryProvider.provideAuthRepository()
    private val userRepository: IUserRepository = RepositoryProvider.provideUserRepository()
    private val driverRepository: IDriverRepository = RepositoryProvider.provideDriverRepository()
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

                startAppFlow(uid)

            }.onFailure { error ->
                Log.e("MainActivity", "❌ שגיאה בהתחברות: ${error.message}")
            }
        }

        // UI
        enableEdgeToEdge()
        setContent {
            WePoolTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun startAppFlow(uid: String) {

        lifecycleScope.launch { // מאפשר לבצע קוד ברקע לצד ui ללא חסימה של ה- ui thread
            try {
                // בדיקה אם המשתמש קיים
                val existingUser = userRepository.getUser(uid)
                val user = existingUser ?: createTestUser(uid).also {
                    userRepository.createOrUpdateUser(it)
                    Log.d("WePoolFlow", "✅ משתמש חדש נוצר")
                }

                // בדיקה אם יש מידע על Driver
                val existingDriver = driverRepository.getDriver(uid)
                val driver = existingDriver ?: createTestDriver(user).also {
                    driverRepository.saveDriver(it)
                    Log.d("WePoolFlow", "✅ Driver חדש נשמר")
                }

                // driverRepository.updatePreferredArrivalTime(user.uid, "10:00")

                // מחשבים זמן יציאה
                val origin = GeoPoint(32.3197, 34.8535) // נחום 20 נתניה

                // 💡 שימוש בפונקציה המעודכנת שמחזירה גם polyline
                val result = driverRepository.calculateDepartureTimeFromArrival(
                    origin = origin,
                    destination = driver.destination!!,
                    arrivalTime = driver.preferredArrivalTime!!
                )

                Log.d("WePoolFlow", "🟢 שעת יציאה משוערת: ${result.departureTime}")
                Log.d("WePoolFlow", "🟢 פוליליין מוצפן: ${result.encodedPolyline}")

                // 🧪 נקודת איסוף לבדיקה
                val pickupPoint = LatLng(32.3025, 34.8602) // המחקר 3 נתניה
                // val pickupPoint = LatLng(32.0714, 34.8125) // אידמית 12 גבעתיים

                val isWithinDetour = RouteMatcher.isPickupWithinDriverDetour(
                    encodedPolyline = result.encodedPolyline,
                    pickupPoint = pickupPoint,
                    maxAllowedDetourMinutes = driver.maxDetourMinutes.toDouble(),
                    arrivalTime = driver.preferredArrivalTime!!,
                    mapsService = mapsService
                )

                if (isWithinDetour) {
                    Log.d("WePoolFlow", "✅ נקודת האיסוף מתאימה ונמצאת בטווח הסטייה המותר.")
                } else {
                    Log.d("WePoolFlow", "❌ נקודת האיסוף חורגת מהסטייה המותרת.")
                }

                /*
                // 🧭 פענוח הפוליליין לרשימת נקודות LatLng
                val decodedPoints = PolylineDecoder.decode(result.encodedPolyline)
                decodedPoints.forEachIndexed { index, point ->
                    Log.d("WePoolFlow", "📍 נקודה ${index + 1}: (${point.latitude}, ${point.longitude})")
                }

                // 🧭 פענוח + פישוט של הפוליליין לרשימת נקודות LatLng
                val simplifiedPoints = PolylineDecoder.decodeAndSimplify(result.encodedPolyline)
                simplifiedPoints.forEachIndexed { index, point ->
                    Log.d("WePoolFlow", "📍 נקודה ${index + 1}: (${point.latitude}, ${point.longitude})")
                }*/

            } catch (e: Exception) {
                Log.e("WePoolFlow", "❌ שגיאה בתהליך: ${e.message}", e)
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
            availableSeats = 3,
            vehicleDetails = "Toyota Corolla 2022",
            maxDetourMinutes = 10,
            preferredArrivalTime = "13:00",
            destination = GeoPoint(32.1798, 34.9133) // יעד לדוגמה: הנביאים 34 כפר סבא
        )
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
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
import com.wepool.app.data.repository.UserRepository
import com.wepool.app.data.repository.DriverRepository
import com.wepool.app.data.remote.GoogleMapsService


class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //checkFirestoreConnection()
        //checkGooglePlayServicesAvailability()

        // התחברות אנונימית אם אין משתמש
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    Log.d("Auth", "משתמש התחבר עם UID: $uid")
                    startAppFlow(uid)
                }
                .addOnFailureListener {
                    Log.e("Auth", "שגיאה בהתחברות: ${it.message}")
                }
        } else {
            val uid = auth.currentUser?.uid!!
            Log.d("Auth", "משתמש כבר מחובר: $uid")
            startAppFlow(uid)
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
        val mapsService = GoogleMapsService(BuildConfig.MAPS_API_KEY)

        val driverRepository = DriverRepository(
            auth = auth,
            firestore = firestore,
            mapsService = mapsService
        )

        val userRepository = UserRepository(
            db = firestore,
            auth = auth
        )

        // יצירת המשתמש והנהג
        val user = createTestUser(uid)
        val driver = createTestDriver(user)

        lifecycleScope.launch {
            try {
                userRepository.createOrUpdateUser(user)
                driverRepository.saveDriver(driver)

                val origin = GeoPoint(32.0853, 34.7818) // תל אביב

                val departureTime = driverRepository.calculateDepartureTimeFromArrival(
                    origin = origin,
                    destination = driver.destination!!,
                    arrivalTime = driver.preferredArrivalTime!!
                )

                Log.d("WePoolTest", "🟢 שעת יציאה משוערת: $departureTime")
            } catch (e: Exception) {
                Log.e("WePoolTest", "❌ שגיאה בתהליך: ${e.message}", e)
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
            preferredArrivalTime = "08:30",
            destination = GeoPoint(32.1093, 34.8555) // יעד לדוגמה: רמת גן
        )
    }

    private fun checkFirestoreConnection() {
        val testRef = firestore.collection("test").document("connection_check")
        testRef.set(mapOf("status" to "connected"))
            .addOnSuccessListener {
                Log.d("FirebaseCheck", "🎉 החיבור ל-Firebase Firestore תקין!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseCheck", "❌ שגיאה בחיבור ל-Firebase", e)
            }
    }

    private fun checkGooglePlayServicesAvailability() {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)

        if (resultCode == ConnectionResult.SUCCESS) {
            Log.d("GooglePlayCheck", "🟢 Google Play Services זמינים.")
        } else {
            Log.e("GooglePlayCheck", "🔴 Google Play Services חסרים או לא זמינים. Code: $resultCode")
            availability.getErrorDialog(this, resultCode, 9000)?.show()
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
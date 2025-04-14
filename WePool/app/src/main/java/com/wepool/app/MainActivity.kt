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
import com.wepool.app.data.repository.UserRepository
import com.wepool.app.data.repository.interfaces.IUserRepository
import com.wepool.app.data.repository.DriverRepository
import com.wepool.app.data.repository.interfaces.IDriverRepository
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.data.remote.GoogleMapsService


class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //checkFirestoreConnection()
        //checkGooglePlayServicesAvailability()

        val email = "test@wepool.com"
        val password = "test1234"

           /* FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    Log.d("Auth", "🟢 משתמש נוצר עם UID: $uid")
                    // שליפת טוקן כדי לוודא שהמשתמש מחובר באופן מלא
                    FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { tokenResult ->
                            Log.d("Token", "🟢 Token: ${tokenResult.token}")
                            // startAppFlow(uid)
                            // deleteAllUsers()
                        }
                        ?.addOnFailureListener { tokenError ->
                            Log.e("Token", "❌ שגיאה בקבלת טוקן: ${tokenError.message}", tokenError)
                        }
                } else {
                    Log.e("Auth", "❌ המשתמש נוצר אך לא התקבל UID")
                }
            }
            .addOnFailureListener {
                Log.e("Auth", "❌ שגיאה ביצירת המשתמש: ${it.message}", it)
            }*/


        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                Log.d("Auth", "משתמש התחבר עם UID: $uid")
                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                    ?.addOnSuccessListener { tokenResult ->
                        Log.d("Token", "🟢 Token: ${tokenResult.token}")
                        startAppFlow(uid!!)
                        //deleteAllUsers()
                    }
                    ?.addOnFailureListener { tokenError ->
                        Log.e("Token", "❌ שגיאה בקבלת טוקן: ${tokenError.message}", tokenError)
                    }
            }
            .addOnFailureListener {
                Log.e("Auth", "שגיאה בהתחברות: ${it.message}")
            }

        /*
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
        }*/


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
        val driverRepository: IDriverRepository = RepositoryProvider.provideDriverRepository(BuildConfig.MAPS_API_KEY)
        val userRepository: IUserRepository = RepositoryProvider.provideUserRepository()

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

                //driverRepository.updatePreferredArrivalTime(user.uid, "21:00")

                // מחשבים זמן יציאה
                val origin = GeoPoint(32.3197, 34.8535) // נחום 20 נתניה
                val departureTime = driverRepository.calculateDepartureTimeFromArrival(
                    origin = origin,
                    destination = driver.destination!!,
                    arrivalTime = driver.preferredArrivalTime!!
                )

                Log.d("WePoolFlow", "🟢 שעת יציאה משוערת: $departureTime")

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

    private fun deleteAllUsers() {
        val driverRepository: IDriverRepository = RepositoryProvider.provideDriverRepository(BuildConfig.MAPS_API_KEY)
        val userRepository: IUserRepository = RepositoryProvider.provideUserRepository()

        lifecycleScope.launch {
            try {
                val usersCollection = firestore.collection("users").get().await()
                for (document in usersCollection.documents) {
                    val uid = document.id
                    userRepository.deleteUser(uid, driverRepository)
                }
                Log.d("WePoolCleanup", "🧹 כל המשתמשים נמחקו בהצלחה.")
            } catch (e: Exception) {
                Log.e("WePoolCleanup", "❌ שגיאה במחיקת כל המשתמשים: ${e.message}", e)
            }
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
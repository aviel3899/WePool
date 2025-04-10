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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.wepool.app.data.model.users.User
import com.wepool.app.data.repository.UserRepository

class MainActivity : ComponentActivity() {

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🟢 בדיקת חיבור ל-Firestore
        /*val db = FirebaseFirestore.getInstance()
        val testRef = db.collection("test").document("connection_check")
        testRef.set(mapOf("status" to "connected"))
            .addOnSuccessListener {
                Log.d("FirebaseCheck", "🎉 החיבור ל-Firebase Firestore תקין!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseCheck", "❌ שגיאה בחיבור ל-Firebase", e)
            }
        // בדיקת חיבור לgoogle services
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)

        if (resultCode == ConnectionResult.SUCCESS) {
            Log.d("GooglePlayCheck", "🟢 Google Play Services זמינים.")
        } else {
            Log.e("GooglePlayCheck", "🔴 Google Play Services חסרים או לא זמינים. Code: $resultCode")
            availability.getErrorDialog(this, resultCode, 9000)?.show()
        }*/

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

        /*val testUser = User(
            uid = "test_user_123",
            name = "Test User",
            email = "test@example.com",
            phoneNumber = "050-0000000",
            companyId = "test_company",
            roles = listOf("DRIVER")
        )

        lifecycleScope.launch {
            userRepository.createOrUpdateUser(testUser)
            Log.d("FirestoreTest", "✅ User written using createOrUpdateUser()")
        }*/

        /*
        lifecycleScope.launch {
            userRepository.deleteUser("test_user_123")
        }*/
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
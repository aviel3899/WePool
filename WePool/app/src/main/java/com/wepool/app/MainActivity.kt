package com.wepool.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth
import com.wepool.app.data.repository.LoginSessionManager
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.infrastructure.navigation.handleNotificationNavigation
import com.wepool.app.notifications.NotificationHelper
import com.wepool.app.ui.screens.*
import com.wepool.app.ui.theme.WePoolTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    init {
        RepositoryProvider.initialize(BuildConfig.MAPS_API_KEY)
    }

    private var navController: NavHostController? = null
    private var wasNavigatedFromIntent = mutableStateOf(false)

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 📦 שמירה ל-SharedPreferences כדי שLoginScreen ידע מה הייתה ההתראה
        val screen = intent?.getStringExtra("screen")
        val rideId = intent?.getStringExtra("rideId")
        val fromNotification = intent?.getStringExtra("fromNotification") == "true"

        Log.d("MainActivity", "📥 onNewIntent: rideId=$rideId, screen=$screen, fromNotification=$fromNotification")

        if (fromNotification && !screen.isNullOrEmpty() && !rideId.isNullOrEmpty()) {
            NotificationHelper.storeNotificationData(this, screen, rideId)
        }

        enableEdgeToEdge()
        setContent {
            WePoolTheme {
                navController = rememberNavController()
                val context = LocalContext.current
                val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
                } else null

                val isLoggedIn = LoginSessionManager.didLoginManually(context)
                val firebaseUser = FirebaseAuth.getInstance().currentUser

                LaunchedEffect(firebaseUser, isLoggedIn) {
                    if (firebaseUser != null && isLoggedIn && !wasNavigatedFromIntent.value) {
                        val uid = firebaseUser.uid
                        navController?.navigate("roleSelection/$uid") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                        wasNavigatedFromIntent.value = true
                    }
                }

                LaunchedEffect(Unit) {
                    RepositoryProvider.provideRideRepository().deactivateExpiredRides()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        notificationPermissionState?.status?.isGranted == false
                    ) {
                        notificationPermissionState.launchPermissionRequest()
                    }

                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancelAll()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController!!,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") { LoginScreen(navController!!) }
                        composable("signup") { SignUpScreen(navController!!) }
                        composable("roleSelection/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            RoleSelectionScreen(uid, navController!!)
                        }
                        composable("intermediate/{uid}?fromLogin={fromLogin}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val fromLogin = it.arguments?.getString("fromLogin")?.toBooleanStrictOrNull() ?: false
                            IntermediateScreen(navController!!, uid, fromLogin)
                        }
                        composable("rideHistory/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            RideHistoryScreen(navController!!, uid)
                        }
                        composable("updateDetails/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            UpdateDetailsScreen(navController!!, uid)
                        }
                        composable("createRideDirection/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            CreateRideDirectionScreen(navController!!, uid)
                        }
                        composable("homeboundRide/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            HomeboundRideCreationScreen(navController!!, uid)
                        }
                        composable("workboundRide/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            WorkboundRideCreationScreen(navController!!, uid)
                        }
                        composable("driverCarDetails/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            DriverCarDetailsScreen(uid, navController!!)
                        }
                        composable("passengerRideDirection/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerRideDirectionScreen(navController!!, uid)
                        }
                        composable("passengerHomeboundRideSearch/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerHomeboundRideSearchScreen(navController!!, uid)
                        }
                        composable("passengerWorkboundRideSearch/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerWorkboundRideSearchScreen(navController!!, uid)
                        }
                        composable("driverMenu/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            DriverMenuScreen(navController!!, uid)
                        }
                        composable("passengerMenu/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerMenuScreen(uid, navController!!)
                        }
                        composable("passengerActiveRides/{uid}?rideId={rideId}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val rideId = it.arguments?.getString("rideId")
                            PassengerActiveRidesScreen(uid = uid, navController = navController!!, rideId = rideId)
                        }
                        composable("driverActiveRides/{uid}?rideId={rideId}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val rideId = it.arguments?.getString("rideId")
                            DriverActiveRidesScreen(uid = uid, navController = navController!!, rideId = rideId)
                        }
                        composable("driverPendingRequests/{uid}?rideId={rideId}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val rideId = it.arguments?.getString("rideId")
                            DriverRequestsScreen(uid = uid, navController = navController!!, filterRideId = rideId)
                        }
                        composable("passengerPendingRequests/{uid}?rideId={rideId}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val rideId = it.arguments?.getString("rideId")
                            PassengerRequestsScreen(uid = uid, navController = navController!!, filterRideId = rideId)
                        }
                        composable("preferredLocations/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PreferredLocationsScreen(uid, navController!!)
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        Log.d("MainActivity", "\uD83E\uDEB9 האפליקציה נסגרת – איפוס דגל התחברות")
        val isFinishingApp = isFinishing || !isChangingConfigurations
        if (isFinishingApp) {
            LoginSessionManager.clearLoginFlag(this)
        }
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val screen = intent.getStringExtra("screen")
        val rideId = intent.getStringExtra("rideId")
        val fromNotification = intent.getBooleanExtra("fromNotification", false) == true

        Log.d("MainActivity", "📥 onNewIntent: rideId=$rideId, screen=$screen, fromNotification=$fromNotification")

        if (fromNotification && !screen.isNullOrEmpty() && !rideId.isNullOrEmpty()) {
            NotificationHelper.storeNotificationData(this, screen, rideId)

            val currentUser = FirebaseAuth.getInstance().currentUser
            val didLoginManually = LoginSessionManager.didLoginManually(this)

            if (currentUser != null && didLoginManually) {
                lifecycleScope.launch {
                    handleNotificationNavigation(this@MainActivity, navController!!)
                    wasNavigatedFromIntent.value = true
                }
            }
        }
    }
}
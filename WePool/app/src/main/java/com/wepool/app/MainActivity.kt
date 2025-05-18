    package com.wepool.app

    import android.app.NotificationManager
    import android.content.Context
    import android.os.Build
    import android.os.Bundle
    import android.util.Log
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.enableEdgeToEdge
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.Scaffold
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.platform.LocalContext
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.rememberNavController
    import com.google.accompanist.permissions.ExperimentalPermissionsApi
    import com.google.accompanist.permissions.isGranted
    import com.google.accompanist.permissions.rememberPermissionState
    import com.google.firebase.auth.FirebaseAuth
    import com.wepool.app.infrastructure.RepositoryProvider
    import com.wepool.app.notifications.NotificationHelper
    import com.wepool.app.ui.screens.*
    import com.wepool.app.ui.theme.WePoolTheme
    import kotlinx.coroutines.delay

    class MainActivity : ComponentActivity() {

        init {
            RepositoryProvider.initialize(BuildConfig.MAPS_API_KEY)
        }

        @OptIn(ExperimentalPermissionsApi::class)
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            enableEdgeToEdge()
            setContent {
                WePoolTheme {
                    val navController = rememberNavController()
                    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        null
                    }
                    val context = LocalContext.current

                    LaunchedEffect(Unit) {
                        val screen = intent?.getStringExtra("screen")
                        val rideId = intent?.getStringExtra("rideId")
                        Log.d("MainActivity", "📦 rideId from intent: $rideId")
                        val fromNotification = intent?.getBooleanExtra("fromNotification", false) ?: false

                        if (fromNotification) {
                            val auth = FirebaseAuth.getInstance()

                            suspend fun waitForUser(timeoutMillis: Long = 3000L): String? {
                                val startTime = System.currentTimeMillis()
                                while (auth.currentUser == null && System.currentTimeMillis() - startTime < timeoutMillis) {
                                    delay(100)
                                }
                                return auth.currentUser?.uid
                            }

                            val uid = waitForUser()

                            if (uid == null) {
                                // לא מחובר – שמור את הנתונים ונעבור ל-login
                                NotificationHelper.storeNotificationNavigationData(this@MainActivity, screen, rideId)
                                navController.navigate("login")
                            } else {
                                val user = RepositoryProvider.provideUserRepository().getUser(uid)
                                val rideRepo = RepositoryProvider.provideRideRepository()
                                val ride = rideRepo.getRide(rideId!!)

                                val isDriverOfRide = ride?.driverId == uid
                                val isPassengerOfRide = ride?.passengers?.contains(uid) == true

                                when (screen) {
                                    "rideStarted", "pickup", "dropoff", "rideUpdated", "rideCancelled" -> {
                                        if (isPassengerOfRide) {
                                            navController.navigate("passengerActiveRides/$uid?rideId=$rideId")
                                        } else if (isDriverOfRide) {
                                            navController.navigate("driverActiveRides/$uid?rideId=$rideId")
                                        } else {
                                            // fallback כללי לפי תפקיד
                                            if (user?.roles?.contains("DRIVER") == true) {
                                                navController.navigate("driverMenu/$uid")
                                            } else {
                                                navController.navigate("passengerMenu/$uid")
                                            }
                                        }
                                    }

                                    "pendingRequests" -> {
                                        if (user?.roles?.contains("DRIVER") == true) {
                                            navController.navigate("driverPendingRequests/$uid?rideId=$rideId")
                                        } else {
                                            navController.navigate("passengerPendingRequests/$uid")
                                        }
                                    }

                                    else -> {
                                        if (user?.roles?.contains("DRIVER") == true) {
                                            navController.navigate("driverMenu/$uid")
                                        } else {
                                            navController.navigate("passengerMenu/$uid")
                                        }
                                    }
                                }
                            }
                        }

                        RepositoryProvider.provideRideRepository().deactivateExpiredRides()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !notificationPermissionState!!.status.isGranted
                        ) {
                            notificationPermissionState.launchPermissionRequest()
                        }

                        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.cancelAll()
                    }


                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "login",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("login") { LoginScreen(navController) }
                            composable("signup") { SignUpScreen(navController) }
                            composable("roleSelection/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                RoleSelectionScreen(uid, navController)
                            }
                            composable("intermediate/{uid}?fromLogin={fromLogin}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                val fromLogin = it.arguments?.getString("fromLogin")?.toBooleanStrictOrNull() ?: false
                                IntermediateScreen(navController, uid, fromLogin)
                            }
                            composable("rideHistoryMenu/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                RideHistoryMenuScreen(navController, uid)
                            }
                            composable("rideHistoryDriver/{uid}") { /* ... */ }
                            composable("rideHistoryPassenger/{uid}") { /* ... */ }
                            composable("rideHistoryCombined/{uid}") { /* ... */ }
                            composable("updateDetails/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                UpdateDetailsScreen(navController, uid)
                            }
                            composable("createRideDirection/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                CreateRideDirectionScreen(navController, uid)
                            }
                            composable("homeboundRide/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                HomeboundRideCreationScreen(navController, uid)
                            }
                            composable("workboundRide/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                WorkboundRideCreationScreen(navController, uid)
                            }
                            composable("driverCarDetails/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                DriverCarDetailsScreen(uid, navController)
                            }
                            composable("passengerRideDirection/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                PassengerRideDirectionScreen(navController, uid)
                            }
                            composable("passengerHomeboundRideSearch/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                PassengerHomeboundRideSearchScreen(navController, uid)
                            }
                            composable("passengerWorkboundRideSearch/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                PassengerWorkboundRideSearchScreen(navController, uid)
                            }
                            composable("driverMenu/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                DriverMenuScreen(navController, uid)
                            }
                            composable("passengerMenu/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                PassengerMenuScreen(uid, navController)
                            }
                            composable("passengerActiveRides/{uid}?rideId={rideId}") { backStackEntry ->
                                val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                                val rideId = backStackEntry.arguments?.getString("rideId")
                                PassengerActiveRidesScreen(uid = uid, navController = navController, rideId = rideId)
                            }
                            composable("driverActiveRides/{uid}?rideId={rideId}") { backStackEntry ->
                                val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                                val rideId = backStackEntry.arguments?.getString("rideId")
                                DriverActiveRidesScreen(uid = uid, navController = navController, rideId = rideId)
                            }
                            composable("driverPendingRequests/{uid}?rideId={rideId}") { backStackEntry ->
                                val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                                val rideId = backStackEntry.arguments?.getString("rideId")
                                DriverRequestsScreen(uid = uid, navController = navController, filterRideId = rideId)
                            }
                            composable("passengerPendingRequests/{uid}") {
                                val uid = it.arguments?.getString("uid") ?: return@composable
                                PassengerRequestsScreen(uid, navController)
                            }
                        }
                    }
                }
            }
        }
    }

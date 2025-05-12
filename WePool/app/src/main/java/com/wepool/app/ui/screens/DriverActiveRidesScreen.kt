package com.wepool.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.infrastructure.navigation.RideNavigationServiceController
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DriverActiveRidesScreen(uid: String, navController: NavController) {
    val context = LocalContext.current
    val driverRepository = RepositoryProvider.provideDriverRepository()
    val rideRepository = RepositoryProvider.provideRideRepository()
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isActionInProgress by remember { mutableStateOf(false) }

    fun refreshRides() {
        coroutineScope.launch {
            try {
                loading = true
                error = null
                rides = driverRepository.getActiveRidesForDriver(uid)
            } catch (e: Exception) {
                error = "❌ שגיאה בטעינת נסיעות: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    // טוען נסיעות בעת פתיחת המסך
    LaunchedEffect(Unit) {
        refreshRides()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Your Active Rides", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            rides.isEmpty() -> Text("You have no active rides at the moment.")
            else -> {
                LazyColumn {
                    items(rides) { ride ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("From: ${ride.startLocation.name}", style = MaterialTheme.typography.bodyMedium)
                                Text("To: ${ride.destination.name}", style = MaterialTheme.typography.bodyMedium)
                                Text("Date: ${ride.date}", style = MaterialTheme.typography.bodyMedium)
                                Text("Departure Time: ${ride.departureTime}", style = MaterialTheme.typography.bodyMedium)
                                Text("Arrival Time: ${ride.arrivalTime}", style = MaterialTheme.typography.bodyMedium)
                                Text("Stops on the way: ${ride.pickupStops.size}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                if (isActionInProgress) return@launch
                                                isActionInProgress = true

                                                if (!locationPermissionState.status.isGranted) {
                                                    locationPermissionState.launchPermissionRequest()
                                                    error = "📍 נדרש לאשר גישה למיקום כדי להתחיל ניווט"
                                                    isActionInProgress = false
                                                    return@launch
                                                }

                                                try {
                                                    RideNavigationServiceController.startRideNavigation(
                                                        context = context,
                                                        rideId = ride.rideId
                                                    )
                                                    Log.d("DriverActiveRides", "▶ ניווט התחיל ל־rideId=${ride.rideId}")
                                                } catch (e: Exception) {
                                                    error = "⚠️ שגיאה בהפעלת ניווט: ${e.message}"
                                                } finally {
                                                    isActionInProgress = false
                                                }
                                            }
                                        },
                                        enabled = !isActionInProgress
                                    ) {
                                        Text("Start Ride")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                if (isActionInProgress) return@launch
                                                isActionInProgress = true
                                                try {
                                                    rideRepository.deleteRide(ride.rideId)
                                                    refreshRides()
                                                    Log.i("DriverActiveRides", "🗑️ נסיעה בוטלה: ${ride.rideId}")
                                                } catch (e: Exception) {
                                                    error = "❌ שגיאה במחיקת נסיעה: ${e.message}"
                                                } finally {
                                                    isActionInProgress = false
                                                }
                                            }
                                        },
                                        enabled = !isActionInProgress
                                    ) {
                                        Text("Cancel Ride")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

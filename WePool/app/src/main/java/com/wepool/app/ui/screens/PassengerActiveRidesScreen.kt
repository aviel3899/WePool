package com.wepool.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun PassengerActiveRidesScreen(uid: String, navController: NavController, rideId: String? = null) {
    val passengerRepository = RepositoryProvider.providePassengerRepository()
    val rideRepository = RepositoryProvider.provideRideRepository()
    val requestRepository = RepositoryProvider.provideRideRequestRepository()
    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var approvalUpdated by remember { mutableStateOf(false) }
    var showTooLateDialog by remember { mutableStateOf(false) }
    var tardiness by remember { mutableStateOf(0) }

    fun refreshRides() {
        scope.launch {
            loading = true
            try {
                rides = passengerRepository.getActiveRidesForPassenger(uid)
                Log.d("PassengerActiveRides", "✅ Refreshed ${rides.size} active rides")
            } catch (e: Exception) {
                error = "❌ Failed to refresh rides: ${e.message}"
                Log.e("PassengerActiveRides", "❌ Error: ${e.message}", e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                rides = passengerRepository.getActiveRidesForPassenger(uid)
                Log.d("PassengerActiveRides", "✅ Found ${rides.size} active rides")
                if (!approvalUpdated) {
                    val allRequests = requestRepository.getRequestsByPassenger(uid)
                    val rideIdsInScreen = rides.map { it.rideId }.toSet()
                    allRequests
                        .filter { it.rideId in rideIdsInScreen && !it.passengerSawApprovedRequest }
                        .forEach { request ->
                            val updated = requestRepository.updatePassengerSawApprovedRequest(
                                rideId = request.rideId,
                                requestId = request.requestId,
                                approved = true
                            )
                            if (!updated) {
                                Log.w("ApprovalUpdate", "⚠ Failed to update approval for ${request.requestId}")
                            }
                        }
                    approvalUpdated = true
                }
            } catch (e: Exception) {
                error = "❌ Failed to load active rides: ${e.message}"
                Log.e("PassengerActiveRides", "❌ Error: ${e.message}", e)
            } finally {
                loading = false
            }
        }
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

        if (loading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
        } else {
            val filteredRides = if (!rideId.isNullOrEmpty()) {
                rides.filter { it.rideId == rideId }
            } else rides

            if (filteredRides.isEmpty()) {
                Text("You have no active rides at the moment.")
            } else {
                LazyColumn {
                    items(filteredRides) { ride ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("From: ${ride.startLocation.name}")
                                Text("To: ${ride.destination.name}")
                                Text("Date: ${ride.date}")

                                if (ride.direction == RideDirection.TO_WORK) {
                                    Text("Arrival Time: ${ride.arrivalTime}")
                                    Text("Pickup Time: ${rideRepository.getPickupTimeForPassenger(ride, uid)}")
                                } else {
                                    Text("Departure Time: ${ride.departureTime}")
                                    Text("Dropoff Time: ${rideRepository.getDropoffTimeForPassenger(ride, uid)}")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val now = LocalTime.now()
                                                    val formatter = DateTimeFormatter.ofPattern("HH:mm")
                                                    val nowFormatted = now.format(formatter)

                                                    val diffMinutes = rideRepository.calculateTimeDifferenceInMinutes(
                                                        nowFormatted,
                                                        ride.departureTime!!
                                                    )

                                                    val minAllowedMinutes = if (ride.direction == RideDirection.TO_WORK) 60 else 5

                                                    if (diffMinutes < minAllowedMinutes) {
                                                        tardiness = minAllowedMinutes
                                                        showTooLateDialog = true
                                                        return@launch
                                                    }

                                                    rideRepository.removePassengerFromRide(ride.rideId, uid, false)

                                                    val request = requestRepository.getRequestsByPassenger(uid).firstOrNull {
                                                        it.rideId == ride.rideId && it.status.name == "ACCEPTED"
                                                    }
                                                    val success = request?.let {
                                                        rideRepository.cancelRideRequest(ride.rideId, it.requestId)
                                                    } ?: false

                                                    if (success) {
                                                        refreshRides()
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("CancelRide", "❌ Error canceling ride: ${e.message}", e)
                                                }
                                            }
                                        }
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
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                navController.navigate("intermediate/$uid?fromLogin=false") {
                    popUpTo("intermediate/$uid?fromLogin=false") {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Home", style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showTooLateDialog) {
        AlertDialog(
            onDismissRequest = { showTooLateDialog = false },
            title = { Text("Too Late") },
            text = { Text("You cannot cancel a ride less than $tardiness minutes before it starts") },
            confirmButton = {
                Button(onClick = { showTooLateDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
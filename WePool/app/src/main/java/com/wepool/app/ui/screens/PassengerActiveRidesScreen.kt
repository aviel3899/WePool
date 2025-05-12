package com.wepool.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun PassengerActiveRidesScreen(uid: String, navController: NavController) {
    val passengerRepository = RepositoryProvider.providePassengerRepository()
    val rideRepository = RepositoryProvider.provideRideRepository()
    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var approvalUpdated by remember { mutableStateOf(false) }

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
                    val requestRepo = RepositoryProvider.provideRideRequestRepository()
                    val allRequests = requestRepo.getRequestsByPassenger(uid)

                    val rideIdsInScreen = rides.map { it.rideId }.toSet()

                    allRequests
                        .filter { it.rideId in rideIdsInScreen && !it.passengerSawApprovedRequest }
                        .forEach { request ->
                            val updated = requestRepo.updatePassengerSawApprovedRequest(
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
        } else if (rides.isEmpty()) {
            Text("You have no active rides at the moment.")
        } else {
            LazyColumn {
                items(rides) { ride ->
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
                                            rideRepository.removePassengerFromRide(ride.rideId, uid)
                                            refreshRides()
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

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
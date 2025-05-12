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
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DriverPendingRequestsScreen(uid: String, navController: NavController) {
    val requestRepo = RepositoryProvider.provideRideRequestRepository()
    val rideRepo = RepositoryProvider.provideRideRepository()
    val coroutineScope = rememberCoroutineScope()

    var pendingRequests by remember { mutableStateOf<List<RideRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val results = requestRepo.getPendingRequestsByDriver(uid)
                pendingRequests = results
                Log.d("DriverPendingRequests", "✅ Found ${results.size} pending requests")
            } catch (e: Exception) {
                error = "❌ Failed to load requests: ${e.message}"
                Log.e("DriverPendingRequests", "❌ Error: ${e.message}", e)
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pending Ride Requests", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            pendingRequests.isEmpty() -> Text("No pending requests found.")
            else -> LazyColumn {
                items(pendingRequests) { request ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ride ID: ${request.rideId}")
                            Text("Passenger ID: ${request.passengerId}")
                            Text("Status: ${request.status.name}")
                            Text("Pickup: ${request.pickupLocation.name}")

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                val ride = rideRepo.getRide(request.rideId)
                                                if (ride == null) {
                                                    Log.w("RideApproval", "⚠ Ride not found: ${request.rideId}")
                                                    return@launch
                                                }

                                                val candidate = RideCandidate(
                                                    ride = ride,
                                                    detourEvaluationResult = request.detourEvaluationResult
                                                )

                                                val success = rideRepo.approvePassengerRequest(
                                                    candidate = candidate,
                                                    requestId = request.requestId,
                                                    passengerId = request.passengerId
                                                )

                                                if (success) {
                                                    Log.d("RideApproval", "✅ Approved request ${request.requestId}")
                                                    pendingRequests = pendingRequests.filterNot {
                                                        it.requestId == request.requestId
                                                    }
                                                } else {
                                                    Log.w("RideApproval", "⚠ Approval failed for ${request.requestId}")
                                                }

                                            } catch (e: Exception) {
                                                Log.e("RideApproval", "❌ Error during approval: ${e.message}", e)
                                            }
                                        }
                                    }
                                ) {
                                    Text("Approve")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                val success = rideRepo.declineAndDeleteRideRequest(
                                                    rideId = request.rideId,
                                                    requestId = request.requestId
                                                )

                                                if (success) {
                                                    Log.d("RideDecline", "✅ Declined request ${request.requestId}")
                                                    pendingRequests = pendingRequests.filterNot {
                                                        it.requestId == request.requestId
                                                    }
                                                } else {
                                                    Log.w("RideDecline", "⚠ Failed to decline request ${request.requestId}")
                                                }

                                            } catch (e: Exception) {
                                                Log.e("RideDecline", "❌ Error during decline: ${e.message}", e)
                                            }
                                        }
                                    }
                                ) {
                                    Text("Decline")
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
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.RequestStatus
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

@Composable
fun PassengerRequestsScreen(uid: String, navController: NavController) {
    val requestStatuses = listOf("All", "Pending", "Accepted", "Declined", "Cancelled")
    var expanded by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(requestStatuses[0]) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<RideRequest>>(emptyList()) }

    val requestRepo = RepositoryProvider.provideRideRequestRepository()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ride Requests", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Filter by status", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(56.dp)
                    ) {
                        Text(selectedStatus, fontSize = 18.sp)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        requestStatuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    selectedStatus = status
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            loading = true
                            error = null
                            try {
                                val allRequests = requestRepo.getRequestsByPassenger(uid)

                                val filtered = if (selectedStatus == "All") {
                                    allRequests
                                } else {
                                    allRequests.filter {
                                        it.status.name.equals(selectedStatus, ignoreCase = true)
                                    }
                                }

                                // 🚨 עדכון השדות לפי הסטטוס שנבחר
                                when (selectedStatus) {
                                    "All" -> {
                                        filtered
                                            .filter {
                                                (!it.passengerSawApprovedRequest && it.status == RequestStatus.ACCEPTED) ||
                                                        (!it.passengerSawDeclinedRequest && it.status == RequestStatus.DECLINED)
                                            }
                                            .forEach { request ->
                                                requestRepo.updatePassengerSawApprovedRequest(
                                                    rideId = request.rideId,
                                                    requestId = request.requestId,
                                                    approved = request.status == RequestStatus.ACCEPTED
                                                )
                                                if (request.status == RequestStatus.DECLINED) {
                                                    requestRepo.updatePassengerSawDeclinedRequest(
                                                        rideId = request.rideId,
                                                        requestId = request.requestId,
                                                        declined = true
                                                    )
                                                }
                                            }
                                    }

                                    "Accepted" -> {
                                        filtered
                                            .filter {
                                                it.status == RequestStatus.ACCEPTED && !it.passengerSawApprovedRequest
                                            }
                                            .forEach { request ->
                                                requestRepo.updatePassengerSawApprovedRequest(
                                                    rideId = request.rideId,
                                                    requestId = request.requestId,
                                                    approved = true
                                                )
                                            }
                                    }

                                    "Declined" -> {
                                        filtered
                                            .filter {
                                                it.status == RequestStatus.DECLINED && !it.passengerSawDeclinedRequest
                                            }
                                            .forEach { request ->
                                                requestRepo.updatePassengerSawDeclinedRequest(
                                                    rideId = request.rideId,
                                                    requestId = request.requestId,
                                                    declined = true
                                                )
                                            }
                                    }
                                }

                                results = filtered
                                Log.d("PassengerRequests", "✅ Loaded ${results.size} requests with status: $selectedStatus")

                            } catch (e: Exception) {
                                error = "❌ Failed to fetch requests: ${e.message}"
                                Log.e("PassengerRequests", "❌ Error: ${e.message}", e)
                            } finally {
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(56.dp)
                ) {
                    Text("Search", fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            results.isEmpty() -> Text("No matching requests found.")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results) { request ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ride ID: ${request.rideId}")
                            Text("Status: ${request.status.name}")
                            Text("Pickup Location: ${request.pickupLocation.name}")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                containerColor = MaterialTheme.colorScheme.surfaceVariant, // soft neutral
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant // high contrast
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
}
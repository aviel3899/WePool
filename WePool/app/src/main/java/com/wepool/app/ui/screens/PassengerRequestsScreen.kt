package com.wepool.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.RequestStatus
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

@Composable
fun PassengerRequestsScreen(uid: String, navController: NavController, filterRideId: String? = null) {
    val requestStatuses = listOf("All", "Pending", "Accepted", "Declined", "Cancelled")
    var expanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<RideRequest>>(emptyList()) }
    var rides by remember { mutableStateOf<Map<String, Ride>>(emptyMap()) }
    var selectedRequest by remember { mutableStateOf<RideRequest?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val defaultTab = if (filterRideId != null) "Declined" else "All"
    var selectedStatus by rememberSaveable { mutableStateOf(defaultTab) }

    val requestRepo = RepositoryProvider.provideRideRequestRepository()
    val rideRepo = RepositoryProvider.provideRideRepository()
    val coroutineScope = rememberCoroutineScope()

    fun refresh() {
        coroutineScope.launch {
            loading = true
            error = null
            try {
                val allRequests = requestRepo.getRequestsByPassenger(uid)
                val baseFiltered = if (selectedStatus == "All") {
                    allRequests
                } else {
                    allRequests.filter {
                        it.status.name.equals(selectedStatus, ignoreCase = true)
                    }
                }

                val filtered = if (!filterRideId.isNullOrEmpty()) {
                    baseFiltered.filter { it.rideId == filterRideId }
                } else {
                    baseFiltered
                }

                when (selectedStatus) {
                    "All" -> filtered.forEach {
                        if (!it.passengerSawApprovedRequest && it.status == RequestStatus.ACCEPTED)
                            requestRepo.updatePassengerSawApprovedRequest(it.rideId, it.requestId, true)
                        if (!it.passengerSawDeclinedRequest && it.status == RequestStatus.DECLINED)
                            requestRepo.updatePassengerSawDeclinedRequest(it.rideId, it.requestId, true)
                    }
                    "Accepted" -> filtered.filter {
                        it.status == RequestStatus.ACCEPTED && !it.passengerSawApprovedRequest
                    }.forEach {
                        requestRepo.updatePassengerSawApprovedRequest(it.rideId, it.requestId, true)
                    }
                    "Declined" -> filtered.filter {
                        it.status == RequestStatus.DECLINED && !it.passengerSawDeclinedRequest
                    }.forEach {
                        requestRepo.updatePassengerSawDeclinedRequest(it.rideId, it.requestId, true)
                    }
                }

                val rideMap: Map<String, Ride> = filtered.mapNotNull { request ->
                    val ride = rideRepo.getRide(request.rideId)
                    if (ride != null) request.rideId to ride else null
                }.toMap()

                rides = rideMap
                results = filtered

                Log.d("PassengerRequests", "\u2705 Loaded ${results.size} requests with status: $selectedStatus")
            } catch (e: Exception) {
                error = "\u274C Failed to fetch requests: ${e.message}"
                Log.e("PassengerRequests", "\u274C Error: ${e.message}", e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Filter by status", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(0.75f).height(56.dp)
                    ) {
                        Text(selectedStatus, fontSize = 18.sp)
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        requestStatuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    selectedStatus = status
                                    expanded = false
                                    refresh()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = { refresh() }, modifier = Modifier.fillMaxWidth(0.75f).height(56.dp)) {
                    Text("Search", fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            results.isEmpty() -> Text("No matching requests found.")
            else -> LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(results) { request ->
                    val ride = rides[request.rideId]

                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ride?.let { r ->
                                Text("Direction: ${if (r.direction == RideDirection.TO_HOME) "To Home" else "To Work"}")
                                val locationLabel = if (r.direction == RideDirection.TO_WORK) "Pickup Location" else "Dropoff Location"
                                Text("$locationLabel: ${request.pickupLocation.name}")
                                val departureTime = if (r.direction == RideDirection.TO_WORK)
                                    request.detourEvaluationResult.pickupLocation?.pickupTime else r.departureTime
                                val arrivalTime = if (r.direction == RideDirection.TO_HOME)
                                    request.detourEvaluationResult.pickupLocation?.dropoffTime else r.arrivalTime
                                val departureLabel = if (r.direction == RideDirection.TO_WORK) "Pickup" else "Departure"
                                val arrivalLabel = if (r.direction == RideDirection.TO_HOME) "Dropoff" else "Arrival"
                                Text("Date: ${r.date} | $departureLabel: $departureTime | $arrivalLabel: $arrivalTime")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (request.status == RequestStatus.PENDING) {
                                Button(
                                    onClick = {
                                        selectedRequest = request
                                        showDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                                ) {
                                    Text("PENDING")
                                }
                            } else {
                                val statusColor = when (request.status) {
                                    RequestStatus.ACCEPTED -> Color(0xFF4CAF50)
                                    RequestStatus.DECLINED -> Color(0xFFF44336)
                                    RequestStatus.CANCELLED -> Color(0xFF9E9E9E)
                                    else -> Color.Gray
                                }
                                Text(
                                    text = request.status.name,
                                    color = statusColor,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                navController.navigate("intermediate/$uid?fromLogin=false") {
                    popUpTo("intermediate/$uid?fromLogin=false") { inclusive = false }
                    launchSingleTop = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(imageVector = Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Home", style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showDialog && selectedRequest != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Pending Request") },
            text = {
                Column {
                    Text("Do you want to cancel this ride request?")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val success = rideRepo.cancelRideRequest(
                                        rideId = selectedRequest!!.rideId,
                                        requestId = selectedRequest!!.requestId
                                    )
                                    if (success) refresh()
                                    showDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                        ) {
                            Text("Cancel\nRequest")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { showDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD))
                        ) {
                            Text("Back")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

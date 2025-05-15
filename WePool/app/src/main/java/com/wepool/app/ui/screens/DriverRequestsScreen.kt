package com.wepool.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.RequestStatus
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

@Composable
fun DriverRequestsScreen(uid: String, navController: NavController) {
    val requestStatuses = listOf("All", "Pending", "Accepted", "Declined")
    var expanded by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(requestStatuses[0]) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<RideRequest>>(emptyList()) }
    var passengerNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val userRepo = RepositoryProvider.provideUserRepository()
    val requestRepo = RepositoryProvider.provideRideRequestRepository()
    val rideRepo = RepositoryProvider.provideRideRepository()
    val coroutineScope = rememberCoroutineScope()

    var selectedRequest by remember { mutableStateOf<RideRequest?>(null) }
    var selectedRideDirection by remember { mutableStateOf<RideDirection?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

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
                                val allRequests = requestRepo.getRequestsByDriver(uid)
                                val filteredRequests = if (selectedStatus == "All") {
                                    allRequests
                                } else {
                                    allRequests.filter {
                                        it.status.name.equals(selectedStatus, ignoreCase = true)
                                    }
                                }
                                results = filteredRequests
                                val namesMap = mutableMapOf<String, String>()
                                val seenIds = mutableSetOf<String>()

                                filteredRequests.forEach { request ->
                                    try {
                                        if (seenIds.add(request.passengerId)) {
                                            val user = userRepo.getUser(request.passengerId)
                                            if (user != null) {
                                                namesMap[request.passengerId] = user.name
                                            }
                                        }
                                        val ride = rideRepo.getRide(request.rideId)
                                        ride?.passengers?.forEach { passengerId ->
                                            if (seenIds.add(passengerId)) {
                                                try {
                                                    val user = userRepo.getUser(passengerId)
                                                    if (user != null) {
                                                        namesMap[passengerId] = user.name
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("DriverRequests", "❌ Failed to fetch passenger name for $passengerId: ${e.message}")
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("DriverRequests", "❌ Error while processing request ${request.requestId}: ${e.message}")
                                    }
                                }
                                passengerNames = namesMap
                                Log.d("DriverRequests", "✅ Loaded ${results.size} requests with status: $selectedStatus")
                            } catch (e: Exception) {
                                error = "❌ Failed to fetch requests: ${e.message}"
                                Log.e("DriverRequests", "❌ Error: ${e.message}", e)
                            } finally {
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.75f).height(56.dp)
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
                    val ride = remember { mutableStateOf<com.wepool.app.data.model.ride.Ride?>(null) }

                    LaunchedEffect(request.rideId) {
                        ride.value = rideRepo.getRide(request.rideId)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ride.value?.let { r ->
                                Text("Direction: ${if (r.direction == RideDirection.TO_HOME) "To Home" else "To Work"}")

                                val locationLabel = if (r.direction == RideDirection.TO_WORK) "Pickup Location" else "Dropoff Location"
                                Text("$locationLabel: ${request.pickupLocation.name}")

                                val otherPassengers = r.passengers.filterNot { it == request.passengerId }
                                val requesterName = passengerNames[request.passengerId] ?: "Unknown"
                                val others = if (otherPassengers.isNotEmpty()) {
                                    otherPassengers.joinToString(", ") { pid -> passengerNames[pid] ?: "Unknown" }
                                } else "None"

                                Text("Date: ${r.date} | Departure: ${r.departureTime} | Arrival: ${r.arrivalTime}")
                                Text("Request by: $requesterName")
                                Text("Other passengers: $others")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            when (request.status) {
                                RequestStatus.PENDING -> {
                                    val statusColor = Color(0xFFFFC107)
                                    Button(
                                        onClick = {
                                            selectedRequest = request
                                            selectedRideDirection = ride.value?.direction
                                            selectedTime = if (ride.value?.direction == RideDirection.TO_WORK)
                                                request.detourEvaluationResult.pickupLocation?.pickupTime
                                            else
                                                request.detourEvaluationResult.pickupLocation?.dropoffTime
                                            showDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = statusColor)
                                    ) {
                                        Text(text = "PENDING")
                                    }
                                }
                                else -> {
                                    val statusColor = when (request.status) {
                                        RequestStatus.ACCEPTED -> Color(0xFF4CAF50)
                                        RequestStatus.DECLINED -> Color(0xFFF44336)
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
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back")
                    }
                }
            }
        }

        if (showDialog && selectedRequest != null) {
            val isToWork = selectedRideDirection == RideDirection.TO_WORK
            val timeLabel = if (isToWork) "Pickup Time" else "Dropoff Time"
            val updatedTimeLabel = if (isToWork) "New Departure Time" else "New Arrival Time"
            val pickupOrDropoff = if (isToWork)
                selectedRequest!!.detourEvaluationResult.pickupLocation?.pickupTime
            else
                selectedRequest!!.detourEvaluationResult.pickupLocation?.dropoffTime
            val updatedTime = selectedRequest!!.detourEvaluationResult.updatedReferenceTime

            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Request Details") },
                text = {
                    Column {
                        Text("$timeLabel: ${pickupOrDropoff ?: "Unknown"}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$updatedTimeLabel: ${updatedTime ?: "Unknown"}")
                    }
                },
                confirmButton = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val ride = rideRepo.getRide(selectedRequest!!.rideId)
                                        if (ride == null) {
                                            Log.w("RideApproval", "⚠ Ride not found: ${selectedRequest!!.rideId}")
                                            showDialog = false
                                            return@launch
                                        }

                                        val candidate = RideCandidate(
                                            ride = ride,
                                            detourEvaluationResult = selectedRequest!!.detourEvaluationResult
                                        )

                                        val success = rideRepo.approvePassengerRequest(
                                            candidate = candidate,
                                            requestId = selectedRequest!!.requestId,
                                            passengerId = selectedRequest!!.passengerId
                                        )

                                        if (success) {
                                            Log.d("RideApproval", "✅ Approved request ${selectedRequest!!.requestId}")
                                            results = results.filterNot {
                                                it.requestId == selectedRequest!!.requestId
                                            }
                                        } else {
                                            Log.w("RideApproval", "⚠ Approval failed for ${selectedRequest!!.requestId}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("RideApproval", "❌ Error during approval: ${e.message}", e)
                                    } finally {
                                        showDialog = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Approve")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val success = rideRepo.declineAndDeleteRideRequest(
                                            rideId = selectedRequest!!.rideId,
                                            requestId = selectedRequest!!.requestId
                                        )

                                        if (success) {
                                            Log.d("RideDecline", "✅ Declined and removed request ${selectedRequest!!.requestId}")
                                            results = results.filterNot { it.requestId == selectedRequest!!.requestId }
                                        } else {
                                            Log.w("RideDecline", "⚠ Failed to decline request ${selectedRequest!!.requestId}")
                                        }

                                    } catch (e: Exception) {
                                        Log.e("RideDecline", "❌ Error during decline: ${e.message}", e)
                                    } finally {
                                        showDialog = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                        ) {
                            Text("Decline")
                        }
                    }
                }
            )
        }
    }
}
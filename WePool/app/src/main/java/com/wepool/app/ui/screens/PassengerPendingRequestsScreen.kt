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
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PassengerPendingRequestsScreen(uid: String, navController: NavController) {
    val requestRepo = RepositoryProvider.provideRideRequestRepository()
    val coroutineScope = rememberCoroutineScope()
    var pendingRequests by remember { mutableStateOf<List<RideRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val results = requestRepo.getPendingRequestsByPassenger(uid)
                pendingRequests = results
                Log.d("PassengerPendingRequests", "✅ Found ${results.size} pending requests")
            } catch (e: Exception) {
                error = "❌ Failed to load requests: ${e.message}"
                Log.e("PassengerPendingRequests", "❌ Error: ${e.message}", e)
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
            pendingRequests.isEmpty() -> Text("You have no pending requests.")
            else -> LazyColumn {
                items(pendingRequests) { request ->
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

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                navController.navigate("passengerMenu/$uid") {
                    popUpTo("passengerPendingRequests/$uid") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
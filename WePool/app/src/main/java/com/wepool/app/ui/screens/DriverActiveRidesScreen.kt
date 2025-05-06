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
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

@Composable
fun DriverActiveRidesScreen(uid: String, navController: NavController) {
    val driverRepository = RepositoryProvider.provideDriverRepository()
    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                rides = driverRepository.getActiveRidesForDriver(uid)
                Log.d("DriverActiveRides", "✅ Found ${rides.size} active rides")
            } catch (e: Exception) {
                error = "❌ Failed to load active rides: ${e.message}"
                Log.e("DriverActiveRides", "❌ Error: ${e.message}", e)
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
                            Text("Departure Time: ${ride.departureTime}")
                            Text("Arrival Time: ${ride.arrivalTime}")
                            Text("Stops on the way: ${ride.pickupStops.size}")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                navController.navigate("driverMenu/$uid") {
                    popUpTo("driverActiveRides/$uid") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
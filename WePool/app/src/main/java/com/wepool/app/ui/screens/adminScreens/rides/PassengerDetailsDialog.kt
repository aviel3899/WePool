package com.wepool.app.ui.screens.adminScreens.rides

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider

@Composable
fun PassengerDetailsDialog(
    showDialog: Boolean,
    ride: Ride,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userRepository = RepositoryProvider.provideUserRepository()

    var passengers by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var showUserDialog by remember { mutableStateOf(false) }

    LaunchedEffect(ride.passengers) {
        val map = mutableMapOf<String, User>()
        ride.passengers.forEach { uid ->
            try {
                userRepository.getUser(uid)?.let { map[uid] = it }
            } catch (e: Exception) {
                Log.e("RidePassengerDialog", "❌ Error loading passenger $uid: ${e.message}")
            }
        }
        passengers = map
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Passenger List")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (passengers.isEmpty()) {
                        Text("No passengers found.")
                    } else {
                        passengers.values.forEach { passenger ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = passenger.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    selectedUser = passenger
                                    showUserDialog = true
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Passenger details")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }

    selectedUser?.let { user ->
        if (showUserDialog) {
            val stop = ride.pickupStops.firstOrNull { it.passengerId == user.uid }
            val (locationLabel, timeLabel, timeValue) = when (ride.direction) {
                RideDirection.TO_WORK -> Triple("Pickup Location", "Pickup Time", stop?.pickupTime ?: "Unknown")
                RideDirection.TO_HOME -> Triple("Dropoff Location", "Dropoff Time", stop?.dropoffTime ?: "Unknown")
                else -> Triple("Location", "Time", "Unknown")
            }
            val locationName = stop?.location?.name ?: "Unknown"

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AlertDialog(
                    onDismissRequest = { showUserDialog = false },
                    title = {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(text = user.name)
                        }
                    },
                    text = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:${user.email}")
                                        }
                                        context.startActivity(intent)
                                    }
                            ) {
                                Icon(Icons.Default.Email, contentDescription = "Email")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Email: ${user.email}", color = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${user.phoneNumber}")
                                        }
                                        context.startActivity(intent)
                                    }
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Phone")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Phone: ${user.phoneNumber}", color = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$locationLabel: $locationName")
                            Text("$timeLabel: $timeValue")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showUserDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

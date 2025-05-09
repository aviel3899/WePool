package com.wepool.app.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.data.model.ride.RideRequestUpdateResult
import kotlinx.coroutines.launch

@Composable
fun IntermediateScreen(navController: NavController, uid: String, cameFromLogin: Boolean = false) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val coroutineScope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(cameFromLogin) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    var updateResult by remember { mutableStateOf<RideRequestUpdateResult?>(null) }

    // Load welcome/update message once
    LaunchedEffect(Unit) {
        try {
            val userRepo = RepositoryProvider.provideUserRepository()
            val updateRepo = RepositoryProvider.provideRideRequestRepository()
            val user = userRepo.getUser(uid)

            if (user != null) {
                if (user.lastLoginTimestamp == null) {
                    dialogMessage = "Hello ${user.name}!\nWe are glad to see you for the first time!"
                } else {
                    val result = updateRepo.getNewRideRequestUpdatesForUser(uid)
                    updateResult = result

                    val x = result.newPendingRequestForDriver.size
                    val y = result.newAcceptedRequestsAsPassenger.size

                    dialogMessage = when {
                        x == 0 && y == 0 -> "Hello ${user.name}!\nSince your last login you have no notifications."
                        x == 0 -> "Hello ${user.name}!\nSince your last login you have:\n$y new ride approvals"
                        y == 0 -> "Hello ${user.name}!\nSince your last login you have:\n$x new ride requests"
                        else -> "Hello ${user.name}!\nSince your last login you have:\n$x new ride requests\n$y new ride approvals"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IntermediateScreen", "❌ Error loading welcome data: ${e.message}", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to WePool", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    navController.navigate("roleSelection/$uid")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Role Selection")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    navController.navigate("rideHistoryMenu/$uid")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ride History")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    navController.navigate("updateDetails/$uid")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Personal Details")
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = {
                    navController.navigate("login")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.error)
            }
        }

        // --- Popup Dialog ---
        if (showDialog && dialogMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(dialogMessage!!, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = {
                            showDialog = false

                            coroutineScope.launch {
                                val requestRepo = RepositoryProvider.provideRideRequestRepository()

                                // ✅ עדכון לנהג - סימון שנצפה
                                updateResult?.newPendingRequestForDriver?.forEach { request ->
                                    if (!request.seenByDriver) {
                                        val updated = requestRepo.updateSeenByDriver(
                                            rideId = request.rideId,
                                            requestId = request.requestId,
                                            seen = true
                                        )
                                        if (!updated) {
                                            Log.w("SeenUpdate", "⚠ Failed to update seen for ${request.requestId}")
                                        }
                                    }
                                }

                                // ✅ עדכון לנוסע - סימון שאושר
                                updateResult?.newAcceptedRequestsAsPassenger?.forEach { request ->
                                    if (!request.approvedByDriver) {
                                        val updated = requestRepo.updateApprovedByDriver(
                                            rideId = request.rideId,
                                            requestId = request.requestId,
                                            approved = true
                                        )
                                        if (!updated) {
                                            Log.w("ApprovalUpdate", "⚠ Failed to update approval for ${request.requestId}")
                                        }
                                    }
                                }
                            }
                        }) {
                            Text("X")
                        }

                    }
                }
            }
        }
    }
}

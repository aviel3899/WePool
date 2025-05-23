package com.wepool.app.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.data.model.ride.RideRequestUpdateResult
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import com.wepool.app.R

@Composable
fun IntermediateScreen(navController: NavController, uid: String, cameFromLogin: Boolean = false) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val coroutineScope = rememberCoroutineScope()

    var hasShownDialog by remember { mutableStateOf(false) }
    val showDialog = cameFromLogin && !hasShownDialog
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    var updateResult by remember { mutableStateOf<RideRequestUpdateResult?>(null) }

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
                    val z = result.newDeclinedRequestsAsPassenger.size

                    dialogMessage = when {
                        x == 0 && y == 0 && z == 0 ->
                            "Hello ${user.name}!\nSince your last login, you have no notifications."
                        x > 0 && y == 0 && z == 0 ->
                            "Hello ${user.name}!\nYou have $x ride requests waiting for your approval."
                        x == 0 && y > 0 && z == 0 ->
                            "Hello ${user.name}!\nYou have $y new ride approvals."
                        x == 0 && y == 0 && z > 0 ->
                            "Hello ${user.name}!\nYou have $z declined ride requests."
                        x > 0 && y > 0 && z == 0 ->
                            "Hello ${user.name}!\nSince your last login:\n$x ride requests need your approval\n$y new ride approvals."
                        x > 0 && y == 0 && z > 0 ->
                            "Hello ${user.name}!\nSince your last login:\n$x ride requests need your approval\n$z declined ride requests."
                        x == 0 && y > 0 && z > 0 ->
                            "Hello ${user.name}!\nSince your last login:\n$y new ride approvals\n$z declined ride requests."
                        else ->
                            "Hello ${user.name}!\nSince your last login:\n$x ride requests need your approval\n$y new ride approvals\n$z declined ride requests."
                    }
                }
            }

            userRepo.updateLastLoginTimestamp(uid, System.currentTimeMillis())

        } catch (e: Exception) {
            Log.e("IntermediateScreen", "❌ Error loading welcome data: ${e.message}", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to WePool", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            val buttonSize = 120.dp
            val iconSize = 72.dp
            val iconColor = Color(0xFF03A9F4)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = { navController.navigate("rideHistory/$uid") },
                        modifier = Modifier.size(buttonSize),
                        shape = MaterialTheme.shapes.medium,
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Ride History",
                            tint = iconColor,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ride\nHistory", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = { navController.navigate("roleSelection/$uid") },
                        modifier = Modifier.size(buttonSize),
                        shape = MaterialTheme.shapes.medium,
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Role Selection",
                            tint = iconColor,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Role\nSelection", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = { navController.navigate("updateDetails/$uid") },
                        modifier = Modifier.size(buttonSize),
                        shape = MaterialTheme.shapes.medium,
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ManageAccounts,
                            contentDescription = "Update Personal Details",
                            tint = iconColor,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Update\nDetails", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = {
                            navController.navigate("preferredLocations/$uid")
                        },
                        modifier = Modifier.size(buttonSize),
                        shape = MaterialTheme.shapes.medium,
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Preferred Locations",
                            tint = iconColor,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Preferred\nLocations", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
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
                            hasShownDialog = true

                            coroutineScope.launch {
                                val requestRepo = RepositoryProvider.provideRideRequestRepository()

                                updateResult?.newAcceptedRequestsAsPassenger?.forEach { request ->
                                    if (!request.passengerSawApprovedRequest) {
                                        val updated = requestRepo.updatePassengerSawApprovedRequest(
                                            rideId = request.rideId,
                                            requestId = request.requestId,
                                            approved = true
                                        )
                                        if (!updated) {
                                            Log.w("ApprovalUpdate", "⚠ Failed to update approval for ${request.requestId}")
                                        }
                                    }
                                }

                                updateResult?.newDeclinedRequestsAsPassenger?.forEach { request ->
                                    if (!request.passengerSawDeclinedRequest) {
                                        val updated = requestRepo.updatePassengerSawDeclinedRequest(
                                            rideId = request.rideId,
                                            requestId = request.requestId,
                                            declined = true
                                        )
                                        if (!updated) {
                                            Log.w("ApprovalUpdate", "⚠ Failed to update approval for ${request.requestId}")
                                        }
                                    }
                                }
                            }
                        }) {
                            Text("OK")
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedButton(
                onClick = {
                    navController.navigate("login")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.logout_svgrepo_com),
                    contentDescription = "Logout",
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", color = MaterialTheme.colorScheme.error)
            }
        }

    }
}

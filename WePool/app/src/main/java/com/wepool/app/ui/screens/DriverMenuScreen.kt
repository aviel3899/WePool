package com.wepool.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.infrastructure.RepositoryProvider

@Composable
fun DriverMenuScreen(navController: NavController, uid: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Driver Menu", style = MaterialTheme.typography.headlineSmall)

            Button(
                onClick = {
                    navController.navigate("createRideDirection/$uid")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create a Ride")
            }

            Button(
                onClick = {
                    navController.navigate("driverPendingRequests/$uid")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Requests")
            }

            Button(
                onClick = {
                    navController.navigate("driverActiveRides/$uid")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Active Rides")
            }

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
}

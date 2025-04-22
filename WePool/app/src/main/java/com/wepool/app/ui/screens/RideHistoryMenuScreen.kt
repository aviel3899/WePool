package com.wepool.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun RideHistoryMenuScreen(navController: NavController, uid: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ride History Menu", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                navController.navigate("rideHistoryDriver/$uid")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ride History as a Driver")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                navController.navigate("rideHistoryPassenger/$uid")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ride History as a Passenger")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                navController.navigate("rideHistoryCombined/$uid")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ride History Combined")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                navController.navigate("intermediate/$uid") {
                    popUpTo("rideHistoryMenu/$uid") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
            }
        }
}
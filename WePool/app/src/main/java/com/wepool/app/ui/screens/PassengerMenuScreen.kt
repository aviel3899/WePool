package com.wepool.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.util.Log

@Composable
fun PassengerMenuScreen(uid: String, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Passenger Menu", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                navController.navigate("passengerRideDirection/$uid")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Join a Ride")
        }

        Button(
            onClick = {
                Log.d("PassengerMenu", "🕓 Show Pending Requests clicked (not implemented yet)")
                navController.navigate("passengerPendingRequests/$uid")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Requests")
        }

        Button(
            onClick = {
                navController.navigate("passengerActiveRides/$uid")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Show Active Rides")
        }

        OutlinedButton(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Back")
        }
    }
}
package com.wepool.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

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
                    // TODO: Hook up to pending rides screen
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Pending Requests")
            }

            Button(
                onClick = {
                    // TODO: Hook up to active rides screen
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
        }
    }
}
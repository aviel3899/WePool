package com.wepool.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun IntermediateScreen(navController: NavController, uid: String) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

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
}
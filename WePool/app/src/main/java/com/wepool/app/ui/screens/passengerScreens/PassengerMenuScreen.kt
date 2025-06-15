package com.wepool.app.ui.screens.passengerScreens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.RequestPage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons

@Composable
fun PassengerMenuScreen(uid: String, navController: NavController) {

    BackgroundWrapper {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 96.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Passenger Menu", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(32.dp))

                    val buttonSize = 120.dp
                    val iconSize = 72.dp
                    val iconColor = Color(0xFF03A9F4)

                    // Join a Ride - Top Row
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedButton(
                            onClick = { navController.navigate("rideDirection/$uid/PASSENGER") },
                            modifier = Modifier.size(buttonSize),
                            shape = MaterialTheme.shapes.medium,
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiPeople,
                                contentDescription = "Join Ride",
                                tint = iconColor,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Join Ride", style = MaterialTheme.typography.labelLarge)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Requests + Active Rides - Bottom Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedButton(
                                onClick = { navController.navigate("passengerPendingRequests/$uid") },
                                modifier = Modifier.size(buttonSize),
                                shape = MaterialTheme.shapes.medium,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RequestPage,
                                    contentDescription = "Requests",
                                    tint = iconColor,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Requests", style = MaterialTheme.typography.labelLarge)
                        }

                        Spacer(modifier = Modifier.width(48.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedButton(
                                onClick = { navController.navigate("passengerActiveRides/$uid") },
                                modifier = Modifier.size(buttonSize),
                                shape = MaterialTheme.shapes.medium,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Active Rides",
                                    tint = iconColor,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Active Rides", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    BottomNavigationButtons(
                        uid = uid,
                        navController = navController,
                        showBackButton = true,
                        showHomeButton = true
                    )
                }
            }
        }
    }
}

package com.wepool.app.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.data.model.ride.Ride

@Composable
fun RideMapDialog(
    ride: Ride,
    extraPickupStop: PickupStop? = null,
    onDismiss: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            },
            text = {
                Column {
                    RideDynamicMap(
                        ride = ride,
                        extraPickupStop = extraPickupStop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = Color(0xFF4CAF50),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = Color(0xFF2196F3),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("End")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = Color(0xFFF44336),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pickup Stops")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = Color(0xFFFFA500),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Potential pickup stop")
                    }
                }
            }
        )
    }
}
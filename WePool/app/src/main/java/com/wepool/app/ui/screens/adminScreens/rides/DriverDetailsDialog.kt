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
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider

@Composable
fun DriverDetailsDialog(
    showDialog: Boolean,
    ride: Ride,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    val userRepository = RepositoryProvider.provideUserRepository()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var driver by remember { mutableStateOf<User?>(null) }
    var showUserDialog by remember { mutableStateOf(false) }

    LaunchedEffect(ride.driverId) {
        try {
            driver = userRepository.getUser(ride.driverId)
        } catch (e: Exception) {
            Log.e("RideDriverDialog", "❌ Failed to load driver: ${e.message}")
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Driver")
                }
            },
            text = {
                driver?.let {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showUserDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Driver details")
                        }
                    }
                } ?: Text("Driver not found.")
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )

        // דיאלוג פנימי להצגת פרטי הנהג
        if (showUserDialog && driver != null) {
            AlertDialog(
                onDismissRequest = { showUserDialog = false },
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = driver!!.name)
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
                                        data = Uri.parse("mailto:${driver!!.email}")
                                    }
                                    context.startActivity(intent)
                                }
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Email")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Email: ${driver!!.email}", color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${driver!!.phoneNumber}")
                                    }
                                    context.startActivity(intent)
                                }
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Phone")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Phone: ${driver!!.phoneNumber}", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showUserDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Close")
                        }
                    }
                }
            )
        }
    }
}

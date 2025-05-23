package com.wepool.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.users.Passenger
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import com.wepool.app.R

@Composable
fun RoleSelectionScreen(
    uid: String,
    navController: NavController
) {
    val userRepository = RepositoryProvider.provideUserRepository()
    val driverRepository = RepositoryProvider.provideDriverRepository()
    val passengerRepository = RepositoryProvider.providePassengerRepository()
    val coroutineScope = rememberCoroutineScope()

    var user by remember { mutableStateOf<User?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        try {
            Log.d("RoleSelectionScreen", "🔄 Loading user with UID: $uid")
            val loadedUser = userRepository.getUser(uid)
            if (loadedUser == null) {
                error = "⚠ No user found for UID: $uid"
            } else {
                user = loadedUser
                Log.d("RoleSelectionScreen", "✅ User loaded: ${loadedUser.name}")
            }
        } catch (e: Exception) {
            error = "❌ Failed to load user: ${e.message}"
            Log.e("RoleSelectionScreen", "❌ Exception: ${e.message}", e)
        } finally {
            loading = false
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
            Text("Choose Your Role", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(32.dp))

            val buttonSize = 120.dp
            val iconSize = 72.dp
            val iconColor = Color(0xFF03A9F4)

            when {
                loading -> CircularProgressIndicator()

                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)

                user != null -> {
                    if (user!!.roles.isEmpty()) {
                        Text("⚠ No roles assigned to this user.")
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            user!!.roles.forEach { role ->
                                val iconRes = when (role) {
                                    "DRIVER" -> R.drawable.steering_wheel_car_svgrepo_com
                                    "PASSENGER" -> R.drawable.seat_belt_svgrepo_com
                                    else -> R.drawable.seat_belt_svgrepo_com
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    OutlinedButton(
                                        onClick = {
                                            Log.d("RoleSelection", "🎯 Role selected: $role")
                                            coroutineScope.launch {
                                                when (role) {
                                                    "PASSENGER" -> {
                                                        val existing = passengerRepository.getPassenger(uid)
                                                        if (existing == null) {
                                                            passengerRepository.savePassengerData(
                                                                uid,
                                                                Passenger(user = user!!)
                                                            )
                                                            Log.d("RoleSelection", "✅ Passenger data created")
                                                        }
                                                        navController.navigate("passengerMenu/$uid")
                                                    }

                                                    "DRIVER" -> {
                                                        val existing = driverRepository.getDriver(uid)
                                                        if (existing == null) {
                                                            navController.navigate("driverCarDetails/$uid")
                                                        } else {
                                                            navController.navigate("driverMenu/$uid")
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(buttonSize),
                                        shape = MaterialTheme.shapes.medium,
                                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = "$role Icon",
                                            tint = Color.Unspecified, // כדי לשמור על הצבע המקורי
                                            modifier = Modifier.size(iconSize)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(role.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
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
                rideId = null,
                navController = navController,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                showBackButton = true,
                showHomeButton = false
            )
        }
    }
}
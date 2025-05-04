package com.wepool.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.users.Passenger
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Choose Your Role", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        when {
            loading -> CircularProgressIndicator()

            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)

            user != null -> {
                if (user!!.roles.isEmpty()) {
                    Text("⚠ No roles assigned to this user.")
                } else {
                    user!!.roles.forEach { role ->
                        Button(
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
                                            navController.navigate("passengerRideDirection/$uid")
                                        }

                                        "DRIVER" -> {
                                            val existing = driverRepository.getDriver(uid)
                                            if (existing == null) {
                                                // navigate to car details input screen
                                                navController.navigate("driverCarDetails/$uid")
                                            } else {
                                                // already has driver data → go to next screen
                                                navController.navigate("driverMenu/$uid")
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(role.uppercase())
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                navController.navigate("intermediate/$uid") {
                    popUpTo("roleSelection/$uid") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
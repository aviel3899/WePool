package com.wepool.app.ui.screens.mainScreens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.R
import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.data.model.users.Passenger
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import kotlinx.coroutines.launch

@Composable
fun RoleSelectionScreen(
    uid: String,
    navController: NavController
) {
    val userRepository = RepositoryProvider.provideUserRepository()

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
                    Text("Choose Your Role", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(32.dp))

                    when {
                        loading -> CircularProgressIndicator()

                        error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)

                        user != null -> {
                            val roleOrder = listOf(
                                UserRole.DRIVER,
                                UserRole.PASSENGER,
                                UserRole.HR_MANAGER,
                                UserRole.ADMIN
                            )
                            val roles = roleOrder.filter { it in user!!.roles }

                            if (roles.isEmpty()) {
                                Text("⚠ No roles assigned to this user.")
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (roles.isNotEmpty()) {
                                        when (roles.size) {
                                            1 -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    RoleButton(roles[0], uid, navController)
                                                }
                                            }

                                            2 -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly
                                                ) {
                                                    RoleButton(roles[0], uid, navController)
                                                    RoleButton(roles[1], uid, navController)
                                                }
                                            }

                                            3 -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly
                                                ) {
                                                    RoleButton(roles[0], uid, navController)
                                                    RoleButton(roles[1], uid, navController)
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    RoleButton(roles[2], uid, navController)
                                                }
                                            }
                                            else -> {
                                                roles.chunked(2).forEach { rowRoles ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceEvenly
                                                    ) {
                                                        rowRoles.forEach { role ->
                                                            RoleButton(role, uid, navController)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text("⚠ No roles assigned to this user.")
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
                        navController = navController,
                        showBackButton = true,
                        showHomeButton = false
                    )
                }
            }
        }
    }
}

@Composable
fun RoleButton(role: UserRole, uid: String, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val driverRepository = RepositoryProvider.provideDriverRepository()
    val passengerRepository = RepositoryProvider.providePassengerRepository()
    val userRepository = RepositoryProvider.provideUserRepository()

    val iconRes = when (role) {
        UserRole.DRIVER -> R.drawable.steering_wheel_car_svgrepo_com
        UserRole.PASSENGER -> R.drawable.seat_belt_svgrepo_com
        UserRole.HR_MANAGER -> R.drawable.hr_manager_svgrepo_com
        UserRole.ADMIN -> R.drawable.admin_svgrepo_com
        UserRole.All -> null
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        val user = userRepository.getUser(uid)
                        when (role) {
                            UserRole.PASSENGER -> {
                                val existing = passengerRepository.getPassenger(uid)
                                if (existing == null && user != null) {
                                    passengerRepository.savePassengerData(uid, Passenger(user))
                                }
                                navController.navigate("passengerMenu/$uid")
                            }

                            UserRole.DRIVER -> {
                                val existing = driverRepository.getDriver(uid)
                                if (existing == null) {
                                    navController.navigate("driverCarDetails/$uid")
                                } else {
                                    navController.navigate("driverMenu/$uid")
                                }
                            }

                            UserRole.HR_MANAGER -> navController.navigate("hrManagerMenu/$uid")
                            UserRole.ADMIN -> navController.navigate("adminMenu/$uid")
                            UserRole.All -> {}
                        }
                    }
                },
                modifier = Modifier.size(120.dp),
                shape = MaterialTheme.shapes.medium,
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                Icon(
                    painter = painterResource(id = iconRes!!),
                    contentDescription = "$role Icon",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(72.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val readableName = role.name
                .lowercase()
                .replace("_", " ")
                .replaceFirstChar { it.uppercase() }

            Text(readableName, style = MaterialTheme.typography.labelLarge)
        }
    }
}


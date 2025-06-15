package com.wepool.app.ui.screens.adminScreens.users

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.R
import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.StatusLabel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserCard(user: User, navController: NavController, onUserStatusChanged: () -> Unit, fromScreen: String = "") {
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showDetails by remember { mutableStateOf(false) }
    var showActivationDialog by remember { mutableStateOf(false) }
    var companyName by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf(user) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val roleToIcon = mapOf(
                            UserRole.ADMIN to R.drawable.admin_svgrepo_com,
                            UserRole.HR_MANAGER to R.drawable.hr_manager_svgrepo_com,
                            UserRole.DRIVER to R.drawable.steering_wheel_car_svgrepo_com,
                            UserRole.PASSENGER to R.drawable.seat_belt_svgrepo_com
                        )

                        listOf(
                            UserRole.ADMIN,
                            UserRole.HR_MANAGER,
                            UserRole.DRIVER,
                            UserRole.PASSENGER
                        ).forEach { role ->
                            if (role in currentUser.roles) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Image(
                                    painter = painterResource(id = roleToIcon[role]!!),
                                    contentDescription = role.name,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    companyName = currentUser.companyCode.let { code ->
                                        companyRepository.getCompanyByCode(code)?.companyName
                                    }
                                }
                                showDetails = true
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.information_svgrepo_com),
                                    contentDescription = "Details",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Unspecified
                                )
                            }

                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${currentUser.phoneNumber}")
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${currentUser.email}")
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            if ((UserRole.DRIVER in currentUser.roles || UserRole.PASSENGER in currentUser.roles) &&
                                UserRole.ADMIN !in currentUser.roles
                            ) {
                                IconButton(onClick = {
                                    if (currentUser.active) {
                                        when (fromScreen) {
                                            "HRManageEmployees" -> {
                                                navController.navigate("hrManagerRides/${currentUser.uid}?filter=true")
                                            }
                                            else -> {
                                                navController.navigate("ridesList/${currentUser.uid}?filter=true")
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Access to ride history is only allowed for active users",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Ride History",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                StatusLabel(
                    active = currentUser.active,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    onClick = {
                        showActivationDialog = true
                    }
                )
            }
        }
    }

    if (showDetails) {
        val formattedLastLogin = remember(currentUser.lastLoginTimestamp) {
            currentUser.lastLoginTimestamp?.let {
                val date = Date(it)
                val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                format.format(date)
            } ?: "Never"
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            AlertDialog(
                onDismissRequest = { showDetails = false },
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text("User Details", modifier = Modifier.align(Alignment.Center))
                    }
                },
                text = {
                    Column {
                        Text("Name: ${currentUser.name}")
                        Text("Permissions: ${currentUser.roles.joinToString()}")
                        companyName?.let { Text("Company: $it") }
                        Text("Last login: $formattedLastLogin")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetails = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }

    if (showActivationDialog) {
        AlertDialog(
            onDismissRequest = { showActivationDialog = false },
            title = {
                Text(
                    text = if (currentUser.active)
                        "Deactivate User"
                    else
                        "Activate User"
                )
            },
            text = {
                Text(
                    text = if (currentUser.active)
                        "Are you sure you want to deactivate this user? They will no longer have access to the app."
                    else
                        "Are you sure you want to activate this user? They will regain access to the app."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        if (currentUser.active) {
                            userRepository.unActivateUser(currentUser.uid)
                        } else {
                            userRepository.activateUser(currentUser.uid)
                        }
                        currentUser = currentUser.copy(active = !currentUser.active)
                        showActivationDialog = false
                        onUserStatusChanged()
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showActivationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

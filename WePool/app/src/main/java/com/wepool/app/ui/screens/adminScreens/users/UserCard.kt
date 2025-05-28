package com.wepool.app.ui.screens.adminScreens.users

import android.content.Intent
import android.net.Uri
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
import com.wepool.app.data.model.enums.UserRole
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserCard(user: User, navController: NavController){
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showDetails by remember { mutableStateOf(false) }
    var companyName by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name,
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
                        if (role in user.roles) {
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
                                companyName = user.companyCode?.let { code ->
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
                                data = Uri.parse("tel:${user.phoneNumber}")
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
                                data = Uri.parse("mailto:${user.email}")
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

                        if (UserRole.DRIVER in user.roles || UserRole.PASSENGER in user.roles) {
                            IconButton(onClick = {
                                navController.navigate("ridesList/${user.uid}?filter=true")
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
        }
    }

    if (showDetails) {
        val formattedLastLogin = remember(user.lastLoginTimestamp) {
            user.lastLoginTimestamp?.let {
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
                        Text(
                            "User Details",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                },
                text = {
                    Column {
                        Text("Name: ${user.name}")
                        Text("Permissions: ${user.roles.joinToString()}")
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
}

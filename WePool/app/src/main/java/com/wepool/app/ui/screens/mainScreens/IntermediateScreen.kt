package com.wepool.app.ui.screens.mainScreens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import com.wepool.app.R
import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.data.model.users.User
import com.wepool.app.ui.components.BackgroundWrapper

@Composable
fun IntermediateScreen(navController: NavController, uid: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var user by remember { mutableStateOf<User?>(null) }

    var hasShownDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    val showDialog = !hasShownDialog && dialogMessage != null

    var showTermsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val userRepo = RepositoryProvider.provideUserRepository()
            user = userRepo.getUser(uid)

            user?.let {
                if (it.lastLoginTimestamp == null) {
                    dialogMessage = "Hello ${it.name}!\nWe are glad to see you for the first time!"
                }
                userRepo.updateLastLoginTimestamp(uid, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            Log.e("IntermediateScreen", "❌ Error loading welcome data: ${e.message}", e)
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

                    user?.name?.let { name ->
                        Text(
                            text = "Hello $name!",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text("Welcome to WePool", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(32.dp))

                    val buttonSize = 120.dp
                    val iconSize = 72.dp
                    val iconColor = Color(0xFF03A9F4)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedButton(
                                onClick = {
                                    val isAdmin = user?.roles?.contains(UserRole.ADMIN) == true
                                    if (user?.active == true || isAdmin) {
                                        navController.navigate("roleSelection/$uid")
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "The user is not active. Please contact your HR Manager",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.size(buttonSize),
                                shape = MaterialTheme.shapes.medium,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Role Selection",
                                    tint = iconColor,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Role\nSelection", textAlign = TextAlign.Center)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedButton(
                                onClick = {
                                    val isAdmin = user?.roles?.contains(UserRole.ADMIN) == true
                                    if (user?.active == true || isAdmin) {
                                        navController.navigate("rideHistory/$uid")
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Access to ride history is only allowed for active users",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.size(buttonSize),
                                shape = MaterialTheme.shapes.medium,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Ride History",
                                    tint = iconColor,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ride\nHistory", textAlign = TextAlign.Center)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedButton(
                                onClick = { navController.navigate("preferredLocations/$uid") },
                                modifier = Modifier.size(buttonSize),
                                shape = MaterialTheme.shapes.medium,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Preferred Locations",
                                    tint = iconColor,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Preferred\nLocations", textAlign = TextAlign.Center)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedButton(
                                onClick = { navController.navigate("updateDetails/$uid") },
                                modifier = Modifier.size(buttonSize),
                                shape = MaterialTheme.shapes.medium,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ManageAccounts,
                                    contentDescription = "Update Personal Details",
                                    tint = iconColor,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Update\nDetails", textAlign = TextAlign.Center)
                        }
                    }
                }

                if (showDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(dialogMessage!!, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(onClick = {
                                    hasShownDialog = true
                                }) {
                                    Text("OK")
                                }
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showTermsDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 96.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Terms Info")
                }

                TermsAndConditionsDialog(
                    showDialog = showTermsDialog,
                    onDismissRequest = { showTermsDialog = false },
                    showCheckbox = false
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = {
                            navController.navigate("login")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.logout_svgrepo_com),
                            contentDescription = "Logout",
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

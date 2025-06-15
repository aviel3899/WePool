package com.wepool.app.ui.screens.driverScreens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.users.Driver
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import kotlinx.coroutines.launch

@Composable
fun DriverCarDetailsScreen(
    uid: String,
    navController: NavController
) {
    val userRepository = RepositoryProvider.provideUserRepository()
    val driverRepository = RepositoryProvider.provideDriverRepository()
    val coroutineScope = rememberCoroutineScope()

    var carDetails by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    BackgroundWrapper {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .padding(bottom = 96.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Enter Your Car Details", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = carDetails,
                        onValueChange = {
                            carDetails = it
                            errorMessage = null
                        },
                        label = { Text("Car Model and Year") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (carDetails.isBlank()) {
                                    errorMessage = "Car details cannot be empty."
                                    return@launch
                                }

                                isLoading = true
                                try {
                                    val user = userRepository.getUser(uid)
                                    if (user != null) {
                                        val driver = Driver(
                                            user = user,
                                            vehicleDetails = carDetails,
                                            activeRideId = emptyList()
                                        )
                                        driverRepository.saveDriver(driver)
                                        Log.d("DriverCarDetails", "✅ Driver saved")

                                        // ✅ Navigate to driverMenu
                                        navController.navigate("driverMenu/$uid") {
                                            popUpTo("driverCarDetails/$uid") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        errorMessage = "User not found."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                    Log.e(
                                        "DriverCarDetails",
                                        "❌ Error saving driver: ${e.message}",
                                        e
                                    )
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text(if (isLoading) "Saving..." else "Save")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    errorMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
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

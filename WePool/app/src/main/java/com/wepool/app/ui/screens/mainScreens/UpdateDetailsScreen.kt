package com.wepool.app.ui.screens.mainScreens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.wepool.app.R
import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.data.model.users.Driver
import com.wepool.app.data.model.users.Passenger
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.RoleSelectionCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun UpdateDetailsScreen(navController: NavController, uid: String) {
    val coroutineScope = rememberCoroutineScope()
    val userRepository = RepositoryProvider.provideUserRepository()
    val driverRepository = RepositoryProvider.provideDriverRepository()
    val passengerRepository = RepositoryProvider.providePassengerRepository()
    val auth = FirebaseAuth.getInstance()

    var user by remember { mutableStateOf<User?>(null) }
    var phoneNumber by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedRoles by remember { mutableStateOf(setOf<UserRole>()) }
    var originalRoles by remember { mutableStateOf(setOf<UserRole>()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val passwordsMatch = remember(newPassword, confirmPassword) {
        confirmPassword.isEmpty() || newPassword == confirmPassword
    }

    LaunchedEffect(uid) {
        try {
            val loadedUser = userRepository.getUser(uid)
            if (loadedUser != null) {
                user = loadedUser
                phoneNumber = loadedUser.phoneNumber
                selectedRoles = loadedUser.roles.toSet()
                originalRoles = loadedUser.roles.toSet()
            }
        } catch (e: Exception) {
            errorMessage = "Error loading user: ${e.message}"
        } finally {
            loading = false
        }
    }

    if (loading) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    BackgroundWrapper {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 160.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Update Personal Details", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Select Roles:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RoleSelectionCard(
                            role = UserRole.DRIVER,
                            isSelected = selectedRoles.contains(UserRole.DRIVER),
                            onClick = {
                                selectedRoles = if (selectedRoles.contains(UserRole.DRIVER))
                                    selectedRoles - UserRole.DRIVER
                                else
                                    selectedRoles + UserRole.DRIVER
                            },
                            iconResId = R.drawable.seat_belt_svgrepo_com,
                            text = "Driver",
                            modifier = Modifier.weight(1f)
                        )
                        RoleSelectionCard(
                            role = UserRole.PASSENGER,
                            isSelected = selectedRoles.contains(UserRole.PASSENGER),
                            onClick = {
                                selectedRoles = if (selectedRoles.contains(UserRole.PASSENGER))
                                    selectedRoles - UserRole.PASSENGER
                                else
                                    selectedRoles + UserRole.PASSENGER
                            },
                            iconResId = R.drawable.steering_wheel_car_svgrepo_com,
                            text = "Passenger",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = {
                                confirmPasswordVisible = !confirmPasswordVisible
                            }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        isError = !passwordsMatch && confirmPassword.isNotEmpty()
                    )

                    if (!passwordsMatch && confirmPassword.isNotEmpty()) {
                        Text(
                            "Passwords do not match",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (!passwordsMatch) {
                                errorMessage = "Passwords do not match. Please try again."
                                return@Button
                            }

                            coroutineScope.launch {
                                try {
                                    if (newPassword.isNotBlank()) {
                                        try {
                                            auth.currentUser?.updatePassword(newPassword)?.await()
                                        } catch (e: Exception) {
                                            errorMessage = "Password update failed: ${e.message}"
                                            return@launch
                                        }
                                    }

                                    val removedRoles = originalRoles - selectedRoles
                                    val addedRoles = selectedRoles - originalRoles

                                    if (UserRole.DRIVER in removedRoles) {
                                        driverRepository.getDriver(uid)?.let {
                                            driverRepository.deleteDriver(uid)
                                        }
                                    }
                                    if (UserRole.PASSENGER in removedRoles) {
                                        passengerRepository.getPassenger(uid)?.let {
                                            passengerRepository.deletePassenger(uid)
                                        }
                                    }

                                    if (UserRole.DRIVER in addedRoles) {
                                        val driver = Driver(user = user!!)
                                        driverRepository.saveDriver(driver)
                                    }
                                    if (UserRole.PASSENGER in addedRoles) {
                                        val passenger = Passenger(user = user!!)
                                        passengerRepository.savePassengerData(uid, passenger)
                                    }

                                    user?.let {
                                        val updatedUser = it.copy(
                                            phoneNumber = phoneNumber,
                                            roles = selectedRoles.toList()
                                        )
                                        userRepository.createOrUpdateUser(updatedUser)
                                    }

                                    navController.navigate("intermediate/$uid") {
                                        popUpTo("updateDetails/$uid") { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Update failed: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save and Exit")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
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

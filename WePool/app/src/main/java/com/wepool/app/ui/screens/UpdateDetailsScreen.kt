package com.wepool.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.UserRole
import com.wepool.app.data.model.users.Driver
import com.wepool.app.data.model.users.Passenger
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun UpdateDetailsScreen(navController: NavController, uid: String) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    val coroutineScope = rememberCoroutineScope()
    val userRepository = RepositoryProvider.provideUserRepository()
    val driverRepository = RepositoryProvider.provideDriverRepository()
    val passengerRepository = RepositoryProvider.providePassengerRepository()
    val auth = FirebaseAuth.getInstance()

    var user by remember { mutableStateOf<User?>(null) }
    var phoneNumber by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var selectedRoles by remember { mutableStateOf(setOf<String>()) }
    var originalRoles by remember { mutableStateOf(setOf<String>()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        try {
            val loadedUser = userRepository.getUser(uid)
            if (loadedUser != null) {
                user = loadedUser
                phoneNumber = loadedUser.phoneNumber ?: ""
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
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
        listOf(UserRole.DRIVER.name, UserRole.PASSENGER.name).forEach { role ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selectedRoles.contains(role),
                    onCheckedChange = {
                        selectedRoles = if (it) {
                            selectedRoles + role
                        } else {
                            selectedRoles - role
                        }
                    }
                )
                Text(role)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password (optional)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val removedRoles = originalRoles - selectedRoles
                        val addedRoles = selectedRoles - originalRoles

                        // Delete subcollections if needed
                        if ("DRIVER" in removedRoles) {
                            val exists = driverRepository.getDriver(uid)
                            if (exists != null) {
                                driverRepository.deleteDriver(uid)
                            }
                        }
                        if ("PASSENGER" in removedRoles) {
                            val exists = passengerRepository.getPassenger(uid)
                            if (exists != null) {
                                passengerRepository.deletePassenger(uid)
                            }
                        }

                        // Create subcollections if added
                        if ("DRIVER" in addedRoles) {
                            val driver = Driver(user = user!!)
                            driverRepository.saveDriver(driver)
                        }
                        if ("PASSENGER" in addedRoles) {
                            val passenger = Passenger(user = user!!)
                            passengerRepository.savePassengerData(uid, passenger)
                        }

                        // Update user data
                        user?.let {
                            val updatedUser = it.copy(
                                phoneNumber = phoneNumber,
                                roles = selectedRoles.toList()
                            )
                            userRepository.createOrUpdateUser(updatedUser)
                        }

                        // Update password if needed
                        if (newPassword.isNotBlank()) {
                            auth.currentUser?.updatePassword(newPassword)
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

        OutlinedButton(
            onClick = {
                navController.navigate("intermediate/$uid") {
                    popUpTo("updateDetails/$uid") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
           }
       }
}
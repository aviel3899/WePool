package com.wepool.app.ui.screens

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.notifications.NotificationHelper
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    val authRepository = RepositoryProvider.provideAuthRepository()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Content of the screen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "Carpool Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Welcome to WePool", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Small "Forgot?" button
            TextButton(
                onClick = { showResetDialog = true },
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot password", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Email and password must not be empty."
                            return@launch
                        }
                        isLoading = true
                        val result = authRepository.loginWithEmailAndPassword(email, password)
                        isLoading = false
                        result.onSuccess { uid ->
                            val (screen, rideId) = NotificationHelper.getStoredNotificationNavigationData(context)
                            NotificationHelper.clearNotificationNavigationData(context)

                            if (!screen.isNullOrEmpty()) {
                                val user = RepositoryProvider.provideUserRepository().getUser(uid)
                                val isDriver = user?.roles?.contains("DRIVER") == true
                                val isPassenger = user?.roles?.contains("PASSENGER") == true

                                when (screen) {
                                    "rideStarted", "pickup", "dropoff", "rideUpdated" -> {
                                        if (isPassenger) navController.navigate("passengerActiveRides/$uid?rideId=$rideId")
                                        else if (isDriver) navController.navigate("driverActiveRides/$uid?rideId=$rideId")
                                    }
                                    "rideCancelled" -> {
                                        if (isPassenger) navController.navigate("passengerActiveRides/$uid?rideId=$rideId")
                                        else if (isDriver) navController.navigate("driverActiveRides/$uid?rideId=$rideId")
                                    }
                                    "pendingRequests" -> {
                                        if (isPassenger) {
                                            navController.navigate("passengerPendingRequests/$uid")
                                        } else if (isDriver) {
                                            navController.navigate("driverPendingRequests/$uid?rideId=$rideId")
                                        }
                                    }
                                    else -> {
                                        if (isPassenger) navController.navigate("passengerMenu/$uid")
                                        else if (isDriver) navController.navigate("driverMenu/$uid")
                                    }
                                }
                            } else {
                                // ניווט רגיל במקרה שאין מידע מהתראה
                                navController.navigate("intermediate/$uid?fromLogin=true") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }.onFailure {
                            errorMessage = it.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isLoading) "Logging in..." else "Log In")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { navController.navigate("signup") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Sign Up")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { activity?.finishAffinity() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Exit", color = MaterialTheme.colorScheme.error)
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                val isSuccess = it.startsWith("📧")
                Text(
                    text = it,
                    color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error // green or red
                )
            }
        }

        // Blur dialog only when shown
        if (showResetDialog) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Background blur behind the dialog
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(16.dp)
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                // Foreground sharp dialog
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reset Password", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { showResetDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close dialog")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Email") },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val success = authRepository.resetPassword(resetEmail)
                                    showResetDialog = false
                                    if (success) {
                                        errorMessage = "📧 Reset email sent. Check your inbox."
                                    } else {
                                        errorMessage = "❌ Failed to send reset email."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Send Reset Email")
                        }
                    }
                }
            }
        }
    }
}
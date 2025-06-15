package com.wepool.app.ui.screens.mainScreens

import android.app.Activity
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.infrastructure.navigation.handleNotificationNavigation
import com.wepool.app.data.repository.LoginSessionManager
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import com.wepool.app.R
import com.wepool.app.ui.components.BackgroundWrapper

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

    BackgroundWrapper {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(24.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Carpool Icon",
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Welcome to WePool",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email", textAlign = TextAlign.Start) },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password", textAlign = TextAlign.Start) },
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
                }

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
                                LoginSessionManager.setDidLoginManually(context, true)

                                val navigated = handleNotificationNavigation(context, navController)
                                if (!navigated) {
                                    navController.navigate("intermediate/$uid") {
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
                    Icon(
                        painter = painterResource(id = R.drawable.login_line_svgrepo_com),
                        contentDescription = "Login Icon",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isLoading) "Logging in..." else "Log In")
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = { navController.navigate("signup") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.contact_details_svgrepo_com),
                        contentDescription = "Sign Up Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Up")
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    val isSuccess = it.startsWith("\uD83D\uDCE7")
                    Text(
                        text = it,
                        color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
            }

            if (showResetDialog) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(16.dp)
                            .background(Color.Black.copy(alpha = 0.3f))
                    )

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

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                OutlinedTextField(
                                    value = resetEmail,
                                    onValueChange = { resetEmail = it },
                                    label = { Text("Email", textAlign = TextAlign.Start) },
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val success = authRepository.resetPassword(resetEmail)
                                        showResetDialog = false
                                        errorMessage = if (success) {
                                            "\uD83D\uDCE7 Reset email sent. Check your inbox."
                                        } else {
                                            "\u274C Failed to send reset email."
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
}

package com.wepool.app.ui.screens.mainScreens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.R
import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.RoleSelectionCard
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(navController: NavController) {
    val authRepository = RepositoryProvider.provideAuthRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var companyCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var termsError by remember { mutableStateOf(false) }

    val availableRoles = listOf(UserRole.DRIVER, UserRole.PASSENGER)
    val selectedRoles = remember {
        mutableStateMapOf<UserRole, Boolean>().apply {
            put(UserRole.DRIVER, false)
            put(UserRole.PASSENGER, false)
        }
    }

    fun isInputValid(): Boolean {
        return name.isNotBlank() &&
                email.isNotBlank() &&
                password.length >= 6 &&
                phoneNumber.isNotBlank() &&
                companyCode.isNotBlank()
    }

    BackgroundWrapper {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .verticalScroll(scrollState)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Create an Account", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(24.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    CustomTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = "Name",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    CustomTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = "Email",
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    CustomTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = "Password",
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    CustomTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = "Confirm Password",
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    CustomTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it; errorMessage = null },
                        label = "Phone Number",
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    CustomTextField(
                        value = companyCode,
                        onValueChange = { companyCode = it; errorMessage = null },
                        label = "Company Code",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Your Role(s)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoleSelectionCard(
                        role = UserRole.DRIVER,
                        isSelected = selectedRoles[UserRole.DRIVER] ?: false,
                        onClick = {
                            selectedRoles[UserRole.DRIVER] =
                                !(selectedRoles[UserRole.DRIVER] ?: false)
                        },
                        iconResId = R.drawable.seat_belt_svgrepo_com,
                        text = "Driver",
                        modifier = Modifier.weight(1f)
                    )
                    RoleSelectionCard(
                        role = UserRole.PASSENGER,
                        isSelected = selectedRoles[UserRole.PASSENGER] ?: false,
                        onClick = {
                            selectedRoles[UserRole.PASSENGER] =
                                !(selectedRoles[UserRole.PASSENGER] ?: false)
                        },
                        iconResId = R.drawable.steering_wheel_car_svgrepo_com,
                        text = "Passenger",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = {
                    if (!isInputValid()) {
                        errorMessage = "Please fill all fields"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Password doesn't match"
                        return@Button
                    }
                    val chosenRoles = selectedRoles.filterValues { it }.keys.toList()
                    if (chosenRoles.isEmpty()) {
                        errorMessage = "Pick at least one role"
                        return@Button
                    }
                    showTermsDialog = true
                }, enabled = !isRegistering, modifier = Modifier.fillMaxWidth()) {
                    Text("Register")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                confirmButton = {
                    TextButton(onClick = { errorMessage = null }) {
                        Text("OK")
                    }
                },
                title = { Text("Error") },
                text = { Text(errorMessage ?: "") }
            )
        }

        TermsAndConditionsDialog(
            showDialog = showTermsDialog,
            onDismissRequest = { showTermsDialog = false },
            showCheckbox = true,
            termsAccepted = termsAccepted,
            onTermsAcceptedChange = { termsAccepted = it; termsError = false },
            termsError = termsError,
            onAccepted = {
                termsError = false
                showTermsDialog = false
                coroutineScope.launch {
                    isRegistering = true

                    val user = User(
                        uid = "",
                        name = name,
                        email = email,
                        phoneNumber = phoneNumber,
                        companyCode = companyCode,
                        banned = false,
                        active = false,
                        roles = selectedRoles.filterValues { it }.keys.toList()
                    )

                    val result = authRepository.signUpWithEmailAndPassword(
                        email = email,
                        password = password,
                        user = user,
                        userRepository = userRepository,
                        companyRepository = companyRepository
                    )

                    isRegistering = false

                    result.onSuccess {
                        navController.navigate("login") {
                            popUpTo("signUp") { inclusive = true }
                        }
                    }.onFailure {
                        Toast.makeText(
                            context,
                            it.message ?: "שגיאה בהרשמה",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onInvalidAcceptance = { termsError = true }
        )
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, textAlign = TextAlign.Start) },
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        modifier = modifier,
        singleLine = true
    )
}

@Composable
fun TermsAndConditionsDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    showCheckbox: Boolean,
    termsAccepted: Boolean = false,
    onTermsAcceptedChange: ((Boolean) -> Unit)? = null,
    termsError: Boolean = false,
    onAccepted: (() -> Unit)? = null,
    onInvalidAcceptance: (() -> Unit)? = null
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                if (showCheckbox && !termsAccepted) {
                    onTermsAcceptedChange?.invoke(false)
                    onInvalidAcceptance?.invoke()
                    return@TextButton
                }
                onAccepted?.invoke() ?: onDismissRequest()
            }) {
                Text("OK")
            }
        },
        title = { Text("Terms and Conditions") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    """
                    • Drivers may create homebound rides with an arrival time at least 10 minutes ahead.
                    • Drivers may create workbound rides with a departure time at least 2 hours ahead.
                    • For homebound rides, drivers may cancel a ride (with passengers) at least 10 minutes before departure.
                    • For workbound rides, drivers may cancel a ride (with passengers) at least 60 minutes before departure.
                    • If there are no passengers, drivers may cancel anytime.

                    • For homebound rides, passengers may join or cancel a ride at least 10 minutes before departure.
                    • For workbound rides, passengers may join or cancel a ride at least 60 minutes before departure.
                    """.trimIndent()
                )
                if (showCheckbox) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = onTermsAcceptedChange
                        )
                        Text("I accept terms and conditions")
                    }
                    if (termsError) {
                        Text(
                            "please accept terms and conditions",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
}
package com.wepool.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.wepool.app.data.model.enums.UserRole
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(navController: NavController) {
    val authRepository = RepositoryProvider.provideAuthRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
    val coroutineScope = rememberCoroutineScope()

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

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {

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
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
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
            availableRoles.forEach { role ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = selectedRoles[role] ?: false,
                        onCheckedChange = { selectedRoles[role] = it }
                    )
                    Text(role.name)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = {
                if (!isInputValid()) {
                    errorMessage = "אנא מלא את כל השדות"
                    return@Button
                }
                if (password != confirmPassword) {
                    errorMessage = "הסיסמאות לא תואמות"
                    return@Button
                }
                val chosenRoles = selectedRoles.filterValues { it }.keys.map { it.name }
                if (chosenRoles.isEmpty()) {
                    errorMessage = "בחר לפחות תפקיד אחד"
                    return@Button
                }

                coroutineScope.launch {
                    isRegistering = true
                    val user = User(
                        uid = "",
                        name = name,
                        email = email,
                        phoneNumber = phoneNumber,
                        companyCode = companyCode,
                        isBanned = false,
                        roles = chosenRoles
                    )
                    val result = authRepository.signUpWithEmailAndPassword(
                        email = email,
                        password = password,
                        user = user,
                        userRepository = userRepository
                    )
                    isRegistering = false
                    result.onSuccess {
                        navController.navigate("login") { popUpTo("signUp") { inclusive = true } }
                    }.onFailure {
                        errorMessage = it.message
                    }
                }
            }, enabled = !isRegistering, modifier = Modifier.fillMaxWidth()) {
                Text("Register")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
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
        label = { Text(label, textAlign = TextAlign.Start) }, // ✅ FIX: no fillMaxWidth()
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        modifier = modifier,
        singleLine=true
    )
}
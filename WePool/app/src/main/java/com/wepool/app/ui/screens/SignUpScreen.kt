package com.wepool.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
    var phoneNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }

    val availableRoles = listOf(UserRole.DRIVER, UserRole.PASSENGER)
    val selectedRoles = remember { mutableStateMapOf<UserRole, Boolean>().apply {
        put(UserRole.DRIVER, false)
        put(UserRole.PASSENGER, false)
    } }

    fun isInputValid(): Boolean {
        return name.isNotBlank() && email.isNotBlank() && password.length >= 6 && phoneNumber.isNotBlank()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create an Account", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                errorMessage = null
            },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
                errorMessage = null
            },
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Select Your Role(s)", style = MaterialTheme.typography.titleMedium)
        availableRoles.forEach { role ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = selectedRoles[role] ?: false,
                    onCheckedChange = { isChecked -> selectedRoles[role] = isChecked }
                )
                Text(role.name)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (!isInputValid()) {
                    errorMessage = "Please fill all fields. Password must be at least 6 characters."
                    return@Button
                }

                val chosenRoles = selectedRoles.filterValues { it }.keys.map { it.name }
                if (chosenRoles.isEmpty()) {
                    errorMessage = "Please select at least one role."
                    return@Button
                }

                coroutineScope.launch {
                    isRegistering = true
                    val newUser = User(
                        uid = "",
                        name = name,
                        email = email,
                        phoneNumber = phoneNumber,
                        companyCode = "",
                        isBanned = false,
                        roles = chosenRoles
                    )
                    val result = authRepository.signUpWithEmailAndPassword(email, password, newUser, userRepository)
                    isRegistering = false
                    result.onSuccess { uid ->
                        navController.navigate("roleSelection/$uid") {
                            popUpTo("login") { inclusive = true }
                        }
                    }.onFailure {
                        errorMessage = it.message
                    }
                }
            },
            enabled = !isRegistering,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { navController.popBackStack() },
            enabled = !isRegistering,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel", color = MaterialTheme.colorScheme.error)
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text("❌ $it", color = MaterialTheme.colorScheme.error)
            }
       }
}
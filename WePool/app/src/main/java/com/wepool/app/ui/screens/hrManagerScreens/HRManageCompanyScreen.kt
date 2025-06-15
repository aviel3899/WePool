package com.wepool.app.ui.screens.hrManagerScreens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.R
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import kotlinx.coroutines.launch
import com.wepool.app.data.repository.interfaces.ICompanyRepository
import com.wepool.app.ui.components.BackgroundWrapper

@Composable
fun HRManageCompanyScreen(uid: String, navController: NavController) {
    val context = LocalContext.current
    val userRepository = RepositoryProvider.provideUserRepository()
    val companyRepository: ICompanyRepository = RepositoryProvider.provideCompanyRepository()
    val mapsService = RepositoryProvider.mapsService
    val coroutineScope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var companyCode by remember { mutableStateOf<String?>(null) }
    var companyName by remember { mutableStateOf<String?>(null) }
    var companyLocation by remember { mutableStateOf<LocationData?>(null) }
    var newCompanyName by remember { mutableStateOf(TextFieldValue("")) }
    var newLocationAddress by remember { mutableStateOf("") }
    var locationSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var companyId by remember { mutableStateOf<String?>(null) }

    // Fetch the company details immediately when the screen is shown
    LaunchedEffect(uid) {
        val user = userRepository.getUser(uid)
        if (user?.companyCode?.isNotBlank() == true) {
            companyCode = user.companyCode
            companyRepository.getCompanyByCode(user.companyCode)?.let {
                companyName = it.companyName
                companyLocation = it.location
                companyId = it.companyId
            }
        } else {
            Toast.makeText(
                context,
                "❌ Failed to retrieve your company code.",
                Toast.LENGTH_LONG
            ).show()
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
                    Column(
                        modifier = Modifier.width(120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedButton(
                            onClick = {
                                showCodeDialog = true
                            },
                            modifier = Modifier.size(120.dp),
                            shape = MaterialTheme.shapes.medium,
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.folder_secret_svgrepo_com),
                                contentDescription = "Show Company Code",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Show Code",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.width(120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedButton(
                                onClick = { showLocationDialog = true },
                                modifier = Modifier.size(120.dp),
                                shape = MaterialTheme.shapes.medium,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.location_pin_svgrepo_com),
                                    contentDescription = "Update Location",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Update Location",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2
                            )
                        }

                        Column(
                            modifier = Modifier.width(120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedButton(
                                onClick = { showDialog = true },
                                modifier = Modifier.size(120.dp),
                                shape = MaterialTheme.shapes.medium,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.edit_svgrepo_com),
                                    contentDescription = "Update Name",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Update Name",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2
                            )
                        }
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

            if (showCodeDialog) {
                AlertDialog(
                    onDismissRequest = { showCodeDialog = false },
                    title = {
                        Text(
                            text = "Company Code",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Text(
                            text = companyCode ?: "N/A",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showCodeDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close", textAlign = TextAlign.Center)
                        }
                    }
                )
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = {
                        Text(
                            text = "Update Company Name",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Current Name: ${companyName ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = newCompanyName,
                                onValueChange = { newCompanyName = it },
                                label = { Text("New Company Name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            errorMessage?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newCompanyName.text.isBlank()) {
                                    errorMessage = "Company name cannot be blank."
                                } else {
                                    errorMessage = null
                                    coroutineScope.launch {
                                        companyCode?.let { code ->
                                            companyRepository.updateCompanyName(
                                                code,
                                                newCompanyName.text
                                            )
                                            Toast.makeText(
                                                context,
                                                "✅ Company name updated successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    showDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }

            if (showLocationDialog) {
                AlertDialog(
                    onDismissRequest = { showLocationDialog = false },
                    title = {
                        Text(
                            text = "Update Company Location",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Current Location: ${companyLocation?.name ?: "N/A"}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newLocationAddress,
                                onValueChange = {
                                    newLocationAddress = it
                                    coroutineScope.launch {
                                        locationSuggestions = mapsService.getAddressSuggestions(it)
                                    }
                                },
                                label = { Text("New Location Address") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            locationSuggestions.forEach { suggestion ->
                                TextButton(onClick = {
                                    newLocationAddress = suggestion
                                    locationSuggestions = emptyList()
                                }) {
                                    Text(suggestion, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        if (!newLocationAddress.isNullOrBlank() && companyId != null) {
                                            companyRepository.updateLocation(
                                                companyId!!,
                                                newLocationAddress
                                            )
                                            Toast.makeText(
                                                context,
                                                "✅ Location updated successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Invalid location.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Error updating location.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    showLocationDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showLocationDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        }
    }
}
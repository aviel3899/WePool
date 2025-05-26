package com.wepool.app.ui.screens.adminScreens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.company.Company
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun AdminAddCompanyDialog(
    uid: String,
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val mapsService = RepositoryProvider.mapsService

    var companyName by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<LocationData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    var autoExpanded by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (companyName.isBlank() || selectedLocation == null) {
                                errorMessage = "Please fill all fields"
                                return@launch
                            }
                            isSubmitting = true
                            try {
                                val generatedCode =
                                    companyRepository.generateRandomUniqueCompanyCode()
                                val companyId = UUID.randomUUID().toString()
                                val newCompany = Company(
                                    companyId = companyId,
                                    companyCode = generatedCode,
                                    companyName = companyName,
                                    location = selectedLocation,
                                    createdAt = Timestamp.now(),
                                    active = true
                                )
                                companyRepository.createOrUpdateCompany(newCompany)
                                Toast.makeText(
                                    context,
                                    "✅ Company added successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    Text(if (isSubmitting) "Saving..." else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Add Company",
                        modifier = Modifier.align(Alignment.Center), // מרכז את הכותרת
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = {
                            companyName = it
                            errorMessage = null
                        },
                        label = { Text("Company Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = {
                                addressInput = it
                                selectedLocation = null
                                coroutineScope.launch {
                                    suggestions = mapsService.getAddressSuggestions(it)
                                    autoExpanded = suggestions.isNotEmpty()
                                }
                                errorMessage = null
                            },
                            label = { Text("Company Address") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    textFieldSize = coordinates.size
                                }
                                .focusRequester(focusRequester),
                            singleLine = true
                        )

                        if (autoExpanded && suggestions.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .width(with(density) { textFieldSize.width.toDp() })
                                    .padding(top = with(density) { textFieldSize.height.toDp() })
                                    .heightIn(max = 200.dp)
                            ) {
                                LazyColumn {
                                    items(suggestions) { suggestion ->
                                        ListItem(
                                            headlineContent = { Text(suggestion) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    addressInput = suggestion
                                                    autoExpanded = false
                                                    coroutineScope.launch {
                                                        selectedLocation =
                                                            mapsService.getCoordinatesFromAddress(
                                                                suggestion
                                                            )
                                                    }
                                                    focusRequester.requestFocus()
                                                }
                                                .padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}

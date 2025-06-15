package com.wepool.app.ui.screens.favoriteLocations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AddLocationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    uid: String,
    coroutineScope: CoroutineScope,
    onLocationAdded: (List<LocationData>) -> Unit
) {
    val mapsService = RepositoryProvider.mapsService
    val userRepository = RepositoryProvider.provideUserRepository()

    var addressInput by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var autoExpanded by remember { mutableStateOf(false) }
    var selectedLocation: LocationData? by remember { mutableStateOf(null) }
    val focusRequester = remember { FocusRequester() }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (showDialog) {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                selectedLocation?.let { newLocation ->
                                    val user = userRepository.getUser(uid)
                                    val existingLocations = user?.favoriteLocations ?: emptyList()

                                    val alreadyExists =
                                        existingLocations.any { it.placeId == newLocation.placeId }

                                    if (alreadyExists) {
                                        println("Location already exists")
                                    } else {
                                        userRepository.addFavoriteLocation(uid, newLocation)
                                        val updatedUser = userRepository.getUser(uid)
                                        onLocationAdded(
                                            updatedUser?.favoriteLocations ?: emptyList()
                                        )
                                        onDismiss()
                                    }
                                }
                            }
                        },
                        enabled = selectedLocation != null
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                },
                title = { Text("Add New Location") },
                text = {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = addressInput,
                                onValueChange = {
                                    addressInput = it
                                    coroutineScope.launch {
                                        suggestions = mapsService.getAddressSuggestions(it)
                                        autoExpanded = suggestions.isNotEmpty()
                                    }
                                },
                                label = { Text("Search Address") },
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
                                        .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                                        .padding(top = with(LocalDensity.current) { textFieldSize.height.toDp() })
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
                    }
                }
            )
        }
    }
}

package com.wepool.app.ui.screens.favoriteLocations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wepool.app.R
import com.wepool.app.data.model.common.LocationData

@Composable
fun FavoriteLocationDropdown(
    label: String,
    locationData: LocationData,
    onLocationSelected: (LocationData) -> Unit,
    onTextChanged: (String) -> Unit,
    suggestions: List<String>,
    favoriteLocations: List<LocationData>,
    modifier: Modifier = Modifier
) {
    var isFavoritesExpanded by remember { mutableStateOf(false) }
    var isSuggestionsExpanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = locationData.name,
                onValueChange = {
                    onTextChanged(it)
                    isSuggestionsExpanded = true
                },
                modifier = Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                        textFieldSize = coordinates.size
                    },
                label = { Text(label) },
                trailingIcon = {
                    Row {
                        if (locationData.name.isNotBlank()) {
                            IconButton(onClick = {
                                onTextChanged("")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear field",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        IconButton(onClick = {
                            isFavoritesExpanded = !isFavoritesExpanded
                            isSuggestionsExpanded = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Select from favorites",
                                tint = Color(0xFFDAA520)
                            )
                        }
                    }
                },
                singleLine = true
            )

            DropdownMenu(
                expanded = isFavoritesExpanded,
                onDismissRequest = { isFavoritesExpanded = false },
                modifier = Modifier
                    .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                    .heightIn(max = 200.dp)
            ) {
                if (favoriteLocations.isEmpty()) {
                    DropdownMenuItem(text = { Text("No saved locations") }, onClick = {})
                } else {
                    favoriteLocations.forEach { location ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (location.note.equals("home", ignoreCase = true)) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.home_house_svgrepo_com),
                                            contentDescription = "Home",
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(28.dp).padding(end = 8.dp)
                                        )
                                        Text("Home")
                                    } else {
                                        Text(location.name)
                                    }
                                }
                            },
                            onClick = {
                                onLocationSelected(location)
                                isFavoritesExpanded = false
                                isSuggestionsExpanded = false
                            }
                        )
                    }
                }
            }

            if (suggestions.isNotEmpty() && isSuggestionsExpanded) {
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
                                        onTextChanged(suggestion)
                                        isSuggestionsExpanded = false
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

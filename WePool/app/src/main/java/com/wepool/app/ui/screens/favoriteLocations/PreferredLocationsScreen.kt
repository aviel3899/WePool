package com.wepool.app.ui.screens.favoriteLocations

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wepool.app.R
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferredLocationsScreen(uid: String, navController: NavController) {
    val context = LocalContext.current

    val userRepository = RepositoryProvider.provideUserRepository()
    val coroutineScope = rememberCoroutineScope()

    var locations by remember { mutableStateOf<List<LocationData>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showHomeDialog by remember { mutableStateOf(false) }
    var homeLocation by remember { mutableStateOf<LocationData?>(null) }

    LaunchedEffect(uid) {
        coroutineScope.launch {
            try {
                val user = userRepository.getUser(uid)
                locations = user?.favoriteLocations ?: emptyList()
            } catch (e: Exception) {
                error = "Error fetching preferred locations"
            } finally {
                loading = false
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Scaffold(
            bottomBar = {
                BottomNavigationButtons(
                    uid = uid,
                    navController = navController,
                    showBackButton = true,
                    showHomeButton = false
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("+", fontSize = 24.sp)
                }
            }
        ) { innerPadding ->
            BackgroundWrapper {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when {
                        loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        error != null -> {
                            Text(
                                text = error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Preferred Locations",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    textAlign = TextAlign.Center
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = {
                                            val home = locations.find {
                                                it.note.trim().equals("home", ignoreCase = true)
                                            }
                                            homeLocation = home
                                            showHomeDialog = true
                                        },
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.home_house_svgrepo_com),
                                            contentDescription = "Back to Home",
                                            modifier = Modifier.size(72.dp),
                                            tint = Color.Unspecified
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 100.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(locations) { location ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(location.name)

                                                if (location.note.trim()
                                                        .equals("Home", ignoreCase = true)
                                                ) {
                                                    Text(
                                                        text = "This is Your home!",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                if (location.note.trim().equals("Home", ignoreCase = true)) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "This is already your Home",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                } else {
                                                                    val updatedList = locations.map {
                                                                        if (it.placeId == location.placeId)
                                                                            it.copy(note = "Home")
                                                                        else
                                                                            it.copy(note = "")
                                                                    }
                                                                    userRepository.updateFavoriteLocations(uid, updatedList)
                                                                    locations = updatedList
                                                                }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.home_house_svgrepo_com),
                                                            contentDescription = "Mark as Home",
                                                            modifier = Modifier.size(28.dp),
                                                            tint = Color.Unspecified
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                userRepository.removeFavoriteLocation(
                                                                    uid,
                                                                    location.placeId
                                                                )
                                                                val updatedUser =
                                                                    userRepository.getUser(uid)
                                                                locations =
                                                                    updatedUser?.favoriteLocations
                                                                        ?: emptyList()
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete location",
                                                            modifier = Modifier.size(28.dp),
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AddLocationDialog(
                        showDialog = showAddDialog,
                        onDismiss = { showAddDialog = false },
                        uid = uid,
                        coroutineScope = coroutineScope,
                        onLocationAdded = { locations = it }
                    )

                    if (showHomeDialog) {
                        AlertDialog(
                            onDismissRequest = { showHomeDialog = false },
                            confirmButton = {
                                TextButton(onClick = { showHomeDialog = false }) {
                                    Text("OK")
                                }
                            },
                            title = { Text("Home Address") },
                            text = {
                                if (homeLocation != null) {
                                    Text(text = homeLocation!!.name)
                                } else {
                                    Text("No location is currently marked as 'Home'.")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

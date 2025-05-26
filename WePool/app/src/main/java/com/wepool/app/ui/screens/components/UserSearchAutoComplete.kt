package com.wepool.app.ui.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun UserSearchAutoComplete(
    modifier: Modifier = Modifier,
    onUserSelected: (uid: String, display: String) -> Unit,
    usersProvider: suspend () -> List<Pair<String, String>>, // uid to "name (email)"
    onClear: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var hrSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    val suggestionMap = remember { mutableStateMapOf<String, Triple<String, String, String>>() }

    var autoExpanded by remember { mutableStateOf(false) }
    var lastEditedField by remember { mutableStateOf("name") }

    fun filterByName(input: String) {
        coroutineScope.launch {
            val allUsers = usersProvider()
            val parsed = allUsers.mapNotNull { (uid, display) ->
                val name = display.substringBefore(" (").trim()
                val email = display.substringAfter("(").removeSuffix(")").trim()
                Triple(uid, name, email).takeIf { name.lowercase().contains(input.lowercase()) }
            }

            suggestionMap.clear()
            parsed.forEach { triple ->
                val (uid, name, email) = triple
                val display = "$name ($email)"
                suggestionMap[display] = Triple(uid, name, email)
            }

            hrSuggestions = suggestionMap.keys.toList()
        }
    }

    fun filterByEmail(input: String) {
        coroutineScope.launch {
            val allUsers = usersProvider()
            val parsed = allUsers.mapNotNull { (uid, display) ->
                val name = display.substringBefore(" (").trim()
                val email = display.substringAfter("(").removeSuffix(")").trim()
                Triple(uid, name, email).takeIf { email.lowercase().contains(input.lowercase()) }
            }

            suggestionMap.clear()
            parsed.forEach { triple ->
                val (uid, name, email) = triple
                val display = "$name ($email)"
                suggestionMap[display] = Triple(uid, name, email)
            }

            hrSuggestions = suggestionMap.keys.toList()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    nameInput = it
                    lastEditedField = "name"
                    filterByName(it)
                    autoExpanded = it.isNotBlank()
                },
                label = { Text("Search by name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        nameInput = ""
                        emailInput = ""
                        autoExpanded = false
                        onClear()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear fields"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = emailInput,
                onValueChange = {
                    emailInput = it
                    lastEditedField = "email"
                    filterByEmail(it)
                    autoExpanded = it.isNotBlank()
                },
                label = { Text("Search by email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                maxLines = 2,
                trailingIcon = {
                    IconButton(onClick = {
                        nameInput = ""
                        emailInput = ""
                        autoExpanded = false
                        onClear()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear fields"
                        )
                    }
                }
            )

            if (autoExpanded && hrSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    LazyColumn {
                        items(hrSuggestions) { suggestion ->
                            ListItem(
                                headlineContent = { Text(suggestion) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val (uid, name, email) = suggestionMap[suggestion]
                                            ?: return@clickable
                                        nameInput = name
                                        emailInput = email
                                        onUserSelected(uid, suggestion)
                                        autoExpanded = false
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

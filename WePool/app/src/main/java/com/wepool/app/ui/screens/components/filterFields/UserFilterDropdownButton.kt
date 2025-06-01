package com.wepool.app.ui.screens.components.filterFields

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wepool.app.data.model.enums.user.UserFilterFields
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.users.UserSearchFilters
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

@Composable
fun UserFilterDropdownButton(
    modifier: Modifier = Modifier,
    availableFilters: List<UserFilterFields>,
    selectedFilters: List<UserFilterFields>,
    onFiltersChanged: (List<UserFilterFields>) -> Unit,
    onQueryChanged: (String?) -> Unit,
    onCompanyCodeChanged: (String?) -> Unit,
    onIsActiveChanged: (Boolean?) -> Unit,
    onClearField: (UserFilterFields) -> Unit,
    limitUserSuggestionsToCompany: Boolean = false,
    usersInCompany: List<User> = emptyList(),
    filters: UserSearchFilters
) {
    val coroutineScope = rememberCoroutineScope()
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(false) }
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }

    var showCompanyField by remember { mutableStateOf(false) }
    var selectedCompanyName by remember { mutableStateOf("") }
    var companySuggestions by remember { mutableStateOf(emptyList<String>()) }

    var showUserField by remember { mutableStateOf(false) }
    var selectedUserQuery by remember { mutableStateOf("") }
    var userSuggestions by remember { mutableStateOf(emptyList<String>()) }

    var showActiveFilter by remember { mutableStateOf(false) }
    var isActiveUser by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(filters) {
        selectedUserQuery = filters.nameOrEmailOrPhone.orEmpty()
        selectedCompanyName = filters.companyCode.orEmpty()
        isActiveUser = filters.isActiveUser
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .onGloballyPositioned { buttonSize = it.size },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (selectedFilters.isEmpty()) "Filter" else "Filters: ${selectedFilters.size}",
                    style = MaterialTheme.typography.labelLarge
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(with(LocalDensity.current) { buttonSize.width.toDp() })
                    .padding(vertical = 4.dp)
            ) {
                availableFilters.forEach { field ->
                    val isSelected = field in selectedFilters
                    DropdownMenuItem(
                        text = {
                            Text(if (isSelected) "✓ ${field.displayName}" else field.displayName)
                        },
                        onClick = {
                            val updated = if (isSelected) selectedFilters - field else selectedFilters + field
                            onFiltersChanged(updated)
                            expanded = false
                        }
                    )
                }
            }

            if (UserFilterFields.USER_NAME in selectedFilters) {
                FilterSection(
                    label = "User",
                    value = selectedUserQuery,
                    visible = showUserField,
                    onShowToggle = { showUserField = it },
                    onClear = {
                        selectedUserQuery = ""
                        userSuggestions = emptyList()
                        onQueryChanged(null)
                        onClearField(UserFilterFields.USER_NAME)
                        onFiltersChanged(selectedFilters - UserFilterFields.USER_NAME)
                    }
                )

                FilterWithTextFieldAndSuggestions(
                    visible = showUserField,
                    label = "Search by name, email or phone",
                    value = selectedUserQuery,
                    suggestions = userSuggestions,
                    onValueChanged = {
                        selectedUserQuery = it
                        coroutineScope.launch {
                            val usersToFilter = if (limitUserSuggestionsToCompany) usersInCompany else userRepository.getAllUsers()
                            userSuggestions = usersToFilter
                                .filter { user ->
                                    user.name.contains(it, ignoreCase = true) ||
                                            user.email.contains(it, ignoreCase = true) ||
                                            user.phoneNumber.contains(it)
                                }
                                .map { "${it.name} (${it.email})" }
                                .distinct()
                        }
                    },
                    onSuggestionSelected = {
                        selectedUserQuery = it
                        onQueryChanged(it)
                        showUserField = false
                        userSuggestions = emptyList()
                    }
                )
            }

            if (UserFilterFields.COMPANY_NAME in selectedFilters) {
                FilterSection(
                    label = "Company",
                    value = selectedCompanyName,
                    visible = showCompanyField,
                    onShowToggle = { showCompanyField = it },
                    onClear = {
                        selectedCompanyName = ""
                        onCompanyCodeChanged(null)
                        onClearField(UserFilterFields.COMPANY_NAME)
                        onFiltersChanged(selectedFilters - UserFilterFields.COMPANY_NAME)
                    }
                )

                FilterWithTextFieldAndSuggestions(
                    visible = showCompanyField,
                    label = "Company",
                    value = selectedCompanyName,
                    suggestions = companySuggestions,
                    onValueChanged = {
                        selectedCompanyName = it
                        coroutineScope.launch {
                            companySuggestions = companyRepository.getAllCompanies()
                                .map { it.companyName }
                                .filter { name -> name.contains(it, ignoreCase = true) }
                        }
                    },
                    onSuggestionSelected = {
                        selectedCompanyName = it
                        onCompanyCodeChanged(it)
                        companySuggestions = emptyList()
                        showCompanyField = false
                    }
                )
            }

            if (UserFilterFields.ACTIVE_USER in selectedFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    FilterChip(
                        selected = showActiveFilter,
                        onClick = { showActiveFilter = !showActiveFilter },
                        label = {
                            Text("Active: ${isActiveUser?.toString() ?: "Any"}")
                        }
                    )
                    IconButton(onClick = {
                        isActiveUser = null
                        onIsActiveChanged(null)
                        showActiveFilter = false
                        onClearField(UserFilterFields.ACTIVE_USER)
                        onFiltersChanged(selectedFilters - UserFilterFields.ACTIVE_USER)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }

                if (showActiveFilter) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            isActiveUser = true
                            onIsActiveChanged(true)
                            showActiveFilter = false
                        }) {
                            Text("Active")
                        }
                        Button(onClick = {
                            isActiveUser = false
                            onIsActiveChanged(false)
                            showActiveFilter = false
                        }) {
                            Text("Inactive")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    label: String,
    value: String,
    visible: Boolean,
    onShowToggle: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        FilterChip(
            selected = visible,
            onClick = { onShowToggle(!visible) },
            label = {
                Text("$label: $value", style = MaterialTheme.typography.bodySmall)
            }
        )
        TextButton(onClick = onClear) {
            Text("Clean", style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
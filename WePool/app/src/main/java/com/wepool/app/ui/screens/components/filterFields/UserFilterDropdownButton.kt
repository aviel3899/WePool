package com.wepool.app.ui.screens.components.filterFields

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.wepool.app.data.model.enums.user.UserRole
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
    onRoleChanged: (UserRole?) -> Unit,
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
    var selectedCompanyCode by remember { mutableStateOf("") }
    var companySuggestions by remember { mutableStateOf(emptyList<String>()) }

    var showPhoneField by remember { mutableStateOf(false) }
    var selectedPhone by remember { mutableStateOf("") }

    var showUserField by remember { mutableStateOf(false) }
    var selectedUserQuery by remember { mutableStateOf("") }
    var userSuggestions by remember { mutableStateOf(emptyList<String>()) }

    var showActiveFilter by remember { mutableStateOf(false) }
    var isActiveUser by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(filters) {
        selectedUserQuery = filters.nameOrEmailOrPhone.orEmpty()
        isActiveUser = filters.isActiveUser
        showUserField = UserFilterFields.USER_NAME in selectedFilters
        showActiveFilter = UserFilterFields.ACTIVE_USER in selectedFilters
        showCompanyField = UserFilterFields.COMPANY_NAME in selectedFilters
        showPhoneField = UserFilterFields.PHONE in selectedFilters

        selectedCompanyCode = filters.companyCode.orEmpty()
        selectedPhone = filters.nameOrEmailOrPhone.orEmpty()
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
                            Text(if (isSelected) "\u2713 ${field.displayName}" else field.displayName)
                        },
                        onClick = {
                            val updated =
                                if (isSelected) selectedFilters - field else selectedFilters + field
                            onFiltersChanged(updated)
                            expanded = false
                        }
                    )
                }
            }

            if (UserFilterFields.COMPANY_NAME in selectedFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = showCompanyField,
                        onClick = { showCompanyField = !showCompanyField },
                        label = {
                            Column {
                                Text(
                                    text = "Company: $selectedCompanyCode",
                                    maxLines = 2,
                                    softWrap = true,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    TextButton(onClick = {
                        selectedCompanyCode = ""
                        showCompanyField = true // ⬅️ פותח את השדה מחדש
                        companySuggestions = emptyList()
                        onClearField(UserFilterFields.COMPANY_NAME)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        selectedCompanyCode = ""
                        companySuggestions = emptyList()
                        showCompanyField = false
                        onClearField(UserFilterFields.COMPANY_NAME)
                        onFiltersChanged(selectedFilters - UserFilterFields.COMPANY_NAME)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                FilterWithTextFieldAndSuggestions(
                    visible = showCompanyField,
                    label = "Company",
                    value = selectedCompanyCode,
                    suggestions = companySuggestions,
                    onValueChanged = {
                        selectedCompanyCode = it
                        onCompanyCodeChanged(it)
                        coroutineScope.launch {
                            val companies = companyRepository.getAllCompanies()
                            companySuggestions = companies.map { it.companyName }
                                .filter { name -> name.contains(it, ignoreCase = true) }
                        }
                    },
                    onSuggestionSelected = {
                        selectedCompanyCode = it
                        onCompanyCodeChanged(it)
                        showCompanyField = false
                        companySuggestions = emptyList()
                    }
                )
            }

            if (UserFilterFields.USER_NAME in selectedFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = showUserField,
                        onClick = { showUserField = !showUserField },
                        modifier = Modifier.widthIn(max = 220.dp),
                        label = {
                            Column {
                                Text(
                                    text = "User: ${selectedUserQuery.takeIf { it.isNotBlank() } ?: ""}",
                                    maxLines = 2,
                                    softWrap = true,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    TextButton(onClick = {
                        selectedUserQuery = ""
                        showUserField = true // ⬅️ פותח את השדה מחדש
                        userSuggestions = emptyList()
                        onClearField(UserFilterFields.USER_NAME)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        selectedUserQuery = ""
                        userSuggestions = emptyList()
                        showUserField = false
                        onClearField(UserFilterFields.USER_NAME)
                        onFiltersChanged(selectedFilters - UserFilterFields.USER_NAME)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                FilterWithTextFieldAndSuggestions(
                    visible = showUserField,
                    label = "Search by name, email or phone",
                    value = selectedUserQuery,
                    suggestions = userSuggestions,
                    onValueChanged = {
                        selectedUserQuery = it
                        coroutineScope.launch {
                            val usersToFilter =
                                if (limitUserSuggestionsToCompany) usersInCompany else userRepository.getAllUsers()
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

            if (UserFilterFields.PHONE in selectedFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = showPhoneField,
                        onClick = { showPhoneField = !showPhoneField },
                        label = {
                            Text(
                                "Phone: ${selectedPhone.takeIf { it.isNotBlank() } ?: ""}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                    TextButton(onClick = {
                        selectedPhone = ""
                        showPhoneField = true // ⬅️ פותח מחדש את השדה לאחר ניקוי
                        onClearField(UserFilterFields.PHONE)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        selectedPhone = ""
                        showPhoneField = false
                        onClearField(UserFilterFields.PHONE)
                        onFiltersChanged(selectedFilters - UserFilterFields.PHONE)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                FilterWithTextFieldAndSuggestions(
                    visible = showPhoneField,
                    label = "Phone",
                    value = selectedPhone,
                    suggestions = emptyList(),
                    onValueChanged = {
                        selectedPhone = it
                        onQueryChanged(it)
                    },
                    onSuggestionSelected = {
                        selectedPhone = it
                        onQueryChanged(it)
                        showPhoneField = false
                    }
                )
            }

            if (UserFilterFields.ACTIVE_USER in selectedFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = showActiveFilter,
                        onClick = { showActiveFilter = !showActiveFilter },
                        label = {
                            Column {
                                Text(
                                    text = "Active: ${isActiveUser?.toString().orEmpty()}",
                                    maxLines = 2,
                                    softWrap = true,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    TextButton(onClick = {
                        isActiveUser = null
                        showActiveFilter = true
                        onIsActiveChanged(null)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        isActiveUser = null
                        showActiveFilter = false
                        onClearField(UserFilterFields.ACTIVE_USER)
                        onFiltersChanged(selectedFilters - UserFilterFields.ACTIVE_USER)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (showActiveFilter) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                isActiveUser = true
                                onIsActiveChanged(true)
                                showActiveFilter = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Active")
                        }
                        Button(
                            onClick = {
                                isActiveUser = false
                                onIsActiveChanged(false)
                                showActiveFilter = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Inactive")
                        }
                    }
                }
            }

            if (UserFilterFields.USER_ROLE in selectedFilters) {
                UserRoleFilterSection(
                    selectedFilters = selectedFilters,
                    role = filters.role,
                    onRoleChanged = onRoleChanged,
                    onClearField = onClearField,
                    onFiltersChanged = onFiltersChanged
                )
            }
        }
    }
}
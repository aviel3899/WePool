package com.wepool.app.ui.screens.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wepool.app.data.model.enums.ride.RideFilterFields
import com.wepool.app.data.model.enums.ride.RideSortFieldsWithOrder
import com.wepool.app.data.model.enums.ride.RideSortFields
import com.wepool.app.data.model.enums.user.UserFilterFields
import com.wepool.app.data.model.enums.user.UserSortFieldWithOrder
import com.wepool.app.data.model.enums.company.CompanyFilterFields
import com.wepool.app.data.model.enums.company.CompanySortFieldWithOrder
import com.wepool.app.data.model.ride.RideSearchFilters
import com.wepool.app.data.model.users.UserSearchFilters
import com.wepool.app.data.model.company.CompanySearchFilters
import com.wepool.app.data.model.enums.user.UserSortFields
import com.wepool.app.data.model.users.RoleOnlyFilters
import com.wepool.app.data.model.users.User
import com.wepool.app.ui.screens.components.filterFields.RideFilterDropdownButton
import com.wepool.app.ui.screens.components.filterFields.UserFilterDropdownButton
import com.wepool.app.ui.screens.components.filterFields.CompanyFilterDropdownButton
import com.wepool.app.ui.screens.components.sortFields.ride.RideSortDropdownButton
import com.wepool.app.ui.screens.components.sortFields.user.UserSortDropdownButton
import com.wepool.app.ui.screens.components.sortFields.company.CompanySortDropdownButton
import kotlinx.coroutines.launch

@Composable
fun <T : Enum<T>> ExpandableCard(
    title: String = "Sort & Filter",
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    showDate: Boolean = true,
    showDepartureTime: Boolean = true,
    showArrivalTime: Boolean = true,
    showAvailableSeats: Boolean = true,
    showCompanyName: Boolean = true,
    showUserName: Boolean = true,
    showSort: Boolean = true,
    showFilter: Boolean = true,
    showCleanAllButton: Boolean = true,
    filters: Any,
    selectedSortFields: List<Any>,
    onSortFieldsChanged: (List<Any>) -> Unit,
    availableFilters: List<T> = emptyList(),
    selectedFilters: List<T> = emptyList(),
    onFiltersChanged: (Any) -> Unit,
    onSearchTriggeredChanged: () -> Unit = {},
    onCleanAllClicked: () -> Unit = {},
    onSelectedFiltersChanged: (List<T>) -> Unit,
    onSearchClicked: () -> Unit,
    usersInCompany: List<User> = emptyList(),
    limitUserSuggestionsToCompany: Boolean = false,
    ridesInCompanyOnly: Boolean = false,
    companyCode: String? = null,
    rideAvailableSortFields: List<RideSortFields> = RideSortFields.values().toList(),
    userAvailableSortFields: List<UserSortFields> = UserSortFields.values().toList(),
    additionalContent: @Composable ColumnScope.() -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .animateContentSize(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }

                @Suppress("UNCHECKED_CAST")
                if (expanded) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

                        if (showFilter) {
                            when (filters) {
                                is RideSearchFilters -> RideFilterDropdownButton(
                                    availableFilters = availableFilters.filterIsInstance<RideFilterFields>(),
                                    selectedFilters = selectedFilters.filterIsInstance<RideFilterFields>(),
                                    onFiltersChanged = { onSelectedFiltersChanged(it as List<T>) },
                                    onCompanyNameChanged = {
                                        onFiltersChanged(filters.copy(companyName = it))
                                    },
                                    onUserQueryChanged = {
                                        onFiltersChanged(filters.copy(userNameOrEmail = it))
                                    },
                                    onDateFromChanged = {
                                        onFiltersChanged(filters.copy(dateFrom = it))
                                    },
                                    onDateToChanged = {
                                        onFiltersChanged(filters.copy(dateTo = it))
                                    },
                                    onTimeFromChanged = {
                                        onFiltersChanged(filters.copy(timeFrom = it))
                                    },
                                    onTimeToChanged = {
                                        onFiltersChanged(filters.copy(timeTo = it))
                                    },
                                    onDirectionChanged = {
                                        onFiltersChanged(filters.copy(direction = it))
                                    },
                                    onClearField = { field ->
                                        val updated = when (field) {
                                            RideFilterFields.COMPANY_NAME -> filters.copy(companyName = null)
                                            RideFilterFields.USER_NAME -> filters.copy(userNameOrEmail = null)
                                            RideFilterFields.DATE_RANGE -> filters.copy(dateFrom = null, dateTo = null)
                                            RideFilterFields.TIME_RANGE -> filters.copy(timeFrom = null, timeTo = null)
                                            RideFilterFields.DIRECTION -> filters.copy(direction = null)
                                            RideFilterFields.PHONE -> filters.copy(userNameOrEmail = null)
                                        }
                                        onFiltersChanged(updated)
                                    },
                                    usersInCompany = usersInCompany,
                                    limitUserSuggestionsToCompany = limitUserSuggestionsToCompany,
                                    filters = filters,
                                )

                                is UserSearchFilters -> UserFilterDropdownButton(
                                    availableFilters = availableFilters.filterIsInstance<UserFilterFields>(),
                                    selectedFilters = selectedFilters.filterIsInstance<UserFilterFields>(),
                                    onFiltersChanged = { onSelectedFiltersChanged(it as List<T>) },
                                    onQueryChanged = {
                                        onFiltersChanged(filters.copy(nameOrEmailOrPhone = it))
                                    },
                                    onCompanyCodeChanged = {
                                        onFiltersChanged(filters.copy(companyCode = it))
                                    },
                                    onIsActiveChanged = {
                                        onFiltersChanged(filters.copy(isActiveUser = it))
                                    },
                                    onRoleChanged = { role ->
                                        onFiltersChanged(filters.copy(role = role))
                                    },
                                    onClearField = { field ->
                                        val updated = when (field) {
                                            UserFilterFields.USER_NAME -> filters.copy(nameOrEmailOrPhone = null)
                                            UserFilterFields.COMPANY_NAME -> filters.copy(companyCode = null)
                                            UserFilterFields.ACTIVE_USER -> filters.copy(isActiveUser = null)
                                            UserFilterFields.PHONE -> filters.copy(nameOrEmailOrPhone = null)
                                            UserFilterFields.USER_ROLE -> filters.copy(role = null)
                                        }
                                        onFiltersChanged(updated)
                                    },
                                    limitUserSuggestionsToCompany = limitUserSuggestionsToCompany,
                                    usersInCompany = usersInCompany,
                                    filters = filters
                                )

                                is RoleOnlyFilters -> {
                                    val tempFilters = UserSearchFilters(role = filters.role)
                                    UserFilterDropdownButton(
                                        availableFilters = availableFilters.filterIsInstance<UserFilterFields>(),
                                        selectedFilters = selectedFilters.filterIsInstance<UserFilterFields>(),
                                        onFiltersChanged = { onSelectedFiltersChanged(it as List<T>) },
                                        onQueryChanged = {},
                                        onCompanyCodeChanged = {},
                                        onIsActiveChanged = {},
                                        onRoleChanged = { role ->
                                            onFiltersChanged(RoleOnlyFilters(role = role))
                                        },
                                        onClearField = { field ->
                                            val updated = when (field) {
                                                UserFilterFields.USER_ROLE -> RoleOnlyFilters(role = null)
                                                else -> filters
                                            }
                                            onFiltersChanged(updated)
                                        },
                                        limitUserSuggestionsToCompany = false,
                                        usersInCompany = emptyList(),
                                        filters = tempFilters
                                    )
                                }

                                is CompanySearchFilters -> CompanyFilterDropdownButton(
                                    availableFilters = availableFilters.filterIsInstance<CompanyFilterFields>(),
                                    selectedFilters = selectedFilters.filterIsInstance<CompanyFilterFields>(),
                                    onFiltersChanged = { onSelectedFiltersChanged(it as List<T>) },
                                    onCompanyNameChanged = {
                                        onFiltersChanged(filters.copy(companyName = it))
                                    },
                                    onClearField = {
                                        onFiltersChanged(filters.copy(companyName = null))
                                    },
                                    filters = filters
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (showSort) {
                            when (filters) {
                                is RideSearchFilters -> RideSortDropdownButton(
                                    selectedSortFields = selectedSortFields.filterIsInstance<RideSortFieldsWithOrder>(),
                                    onSortFieldsChanged = onSortFieldsChanged,
                                    availableSortFields = rideAvailableSortFields
                                )

                                is UserSearchFilters -> UserSortDropdownButton(
                                    selectedSortFields = selectedSortFields.filterIsInstance<UserSortFieldWithOrder>(),
                                    onSortFieldsChanged = onSortFieldsChanged,
                                    availableSortFields = userAvailableSortFields
                                )

                                is RoleOnlyFilters -> RideSortDropdownButton(
                                    selectedSortFields = selectedSortFields.filterIsInstance<RideSortFieldsWithOrder>(),
                                    onSortFieldsChanged = onSortFieldsChanged,
                                    availableSortFields = rideAvailableSortFields
                                )

                                is CompanySearchFilters -> CompanySortDropdownButton(
                                    selectedSortFields = selectedSortFields.filterIsInstance<CompanySortFieldWithOrder>(),
                                    onSortFieldsChanged = onSortFieldsChanged
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        additionalContent()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    if (showCleanAllButton) {
                        OutlinedButton(
                            onClick = {
                                when (filters) {
                                    is RideSearchFilters -> {
                                        onFiltersChanged(
                                            filters.copy(
                                                companyName = null,
                                                userNameOrEmail = null,
                                                dateFrom = null,
                                                dateTo = null,
                                                timeFrom = null,
                                                timeTo = null,
                                                direction = null,
                                                sortFields = emptyList()
                                            )
                                        )
                                        onSortFieldsChanged(emptyList<RideSortFieldsWithOrder>())
                                    }

                                    is UserSearchFilters -> {
                                        onFiltersChanged(
                                            filters.copy(
                                                nameOrEmailOrPhone = null,
                                                companyCode = null,
                                                isActiveUser = null,
                                                sortFields = emptyList(),
                                                role = null
                                            )
                                        )
                                        onSortFieldsChanged(emptyList<UserSortFieldWithOrder>())
                                    }

                                    is RoleOnlyFilters -> {
                                        onFiltersChanged(RoleOnlyFilters())
                                        onSortFieldsChanged(emptyList<RideSortFieldsWithOrder>())
                                    }

                                    is CompanySearchFilters -> {
                                        onFiltersChanged(
                                            filters.copy(
                                                companyName = null,
                                                sortFields = emptyList()
                                            )
                                        )
                                        onSortFieldsChanged(emptyList<CompanySortFieldWithOrder>())
                                    }
                                }
                                onSelectedFiltersChanged(emptyList())
                                onSearchTriggeredChanged()
                                onCleanAllClicked()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clean All")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            onSearchTriggeredChanged()
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(50)
                                onSearchClicked()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Search")
                    }
                }
            }
        }
    }
}

package com.wepool.app.ui.screens.components

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wepool.app.data.model.enums.FilterFields
import com.wepool.app.data.model.enums.SortFields
import com.wepool.app.data.model.ride.RideSearchFilters
import com.wepool.app.data.model.users.User
import com.wepool.app.ui.screens.components.filterFields.RideFilterDropdownButton
import com.wepool.app.ui.screens.components.sortFields.RideSortDropdownButton

@Composable
fun ExpandableSortCard(
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
    filters: RideSearchFilters,
    selectedSortFields: List<SortFields>,
    onSortFieldsChanged: (List<SortFields>) -> Unit,
    availableFilters: List<FilterFields> = emptyList(),
    selectedFilters: List<FilterFields> = emptyList(),
    onFiltersChanged: (RideSearchFilters) -> Unit,
    onSearchTriggeredChanged: () -> Unit = {},
    onCleanAllClicked: () -> Unit = {},
    onSelectedFiltersChanged: (List<FilterFields>) -> Unit,
    onSearchClicked: () -> Unit,
    usersInCompany: List<User> = emptyList(),
    limitUserSuggestionsToCompany: Boolean = false,
    ridesInCompanyOnly: Boolean = false,
    companyCode: String? = null,
    additionalContent: @Composable ColumnScope.() -> Unit = {}
) {
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

                if (expanded) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

                        if (showFilter) {
                            RideFilterDropdownButton(
                                availableFilters = availableFilters,
                                selectedFilters = selectedFilters,
                                onFiltersChanged = onSelectedFiltersChanged,
                                onCompanyNameChanged = {
                                    val updated = filters.copy(companyName = it)
                                    onFiltersChanged(updated)
                                },
                                onUserQueryChanged = {
                                    val updated = filters.copy(userNameOrEmail = it)
                                    onFiltersChanged(updated)
                                },
                                onDateFromChanged = {
                                    val updated = filters.copy(dateFrom = it)
                                    onFiltersChanged(updated)
                                },
                                onDateToChanged = {
                                    val updated = filters.copy(dateTo = it)
                                    onFiltersChanged(updated)
                                },
                                onTimeFromChanged = {
                                    val updated = filters.copy(timeFrom = it)
                                    onFiltersChanged(updated)
                                },
                                onTimeToChanged = {
                                    val updated = filters.copy(timeTo = it)
                                    onFiltersChanged(updated)
                                },
                                onDirectionChanged = {
                                    val updated = filters.copy(direction = it)
                                    onFiltersChanged(updated)
                                },
                                onClearField = { field ->
                                    val updated = when (field) {
                                        FilterFields.COMPANY_NAME -> filters.copy(companyName = null)
                                        FilterFields.USER_NAME -> filters.copy(userNameOrEmail = null)
                                        FilterFields.DATE_RANGE -> filters.copy(dateFrom = null, dateTo = null)
                                        FilterFields.TIME_RANGE -> filters.copy(timeFrom = null, timeTo = null)
                                        FilterFields.DIRECTION -> filters.copy(direction = null)
                                        FilterFields.PHONE -> filters.copy(userNameOrEmail = null)
                                    }
                                    onFiltersChanged(updated)
                                },
                                usersInCompany = usersInCompany,
                                limitUserSuggestionsToCompany = limitUserSuggestionsToCompany,
                                filters = filters,
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (showSort) {
                            RideSortDropdownButton(
                                selectedSortFields = selectedSortFields,
                                onSortFieldsChanged = onSortFieldsChanged,
                                showDate = showDate,
                                showDepartureTime = showDepartureTime,
                                showArrivalTime = showArrivalTime,
                                showAvailableSeats = showAvailableSeats,
                                showCompanyName = showCompanyName,
                                showUserName = showUserName,
                            )

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
                                onFiltersChanged(
                                    RideSearchFilters(
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
                                onSelectedFiltersChanged(emptyList())
                                onSortFieldsChanged(emptyList())
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
                            val missingFields = mutableListOf<String>()
                            if (FilterFields.COMPANY_NAME in selectedFilters && filters.companyName.isNullOrBlank()) {
                                missingFields.add(FilterFields.COMPANY_NAME.displayName)
                            }
                            if (FilterFields.USER_NAME in selectedFilters && filters.userNameOrEmail.isNullOrBlank()) {
                                missingFields.add(FilterFields.USER_NAME.displayName)
                            }
                            if (FilterFields.DATE_RANGE in selectedFilters && (filters.dateFrom.isNullOrBlank() || filters.dateTo.isNullOrBlank())) {
                                missingFields.add(FilterFields.DATE_RANGE.displayName)
                            }
                            if (FilterFields.TIME_RANGE in selectedFilters && (filters.timeFrom.isNullOrBlank() || filters.timeTo.isNullOrBlank())) {
                                missingFields.add(FilterFields.TIME_RANGE.displayName)
                            }
                            if (FilterFields.DIRECTION in selectedFilters && filters.direction == null) {
                                missingFields.add(FilterFields.DIRECTION.displayName)
                            }
                            if (FilterFields.PHONE in selectedFilters && filters.userNameOrEmail.isNullOrBlank()) {
                                missingFields.add(FilterFields.PHONE.displayName)
                            }

                            if (missingFields.isNotEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Please fill in: ${missingFields.joinToString()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            if (ridesInCompanyOnly && !companyCode.isNullOrBlank()) {
                                onFiltersChanged(filters.copy(companyName = companyCode))
                            } else {
                                onFiltersChanged(filters)
                            }

                            onSearchClicked()
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

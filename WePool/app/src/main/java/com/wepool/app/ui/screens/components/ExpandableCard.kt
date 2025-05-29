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
import com.wepool.app.data.model.enums.FilterField
import com.wepool.app.data.model.enums.SortFields
import com.wepool.app.data.model.ride.RideSearchFilters
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
    showUserEmail: Boolean = true,
    selectedSortFields: List<SortFields>,
    onSortFieldsChanged: (List<SortFields>) -> Unit,
    availableFilters: List<FilterField> = emptyList(),
    selectedFilters: List<FilterField> = emptyList(),
    onFiltersChanged: (RideSearchFilters) -> Unit,
    onSelectedFiltersChanged: (List<FilterField>) -> Unit,
    onSearchClicked: () -> Unit,
    additionalContent: @Composable ColumnScope.() -> Unit = {}
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    var filters by remember { mutableStateOf(RideSearchFilters()) }
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

                        RideFilterDropdownButton(
                            availableFilters = availableFilters,
                            selectedFilters = selectedFilters,
                            onFiltersChanged = onSelectedFiltersChanged,
                            onCompanyNameChanged = {
                                filters = filters.copy(companyName = it)
                                onFiltersChanged(filters)
                            },
                            onUserQueryChanged = {
                                filters = filters.copy(userNameOrEmail = it)
                                onFiltersChanged(filters)
                            },
                            onDateFromChanged = {
                                filters = filters.copy(dateFrom = it)
                                onFiltersChanged(filters)
                            },
                            onDateToChanged = {
                                filters = filters.copy(dateTo = it)
                                onFiltersChanged(filters)
                            },
                            onTimeFromChanged = {
                                filters = filters.copy(timeFrom = it)
                                onFiltersChanged(filters)
                            },
                            onTimeToChanged = {
                                filters = filters.copy(timeTo = it)
                                onFiltersChanged(filters)
                            },
                            onDirectionChanged = {
                                filters = filters.copy(direction = it)
                                onFiltersChanged(filters)
                            },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                        additionalContent()
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                filters = RideSearchFilters()
                                onFiltersChanged(filters)
                                onSelectedFiltersChanged(emptyList())
                                onSortFieldsChanged(emptyList())
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clean All")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            val missingFields = mutableListOf<String>()
                            if (FilterField.COMPANY_NAME in selectedFilters && filters.companyName.isNullOrBlank()) {
                                missingFields.add(FilterField.COMPANY_NAME.displayName)
                            }
                            if (FilterField.USER_NAME in selectedFilters && filters.userNameOrEmail.isNullOrBlank()) {
                                missingFields.add(FilterField.USER_NAME.displayName)
                            }
                            if (FilterField.DATE_RANGE in selectedFilters && (filters.dateFrom.isNullOrBlank() || filters.dateTo.isNullOrBlank())) {
                                missingFields.add(FilterField.DATE_RANGE.displayName)
                            }
                            if (FilterField.TIME_RANGE in selectedFilters && (filters.timeFrom.isNullOrBlank() || filters.timeTo.isNullOrBlank())) {
                                missingFields.add(FilterField.TIME_RANGE.displayName)
                            }
                            if (FilterField.DIRECTION in selectedFilters && filters.direction == null) {
                                missingFields.add(FilterField.DIRECTION.displayName)
                            }

                            if (missingFields.isNotEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Please fill in: ${missingFields.joinToString()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
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

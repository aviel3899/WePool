package com.wepool.app.ui.screens.components.filterFields

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.wepool.app.data.model.enums.FilterField
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RideFilterDropdownButton(
    modifier: Modifier = Modifier,
    availableFilters: List<FilterField>,
    selectedFilters: List<FilterField>,
    onFiltersChanged: (List<FilterField>) -> Unit,
    onCompanyNameChanged: (String?) -> Unit,
    onUserQueryChanged: (String?) -> Unit,
    onDateFromChanged: (String?) -> Unit,
    onDateToChanged: (String?) -> Unit,
    onTimeFromChanged: (String?) -> Unit,
    onTimeToChanged: (String?) -> Unit,
    onDirectionChanged: (RideDirection?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val userRepository = RepositoryProvider.provideUserRepository()

    var expanded by remember { mutableStateOf(false) }
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }

    var selectedCompanyName by remember { mutableStateOf("") }
    var showCompanyField by remember { mutableStateOf(false) }
    var companySuggestions by remember { mutableStateOf(emptyList<String>()) }

    var selectedUserQuery by remember { mutableStateOf("") }
    var showUserField by remember { mutableStateOf(false) }
    var userSuggestions by remember { mutableStateOf(emptyList<String>()) }

    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var showDateRange by remember { mutableStateOf(false) }

    var timeFrom by remember { mutableStateOf("") }
    var timeTo by remember { mutableStateOf("") }
    var showTimeRange by remember { mutableStateOf(false) }

    var selectedDirection by remember { mutableStateOf<RideDirection?>(null) }
    var showDirectionField by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

        Column(modifier = modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .onGloballyPositioned { coordinates -> buttonSize = coordinates.size },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                val label =
                    if (selectedFilters.isEmpty()) "Filter" else "Filters: ${selectedFilters.size}"
                Text(label, style = MaterialTheme.typography.labelLarge)
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
                            Text(
                                if (isSelected) "\u2713 ${field.displayName}" else field.displayName
                            )
                        },
                        onClick = {
                            val updatedFilters = if (isSelected)
                                selectedFilters - field else selectedFilters + field
                            onFiltersChanged(updatedFilters)
                            expanded = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (FilterField.COMPANY_NAME in selectedFilters) {
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
                                    text = "Company: $selectedCompanyName",
                                    maxLines = 2,
                                    softWrap = true,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    TextButton(onClick = {
                        selectedCompanyName = ""
                        onCompanyNameChanged(null)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        selectedCompanyName = ""
                        showCompanyField = false
                        onCompanyNameChanged(null)
                        onFiltersChanged(selectedFilters - FilterField.COMPANY_NAME)
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
                    value = selectedCompanyName,
                    suggestions = companySuggestions,
                    onValueChanged = {
                        selectedCompanyName = it
                        onCompanyNameChanged(it)
                        coroutineScope.launch {
                            val companies = companyRepository.getAllCompanies()
                            companySuggestions = companies.map { c -> c.companyName }
                                .filter { name -> name.contains(it, true) }
                        }
                    },
                    onSuggestionSelected = {
                        selectedCompanyName = it
                        onCompanyNameChanged(it)
                        companySuggestions = emptyList()
                        showCompanyField = false
                    }
                )
            }

            if (FilterField.USER_NAME in selectedFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = showUserField,
                        onClick = { showUserField = !showUserField },
                        label = {
                            Column {
                                Text(
                                    text = "User: $selectedUserQuery",
                                    maxLines = 2,
                                    softWrap = true,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    TextButton(onClick = {
                        selectedUserQuery = ""
                        onUserQueryChanged(null)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        selectedUserQuery = ""
                        showUserField = false
                        onUserQueryChanged(null)
                        onFiltersChanged(selectedFilters - FilterField.USER_NAME)
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
                    label = "User",
                    value = selectedUserQuery,
                    suggestions = userSuggestions,
                    onValueChanged = {
                        selectedUserQuery = it
                        onUserQueryChanged(it)
                        coroutineScope.launch {
                            val users = userRepository.getAllUsers()
                            userSuggestions = users.map { u -> "${u.name} (${u.email})" }
                                .filter { label -> label.contains(it, true) }
                        }
                    },
                    onSuggestionSelected = {
                        selectedUserQuery = it
                        onUserQueryChanged(it)
                        userSuggestions = emptyList()
                        showUserField = false
                    }
                )
            }

            if (FilterField.DATE_RANGE in selectedFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = showDateRange,
                        onClick = { showDateRange = !showDateRange },
                        label = {
                            Column {
                                Text(
                                    text = "Date: $dateFrom - $dateTo",
                                    maxLines = 2,
                                    softWrap = true,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    TextButton(onClick = {
                        dateFrom = ""
                        dateTo = ""
                        onDateFromChanged(null)
                        onDateToChanged(null)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        dateFrom = ""
                        dateTo = ""
                        showDateRange = false
                        onDateFromChanged(null)
                        onDateToChanged(null)
                        onFiltersChanged(selectedFilters - FilterField.DATE_RANGE)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                FilterWithRangeButtons(
                    visible = showDateRange,
                    labelPrefix = "Date",
                    fromValue = dateFrom,
                    toValue = dateTo,
                    onFromClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val value = "%04d-%02d-%02d".format(y, m + 1, d)
                                dateFrom = value
                                onDateFromChanged(value)
                            },
                            calendar[Calendar.YEAR],
                            calendar[Calendar.MONTH],
                            calendar[Calendar.DAY_OF_MONTH]
                        ).show()
                    },
                    onToClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val value = "%04d-%02d-%02d".format(y, m + 1, d)
                                if (dateFrom.isNotBlank()) {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val fromDate = sdf.parse(dateFrom)
                                    val toDate = sdf.parse(value)
                                    if (toDate!!.before(fromDate)) {
                                        Toast.makeText(
                                            context,
                                            "\uD83D\uDCC5 'To' date must be after 'From'",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@DatePickerDialog
                                    }
                                }
                                dateTo = value
                                onDateToChanged(value)
                            },
                            calendar[Calendar.YEAR],
                            calendar[Calendar.MONTH],
                            calendar[Calendar.DAY_OF_MONTH]
                        ).show()
                    },
                )
            }

            if (FilterField.TIME_RANGE in selectedFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = showTimeRange,
                        onClick = { showTimeRange = !showTimeRange },
                        label = {
                            Column {
                                Text(
                                    text = "Time: $timeFrom - $timeTo",
                                    maxLines = 2,
                                    softWrap = true,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    TextButton(onClick = {
                        timeFrom = ""
                        timeTo = ""
                        onTimeFromChanged(null)
                        onTimeToChanged(null)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        timeFrom = ""
                        timeTo = ""
                        showTimeRange = false
                        onTimeFromChanged(null)
                        onTimeToChanged(null)
                        onFiltersChanged(selectedFilters - FilterField.TIME_RANGE)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                FilterWithRangeButtons(
                    visible = showTimeRange,
                    labelPrefix = "Time",
                    fromValue = timeFrom,
                    toValue = timeTo,
                    onFromClick = {
                        val calendar = Calendar.getInstance()
                        TimePickerDialog(context, { _, hour, minute ->
                            val value = "%02d:%02d".format(hour, minute)
                            timeFrom = value
                            onTimeFromChanged(value)
                        }, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE], true).show()
                    },
                    onToClick = {
                        val calendar = Calendar.getInstance()
                        TimePickerDialog(context, { _, hour, minute ->
                            val value = "%02d:%02d".format(hour, minute)
                            if (timeFrom.isNotBlank()) {
                                val (fromHour, fromMin) = timeFrom.split(":").map { it.toInt() }
                                val fromTotal = fromHour * 60 + fromMin
                                val toTotal = hour * 60 + minute
                                if (toTotal < fromTotal) {
                                    Toast.makeText(
                                        context,
                                        "\u23F0 'To' must be after 'From'",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@TimePickerDialog
                                }
                            }
                            timeTo = value
                            onTimeToChanged(value)
                        }, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE], true).show()
                    },
                )
            }

            if (FilterField.DIRECTION in selectedFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = showDirectionField,
                        onClick = { showDirectionField = !showDirectionField },
                        label = {
                            Column {
                                Text(
                                    text = "Direction: ${selectedDirection?.name ?: ""}",
                                    maxLines = 2,
                                    softWrap = true,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    TextButton(onClick = {
                        selectedDirection = null
                        onDirectionChanged(null)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        selectedDirection = null
                        showDirectionField = false
                        onDirectionChanged(null)
                        onFiltersChanged(selectedFilters - FilterField.DIRECTION)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (showDirectionField) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                selectedDirection = RideDirection.TO_HOME
                                onDirectionChanged(RideDirection.TO_HOME)
                                showDirectionField = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("To Home")
                        }
                        Button(
                            onClick = {
                                selectedDirection = RideDirection.TO_WORK
                                onDirectionChanged(RideDirection.TO_WORK)
                                showDirectionField = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("To Work")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterWithTextFieldAndSuggestions(
    visible: Boolean,
    label: String,
    value: String,
    suggestions: List<String>,
    onValueChanged: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
) {
    if (!visible) return

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChanged,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth()
            )
            if (suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    LazyColumn {
                        items(suggestions) { suggestion ->
                            ListItem(
                                headlineContent = { Text(suggestion) },
                                modifier = Modifier
                                    .clickable {
                                        onSuggestionSelected(suggestion)
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

@Composable
private fun FilterWithRangeButtons(
    labelPrefix: String,
    fromValue: String,
    toValue: String,
    visible: Boolean,
    onFromClick: () -> Unit,
    onToClick: () -> Unit
) {
    if (!visible) return

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(labelPrefix, style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onFromClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("From${if (fromValue.isNotBlank()) ": $fromValue" else ""}")
                }
                Button(
                    onClick = onToClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("To${if (toValue.isNotBlank()) ": $toValue" else ""}")
                }
            }
        }
    }
}


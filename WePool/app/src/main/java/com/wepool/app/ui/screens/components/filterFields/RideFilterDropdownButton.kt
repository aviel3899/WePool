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
import com.wepool.app.data.model.enums.ride.RideFilterFields
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.ride.RideSearchFilters
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RideFilterDropdownButton(
    modifier: Modifier = Modifier,
    availableFilters: List<RideFilterFields>,
    selectedFilters: List<RideFilterFields>,
    onFiltersChanged: (List<RideFilterFields>) -> Unit,
    onCompanyNameChanged: (String?) -> Unit,
    onUserQueryChanged: (String?) -> Unit,
    onDateFromChanged: (String?) -> Unit,
    onDateToChanged: (String?) -> Unit,
    onTimeFromChanged: (String?) -> Unit,
    onTimeToChanged: (String?) -> Unit,
    onDirectionChanged: (RideDirection?) -> Unit,
    onClearField: (RideFilterFields) -> Unit,
    limitUserSuggestionsToCompany: Boolean = false,
    usersInCompany: List<User> = emptyList(),
    filters: RideSearchFilters,
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

    var selectedPhone by remember { mutableStateOf("") }
    var showPhoneField by remember { mutableStateOf(false) }

    LaunchedEffect(filters) {
        selectedUserQuery = filters.userNameOrEmail ?: ""
        selectedCompanyName = filters.companyName ?: ""
        dateFrom = filters.dateFrom ?: ""
        dateTo = filters.dateTo ?: ""
        timeFrom = filters.timeFrom ?: ""
        timeTo = filters.timeTo ?: ""
        selectedDirection = filters.direction
    }

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

            if (RideFilterFields.COMPANY_NAME in selectedFilters) {
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
                        showCompanyField = true
                        onClearField(RideFilterFields.COMPANY_NAME)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        selectedCompanyName = ""
                        showCompanyField = false
                        onClearField(RideFilterFields.COMPANY_NAME)
                        onFiltersChanged(selectedFilters - RideFilterFields.COMPANY_NAME)
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

            if (RideFilterFields.USER_NAME in selectedFilters) {
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
                        showUserField = true
                        onClearField(RideFilterFields.USER_NAME)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        selectedUserQuery = ""
                        showUserField = false
                        onClearField(RideFilterFields.USER_NAME)
                        onFiltersChanged(selectedFilters - RideFilterFields.USER_NAME)
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
                    label = "Search by name or email",
                    value = selectedUserQuery,
                    suggestions = userSuggestions,
                    onValueChanged = {
                        selectedUserQuery = it
                        coroutineScope.launch {
                            val usersToFilter = if (limitUserSuggestionsToCompany) {
                                usersInCompany
                            } else {
                                userRepository.getAllUsers()
                            }

                            userSuggestions = usersToFilter
                                .filter { user ->
                                    user.name.contains(it, ignoreCase = true) ||
                                            user.email.contains(it, ignoreCase = true)
                                }
                                .map { user -> "${user.name} (${user.email})" }
                                .distinct()
                        }
                    },
                    onSuggestionSelected = {
                        selectedUserQuery = it
                        onUserQueryChanged(it)
                        showUserField = false
                        userSuggestions = emptyList()
                    }
                )
            }

            if (RideFilterFields.PHONE in selectedFilters) {
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
                                text = "Phone: ${selectedPhone.takeIf { it.isNotBlank() } ?: ""}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                    TextButton(onClick = {
                        selectedPhone = ""
                        showPhoneField = true
                        onClearField(RideFilterFields.PHONE)
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        selectedPhone = ""
                        showPhoneField = false
                        onClearField(RideFilterFields.PHONE)
                        onFiltersChanged(selectedFilters - RideFilterFields.PHONE)
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
                        onUserQueryChanged(it)
                    },
                    onSuggestionSelected = {
                        selectedPhone = it
                        onUserQueryChanged(it)
                        showPhoneField = false
                    }
                )
            }

            if (RideFilterFields.DATE_RANGE in selectedFilters) {
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
                                    text = "Date:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (dateFrom.isNotBlank()) {
                                    Text(
                                        text = "From: $dateFrom",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (dateTo.isNotBlank()) {
                                    Text(
                                        text = "To:   $dateTo",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    )

                    TextButton(onClick = {
                        onClearField(RideFilterFields.DATE_RANGE)
                        dateFrom = ""
                        dateTo = ""
                        showDateRange = true
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(onClick = {
                        onClearField(RideFilterFields.DATE_RANGE)
                        showDateRange = false
                        dateFrom = ""
                        dateTo = ""
                        onFiltersChanged(selectedFilters - RideFilterFields.DATE_RANGE)
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
                                showDateRange = false
                            },
                            calendar[Calendar.YEAR],
                            calendar[Calendar.MONTH],
                            calendar[Calendar.DAY_OF_MONTH]
                        ).show()
                    },
                )
            }

            if (RideFilterFields.TIME_RANGE in selectedFilters) {
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
                                Text(text = "Time:", style = MaterialTheme.typography.bodySmall)
                                if (timeFrom.isNotBlank()) {
                                    Text(
                                        text = "From: $timeFrom",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (timeTo.isNotBlank()) {
                                    Text(
                                        text = "To: $timeTo",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    )

                    TextButton(onClick = {
                        onClearField(RideFilterFields.TIME_RANGE)
                        timeFrom = ""
                        timeTo = ""
                        showTimeRange = true
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(onClick = {
                        timeFrom = ""
                        timeTo = ""
                        showTimeRange = false
                        onClearField(RideFilterFields.TIME_RANGE)
                        onFiltersChanged(selectedFilters - RideFilterFields.TIME_RANGE)
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
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val value = "%02d:%02d".format(hour, minute)
                                timeFrom = value
                                onTimeFromChanged(value)
                            },
                            calendar[Calendar.HOUR_OF_DAY],
                            calendar[Calendar.MINUTE],
                            true
                        ).show()
                    },
                    onToClick = {
                        val calendar = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
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
                                showTimeRange = false
                            },
                            calendar[Calendar.HOUR_OF_DAY],
                            calendar[Calendar.MINUTE],
                            true
                        ).show()
                    }
                )
            }

            if (RideFilterFields.DIRECTION in selectedFilters) {
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
                        onClearField(RideFilterFields.DIRECTION)
                        showDirectionField = true
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        selectedDirection = null
                        showDirectionField = false
                        onClearField(RideFilterFields.DIRECTION)
                        onFiltersChanged(selectedFilters - RideFilterFields.DIRECTION)
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
fun FilterWithTextFieldAndSuggestions(
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
fun FilterWithRangeButtons(
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
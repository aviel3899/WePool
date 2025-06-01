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
import com.wepool.app.data.model.enums.company.CompanyFilterFields
import com.wepool.app.data.model.company.CompanySearchFilters
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

@Composable
fun CompanyFilterDropdownButton(
    modifier: Modifier = Modifier,
    availableFilters: List<CompanyFilterFields>,
    selectedFilters: List<CompanyFilterFields>,
    onFiltersChanged: (List<CompanyFilterFields>) -> Unit,
    onCompanyNameChanged: (String?) -> Unit,
    onClearField: (CompanyFilterFields) -> Unit,
    filters: CompanySearchFilters
) {
    val coroutineScope = rememberCoroutineScope()
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(false) }
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }

    var showCompanyField by remember { mutableStateOf(false) }
    var selectedCompanyName by remember { mutableStateOf("") }
    var companySuggestions by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(filters) {
        selectedCompanyName = filters.companyName.orEmpty()
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
                            Text(if (isSelected) "✓ ${field.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}" else field.name)
                        },
                        onClick = {
                            val updated = if (isSelected) selectedFilters - field else selectedFilters + field
                            onFiltersChanged(updated)
                            expanded = false
                        }
                    )
                }
            }

            if (CompanyFilterFields.COMPANY_NAME in selectedFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = showCompanyField,
                        onClick = { showCompanyField = !showCompanyField },
                        label = {
                            Text("Company: $selectedCompanyName", style = MaterialTheme.typography.bodySmall)
                        }
                    )
                    TextButton(onClick = {
                        selectedCompanyName = ""
                        companySuggestions = emptyList()
                        onCompanyNameChanged(null)
                        showCompanyField = true
                    }) {
                        Text("Clean", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        selectedCompanyName = ""
                        companySuggestions = emptyList()
                        showCompanyField = false
                        onCompanyNameChanged(null)
                        onClearField(CompanyFilterFields.COMPANY_NAME)
                        if (CompanyFilterFields.COMPANY_NAME in selectedFilters) {
                            onFiltersChanged(selectedFilters - CompanyFilterFields.COMPANY_NAME)
                        }
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
                        coroutineScope.launch {
                            companySuggestions = companyRepository.getAllCompanies()
                                .map { it.companyName }
                                .filter { name -> name.contains(it, ignoreCase = true) }
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
        }
    }
}

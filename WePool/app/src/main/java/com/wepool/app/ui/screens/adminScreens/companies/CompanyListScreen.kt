package com.wepool.app.ui.screens.adminScreens.companies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wepool.app.data.model.company.Company
import com.wepool.app.data.model.company.CompanySearchFilters
import com.wepool.app.data.model.enums.company.CompanyFilterFields
import com.wepool.app.data.model.enums.company.CompanySortFieldWithOrder
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.adminScreens.AdminAddCompanyDialog
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.ExpandableCard
import com.wepool.app.ui.screens.components.sortFields.company.CompanySortManager
import kotlinx.coroutines.launch

@Composable
fun CompanyListScreen(uid: String, navController: NavController) {
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
    val coroutineScope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    var filteredCompanies by remember { mutableStateOf<List<Company>>(emptyList()) }
    var filters by remember { mutableStateOf(CompanySearchFilters()) }
    var selectedFilters by remember { mutableStateOf<List<CompanyFilterFields>>(emptyList()) }
    var selectedSortFields by remember { mutableStateOf<List<CompanySortFieldWithOrder>>(emptyList()) }
    var hasFilterBeenApplied by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            companies = companyRepository.getAllCompanies()
            users = userRepository.getAllUsers()
        } catch (e: Exception) {
            error = "\u274C Error loading companies: ${e.message}"
        }
    }

    fun refreshCompanies() {
        coroutineScope.launch {
            try {
                loading = true
                error = null

                val all = companyRepository.getAllCompanies()
                companies = all

                val nameQuery = filters.companyName.orEmpty().trim()

                val filtered = all.filter { company ->
                    val matchesName = nameQuery.isBlank() || company.companyName.contains(
                        nameQuery,
                        ignoreCase = true
                    )

                    selectedFilters.all { field ->
                        when (field) {
                            CompanyFilterFields.COMPANY_NAME -> matchesName
                            else -> true
                        }
                    }
                }

                val sorted = CompanySortManager.sortCompanies(filtered, selectedSortFields)

                filteredCompanies = sorted
                hasFilterBeenApplied = true
            } catch (e: Exception) {
                error = "\u274C Error loading companies: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    BackgroundWrapper {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {

                    ExpandableCard(
                        selectedSortFields = selectedSortFields,
                        onSortFieldsChanged = {
                            selectedSortFields = it.filterIsInstance<CompanySortFieldWithOrder>()
                            filters = filters.copy(sortFields = selectedSortFields)
                        },
                        availableFilters = listOf(CompanyFilterFields.COMPANY_NAME),
                        selectedFilters = selectedFilters,
                        onSelectedFiltersChanged = {
                            selectedFilters = it.filterIsInstance<CompanyFilterFields>()
                        },
                        filters = filters,
                        onFiltersChanged = {
                            if (it is CompanySearchFilters) filters = it
                        },
                        onSearchClicked = { refreshCompanies() },
                        showDate = false,
                        showArrivalTime = false,
                        showAvailableSeats = false,
                        showDepartureTime = false,
                        showCompanyName = true,
                        showUserName = false,
                        showSort = true,
                        showFilter = true,
                        showCleanAllButton = true,
                        usersInCompany = users,
                        limitUserSuggestionsToCompany = false,
                        onSearchTriggeredChanged = { hasFilterBeenApplied = false },
                        onCleanAllClicked = {
                            filters = CompanySearchFilters()
                            selectedFilters = emptyList()
                            selectedSortFields = emptyList()
                            filteredCompanies = emptyList()
                            hasFilterBeenApplied = false

                            coroutineScope.launch {
                                kotlinx.coroutines.delay(50)
                                hasFilterBeenApplied = false
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        error?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        when {
                            !hasFilterBeenApplied -> Text(
                                "Please enter filters and press Search.",
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))

                            filteredCompanies.isEmpty() -> Text(
                                "No companies found.",
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            else -> {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filteredCompanies) { company ->
                                        CompanyCard(
                                            company = company,
                                            onCompanyUpdated = { refreshCompanies() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        BottomNavigationButtons(
                            uid = uid,
                            navController = navController,
                            showBackButton = true,
                            showHomeButton = true,
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 32.dp, bottom = 96.dp)
                ) {
                    Text("+", fontSize = 24.sp)
                }

                if (showAddDialog) {
                    AdminAddCompanyDialog(
                        uid = uid,
                        showDialog = showAddDialog,
                        onDismiss = {
                            showAddDialog = false
                            refreshCompanies()
                        }
                    )
                }
            }
        }
    }
}
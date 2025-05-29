package com.wepool.app.ui.screens.adminScreens.users

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
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.FilterFields
import com.wepool.app.data.model.enums.SortFields
import com.wepool.app.data.model.ride.RideSearchFilters
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.ExpandableSortCard
import com.wepool.app.data.model.company.Company
import kotlinx.coroutines.launch

@Composable
fun UserListScreen(uid: String, navController: NavController) {
    val userRepository = RepositoryProvider.provideUserRepository()
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val coroutineScope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var filteredUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    var companyMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var filters by remember { mutableStateOf(RideSearchFilters()) }
    var selectedFilters by remember { mutableStateOf<List<FilterFields>>(emptyList()) }
    var searchTriggered by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                loading = true
                users = userRepository.getAllUsers()
                companies = companyRepository.getAllCompanies()
                companyMap = companies.associateBy({ it.companyCode }, { it.companyName })
                filteredUsers = emptyList()
            } catch (e: Exception) {
                error = "\u274C Failed to load users or companies: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    fun applyUserSearch(allUsers: List<User>) {
        val query = filters.userNameOrEmail.orEmpty().trim()
        val companyQuery = filters.companyName.orEmpty().trim()

        val filtered = if (selectedFilters.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { user ->
                val matchesQuery =
                    if (FilterFields.USER_NAME !in selectedFilters && FilterFields.PHONE !in selectedFilters) {
                        true
                    } else {
                        query.isBlank() ||
                                user.name.contains(query, ignoreCase = true) ||
                                user.email.contains(query, ignoreCase = true) ||
                                "${user.name} (${user.email})".contains(query, ignoreCase = true) ||
                                user.phoneNumber.contains(query)
                    }

                val matchesCompany =
                    if (FilterFields.COMPANY_NAME !in selectedFilters) {
                        true
                    } else {
                        val companyName = companyMap[user.companyCode].orEmpty()
                        companyQuery.isBlank() || companyName.contains(companyQuery, ignoreCase = true)
                    }

                matchesQuery && matchesCompany
            }
        }

        filteredUsers = when (filters.sortFields.firstOrNull()) {
            SortFields.USER -> filtered.sortedByDescending { it.name }
            SortFields.COMPANY_NAME -> filtered.sortedByDescending {
                companyMap[it.companyCode].orEmpty()
            }
            else -> filtered
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 96.dp)) {
                ExpandableSortCard(
                    title = "Search User",
                    filters = filters,
                    selectedSortFields = filters.sortFields,
                    onSortFieldsChanged = { filters = filters.copy(sortFields = it) },
                    availableFilters = listOf(
                        FilterFields.USER_NAME,
                        FilterFields.COMPANY_NAME,
                        FilterFields.PHONE
                    ),
                    selectedFilters = selectedFilters,
                    onSelectedFiltersChanged = { selectedFilters = it },
                    onFiltersChanged = { filters = it },
                    onSearchClicked = {
                        searchTriggered = true
                        applyUserSearch(users)
                    },
                    showDate = false,
                    showDepartureTime = false,
                    showArrivalTime = false,
                    showAvailableSeats = false,
                    showCompanyName = true,
                    showUserName = true,
                    showSort = true,
                    showFilter = true,
                    showCleanAllButton = true,
                    usersInCompany = users,
                    limitUserSuggestionsToCompany = false,
                    onSearchTriggeredChanged = { searchTriggered = false },
                    onCleanAllClicked = {
                        filters = RideSearchFilters()
                        selectedFilters = emptyList()
                        filteredUsers = emptyList()
                        searchTriggered = false
                        coroutineScope.launch { kotlinx.coroutines.delay(50) }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    when {
                        loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        error != null -> Text(text = error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.CenterHorizontally))
                        !searchTriggered -> Text("Please enter a search and press Search.", modifier = Modifier.align(Alignment.CenterHorizontally))
                        filteredUsers.isEmpty() -> Text("No users found.", modifier = Modifier.align(Alignment.CenterHorizontally))
                        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredUsers) { user ->
                                UserCard(
                                    user = user,
                                    navController = navController,
                                    onUserStatusChanged = {
                                        coroutineScope.launch {
                                            users = userRepository.getAllUsers()
                                            applyUserSearch(users)
                                        }
                                    },
                                    fromScreen = "UserList"
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                BottomNavigationButtons(
                    uid = uid,
                    navController = navController,
                    showBackButton = true,
                    showHomeButton = true
                )
            }
        }
    }
}

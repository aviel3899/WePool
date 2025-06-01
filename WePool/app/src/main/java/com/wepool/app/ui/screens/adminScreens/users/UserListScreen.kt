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
import com.wepool.app.data.model.enums.user.UserFilterFields
import com.wepool.app.data.model.enums.user.UserSortFields
import com.wepool.app.data.model.enums.SortOrder
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.users.UserSearchFilters
import com.wepool.app.data.model.company.Company
import com.wepool.app.data.model.enums.user.UserSortFieldWithOrder
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.ExpandableCard
import com.wepool.app.ui.screens.components.sortFields.user.UserSortManager
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
    var filters by remember {
        mutableStateOf(
            UserSearchFilters(
                sortFields = emptyList()
            )
        )
    }
    var selectedFilters by remember { mutableStateOf<List<UserFilterFields>>(emptyList()) }
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
        val query = filters.nameOrEmailOrPhone.orEmpty().trim()
        val companyQuery = filters.companyCode.orEmpty().trim()

        val filtered = allUsers.filter { user ->
            val matchesQuery =
                if (UserFilterFields.USER_NAME !in selectedFilters && UserFilterFields.PHONE !in selectedFilters) {
                    true
                } else {
                    query.isBlank() ||
                            user.name.contains(query, ignoreCase = true) ||
                            user.email.contains(query, ignoreCase = true) ||
                            "${user.name} (${user.email})".contains(query, ignoreCase = true) ||
                            user.phoneNumber.contains(query)
                }

            val matchesCompany =
                if (UserFilterFields.COMPANY_NAME !in selectedFilters) {
                    true
                } else {
                    val companyName = companyMap[user.companyCode].orEmpty()
                    companyQuery.isBlank() || companyName.contains(companyQuery, ignoreCase = true)
                }

            val matchesStatus =
                filters.isActiveUser == null || user.active == filters.isActiveUser

            matchesQuery && matchesCompany && matchesStatus
        }

        filteredUsers = UserSortManager.sortUsers(
            filtered,
            filters.sortFields,
            companyMap
        )
    }

    BackgroundWrapper {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxSize()
                        .padding(bottom = 96.dp)
                ) {
                    ExpandableCard(
                        title = "Search User",
                        filters = filters,
                        selectedSortFields = filters.sortFields,
                        onSortFieldsChanged = { sortFields ->
                            filters =
                                filters.copy(sortFields = sortFields.filterIsInstance<UserSortFieldWithOrder>())
                        },
                        availableFilters = listOf(
                            UserFilterFields.USER_NAME,
                            UserFilterFields.COMPANY_NAME,
                            UserFilterFields.PHONE,
                            UserFilterFields.ACTIVE_USER
                        ),
                        selectedFilters = selectedFilters,
                        onSelectedFiltersChanged = {
                            selectedFilters = it.filterIsInstance<UserFilterFields>()
                        },
                        onFiltersChanged = { updated ->
                            if (updated is UserSearchFilters) {
                                filters = updated
                            }
                        },
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
                            filters = UserSearchFilters()
                            selectedFilters = emptyList()
                            filteredUsers = emptyList()
                            searchTriggered = false
                            coroutineScope.launch { kotlinx.coroutines.delay(50) }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        when {
                            loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            error != null -> Text(
                                text = error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            !searchTriggered -> Text(
                                "Please enter filters and press Search.",
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            filteredUsers.isEmpty() -> Text(
                                "No users found.",
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

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
}

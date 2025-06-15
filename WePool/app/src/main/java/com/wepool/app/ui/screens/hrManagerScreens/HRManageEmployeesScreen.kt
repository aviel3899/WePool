package com.wepool.app.ui.screens.hrManagerScreens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.user.UserFilterFields
import com.wepool.app.data.model.enums.user.UserSortFieldWithOrder
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.users.UserSearchFilters
import com.wepool.app.data.model.company.Company
import com.wepool.app.data.model.enums.user.UserSortFields
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.ExpandableCard
import com.wepool.app.ui.screens.adminScreens.users.UserCard
import com.wepool.app.ui.screens.components.sortFields.user.UserSortManager
import kotlinx.coroutines.launch

@Composable
fun HRManageEmployeesScreen(uid: String, navController: NavController) {
    val context = LocalContext.current

    val userRepository = RepositoryProvider.provideUserRepository()
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val coroutineScope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var filteredUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var company by remember { mutableStateOf<Company?>(null) }
    var filters by remember { mutableStateOf(UserSearchFilters()) }
    var selectedFilters by remember { mutableStateOf<List<UserFilterFields>>(emptyList()) }
    var searchTriggered by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun applySearch(allUsers: List<User>) {
        val query = filters.nameOrEmailOrPhone.orEmpty().trim()

        val filtered = allUsers.filter { user ->
            val matchesUser =
                if (UserFilterFields.USER_NAME in selectedFilters || UserFilterFields.PHONE in selectedFilters) {
                    query.isBlank() ||
                            user.name.contains(query, ignoreCase = true) ||
                            user.email.contains(query, ignoreCase = true) ||
                            "${user.name} (${user.email})".contains(query, ignoreCase = true) ||
                            user.phoneNumber.contains(query)
                } else true

            val matchesStatus = filters.isActiveUser == null || user.active == filters.isActiveUser

            matchesUser && matchesStatus
        }

        val filteredSortFields =
            filters.sortFields.filter { it.field != UserSortFields.COMPANY_NAME }

        filteredUsers = UserSortManager.sortUsers(
            filtered,
            filteredSortFields,
            emptyMap()
        )
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                loading = true
                company = companyRepository.getCompanyByHrUid(uid)
                users =
                    company?.let { userRepository.getUsersByCompany(it.companyCode) } ?: emptyList()
                filteredUsers = emptyList()
            } catch (e: Exception) {
                error = "\u274C Failed to load employees or company info: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    BackgroundWrapper {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp)) {
                    ExpandableCard(
                        title = "Search Employees",
                        filters = filters,
                        selectedSortFields = filters.sortFields.filter { it.field != UserSortFields.COMPANY_NAME },
                        onSortFieldsChanged = { sortFields ->
                            filters = filters.copy(
                                sortFields = sortFields
                                    .filterIsInstance<UserSortFieldWithOrder>()
                                    .filter { it.field != UserSortFields.COMPANY_NAME }
                            )
                        },
                        availableFilters = listOf(
                            UserFilterFields.USER_NAME,
                            UserFilterFields.PHONE,
                            UserFilterFields.ACTIVE_USER
                        ),
                        userAvailableSortFields = listOf(
                            UserSortFields.USER_NAME,
                            UserSortFields.USER_EMAIL,
                            UserSortFields.USER_PHONE
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
                            val query = filters.nameOrEmailOrPhone.orEmpty().trim()

                            val isNameOrPhoneSelected =
                                selectedFilters.contains(UserFilterFields.USER_NAME) || selectedFilters.contains(
                                    UserFilterFields.PHONE
                                )
                            val isActiveSelected =
                                selectedFilters.contains(UserFilterFields.ACTIVE_USER)

                            if (isNameOrPhoneSelected && query.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Please enter a name, email, or phone number to search.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@ExpandableCard
                            }

                            if (isActiveSelected && filters.isActiveUser == null) {
                                Toast.makeText(
                                    context,
                                    "Please select a user status to search.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@ExpandableCard
                            }

                            searchTriggered = true
                            applySearch(users)
                        },
                        showDate = false,
                        showDepartureTime = false,
                        showArrivalTime = false,
                        showAvailableSeats = false,
                        showCompanyName = false,
                        showUserName = true,
                        showSort = true,
                        showFilter = true,
                        showCleanAllButton = true,
                        usersInCompany = users,
                        limitUserSuggestionsToCompany = true,
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

                    Column(modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)) {
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
                                "No employees found.",
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredUsers) { user ->
                                    UserCard(
                                        user = user,
                                        navController = navController,
                                        onUserStatusChanged = {
                                            coroutineScope.launch {
                                                users = company?.let {
                                                    userRepository.getUsersByCompany(it.companyCode)
                                                } ?: emptyList()
                                                applySearch(users)
                                            }
                                        },
                                        fromScreen = "HRManageEmployees"
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

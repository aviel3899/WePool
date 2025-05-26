package com.wepool.app.ui.screens.adminScreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wepool.app.data.model.company.Company
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import kotlinx.coroutines.launch

@Composable
fun CompanyListScreen(uid: String, navController: NavController) {
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val coroutineScope = rememberCoroutineScope()

    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    var filteredCompanies by remember { mutableStateOf<List<Company>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchInput by remember { mutableStateOf("") }
    var filterExpanded by remember { mutableStateOf(true) }
    var hasFilterBeenApplied by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var autoExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            companies = companyRepository.getAllCompanies()
        } catch (e: Exception) {
            error = "\u274C Error loading companies: ${e.message}"
        }
    }

    fun filterCompanies(input: String) {
        val queryWords = input.trim().lowercase().split(" ").filter { it.isNotBlank() }
        suggestions = if (queryWords.isNotEmpty()) {
            companies.map { it.companyName }.filter { name ->
                val companyWords = name.lowercase().split(" ")
                queryWords.any { q -> companyWords.any { c -> c.contains(q) || q.contains(c) } }
            }
        } else emptyList()
    }

    fun refreshCompanies() {
        coroutineScope.launch {
            try {
                loading = true
                error = null
                companies = companyRepository.getAllCompanies()
                val queryWords =
                    searchInput.trim().lowercase().split(" ").filter { it.isNotBlank() }
                filteredCompanies = if (queryWords.isEmpty()) {
                    companies
                } else {
                    companies.filter { company ->
                        val companyWords = company.companyName.lowercase().split(" ")
                        queryWords.any { q -> companyWords.any { c -> c.contains(q) || q.contains(c) } }
                    }
                }
                suggestions = emptyList()
                hasFilterBeenApplied = true
            } catch (e: Exception) {
                error = "\u274C שגיאה בטעינת חברות: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = { filterExpanded = !filterExpanded },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = if (filterExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }

                            Text(
                                text = "Search Company",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        if (filterExpanded) {
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = searchInput,
                                onValueChange = {
                                    searchInput = it
                                    filterCompanies(it)
                                    autoExpanded = it.isNotBlank() && suggestions.isNotEmpty()
                                },
                                label = {
                                    Text(
                                        text = "Enter company name",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(0.85f),
                                singleLine = false,
                                maxLines = Int.MAX_VALUE,
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center
                                ),
                                trailingIcon = {
                                    if (searchInput.isNotBlank()) {
                                        IconButton(onClick = {
                                            searchInput = ""
                                            suggestions = emptyList()
                                            autoExpanded = false
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            )

                            if (autoExpanded && suggestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .heightIn(max = 200.dp)
                                ) {
                                    LazyColumn {
                                        items(suggestions) { suggestion ->
                                            ListItem(
                                                headlineContent = { Text(suggestion) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        searchInput = suggestion
                                                        suggestions = emptyList()
                                                    }
                                                    .padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { refreshCompanies() },
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Search")
                            }
                        }
                    }
                }

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
                            "Please enter a search and press Refresh.",
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

                BottomNavigationButtons(
                    uid = uid,
                    navController = navController,
                    showBackButton = true,
                    showHomeButton = true
                )
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

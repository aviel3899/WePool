package com.wepool.app.ui.screens.adminScreens.companies

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wepool.app.R
import com.wepool.app.data.model.company.Company
import com.wepool.app.data.model.users.HRManager
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.UserSearchAutoComplete
import kotlinx.coroutines.launch

@Composable
fun CompanyCard(
    company: Company,
    onCompanyUpdated: () -> Unit
) {
    val userRepository = RepositoryProvider.provideUserRepository()
    val hrManagerRepository = RepositoryProvider.provideHRManagerRepository()
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val coroutineScope = rememberCoroutineScope()

    var showDetails by remember { mutableStateOf(false) }
    var showRemove by remember { mutableStateOf(false) }
    var showSetHr by remember { mutableStateOf(false) }
    var showEmployees by remember { mutableStateOf(false) }

    var selectedHrUid by remember { mutableStateOf<String?>(null) }
    var employeeNames by remember { mutableStateOf<List<String>>(emptyList()) }

    var showHrConfirmationDialog by remember { mutableStateOf(true) }

    fun removeCompany(context: Context) {
        coroutineScope.launch {
            try {
                companyRepository.deleteCompanyById(company.companyId, hrManagerRepository)
                Toast.makeText(context, "🗑️ Company removed successfully", Toast.LENGTH_SHORT)
                    .show()
                onCompanyUpdated()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "❌ Failed to remove company: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun setHrManager(context: Context) {
        coroutineScope.launch {
            try {
                val hrManagerUid = selectedHrUid ?: return@launch

                if (!company.employees.contains(hrManagerUid)) {
                    companyRepository.addEmployeeToCompany(company.companyId, hrManagerUid)
                    userRepository.updateUserCompanyCode(hrManagerUid, company.companyCode)
                    Toast.makeText(context, "✅ HR added as employee", Toast.LENGTH_SHORT).show()
                }

                companyRepository.setHrManager(
                    companyId = company.companyId,
                    hrManagerUid = hrManagerUid,
                    hrManagerRepository = hrManagerRepository
                )

                val updatedUser = userRepository.getUser(hrManagerUid) ?: return@launch

                val hrManager = HRManager(
                    user = updatedUser,
                    managedCompanyId = company.companyId
                )
                hrManagerRepository.saveHRManager(hrManagerUid, hrManager)

                onCompanyUpdated()

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "❌ Failed to set HR Manager: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun loadEmployeeNames() {
        coroutineScope.launch {
            val names = company.employees.mapNotNull { uid ->
                try {
                    userRepository.getUser(uid)?.name
                } catch (e: Exception) {
                    null
                }
            }
            employeeNames = names
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = company.companyName,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        IconButton(onClick = {
                            loadEmployeeNames()
                            showEmployees = true
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.employees_svgrepo_com),
                                contentDescription = "Employees",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Unspecified
                            )
                        }

                        IconButton(onClick = { showSetHr = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.hr_manager_svgrepo_com),
                                contentDescription = "Set HR",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Unspecified
                            )
                        }

                        IconButton(onClick = { showDetails = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.information_svgrepo_com),
                                contentDescription = "Details",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Unspecified
                            )
                        }

                        IconButton(onClick = { showRemove = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.trash_svgrepo_com),
                                contentDescription = "Remove",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }

                }
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (showRemove) {
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = { showRemove = false },
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Confirm Removal",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = { Text("Are you sure you want to remove ${company.companyName}?") },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                removeCompany(context)
                                showRemove = false
                            }
                        ) { Text("Remove") }

                        TextButton(onClick = { showRemove = false }) { Text("Cancel") }
                    }
                },
                dismissButton = {}
            )
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (showDetails) {
            AlertDialog(
                onDismissRequest = { showDetails = false },
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Company Details",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = {
                    Column {
                        Text("Name: ${company.companyName}")
                        Text("Code: ${company.companyCode}")
                        Text("Active: ${if (company.active) "Yes" else "No"}")
                        Text("Created: ${company.createdAt.toDate()}")
                        company.location?.let {
                            Text("Location: ${it.name}")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetails = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (showSetHr) {
            if (showHrConfirmationDialog) {
                val currentHrUid = company.hrManagerUid
                val currentHrName by produceState<String?>(initialValue = null) {
                    value = currentHrUid?.let {
                        try {
                            userRepository.getUser(it)?.name
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                AlertDialog(
                    onDismissRequest = { showSetHr = false },
                    title = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Current HR Manager",
                                modifier = Modifier.align(Alignment.Center), // מרכז את הכותרת
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            currentHrName?.let { name ->
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "is the current HR manager",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            } ?: Text(
                                text = "No HR manager is currently assigned.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween // כפתורים בצדדים מנוגדים
                        ) {
                            TextButton(onClick = { showHrConfirmationDialog = false }) { Text("Change") }
                            TextButton(onClick = {
                                showSetHr = false
                                showHrConfirmationDialog = true
                            }) { Text("Cancel") }
                        }
                    }
                )
            } else {
                AlertDialog(
                    onDismissRequest = {
                        showSetHr = false
                        showHrConfirmationDialog = true
                    },
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Select HR Manager")
                        }
                    },
                    text = {
                        UserSearchAutoComplete(
                            onUserSelected = { uid, _ -> selectedHrUid = uid },
                            onClear = { selectedHrUid = null },
                            usersProvider = {
                                try {
                                    userRepository.getAllUsers().map { user ->
                                        user.uid to "${user.name} (${user.email})"
                                    }
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                        )
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween // כפתורים בצדדים מנוגדים
                        ) {
                            val context = LocalContext.current

                            TextButton(onClick = {
                                setHrManager(context)
                                showSetHr = false
                                showHrConfirmationDialog = true
                            }) {
                                Text("Confirm")
                            }

                            TextButton(onClick = {
                                showSetHr = false
                                showHrConfirmationDialog = true
                            }) {
                                Text("Cancel")
                            }
                        }
                    }
                )
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (showEmployees) {
            AlertDialog(
                onDismissRequest = { showEmployees = false },
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Employees",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = {
                    Column {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Total: ${employeeNames.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        employeeNames.forEach { name ->
                            Text(
                                text = name,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showEmployees = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }

}

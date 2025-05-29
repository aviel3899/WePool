package com.wepool.app.ui.screens.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wepool.app.data.model.company.Company
import com.wepool.app.data.model.users.User
import com.wepool.app.data.repository.interfaces.ICompanyRepository
import com.wepool.app.data.repository.interfaces.IUserRepository
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

@Composable
fun RideDataProvider(
    userRepository: IUserRepository = RepositoryProvider.provideUserRepository(),
    companyRepository: ICompanyRepository = RepositoryProvider.provideCompanyRepository(),
    content: @Composable (
        userMap: Map<String, User>,
        companyNameMap: Map<String, String>
    ) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var userMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var companyNameMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val users = userRepository.getAllUsers()
                val companies = companyRepository.getAllCompanies()
                userMap = users.associateBy { it.uid }
                companyNameMap = companies.associate { it.companyCode to it.companyName }
            } catch (e: Exception) {
                errorMessage = "Failed to load ride-related data"
            } finally {
                isLoading = false
            }
        }
    }

    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        errorMessage != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage ?: "Unknown error")
            }
        }

        else -> {
            content(userMap, companyNameMap)
        }
    }
}

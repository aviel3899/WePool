package com.wepool.app.ui.screens.adminScreens

import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.wepool.app.data.model.company.Company
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

@Composable
fun AdminRemoveCompanyDialog(
    showDialog: Boolean,
    companyToRemove: Company?,
    onDismiss: () -> Unit,
    onCompanyRemoved: () -> Unit
) {
    if (!showDialog || companyToRemove == null) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val companyRepository = RepositoryProvider.provideCompanyRepository()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Company") },
        text = { Text("Are you sure you want to delete '${companyToRemove.companyName}'?") },
        confirmButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        companyRepository.deleteCompanyById(companyToRemove.companyId)
                        Toast.makeText(context, "🗑️ Company removed successfully", Toast.LENGTH_SHORT).show()
                        onCompanyRemoved()
                        onDismiss()
                    }
                }
            ) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}

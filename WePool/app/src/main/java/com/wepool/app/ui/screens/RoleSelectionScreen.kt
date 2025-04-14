package com.wepool.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wepool.app.data.model.users.User

@Composable
fun RoleSelectionScreen(
    user: User,
    onRoleSelected: (String) -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Select Your Role",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            user.roles.forEach { role ->
                Button(
                    onClick = { onRoleSelected(role) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(role)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Cancel")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoleSelectionScreenPreview() {
    val previewUser = User(
        uid = "preview123",
        name = "Jane Doe",
        email = "jane@wepool.com",
        roles = listOf("Driver", "Passenger", "HR Manager", "Admin")
    )

    RoleSelectionScreen(
        user = previewUser,
        onRoleSelected = {},
        onCancel = {}
    )
}
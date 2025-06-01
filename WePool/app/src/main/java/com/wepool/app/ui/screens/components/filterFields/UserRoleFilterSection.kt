package com.wepool.app.ui.screens.components.filterFields

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wepool.app.data.model.enums.user.UserFilterFields
import com.wepool.app.data.model.enums.user.UserRole

@Composable
fun UserRoleFilterSection(
    selectedFilters: List<UserFilterFields>,
    role: UserRole?,
    onRoleChanged: (UserRole?) -> Unit,
    onClearField: (UserFilterFields) -> Unit,
    onFiltersChanged: (List<UserFilterFields>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    if (UserFilterFields.USER_ROLE in selectedFilters) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {

            // כותרת Role לחיצה פותחת/סוגרת את האפשרויות
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Role: " + when (role) {
                        UserRole.DRIVER -> "Driver"
                        UserRole.PASSENGER -> "Passenger"
                        UserRole.All -> "All"
                        else -> "Select"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = {
                    onRoleChanged(null)
                    onClearField(UserFilterFields.USER_ROLE)
                }) {
                    Text("Clean", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = {
                    onRoleChanged(null)
                    expanded = false
                    onClearField(UserFilterFields.USER_ROLE)
                    onFiltersChanged(selectedFilters - UserFilterFields.USER_ROLE)
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // הצגת האפשרויות אם פתוח
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(UserRole.All, UserRole.DRIVER, UserRole.PASSENGER).forEach { option ->
                        val label = when (option) {
                            UserRole.All -> "All"
                            UserRole.DRIVER -> "Driver"
                            UserRole.PASSENGER -> "Passenger"
                            else -> ""
                        }
                        val isSelected = role == option

                        OutlinedButton(
                            onClick = {
                                onRoleChanged(option)
                                // לא סוגר את expanded כאן
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

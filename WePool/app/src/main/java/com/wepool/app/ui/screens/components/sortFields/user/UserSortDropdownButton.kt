package com.wepool.app.ui.screens.components.sortFields.user

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wepool.app.data.model.enums.SortOrder
import com.wepool.app.data.model.enums.user.UserSortFieldWithOrder
import com.wepool.app.data.model.enums.user.UserSortFields

@Composable
fun UserSortDropdownButton(
    modifier: Modifier = Modifier,
    showName: Boolean = true,
    showEmail: Boolean = true,
    showPhone: Boolean = true,
    showCompanyName: Boolean = true,
    selectedSortFields: List<UserSortFieldWithOrder>,
    onSortFieldsChanged: (List<UserSortFieldWithOrder>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }

    val options = listOfNotNull(
        UserSortFields.USER_NAME.takeIf { showName },
        UserSortFields.USER_EMAIL.takeIf { showEmail },
        UserSortFields.USER_PHONE.takeIf { showPhone },
        UserSortFields.COMPANY_NAME.takeIf { showCompanyName },
    )

    val current = selectedSortFields.firstOrNull()
    val selectedField = current?.field
    val label = current?.let {
        val fieldLabel = when (it.field) {
            UserSortFields.USER_NAME -> "Name"
            UserSortFields.USER_EMAIL -> "Email"
            UserSortFields.USER_PHONE -> "Phone"
            UserSortFields.COMPANY_NAME -> "Company"
        }
        val orderSymbol = if (it.order == SortOrder.ASCENDING) "↑" else "↓"
        "$fieldLabel $orderSymbol"
    } ?: "Sort"

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .height(36.dp)
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates -> buttonSize = coordinates.size },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(with(LocalDensity.current) { buttonSize.width.toDp() })
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (option) {
                                            UserSortFields.USER_NAME -> "Name"
                                            UserSortFields.USER_EMAIL -> "Email"
                                            UserSortFields.USER_PHONE -> "Phone"
                                            UserSortFields.COMPANY_NAME -> "Company"
                                        }
                                    )
                                },
                                onClick = {
                                    onSortFieldsChanged(
                                        listOf(UserSortFieldWithOrder(option, SortOrder.ASCENDING))
                                    )
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedSortFields.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSortFieldsChanged(emptyList())
                        },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Sort")
                    }
                }
            }

            if (selectedField != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            onSortFieldsChanged(listOf(UserSortFieldWithOrder(selectedField, SortOrder.ASCENDING)))
                        }
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Ascending")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ascending")
                    }

                    OutlinedButton(
                        onClick = {
                            onSortFieldsChanged(listOf(UserSortFieldWithOrder(selectedField, SortOrder.DESCENDING)))
                        }
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Descending")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Descending")
                    }
                }
            }
        }
    }
}

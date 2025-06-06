package com.wepool.app.ui.screens.components.sortFields.ride

import androidx.compose.foundation.border
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
import com.wepool.app.data.model.enums.ride.RideSortFieldsWithOrder
import com.wepool.app.data.model.enums.ride.RideSortFields
import com.wepool.app.data.model.enums.SortOrder

@Composable
fun RideSortDropdownButton(
    modifier: Modifier = Modifier,
    selectedSortFields: List<RideSortFieldsWithOrder>,
    onSortFieldsChanged: (List<RideSortFieldsWithOrder>) -> Unit,
    availableSortFields: List<RideSortFields> = listOf(
        RideSortFields.COMPANY_NAME,
        RideSortFields.USER_NAME,
        RideSortFields.DATE,
        RideSortFields.DEPARTURE_TIME,
        RideSortFields.ARRIVAL_TIME,
        RideSortFields.AVAILABLE_SEATS
    )
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }

    val options = availableSortFields

    val current = selectedSortFields.firstOrNull()
    val selectedField = current?.field
    val selectedOrder = current?.order
    val isAscendingSelected = selectedOrder == SortOrder.ASCENDING
    val isDescendingSelected = selectedOrder == SortOrder.DESCENDING

    val label = current?.let {
        val fieldLabel = when (it.field) {
            RideSortFields.USER_NAME -> "User"
            else -> it.field.name.replace("_", " ")
                .lowercase()
                .replaceFirstChar(Char::uppercase)
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
                                            RideSortFields.USER_NAME -> "User"
                                            else -> option.name.replace("_", " ")
                                                .lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                        }
                                    )
                                },
                                onClick = {
                                    onSortFieldsChanged(
                                        listOf(RideSortFieldsWithOrder(option, SortOrder.ASCENDING))
                                    )
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedSortFields.isNotEmpty()) {
                    IconButton(
                        onClick = { onSortFieldsChanged(emptyList()) },
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isAscendingSelected) Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.medium
                                ) else Modifier
                            )
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSortFieldsChanged(
                                    listOf(
                                        RideSortFieldsWithOrder(
                                            selectedField,
                                            SortOrder.ASCENDING
                                        )
                                    )
                                )
                            },
                            modifier = Modifier
                                .height(32.dp)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Ascending",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Asc", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isDescendingSelected) Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.medium
                                ) else Modifier
                            )
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSortFieldsChanged(
                                    listOf(
                                        RideSortFieldsWithOrder(
                                            selectedField,
                                            SortOrder.DESCENDING
                                        )
                                    )
                                )
                            },
                            modifier = Modifier
                                .height(32.dp)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Descending",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Desc", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

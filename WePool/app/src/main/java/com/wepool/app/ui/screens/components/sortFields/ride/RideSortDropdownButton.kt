package com.wepool.app.ui.screens.components.sortFields.ride

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
    showDate: Boolean = true,
    showDepartureTime: Boolean = true,
    showArrivalTime: Boolean = true,
    showAvailableSeats: Boolean = true,
    showCompanyName: Boolean = true,
    showUserName: Boolean = true,
    selectedSortFields: List<RideSortFieldsWithOrder>,
    onSortFieldsChanged: (List<RideSortFieldsWithOrder>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }

    val options = listOfNotNull(
        RideSortFields.COMPANY_NAME.takeIf { showCompanyName },
        RideSortFields.USER_NAME.takeIf { showUserName },
        RideSortFields.DATE.takeIf { showDate },
        RideSortFields.DEPARTURE_TIME.takeIf { showDepartureTime },
        RideSortFields.ARRIVAL_TIME.takeIf { showArrivalTime },
        RideSortFields.AVAILABLE_SEATS.takeIf { showAvailableSeats },
    )

    val current = selectedSortFields.firstOrNull()
    val selectedField = current?.field
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
                    OutlinedButton(
                        onClick = {
                            onSortFieldsChanged(listOf(RideSortFieldsWithOrder(selectedField, SortOrder.ASCENDING)))
                        }
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Ascending")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ascending")
                    }

                    OutlinedButton(
                        onClick = {
                            onSortFieldsChanged(listOf(RideSortFieldsWithOrder(selectedField, SortOrder.DESCENDING)))
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

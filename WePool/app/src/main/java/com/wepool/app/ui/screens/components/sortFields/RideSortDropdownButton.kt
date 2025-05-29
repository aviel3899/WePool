package com.wepool.app.ui.screens.components.sortFields

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.wepool.app.data.model.enums.SortFields

@Composable
fun RideSortDropdownButton(
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    showDepartureTime: Boolean = true,
    showArrivalTime: Boolean = true,
    showAvailableSeats: Boolean = true,
    showCompanyName: Boolean = true,
    showUserName: Boolean = true,
    selectedSortFields: List<SortFields>,
    onSortFieldsChanged: (List<SortFields>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }

    val options = listOfNotNull(
        SortFields.COMPANY_NAME.takeIf { showCompanyName },
        SortFields.USER.takeIf { showUserName },
        SortFields.DATE.takeIf { showDate },
        SortFields.DEPARTURE_TIME.takeIf { showDepartureTime },
        SortFields.ARRIVAL_TIME.takeIf { showArrivalTime },
        SortFields.AVAILABLE_SEATS.takeIf { showAvailableSeats },
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .height(36.dp)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            buttonSize = coordinates.size
                        },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    val label = if (selectedSortFields.isEmpty()) "Sort"
                    else selectedSortFields.joinToString {
                        when (it) {
                            SortFields.USER -> "User"
                            else -> it.name.lowercase().replace("_", " ")
                                .replaceFirstChar(Char::uppercase)
                        }
                    }
                    Text(label, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(with(LocalDensity.current) { buttonSize.width.toDp() })
                ) {
                    options.forEach { option ->
                        val isSelected = selectedSortFields.contains(option)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isSelected) Modifier
                                                .border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(4.dp)
                                            else Modifier.padding(4.dp)
                                        )
                                ) {
                                    Text(
                                        when (option) {
                                            SortFields.USER -> "User"
                                            else -> option.name.replace("_", " ")
                                                .lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                        }
                                    )
                                }
                            },
                            onClick = {
                                onSortFieldsChanged(listOf(option))
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
    }
}

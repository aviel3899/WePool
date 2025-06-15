package com.wepool.app.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusLabel(
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val label = if (active) "Active" else "Inactive"
    val backgroundColor = if (active) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    Text(
        text = label,
        color = Color.White,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

package com.hereliesaz.et2bruteforce.ui.aznavrail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.et2bruteforce.ui.aznavrail.model.AzNavItem

@Composable
fun AzNavRailButton(
    item: AzNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    size: Dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(item.color ?: if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = if (item.isToggle) (if (item.isChecked == true) "ON" else "OFF")
                   else if (item.isCycler) (item.selectedOption?.take(1) ?: "?")
                   else item.text.take(2),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
    }
}

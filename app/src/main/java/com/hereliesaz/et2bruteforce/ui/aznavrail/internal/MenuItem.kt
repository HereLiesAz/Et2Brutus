package com.hereliesaz.et2bruteforce.ui.aznavrail.internal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.et2bruteforce.ui.aznavrail.model.AzNavItem

@Composable
fun MenuItem(
    item: AzNavItem,
    navController: NavController?,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    onCyclerClick: (() -> Unit)?,
    onToggle: () -> Unit,
    onItemClick: () -> Unit,
    onHostClick: (() -> Unit)? = null
) {
    Text(
        text = if (item.isToggle) (if (item.isChecked == true) item.toggleOnText else item.toggleOffText)
               else if (item.isCycler) (item.selectedOption ?: "")
               else item.text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (item.isCycler) {
                    onCyclerClick?.invoke()
                } else if (item.isHost) {
                    onHostClick?.invoke()
                } else {
                    onClick?.invoke()
                    onItemClick()
                    if (item.collapseOnClick) {
                        onToggle()
                    }
                }
            }
            .padding(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
}

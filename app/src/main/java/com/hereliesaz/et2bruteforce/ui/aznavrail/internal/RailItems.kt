package com.hereliesaz.et2bruteforce.ui.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavController
import com.hereliesaz.et2bruteforce.ui.aznavrail.AzNavRailScope
import com.hereliesaz.et2bruteforce.ui.aznavrail.model.AzNavItem
import com.hereliesaz.et2bruteforce.ui.aznavrail.AzNavRailButton

@Composable
fun RailItems(
    items: List<AzNavItem>,
    scope: AzNavRailScope,
    navController: NavController?,
    currentDestination: String?,
    buttonSize: Dp,
    onRailCyclerClick: (AzNavItem) -> Unit,
    onItemSelected: (AzNavItem) -> Unit,
    hostStates: Map<String, Boolean>,
    packRailButtons: Boolean
) {
    items.filter { it.isRailItem && !it.isSubItem }.forEach { item ->
        AzNavRailButton(
            item = item,
            isSelected = item.route == currentDestination,
            onClick = {
                if (item.isCycler) {
                    onRailCyclerClick(item)
                } else if (item.isHost) {
                    // Host click?
                } else {
                    scope.onClickMap[item.id]?.invoke()
                    onItemSelected(item)
                }
            },
            size = buttonSize
        )
    }
}

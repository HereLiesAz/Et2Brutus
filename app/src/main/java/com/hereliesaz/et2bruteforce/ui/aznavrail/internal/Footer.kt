package com.hereliesaz.et2bruteforce.ui.aznavrail.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.et2bruteforce.ui.aznavrail.AzNavRailScopeImpl

@Composable
internal fun Footer(
    appName: String,
    onToggle: () -> Unit,
    onUndock: () -> Unit,
    scope: AzNavRailScopeImpl
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (scope.enableRailDragging) {
             Button(onClick = onUndock) {
                 Text("Undock")
             }
        }
    }
}

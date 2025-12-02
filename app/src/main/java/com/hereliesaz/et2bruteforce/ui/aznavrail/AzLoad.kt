package com.hereliesaz.et2bruteforce.ui.aznavrail

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

@Composable
fun AzLoad() {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

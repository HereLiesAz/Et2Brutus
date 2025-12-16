package com.hereliesaz.et2bruteforce.ui.aznavrail.service

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset

data class AzNavRailOverlayController(
    val contentOffset: State<IntOffset>,
    val onDragStart: () -> Unit,
    val onDrag: (Offset) -> Unit,
    val onDragEnd: () -> Unit
)

val LocalAzNavRailOverlayController = compositionLocalOf<AzNavRailOverlayController?> { null }

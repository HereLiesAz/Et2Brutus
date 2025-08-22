package com.hereliesaz.et2bruteforce.model

import androidx.compose.ui.graphics.Color
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor5
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor6
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor7

fun NodeType.getColor(): Color {
    return when (this) {
        NodeType.INPUT -> WalkthroughColor5
        NodeType.SUBMIT -> WalkthroughColor6
        NodeType.POPUP -> WalkthroughColor7
    }
}

package com.hereliesaz.et2bruteforce.ui.aznavrail.util.geometry

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

fun Density.intPxToSp(px: Int): TextUnit {
    return (px / fontScale / density).sp
}

fun Density.spRoundToPx(sp: TextUnit): Int {
    return sp.toPx().roundToInt()
}

fun Density.spToIntPx(sp: TextUnit): Int {
    return sp.toPx().toInt()
}

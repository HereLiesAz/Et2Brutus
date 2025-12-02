package com.hereliesaz.et2bruteforce.ui.aznavrail.internal

import android.util.Log

object AzNavRailLogger {
    private const val TAG = "AzNavRail"
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}

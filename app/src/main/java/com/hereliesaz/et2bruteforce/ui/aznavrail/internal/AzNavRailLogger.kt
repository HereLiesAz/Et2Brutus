package com.hereliesaz.et2bruteforce.ui.aznavrail.internal

import android.util.Log

object AzNavRailLogger {
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
}

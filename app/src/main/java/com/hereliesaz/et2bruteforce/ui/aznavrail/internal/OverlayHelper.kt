package com.hereliesaz.et2bruteforce.ui.aznavrail.internal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

internal object OverlayHelper {
    fun launch(context: Context, serviceClass: Class<*>) {
        if (Settings.canDrawOverlays(context)) {
            val intent = Intent(context, serviceClass)
            ContextCompat.startForegroundService(context, intent)
        } else {
             val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
             intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
             context.startActivity(intent)
        }
    }
}

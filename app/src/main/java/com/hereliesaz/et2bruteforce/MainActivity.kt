package com.hereliesaz.et2bruteforce

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hereliesaz.et2bruteforce.services.BruteforceAccessibilityService
import com.hereliesaz.et2bruteforce.services.FloatingControlService
import com.hereliesaz.et2bruteforce.ui.theme.Et2BruteForceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ActivityResultLauncher for SYSTEM_ALERT_WINDOW permission
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission is required for the floating controls.", Toast.LENGTH_LONG).show()
            } else {
                // Permission granted, potentially start service automatically or update UI
                Toast.makeText(this, "Overlay permission granted.", Toast.LENGTH_SHORT).show()
            }
        }
        // Recompose to update button states
        // This requires a way to trigger recomposition, e.g., updating a MutableState
        // For simplicity, assume user clicks button again to check state.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Et2BruteForceTheme {
                MainScreen(
                    onRequestOverlayPermission = ::requestOverlayPermission,
                    onCheckAccessibilityPermission = ::isAccessibilityServiceEnabled,
                    onRequestAccessibilityPermission = ::requestAccessibilityPermission,
                    onStartService = ::startFloatingService,
                    onStopService = ::stopFloatingService
                )
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(this, "Overlay permission is implicitly granted on this Android version.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Overlay permission already granted.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceId = "$packageName/${BruteforceAccessibilityService::class.java.canonicalName}"
        Log.d("MainActivity", "Checking Accessibility Service: $serviceId")
        val settingValue = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return settingValue?.contains(serviceId, ignoreCase = false) ?: false
    }

    private fun requestAccessibilityPermission() {
        Toast.makeText(this, "Please enable the '${getString(R.string.accessibility_service_label)}' in Accessibility settings.", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        // User needs to manually enable it. App can't do it programmatically.
    }

    private fun startFloatingService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission required first.", Toast.LENGTH_SHORT).show()
            requestOverlayPermission()
            return
        }
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Accessibility Service required first.", Toast.LENGTH_SHORT).show()
            requestAccessibilityPermission()
            return
        }

        Log.i("MainActivity", "Starting FloatingControlService")
        startService(Intent(this, FloatingControlService::class.java))
        Toast.makeText(this, "Floating service started.", Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingService() {
        Log.i("MainActivity", "Stopping FloatingControlService")
        stopService(Intent(this, FloatingControlService::class.java))
        Toast.makeText(this, "Floating service stopped.", Toast.LENGTH_SHORT).show()
    }
}


@Composable
fun MainScreen(
    onRequestOverlayPermission: () -> Unit,
    onCheckAccessibilityPermission: () -> Boolean,
    onRequestAccessibilityPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    // Use LaunchedEffect or rememberUpdatedState if checks need to re-run automatically
    var hasOverlayPerm by remember { mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true) }
    var hasAccessPerm by remember { mutableStateOf(onCheckAccessibilityPermission()) }

    // Simple refresh mechanism - user clicks check buttons
    fun refreshPermissions() {
        hasOverlayPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
        hasAccessPerm = onCheckAccessibilityPermission()
    }


    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Screen Bruteforcer Setup", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            Text("Permissions Required:")
            Spacer(modifier = Modifier.height(8.dp))

            // Overlay Permission
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Draw Over Other Apps: ")
                Text(if (hasOverlayPerm) "GRANTED" else "Needed", color = if (hasOverlayPerm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onRequestOverlayPermission, enabled = !hasOverlayPerm) {
                    Text("Request")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Accessibility Permission
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Accessibility Service: ")
                Text(if (hasAccessPerm) "ENABLED" else "Needed", color = if (hasAccessPerm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onRequestAccessibilityPermission, enabled = !hasAccessPerm) {
                    Text("Go To Settings")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = ::refreshPermissions) {
                Text("Re-Check Permissions")
            }


            Spacer(modifier = Modifier.height(32.dp))

            Text("Service Control:")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onStartService, enabled = hasOverlayPerm && hasAccessPerm) {
                    Text("Start Service")
                }
                Button(onClick = onStopService) {
                    Text("Stop Service")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Enable permissions, then start the service. A floating button will appear.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

        }
    }
}
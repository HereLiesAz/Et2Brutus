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
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.et2bruteforce.ui.theme.PermissionGranted
import com.hereliesaz.et2bruteforce.ui.theme.PermissionDenied
import com.hereliesaz.et2bruteforce.services.BruteforceAccessibilityService
import com.hereliesaz.et2bruteforce.comms.AccessibilityCommsManager
import com.hereliesaz.et2bruteforce.services.FloatingControlService
import com.hereliesaz.et2bruteforce.ui.theme.Et2BruteForceTheme
import com.hereliesaz.et2bruteforce.viewmodel.BruteforceViewModel
import android.view.KeyEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import com.hereliesaz.et2bruteforce.data.SettingsRepository
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var commsManager: AccessibilityCommsManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val viewModel: BruteforceViewModel by viewModels()

    // ActivityResultLauncher for SYSTEM_ALERT_WINDOW permission
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, getString(R.string.main_overlay_permission_toast), Toast.LENGTH_LONG).show()
            } else {
                // Permission granted, potentially start service automatically or update UI
                Toast.makeText(this, getString(R.string.main_overlay_permission_granted), Toast.LENGTH_SHORT).show()
            }
        }
        // Recompose to update button states
        // This requires a way to trigger recomposition, e.g., updating a MutableState
        // For simplicity, assume user clicks button again to check state.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launcher for dictionary file selection
        val dictionaryPickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let {
                // Persist permission to read the file across device reboots
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                    lifecycleScope.launch {
                        commsManager.reportDictionaryUri(uri)
                    }
                    Toast.makeText(this, getString(R.string.main_dictionary_selected), Toast.LENGTH_SHORT).show()
                } catch (e: SecurityException) {
                    Log.e("MainActivity", "Failed to take persistable URI permission", e)
                    Toast.makeText(this, getString(R.string.main_dictionary_error), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Listen for requests to open the dictionary picker
        lifecycleScope.launch {
            commsManager.openDictionaryPickerRequest.collect {
                dictionaryPickerLauncher.launch(arrayOf("*/*")) // Or specific MIME types
            }
        }

        lifecycleScope.launch {
            val settings = settingsRepository.getSettingsSnapshot()
            if (!settings.walkthroughCompleted) {
                startActivity(Intent(this@MainActivity, WalkthroughActivity::class.java))
            }
        }

        setContent {
            Et2BruteForceTheme {
                MainScreen(
                    viewModel = viewModel,
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
            Toast.makeText(this, getString(R.string.main_overlay_permission_implicit), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.main_overlay_permission_already_granted), Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, getString(R.string.main_accessibility_permission_toast, getString(R.string.accessibility_service_label)), Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        // User needs to manually enable it. App can't do it programmatically.
    }

    private fun startFloatingService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, getString(R.string.main_overlay_permission_required), Toast.LENGTH_SHORT).show()
            requestOverlayPermission()
            return
        }
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, getString(R.string.main_accessibility_permission_required), Toast.LENGTH_SHORT).show()
            requestAccessibilityPermission()
            return
        }

        Log.i("MainActivity", "Starting FloatingControlService")
        startService(Intent(this, FloatingControlService::class.java))
        Toast.makeText(this, getString(R.string.main_service_started), Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingService() {
        Log.i("MainActivity", "Stopping FloatingControlService")
        stopService(Intent(this, FloatingControlService::class.java))
        Toast.makeText(this, getString(R.string.main_service_stopped), Toast.LENGTH_SHORT).show()
    }

}


@Composable
fun MainScreen(
    viewModel: BruteforceViewModel,
    onRequestOverlayPermission: () -> Unit,
    onCheckAccessibilityPermission: () -> Boolean,
    onRequestAccessibilityPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.main_setup_title), style = MaterialTheme.typography.headlineMedium)

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.main_permissions_required), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    PermissionRow(
                        permissionName = stringResource(R.string.main_draw_over_apps),
                        isGranted = hasOverlayPerm,
                        onRequestPermission = onRequestOverlayPermission
                    )

                    PermissionRow(
                        permissionName = stringResource(R.string.main_accessibility_service),
                        isGranted = hasAccessPerm,
                        onRequestPermission = onRequestAccessibilityPermission
                    )

                    Button(onClick = refreshPermissions) {
                        Text(stringResource(R.string.main_recheck_permissions))
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.main_service_control), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = onStartService, enabled = hasOverlayPerm && hasAccessPerm) {
                            Text(stringResource(R.string.main_start_service))
                        }
                        Button(onClick = onStopService) {
                            Text(stringResource(R.string.main_stop_service))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.main_service_info),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Button(onClick = {
                context.startActivity(Intent(context, InstructionActivity::class.java))
            }) {
                Text(stringResource(R.string.main_view_instructions))
            }
        }
    }
}

@Composable
fun PermissionRow(
    permissionName: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(permissionName)
        Text(
            text = if (isGranted) stringResource(R.string.main_granted) else stringResource(R.string.main_needed),
            color = if (isGranted) PermissionGranted else PermissionDenied
        )
        Button(onClick = onRequestPermission, enabled = !isGranted) {
            Text(if (isGranted) stringResource(R.string.main_granted) else stringResource(R.string.main_request))
        }
    }
}
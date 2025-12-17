package com.hereliesaz.et2bruteforce.ui.overlay

import android.graphics.Point
import android.graphics.Rect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.et2bruteforce.model.BruteforceState
import com.hereliesaz.et2bruteforce.model.BruteforceStatus
import com.hereliesaz.et2bruteforce.model.CharacterSetType
import com.hereliesaz.et2bruteforce.R
import com.hereliesaz.et2bruteforce.model.NodeType
import com.hereliesaz.et2bruteforce.model.getColor
import com.hereliesaz.et2bruteforce.model.Profile
import com.hereliesaz.et2bruteforce.services.FloatingControlService.Companion.MAIN_CONTROLLER_KEY
import com.hereliesaz.et2bruteforce.ui.theme.*
import com.hereliesaz.et2bruteforce.ui.aznavrail.AzNavRail
import com.hereliesaz.et2bruteforce.ui.aznavrail.AzButton
import com.hereliesaz.et2bruteforce.ui.aznavrail.model.AzButtonShape
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RootOverlay(
    viewKey: Any,
    uiState: BruteforceState,
    highlightedInfo: com.hereliesaz.et2bruteforce.model.HighlightInfo?,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: (Point) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    onToggleActionButtons: () -> Unit,
    onSelectDictionary: () -> Unit,
    onUpdateLength: (Int) -> Unit,
    onUpdateCharset: (CharacterSetType) -> Unit,
    onUpdatePace: (Long) -> Unit,
    onToggleResume: (Boolean) -> Unit,
    onToggleSingleAttemptMode: (Boolean) -> Unit,
    onUpdateSuccessKeywords: (List<String>) -> Unit,
    onUpdateCaptchaKeywords: (List<String>) -> Unit,
    profiles: List<Profile>,
    saveError: String?,
    onLoadProfile: (Profile) -> Unit,
    onSaveProfile: (String) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    onRenameProfile: (Profile, String) -> Unit,
    onUpdateMask: (String) -> Unit,
    onUpdateHybridModeEnabled: (Boolean) -> Unit,
    onUpdateHybridSuffixes: (List<String>) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (highlightedInfo != null) {
            HighlightBox(bounds = highlightedInfo.bounds, nodeType = highlightedInfo.nodeType)
        }
        when (viewKey) {
            is NodeType -> {
                ConfigButtonUi(
                    nodeType = viewKey,
                    uiState = uiState,
                    onDrag = { x, y -> onDrag(x, y) },
                    onDragEnd = onDragEnd
                )
            }
            MAIN_CONTROLLER_KEY -> {
                MainControllerUi(
                    uiState = uiState,
                    onDrag = onDrag,
                    onStart = onStart,
                    onPause = onPause,
                    onStop = onStop,
                    onClose = onClose,
                    onToggleActionButtons = onToggleActionButtons,
                    onSelectDictionary = onSelectDictionary,
                    onUpdateLength = onUpdateLength,
                    onUpdateCharset = onUpdateCharset,
                    onUpdatePace = onUpdatePace,
                    onToggleResume = onToggleResume,
                    onToggleSingleAttemptMode = onToggleSingleAttemptMode,
                    onUpdateSuccessKeywords = onUpdateSuccessKeywords,
                    onUpdateCaptchaKeywords = onUpdateCaptchaKeywords,
                    onUpdateMask = onUpdateMask,
                    onUpdateHybridModeEnabled = onUpdateHybridModeEnabled,
                    onUpdateHybridSuffixes = onUpdateHybridSuffixes,
                    profiles = profiles,
                    saveError = saveError,
                    onLoadProfile = onLoadProfile,
                    onSaveProfile = onSaveProfile,
                    onDeleteProfile = onDeleteProfile,
                    onRenameProfile = onRenameProfile
                )
            }
        }
    }
}

@Composable
fun HighlightBox(bounds: Rect, nodeType: NodeType) {
    val color = nodeType.getColor()
    val density = LocalDensity.current
    val x = with(density) { bounds.left.toDp() }
    val y = with(density) { bounds.top.toDp() }
    val width = with(density) { bounds.width().toDp() }
    val height = with(density) { bounds.height().toDp() }

    Box(
        modifier = Modifier
            .offset(x, y)
            .size(width, height)
            .background(color.copy(alpha = 0.3f))
            .border(2.dp, color)
    )
}

@Composable
fun MainControllerUi(
    uiState: BruteforceState,
    onDrag: (Float, Float) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    onToggleActionButtons: () -> Unit,
    onSelectDictionary: () -> Unit,
    onUpdateLength: (Int) -> Unit,
    onUpdateCharset: (CharacterSetType) -> Unit,
    onUpdatePace: (Long) -> Unit,
    onToggleResume: (Boolean) -> Unit,
    onToggleSingleAttemptMode: (Boolean) -> Unit,
    onUpdateSuccessKeywords: (List<String>) -> Unit,
    onUpdateCaptchaKeywords: (List<String>) -> Unit,
    onUpdateMask: (String) -> Unit,
    onUpdateHybridModeEnabled: (Boolean) -> Unit,
    onUpdateHybridSuffixes: (List<String>) -> Unit,
    profiles: List<Profile>,
    saveError: String?,
    onLoadProfile: (Profile) -> Unit,
    onSaveProfile: (String) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    onRenameProfile: (Profile, String) -> Unit
) {
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    val status = uiState.status

    AzNavRail {
        azSettings(
            onRailDrag = onDrag,
            onOverlayDrag = onDrag
        )

        azRailItem(
            id = "start",
            text = "Start",
            disabled = !(status == BruteforceStatus.READY || status == BruteforceStatus.PAUSED),
            onClick = onStart
        )
        azRailItem(
            id = "pause",
            text = "Pause",
            disabled = status != BruteforceStatus.RUNNING,
            onClick = onPause
        )
        azRailItem(
            id = "stop",
            text = "Stop",
            disabled = status == BruteforceStatus.IDLE,
            onClick = onStop
        )
        azMenuItem(
            id = "dictionary",
            text = "Dictionary",
            onClick = onSelectDictionary
        )
        azMenuItem(
            id = "settings",
            text = "Settings",
            onClick = { showSettingsDialog = true }
        )
        azMenuItem(
            id = "buttons",
            text = if (uiState.actionButtonsEnabled) "Disable Buttons" else "Enable Buttons",
            onClick = onToggleActionButtons
        )
        azMenuItem(
            id = "profiles",
            text = "Profiles",
            onClick = { showProfileDialog = true }
        )
        azMenuItem(
            id = "close",
            text = "Close",
            onClick = onClose
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            uiState = uiState,
            onUpdateLength = onUpdateLength,
            onUpdateCharset = onUpdateCharset,
            onUpdatePace = onUpdatePace,
            onToggleResume = onToggleResume,
            onToggleSingleAttemptMode = onToggleSingleAttemptMode,
            onUpdateSuccessKeywords = onUpdateSuccessKeywords,
            onUpdateCaptchaKeywords = onUpdateCaptchaKeywords,
            onUpdateMask = onUpdateMask,
            onUpdateHybridModeEnabled = onUpdateHybridModeEnabled,
            onUpdateHybridSuffixes = onUpdateHybridSuffixes,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showProfileDialog) {
        ProfileManagementDialog(
            profiles = profiles,
            saveError = saveError,
            onLoadProfile = onLoadProfile,
            onSaveProfile = onSaveProfile,
            onDeleteProfile = onDeleteProfile,
            onRenameProfile = onRenameProfile,
            onDismiss = { showProfileDialog = false }
        )
    }
}

@Composable
fun ConfigButtonUi(
    nodeType: NodeType,
    uiState: BruteforceState,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: (Point) -> Unit
) {
    val buttonConfig = uiState.buttonConfigs[nodeType]
    val isIdentified = buttonConfig?.identifiedNodeInfo != null
    var fabCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(contentAlignment = Alignment.Center) {
        val text = when (nodeType) {
            NodeType.INPUT -> "Input"
            NodeType.SUBMIT -> "Submit"
            NodeType.POPUP -> "Popup"
        }
        AzButton(
            onClick = { /* Drag only */ },
            text = text,
            color = nodeType.getColor(),
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    fabCoordinates = coordinates
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        },
                        onDragEnd = {
                            fabCoordinates?.let {
                                val rootPos = it.positionInRoot()
                                val center = Point(
                                    (rootPos.x + it.size.width / 2f).roundToInt(),
                                    (rootPos.y + it.size.height / 2f).roundToInt()
                                )
                                onDragEnd(center)
                            }
                        }
                    )
                }
        )
        if (isIdentified) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Identified",
                tint = Color.Green,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    uiState: BruteforceState,
    onUpdateLength: (Int) -> Unit,
    onUpdateCharset: (CharacterSetType) -> Unit,
    onUpdatePace: (Long) -> Unit,
    onToggleResume: (Boolean) -> Unit,
    onToggleSingleAttemptMode: (Boolean) -> Unit,
    onUpdateSuccessKeywords: (List<String>) -> Unit,
    onUpdateCaptchaKeywords: (List<String>) -> Unit,
    onUpdateMask: (String) -> Unit,
    onUpdateHybridModeEnabled: (Boolean) -> Unit,
    onUpdateHybridSuffixes: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val settings = uiState.settings
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advanced Settings") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Length: ${settings.characterLength}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.characterLength.toFloat(),
                    onValueChange = { onUpdateLength(it.roundToInt()) },
                    valueRange = 1f..12f,
                    steps = 10
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Character Set:", style = MaterialTheme.typography.bodyMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    CharacterSetChip(CharacterSetType.LETTERS, settings.characterSetType, onUpdateCharset)
                    CharacterSetChip(CharacterSetType.NUMBERS, settings.characterSetType, onUpdateCharset)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    CharacterSetChip(CharacterSetType.LETTERS_NUMBERS, settings.characterSetType, onUpdateCharset)
                    CharacterSetChip(CharacterSetType.ALPHANUMERIC_SPECIAL, settings.characterSetType, onUpdateCharset)
                }
                Spacer(modifier = Modifier.height(12.dp))

                var paceText by remember(settings.attemptPaceMillis) { mutableStateOf(settings.attemptPaceMillis.toString()) }
                OutlinedTextField(
                    value = paceText,
                    onValueChange = {
                        paceText = it
                        it.toLongOrNull()?.let { pace -> onUpdatePace(pace) }
                    },
                    label = { Text("Pace (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    leadingIcon = { Icon(Icons.Default.Timer, null) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = settings.resumeFromLast,
                        onCheckedChange = onToggleResume
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resume from last attempt", style = MaterialTheme.typography.bodyMedium)
                }
                if (settings.resumeFromLast && settings.lastAttempt != null) {
                    Text(" Last: ${settings.lastAttempt}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = settings.singleAttemptMode,
                        onCheckedChange = onToggleSingleAttemptMode
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Single Attempt Mode", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))

                var successKeywordsText by remember(uiState.settings.successKeywords) { mutableStateOf(uiState.settings.successKeywords.joinToString(",")) }
                OutlinedTextField(
                    value = successKeywordsText,
                    onValueChange = {
                        successKeywordsText = it
                        onUpdateSuccessKeywords(it.split(',').map { kw -> kw.trim() }.filter { kw -> kw.isNotEmpty() })
                    },
                    label = { Text("Success Keywords (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                var captchaKeywordsText by remember(uiState.settings.captchaKeywords) { mutableStateOf(uiState.settings.captchaKeywords.joinToString(",")) }
                OutlinedTextField(
                    value = captchaKeywordsText,
                    onValueChange = {
                        captchaKeywordsText = it
                        onUpdateCaptchaKeywords(it.split(',').map { kw -> kw.trim() }.filter { kw -> kw.isNotEmpty() })
                    },
                    label = { Text("CAPTCHA Keywords (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterSetChip(
    type: CharacterSetType,
    selectedType: CharacterSetType,
    onUpdateCharset: (CharacterSetType) -> Unit
) {
    FilterChip(
        selected = type == selectedType,
        onClick = { onUpdateCharset(type) },
        label = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
        modifier = Modifier.height(32.dp)
    )
}

@Composable
private fun SuccessConfirmation(
    candidate: String,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Success Detected!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("Password Found: $candidate", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onConfirm) { Text("Confirm") }
            Button(onClick = onReject, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Reject") }
        }
    }
}

private fun getStatusMessage(uiState: BruteforceState): String {
    return when (uiState.status) {
        BruteforceStatus.IDLE -> "Idle"
        BruteforceStatus.READY -> "Ready"
        BruteforceStatus.RUNNING -> "Running: ${uiState.currentAttempt ?: ""} (${uiState.attemptCount})"
        BruteforceStatus.PAUSED -> "Paused (${uiState.attemptCount})"
        BruteforceStatus.CAPTCHA_DETECTED -> "CAPTCHA!"
        BruteforceStatus.SUCCESS_DETECTED -> "Success!"
        BruteforceStatus.DICTIONARY_LOAD_FAILED -> "Dict Load Fail"
        BruteforceStatus.ERROR -> "Error"
    }
}

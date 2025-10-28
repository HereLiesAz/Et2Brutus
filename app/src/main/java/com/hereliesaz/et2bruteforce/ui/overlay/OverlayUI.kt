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
import com.hereliesaz.et2bruteforce.services.FloatingControlService.Companion.MAIN_CONTROLLER_KEY
import com.hereliesaz.et2bruteforce.ui.theme.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RootOverlay(
    viewKey: Any,
    uiState: BruteforceState,
    highlightedInfo: com.hereliesaz.et2bruteforce.model.HighlightInfo?,
    onDrag: (Point) -> Unit,
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
    onLoadProfile: (Profile) -> Unit,
    onSaveProfile: (String) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    onRenameProfile: (Profile, String) -> Unit
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
                    onDrag = { point -> onDrag(point) },
                    onDragEnd = onDragEnd
                )
            }
            MAIN_CONTROLLER_KEY -> {
                MainControllerUi(
                    uiState = uiState,
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
    onLoadProfile: (Profile) -> Unit,
    onSaveProfile: (String) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    onRenameProfile: (Profile, String) -> Unit
) {
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    val fabSize = 56.dp

    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(fabSize / 2)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        ExpandableFabMenu(
            uiState = uiState,
            onStart = onStart,
            onPause = onPause,
            onStop = onStop,
            onClose = onClose,
            onToggleActionButtons = onToggleActionButtons,
            onShowSettings = { showSettingsDialog = true },
            onSelectDictionary = onSelectDictionary,
            onShowProfileManagement = { showProfileDialog = true }
        )

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
                onLoadProfile = onLoadProfile,
                onSaveProfile = onSaveProfile,
                onDeleteProfile = onDeleteProfile,
                onRenameProfile = onRenameProfile,
                onDismiss = { showProfileDialog = false }
            )
        }
    }
}

@Composable
fun ConfigButtonUi(
    nodeType: NodeType,
    uiState: BruteforceState,
    onDrag: (Point) -> Unit,
    onDragEnd: (Point) -> Unit
) {
    val buttonConfig = uiState.buttonConfigs[nodeType]
    val isIdentified = buttonConfig?.identifiedNodeInfo != null
    var fabCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(contentAlignment = Alignment.Center) {
        FloatingActionButton(
            onClick = { /* Drag only */ },
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .onGloballyPositioned { coordinates ->
                    fabCoordinates = coordinates
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            fabCoordinates?.let {
                                val rootPos = it.positionInRoot()
                                val center = Point(
                                    (rootPos.x + it.size.width / 2f).roundToInt(),
                                    (rootPos.y + it.size.height / 2f).roundToInt()
                                )
                                onDrag(center)
                            }
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
                },
            containerColor = if (isIdentified) {
                nodeType.getColor()
            } else MaterialTheme.colorScheme.secondaryContainer
        ) {
            val icon = when (nodeType) {
                NodeType.INPUT -> Icons.Default.TextFields
                NodeType.SUBMIT -> Icons.Default.ArrowForward
                NodeType.POPUP -> Icons.Default.Warning
            }
            Icon(icon, contentDescription = "$nodeType Button")
        }
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

private data class MindMapActionItem(
    val icon: ImageVector,
    val text: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
private fun ExpandableFabMenu(
    uiState: BruteforceState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    onToggleActionButtons: () -> Unit,
    onShowSettings: () -> Unit,
    onSelectDictionary: () -> Unit,
    onShowProfileManagement: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val status = uiState.status
    val actionButtonsEnabled = uiState.actionButtonsEnabled

    val items = remember(status, actionButtonsEnabled) {
        listOf(
            MindMapActionItem(
                icon = Icons.Default.PlayArrow,
                text = "Start",
                enabled = status == BruteforceStatus.READY || status == BruteforceStatus.PAUSED,
                onClick = onStart
            ),
            MindMapActionItem(
                icon = Icons.Default.Pause,
                text = "Pause",
                enabled = status == BruteforceStatus.RUNNING,
                onClick = onPause
            ),
            MindMapActionItem(
                icon = Icons.Default.Stop,
                text = "Stop",
                enabled = status != BruteforceStatus.IDLE,
                onClick = onStop
            ),
            MindMapActionItem(
                icon = Icons.Default.Book,
                text = "Dictionary",
                onClick = onSelectDictionary
            ),
            MindMapActionItem(
                icon = Icons.Default.Settings,
                text = "Settings",
                onClick = onShowSettings
            ),
            MindMapActionItem(
                icon = if (actionButtonsEnabled) Icons.Default.Cancel else Icons.Filled.PlayCircleOutline,
                text = if (actionButtonsEnabled) "Disable Buttons" else "Enable Buttons",
                onClick = onToggleActionButtons
            ),
            MindMapActionItem(
                icon = Icons.Default.Close,
                text = "Close",
                onClick = onClose
            ),
            MindMapActionItem(
                icon = Icons.Default.Person,
                text = "Profiles",
                onClick = onShowProfileManagement
            )
        )
    }

    Box(contentAlignment = Alignment.TopEnd) {
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = "App Icon",
                modifier = Modifier.size(56.dp)
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(top = 64.dp), // fab size (56) + spacing (8)

                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEachIndexed { index, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = when (index) {
                                0 -> ButtonColor1
                                1 -> ButtonColor2
                                2 -> ButtonColor3
                                3 -> ButtonColor4
                                4 -> ButtonColor5
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = item.text,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = { if (item.enabled) item.onClick() },
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                            containerColor = when (index) {
                                0 -> ButtonColor1
                                1 -> ButtonColor2
                                2 -> ButtonColor3
                                3 -> ButtonColor4
                                4 -> ButtonColor5
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Icon(item.icon, contentDescription = item.text, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            shape = CircleShape,
        ) {
            Icon(
                painter = painterResource(id = com.hereliesaz.et2bruteforce.R.drawable.ic_launcher_foreground),
                contentDescription = "App Icon",
                modifier = Modifier.size(56.dp)
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

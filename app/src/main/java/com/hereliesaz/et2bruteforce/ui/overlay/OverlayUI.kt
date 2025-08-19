package com.hereliesaz.et2bruteforce.ui.overlay

import android.graphics.Point
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.et2bruteforce.model.BruteforceState
import com.hereliesaz.et2bruteforce.model.BruteforceStatus
import com.hereliesaz.et2bruteforce.model.CharacterSetType
import com.hereliesaz.et2bruteforce.model.NodeType
import com.hereliesaz.et2bruteforce.services.FloatingControlService.Companion.MAIN_CONTROLLER_KEY
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RootOverlay(
    viewKey: Any,
    uiState: BruteforceState,
    onDrag: (deltaX: Float, deltaY: Float) -> Unit,
    onDragEnd: (Point) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSelectDictionary: () -> Unit,
    onUpdateLength: (Int) -> Unit,
    onUpdateCharset: (CharacterSetType) -> Unit,
    onUpdatePace: (Long) -> Unit,
    onToggleResume: (Boolean) -> Unit,
    onToggleSingleAttemptMode: (Boolean) -> Unit,
    onUpdateSuccessKeywords: (List<String>) -> Unit,
    onUpdateCaptchaKeywords: (List<String>) -> Unit
) {
    when (viewKey) {
        is NodeType -> {
            ConfigButtonUi(
                nodeType = viewKey,
                uiState = uiState,
                onDrag = onDrag,
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
                onSelectDictionary = onSelectDictionary,
                onUpdateLength = onUpdateLength,
                onUpdateCharset = onUpdateCharset,
                onUpdatePace = onUpdatePace,
                onToggleResume = onToggleResume,
                onToggleSingleAttemptMode = onToggleSingleAttemptMode,
                onUpdateSuccessKeywords = onUpdateSuccessKeywords,
                onUpdateCaptchaKeywords = onUpdateCaptchaKeywords
            )
        }
    }
}

@Composable
fun MainControllerUi(
    uiState: BruteforceState,
    onDrag: (deltaX: Float, deltaY: Float) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSelectDictionary: () -> Unit,
    onUpdateLength: (Int) -> Unit,
    onUpdateCharset: (CharacterSetType) -> Unit,
    onUpdatePace: (Long) -> Unit,
    onToggleResume: (Boolean) -> Unit,
    onToggleSingleAttemptMode: (Boolean) -> Unit,
    onUpdateSuccessKeywords: (List<String>) -> Unit,
    onUpdateCaptchaKeywords: (List<String>) -> Unit
) {
    var showOptionsMenu by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val fabSize = 56.dp

    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(fabSize / 2)
    ) {
        MindMapOptionsMenu(
            isVisible = showOptionsMenu,
            uiState = uiState,
            onStart = onStart,
            onPause = onPause,
            onStop = onStop,
            onShowSettings = { showSettingsDialog = true },
            onSelectDictionary = onSelectDictionary
        )

        FloatingActionButton(
            onClick = { showOptionsMenu = !showOptionsMenu },
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.Center)
                .size(fabSize)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
        ) {
            val icon = when {
                showOptionsMenu -> Icons.Default.Close
                uiState.status == BruteforceStatus.RUNNING -> Icons.Default.Pause
                uiState.status == BruteforceStatus.PAUSED || uiState.status == BruteforceStatus.READY -> Icons.Default.PlayArrow
                uiState.status == BruteforceStatus.SUCCESS_DETECTED -> Icons.Default.CheckCircle
                uiState.status == BruteforceStatus.CAPTCHA_DETECTED -> Icons.Default.Security
                uiState.status == BruteforceStatus.ERROR || uiState.status == BruteforceStatus.DICTIONARY_LOAD_FAILED -> Icons.Default.Error
                else -> Icons.Default.Menu
            }
            Icon(icon, contentDescription = "Main Action Button")
        }

        if (showSettingsDialog) {
            CustomSettingsDialog(
                uiState = uiState,
                onUpdateLength = onUpdateLength,
                onUpdateCharset = onUpdateCharset,
                onUpdatePace = onUpdatePace,
                onToggleResume = onToggleResume,
                onToggleSingleAttemptMode = onToggleSingleAttemptMode,
                onUpdateSuccessKeywords = onUpdateSuccessKeywords,
                onUpdateCaptchaKeywords = onUpdateCaptchaKeywords,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
fun ConfigButtonUi(
    nodeType: NodeType,
    uiState: BruteforceState,
    onDrag: (deltaX: Float, deltaY: Float) -> Unit,
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
                },
            containerColor = if (isIdentified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        ) {
            val icon = when (nodeType) {
                NodeType.INPUT -> Icons.Default.Keyboard
                NodeType.SUBMIT -> Icons.Default.Send
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
private fun MindMapOptionsMenu(
    isVisible: Boolean,
    uiState: BruteforceState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onShowSettings: () -> Unit,
    onSelectDictionary: () -> Unit
) {
    val status = uiState.status

    val items = remember(status) {
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
        )
    }

    val animationProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    if (animationProgress > 0f) {
        val primaryColor = MaterialTheme.colorScheme.primary // Read color here
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = animationProgress)
        ) {
            val radius = 90.dp
            val angleStep = 360f / items.size

            Canvas(modifier = Modifier.matchParentSize()) {
                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                items.forEachIndexed { index, _ ->
                    val angleRad = Math.toRadians((angleStep * index - 90).toDouble()).toFloat()
                    val nodeCenter = androidx.compose.ui.geometry.Offset(
                        x = center.x + (radius.toPx() * animationProgress) * cos(angleRad),
                        y = center.y + (radius.toPx() * animationProgress) * sin(angleRad)
                    )
                    drawLine(
                        color = primaryColor, // Use the variable here
                        start = center,
                        end = nodeCenter,
                        strokeWidth = 2.dp.toPx() * animationProgress,
                        cap = StrokeCap.Round
                    )
                }
            }

            for ((index, item) in items.withIndex()) {
                val angleRad = Math.toRadians((angleStep * index - 90).toDouble()).toFloat()
                val density = LocalDensity.current
                val xOffset = with(density) { (radius.toPx() * cos(angleRad)).toDp() }
                val yOffset = with(density) { (radius.toPx() * sin(angleRad)).toDp() }

                Column(
                    modifier = Modifier.align(Alignment.Center).offset(x = xOffset, y = yOffset),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloatingActionButton(
                        onClick = { if (item.enabled) item.onClick() },
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(item.icon, contentDescription = item.text, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Text(
                        text = item.text,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomSettingsDialog(
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

    // Full-screen box to act as a scrim, consuming touch events
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) {
                detectTapGestures {
                    // Consume taps to prevent interaction with elements underneath
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 500.dp) // Set a max height for scrollability
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Advanced Settings", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content area
                Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
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

                Spacer(modifier = Modifier.height(16.dp))
                // Close button at the bottom
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
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

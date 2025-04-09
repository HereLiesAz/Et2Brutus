package com.hereliesaz.et2bruteforce.ui.overlay

import android.graphics.Point
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.hereliesaz.et2bruteforce.model.BruteforceState
import com.hereliesaz.et2bruteforce.model.BruteforceStatus
import com.hereliesaz.et2bruteforce.model.CharacterSetType
import kotlin.math.roundToInt

// Main entry point for the overlay UI
@Composable
fun OverlayUi(
    uiState: BruteforceState,
    onDrag: (deltaX: Float, deltaY: Float) -> Unit,
    onTap: () -> Unit, // Placeholder for simple tap action if needed
    onIdentifyInput: (Point) -> Unit,
    onIdentifySubmit: (Point) -> Unit,
    onIdentifyPopup: (Point) -> Unit,
    onUpdateLength: (Int) -> Unit,
    onUpdateCharset: (CharacterSetType) -> Unit,
    onUpdatePace: (Long) -> Unit,
    onToggleResume: (Boolean) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onConfirmSuccess: () -> Unit,
    onRejectSuccess: () -> Unit,
    onSelectDictionary: () -> Unit // Callback to request dictionary selection
) {
    var showOptionsMenu by rememberSaveable { mutableStateOf(false) }
    var fabCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var fabOffset by remember { mutableStateOf(IntOffset.Zero) } // For positioning popup

    val isIdentifying = uiState.status == BruteforceStatus.CONFIGURING_INPUT ||
            uiState.status == BruteforceStatus.CONFIGURING_SUBMIT ||
            uiState.status == BruteforceStatus.CONFIGURING_POPUP

    val density = LocalDensity.current

    Box(modifier = Modifier.wrapContentSize()) { // Wrap content to allow placement

        FloatingActionButton(
            onClick = {
                if (!isIdentifying) { // Only toggle menu if not in identification mode
                    showOptionsMenu = !showOptionsMenu
                    onTap() // Call generic tap handler
                }
                // If identifying, the drag gesture end handles coordinate reporting
            },
            shape = CircleShape,
            modifier = Modifier
                .offset { fabOffset } // Apply offset for popup positioning if needed
                .onGloballyPositioned { coordinates ->
                    fabCoordinates = coordinates
                    // Update offset based on position if needed for popup
                    // fabOffset = IntOffset(coordinates.positionInRoot().x.roundToInt(), coordinates.positionInRoot().y.roundToInt())
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { showOptionsMenu = false }, // Hide menu on drag start
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        },
                        onDragEnd = {
                            // If in identification mode, report coordinates
                            fabCoordinates?.let { coords ->
                                val rootPos = coords.positionInRoot()
                                val centerX = rootPos.x + coords.size.width / 2f
                                val centerY = rootPos.y + coords.size.height / 2f
                                val point = Point(centerX.roundToInt(), centerY.roundToInt())

                                when (uiState.status) {
                                    BruteforceStatus.CONFIGURING_INPUT -> onIdentifyInput(point)
                                    BruteforceStatus.CONFIGURING_SUBMIT -> onIdentifySubmit(point)
                                    BruteforceStatus.CONFIGURING_POPUP -> onIdentifyPopup(point)
                                    else -> { /* Do nothing on drag end */ }
                                }
                            }
                        }
                    )
                }
        ) {
            // Change icon based on state
            val icon = when (uiState.status) {
                BruteforceStatus.RUNNING -> Icons.Default.Pause
                BruteforceStatus.PAUSED, BruteforceStatus.READY -> Icons.Default.PlayArrow
                BruteforceStatus.CONFIGURING_INPUT,
                BruteforceStatus.CONFIGURING_SUBMIT,
                BruteforceStatus.CONFIGURING_POPUP -> Icons.Default.GpsFixed // Target icon
                BruteforceStatus.SUCCESS_DETECTED -> Icons.Default.CheckCircle
                BruteforceStatus.CAPTCHA_DETECTED -> Icons.Default.Security // Shield or similar
                BruteforceStatus.ERROR,
                BruteforceStatus.DICTIONARY_LOAD_FAILED -> Icons.Default.Error
                else -> Icons.Default.Settings // Idle state
            }
            Icon(icon, contentDescription = "Main Action Button")
        }

        // Options Menu Popup (Mind Map style needs custom layout, using simple Popup for now)
        if (showOptionsMenu) {
            // Calculate offset relative to the FAB if needed
            val popupOffset = DpOffset(x = 60.dp, y = (-30).dp) // Adjust as needed

            Popup(
                alignment = Alignment.TopStart, // Align relative to parent Box
                offset = popupOffset,
                properties = PopupProperties(focusable = false, dismissOnClickOutside = true),
                onDismissRequest = { showOptionsMenu = false }
            ) {
                OptionsMenuContent(
                    uiState = uiState,
                    onUpdateLength = onUpdateLength,
                    onUpdateCharset = onUpdateCharset,
                    onUpdatePace = onUpdatePace,
                    onToggleResume = onToggleResume,
                    onStart = onStart,
                    onPause = onPause,
                    onStop = onStop,
                    onConfirmSuccess = onConfirmSuccess,
                    onRejectSuccess = onRejectSuccess,
                    onSelectDictionary = onSelectDictionary,
                    onClose = { showOptionsMenu = false }
                )
            }
        }

        // Status indicator Text (optional, could be part of options menu)
        AnimatedVisibility(
            visible = uiState.status != BruteforceStatus.IDLE && !showOptionsMenu,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = getStatusMessage(uiState),
                modifier = Modifier
                    .offset(y = 60.dp) // Position below FAB
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Composable for the contents of the options popup/menu
@Composable
private fun OptionsMenuContent(
    uiState: BruteforceState,
    onUpdateLength: (Int) -> Unit,
    onUpdateCharset: (CharacterSetType) -> Unit,
    onUpdatePace: (Long) -> Unit,
    onToggleResume: (Boolean) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onConfirmSuccess: () -> Unit,
    onRejectSuccess: () -> Unit,
    onSelectDictionary: () -> Unit,
    onClose: () -> Unit
) {
    val settings = uiState.settings
    val status = uiState.status

    Card(
        modifier = Modifier.widthIn(max = 300.dp), // Limit width
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Options", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Options")
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Status Specific Actions
            if (status == BruteforceStatus.SUCCESS_DETECTED) {
                SuccessConfirmation(uiState.successCandidate ?: "", onConfirmSuccess, onRejectSuccess)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            } else if (status == BruteforceStatus.CAPTCHA_DETECTED) {
                Text("CAPTCHA Detected!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Text("Manual intervention required.", style = MaterialTheme.typography.bodySmall)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // General Controls (Start/Pause/Stop)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = onStart, enabled = status == BruteforceStatus.READY || status == BruteforceStatus.PAUSED) { Text("Start") }
                Button(onClick = onPause, enabled = status == BruteforceStatus.RUNNING) { Text("Pause") }
                Button(onClick = onStop, enabled = status != BruteforceStatus.IDLE) { Text("Stop") }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Character Length Slider
            Text("Length: ${settings.characterLength}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.characterLength.toFloat(),
                onValueChange = { onUpdateLength(it.roundToInt()) },
                valueRange = 1f..12f,
                steps = 10 // 11 steps for 12 values (0 to 11 corresponds to 1 to 12)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Character Set Picker
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


            // Pace Input
            var paceText by remember(settings.attemptPaceMillis) { mutableStateOf(settings.attemptPaceMillis.toString()) }
            OutlinedTextField(
                value = paceText,
                onValueChange = {
                    paceText = it
                    it.toLongOrNull()?.let { pace -> onUpdatePace(pace) }
                },
                label = { Text("Pace (ms)") },
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                leadingIcon = { Icon(Icons.Default.Timer, null)}
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Dictionary Selection
            Button(onClick = onSelectDictionary, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Book, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(settings.dictionaryUri?.substringAfterLast('/') ?: "Load Dictionary", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Resume Toggle
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

            // Display Current Attempt / Progress (if running)
            if (status == BruteforceStatus.RUNNING || status == BruteforceStatus.PAUSED) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Attempt #${uiState.attemptCount}: ${uiState.currentAttempt ?: "Starting..."}", style = MaterialTheme.typography.bodyMedium)
                if (settings.dictionaryUri != null && uiState.dictionaryLoadProgress < 1.0f && uiState.dictionaryLoadProgress > 0f) {
                    LinearProgressIndicator(progress = uiState.dictionaryLoadProgress, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
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
        modifier = Modifier.height(32.dp) // Make chips smaller
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

// Helper to get a user-friendly status message
private fun getStatusMessage(uiState: BruteforceState): String {
    return when (uiState.status) {
        BruteforceStatus.IDLE -> "Idle"
        BruteforceStatus.CONFIGURING_INPUT -> "Tap over Input Field"
        BruteforceStatus.CONFIGURING_SUBMIT -> "Tap over Submit Button"
        BruteforceStatus.CONFIGURING_POPUP -> "Tap over Popup Button"
        BruteforceStatus.READY -> "Ready"
        BruteforceStatus.RUNNING -> "Running: ${uiState.currentAttempt ?: ""} (${uiState.attemptCount})"
        BruteforceStatus.PAUSED -> "Paused (${uiState.attemptCount})"
        BruteforceStatus.CAPTCHA_DETECTED -> "CAPTCHA!"
        BruteforceStatus.SUCCESS_DETECTED -> "Success!"
        BruteforceStatus.DICTIONARY_LOAD_FAILED -> "Dict Load Fail"
        BruteforceStatus.ERROR -> "Error"
    }
}

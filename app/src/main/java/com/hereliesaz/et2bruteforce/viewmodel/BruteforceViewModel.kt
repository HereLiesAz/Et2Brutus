package com.hereliesaz.et2bruteforce.viewmodel

import androidx.compose.ui.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.et2bruteforce.comms.*
import com.hereliesaz.et2bruteforce.data.SettingsRepository
import com.hereliesaz.et2bruteforce.domain.BruteforceEngine
import com.hereliesaz.et2bruteforce.model.BruteforceState
import com.hereliesaz.et2bruteforce.model.BruteforceSettings
import com.hereliesaz.et2bruteforce.model.BruteforceStatus
import com.hereliesaz.et2bruteforce.model.CharacterSetType
import com.hereliesaz.et2bruteforce.model.HighlightInfo
import com.hereliesaz.et2bruteforce.model.NodeType
import kotlinx.coroutines.delay
import com.hereliesaz.et2bruteforce.model.Profile
import com.hereliesaz.et2bruteforce.services.NodeInfo
import com.hereliesaz.et2bruteforce.services.ScreenAnalysisResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor5
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor6
import com.hereliesaz.et2bruteforce.ui.theme.WalkthroughColor7
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class BruteforceViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val bruteforceEngine: BruteforceEngine,
    private val commsManager: AccessibilityCommsManager
) : ViewModel() {

    companion object {
        private const val TAG = "BruteforceViewModel"
    }

    private val _uiState = MutableStateFlow(BruteforceState())
    val uiState: StateFlow<BruteforceState> = _uiState.asStateFlow()

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private var bruteforceJob: Job? = null
    private var highlightJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            settingsRepository.profilesFlow.collect { profiles ->
                _profiles.value = profiles
            }
        }
        observeAccessibilityEvents()
    }

    fun saveProfile(name: String) {
        viewModelScope.launch {
            val existingProfile = _profiles.value.find { it.name == name }
            if (existingProfile != null) {
                _saveError.value = "A profile with this name already exists."
                return@launch
            }
            _saveError.value = null
            val newProfile = Profile(name, _uiState.value.buttonConfigs)
            val updatedProfiles = _profiles.value + newProfile
            settingsRepository.saveProfiles(updatedProfiles)
        }
    }

    fun loadProfile(profile: Profile) {
        _uiState.update { it.copy(buttonConfigs = profile.buttonConfigs) }
        checkIfReady()
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            val updatedProfiles = _profiles.value - profile
            settingsRepository.saveProfiles(updatedProfiles)
        }
    }

    fun renameProfile(profile: Profile, newName: String) {
        viewModelScope.launch {
            val updatedProfiles = _profiles.value.map {
                if (it == profile) {
                    it.copy(name = newName)
                } else {
                    it
                }
            }
            settingsRepository.saveProfiles(updatedProfiles)
        }
    }

    private fun observeAccessibilityEvents() {
        commsManager.nodeIdentifiedEvent
            .onEach { event ->
                Log.d(TAG, "Received NodeIdentifiedEvent [${event.requestId}]: ${event.nodeType}, Success: ${event.nodeInfo != null}")
                handleNodeIdentificationResult(event)
            }
            .launchIn(viewModelScope)

        commsManager.nodeHighlightedEvent
            .onEach { event ->
                Log.d(TAG, "Received NodeHighlightedEvent [${event.requestId}]: Success: ${event.bounds != null}")
                val bounds = event.bounds
                if (bounds != null) {
                    _uiState.update { it.copy(highlightedInfo = HighlightInfo(bounds, event.nodeType)) }
                } else {
                    _uiState.update { it.copy(highlightedInfo = null) }
                }
            }
            .launchIn(viewModelScope)
        // No need for active listeners for ActionCompleted or AnalysisResult here
        // as the bruteforce loop handles them via requestAndWaitForEvent
    }

    private fun handleNodeIdentificationResult(event: NodeIdentifiedEvent) {
        _uiState.update { currentState ->
            val newConfigs = currentState.buttonConfigs.toMutableMap()
            val existingConfig = newConfigs[event.nodeType]
            if (existingConfig != null) {
                newConfigs[event.nodeType] = existingConfig.copy(identifiedNodeInfo = event.nodeInfo)
                Log.d(TAG, "Updated node info for ${event.nodeType}. Success: ${event.nodeInfo != null}")
            }
            currentState.copy(buttonConfigs = newConfigs)
        }
        // After updating, check if we are now ready to start
        checkIfReady()
    }

    private fun checkIfReady() {
        val configs = uiState.value.buttonConfigs
        val inputReady = configs[NodeType.INPUT]?.identifiedNodeInfo != null
        val submitReady = configs[NodeType.SUBMIT]?.identifiedNodeInfo != null
        if (inputReady && submitReady && uiState.value.status == BruteforceStatus.IDLE) {
            updateStatus(BruteforceStatus.READY)
        } else if (!inputReady || !submitReady) {
            if (uiState.value.status == BruteforceStatus.READY) {
                updateStatus(BruteforceStatus.IDLE)
            }
        }
    }

    // --- Configuration Methods ---
    fun updateCharacterLength(length: Int) {
        viewModelScope.launch {
            val newLength = length.coerceIn(1, 12)
            settingsRepository.updateCharacterLength(newLength)
            settingsRepository.updateLastAttempt(null)
            _uiState.update { it.copy(attemptCount = 0) }
        }
    }
    fun updateCharacterSet(type: CharacterSetType) {
        viewModelScope.launch {
            settingsRepository.updateCharacterSetType(type)
            settingsRepository.updateLastAttempt(null)
            _uiState.update { it.copy(attemptCount = 0) }
        }
    }
    fun updateAttemptPace(pace: Long) {
        viewModelScope.launch {
            val newPace = pace.coerceIn(0, 10000)
            settingsRepository.updateAttemptPace(newPace)
        }
    }
    fun updateDictionaryUri(uri: Uri?) {
        viewModelScope.launch {
            settingsRepository.updateDictionaryUri(uri)
            if (uri != null) settingsRepository.updateLastAttempt(null)
            _uiState.update { it.copy(attemptCount = 0) }
        }
    }
    fun toggleResumeFromLast(resume: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateResumeFromLast(resume)
        }
    }

    fun toggleSingleAttemptMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSingleAttemptMode(enabled)
        }
    }

    fun updateSuccessKeywords(keywords: List<String>) {
        viewModelScope.launch {
            settingsRepository.updateSuccessKeywords(keywords)
        }
    }

    fun updateCaptchaKeywords(keywords: List<String>) {
        viewModelScope.launch {
            settingsRepository.updateCaptchaKeywords(keywords)
        }
    }

    // --- New Action Request Methods ---
    fun updateButtonPosition(viewKey: Any, newPosition: Point) {
        when (viewKey) {
            is NodeType -> {
                _uiState.update { currentState ->
                    val newConfigs = currentState.buttonConfigs.toMutableMap()
                    val existingConfig = newConfigs[viewKey]
                    if (existingConfig != null) {
                        newConfigs[viewKey] = existingConfig.copy(position = newPosition)
                    }
                    currentState.copy(buttonConfigs = newConfigs)
                }
                highlightNodeAt(newPosition, viewKey)
            }
            is String -> { // Assuming MAIN_CONTROLLER_KEY is a String
                viewModelScope.launch {
                    settingsRepository.updateControllerPosition(newPosition)
                }
            }
        }
    }

    fun identifyNodeAt(nodeType: NodeType, point: Point) {
        val requestId = generateRequestId()
        Log.d(TAG, "Requesting node identification for $nodeType at $point [${requestId}]")
        viewModelScope.launch {
            commsManager.requestNodeIdentification(NodeIdentificationRequest(point, nodeType, requestId))
        }
    }

    fun highlightNodeAt(point: Point, nodeType: NodeType) {
        highlightJob?.cancel()
        highlightJob = viewModelScope.launch {
            delay(50) // Debounce delay
            val requestId = generateRequestId()
            Log.d(TAG, "Requesting node highlight for $nodeType at $point [${requestId}]")
            commsManager.requestNodeHighlight(HighlightNodeRequest(point, nodeType, requestId))
        }
    }

    fun clearHighlight() {
        _uiState.update { it.copy(highlightedInfo = null) }
    }

    fun toggleActionButtons() {
        _uiState.update { it.copy(actionButtonsEnabled = !it.actionButtonsEnabled) }
    }

    // --- Bruteforce Action Methods ---
    fun startBruteforce() {
        val currentUiState = uiState.value
        if (currentUiState.status != BruteforceStatus.READY && currentUiState.status != BruteforceStatus.PAUSED) {
            Log.w(TAG, "Cannot start, state is ${currentUiState.status}")
            _uiState.update { it.copy(errorMessage = "Not in a startable state.") }
            return
        }
        val inputNode = currentUiState.buttonConfigs[NodeType.INPUT]?.identifiedNodeInfo
        val submitNode = currentUiState.buttonConfigs[NodeType.SUBMIT]?.identifiedNodeInfo
        val popupNode = currentUiState.buttonConfigs[NodeType.POPUP]?.identifiedNodeInfo

        if (inputNode == null || submitNode == null) {
            Log.e(TAG, "Cannot start, input or submit node not identified.")
            _uiState.update { it.copy(errorMessage = "Input or Submit node missing.") }
            updateStatus(BruteforceStatus.ERROR)
            return
        }

        Log.i(TAG, "Starting bruteforce process...")
        updateStatus(BruteforceStatus.RUNNING)
        _uiState.update { it.copy(errorMessage = null, successCandidate = null) }

        bruteforceJob?.cancel()
        bruteforceJob = viewModelScope.launch {
            // Make local copies of potentially mutable state for this run
            val currentSettings = settingsRepository.getSettingsSnapshot() // Use snapshot for consistency during run
            var attemptCounter = currentUiState.attemptCount // Resume count if paused

            try {
                // --- Dictionary Phase ---
                if (!currentSettings.dictionaryUri.isNullOrEmpty()) {
                    Log.d(TAG, "Starting dictionary phase.")
                    var dictionaryCompletedSuccessfully = false
                    bruteforceEngine.generateDictionaryCandidates(currentSettings)
                        .onCompletion { if (it == null) dictionaryCompletedSuccessfully = true } // Mark completion if no error
                        .catch { e ->
                            Log.e(TAG, "Error in dictionary flow", e)
                            _uiState.update { it.copy(status = BruteforceStatus.DICTIONARY_LOAD_FAILED, errorMessage = "Dictionary Error: ${e.message}") }
                            // Stop on dictionary error
                            bruteforceJob?.cancel() // Cancel the collector coroutine
                        }
                        .collect { (candidate, progress) ->
                            ensureActive() // Check if job was cancelled externally (e.g., user stop)
                            if (uiState.value.status != BruteforceStatus.RUNNING) throw CancellationException("Status changed externally.")

                            Log.v(TAG, "Attempting dictionary word: $candidate")
                            _uiState.update { it.copy(currentAttempt = candidate, dictionaryLoadProgress = progress, attemptCount = attemptCounter + 1) } // Update count immediately

                            val attemptResult = performSingleAttempt(inputNode, submitNode, popupNode, candidate, currentSettings)

                            // Update last attempt *after* successful processing of the attempt
                            settingsRepository.updateLastAttempt(candidate)
                            attemptCounter++ // Increment logical counter after successful attempt cycle

                            // Handle result (cancel job if needed)
                            handleAttemptResult(attemptResult, candidate)

                            // If single attempt mode is on, pause here.
                            if (currentSettings.singleAttemptMode) {
                                pauseBruteforce()
                                return@collect
                            }
                        }
                    // Reset last attempt only if dictionary completed successfully and we are still running
                    if (dictionaryCompletedSuccessfully && isActive && uiState.value.status == BruteforceStatus.RUNNING) {
                        Log.d(TAG,"Dictionary phase complete, resetting last attempt for permutations.")
                        settingsRepository.updateLastAttempt(null)
                    }
                } else {
                    Log.d(TAG, "Skipping dictionary phase.")
                }

                // --- Permutation Phase ---
                if (isActive && uiState.value.status == BruteforceStatus.RUNNING) {
                    Log.d(TAG, "Starting permutation phase.")
                    val permutationSettings = settingsRepository.getSettingsSnapshot() // Get fresh settings (lastAttempt might be null now)

                    bruteforceEngine.generatePermutationCandidates(permutationSettings)
                        .catch { e ->
                            Log.e(TAG, "Error in permutation flow", e)
                            updateStatus(BruteforceStatus.ERROR)
                            _uiState.update { it.copy(errorMessage = "Permutation Error: ${e.message}") }
                            bruteforceJob?.cancel()
                        }
                        .collect { candidate ->
                            ensureActive()
                            if (uiState.value.status != BruteforceStatus.RUNNING) throw CancellationException("Status changed externally.")

                            Log.v(TAG, "Attempting permutation: $candidate")
                            _uiState.update { it.copy(currentAttempt = candidate, dictionaryLoadProgress = 1f, attemptCount = attemptCounter + 1)} // Update count immediately

                            val attemptResult = performSingleAttempt(inputNode, submitNode, popupNode, candidate, permutationSettings)

                            settingsRepository.updateLastAttempt(candidate)
                            attemptCounter++

                            handleAttemptResult(attemptResult, candidate)

                            // If single attempt mode is on, pause here.
                            if (permutationSettings.singleAttemptMode) {
                                pauseBruteforce()
                                return@collect
                            }
                        }
                }

                // --- Job Completion ---
                if (isActive && uiState.value.status == BruteforceStatus.RUNNING) {
                    Log.i(TAG, "Bruteforce finished all candidates without success.")
                    updateStatus(BruteforceStatus.IDLE)
                    settingsRepository.updateLastAttempt(null)
                    _uiState.update { it.copy(attemptCount = 0) }
                }

            } catch (e: CancellationException) {
                Log.i(TAG, "Bruteforce job cancelled or stopped externally. Status: ${uiState.value.status}")
                // Don't update status here if it was changed externally (e.g., Pause/Stop button)
            } catch (e: ActionFailedException) {
                Log.e(TAG, "Accessibility action failed during bruteforce: ${e.message}")
                if (isActive) { // Avoid changing status if already cancelled
                    updateStatus(BruteforceStatus.ERROR)
                    _uiState.update { it.copy(errorMessage = "Action Failed: ${e.message}") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during bruteforce execution", e)
                if (isActive) {
                    updateStatus(BruteforceStatus.ERROR)
                    _uiState.update { it.copy(errorMessage = "Runtime Error: ${e.message}") }
                }
            } finally {
                Log.d(TAG,"Bruteforce Job Coroutine exiting.")
            }
        }
    }

    // Helper function to handle result from performSingleAttempt
    private suspend fun handleAttemptResult(result: AttemptResult, candidate: String) {
        when (result) {
            AttemptResult.SUCCESS -> {
                updateStatus(BruteforceStatus.SUCCESS_DETECTED)
                _uiState.update { it.copy(successCandidate = candidate)}
                bruteforceJob?.cancel() // Stop the current job
            }
            AttemptResult.CAPTCHA -> {
                updateStatus(BruteforceStatus.CAPTCHA_DETECTED)
                _uiState.update { it.copy(errorMessage = "CAPTCHA Detected!") }
                bruteforceJob?.cancel() // Stop the current job
            }
            AttemptResult.POPUP_UNHANDLED -> {
                // Pause, requiring user intervention
                updateStatus(BruteforceStatus.PAUSED)
                _uiState.update { it.copy(errorMessage = "Popup detected, configure popup button.")}
                bruteforceJob?.cancel() // Stop the current job
            }
            AttemptResult.FAILURE -> {
                // Wait before next attempt (pace is handled in settings now)
                val currentPace = settingsRepository.getSettingsSnapshot().attemptPaceMillis
                if (currentPace > 0) delay(currentPace)
            }
        }
    }

    // Custom exception class
    class ActionFailedException(message: String): RuntimeException(message)

    // Performs one full attempt cycle (input -> click -> analyze -> handle popup?)
    private suspend fun performSingleAttempt(
        inputNode: NodeInfo,
        submitNode: NodeInfo,
        popupNode: NodeInfo?, // Can be null
        candidate: String,
        settings: BruteforceSettings
    ): AttemptResult = suspendCoroutine { continuation ->
        val attemptScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob())
        attemptScope.launch {
            try {
                val inputRequestId = generateRequestId()
                val submitRequestId = generateRequestId()
                val analysisRequestId = generateRequestId()
                var popupRequestId: String?

                // 1. Input Text
                Log.d(TAG, "performSingleAttempt: Requesting input text [${inputRequestId}]")
                val inputTextSuccess = requestActionAndWait(inputRequestId, 5000L) { // Add timeout
                    commsManager.requestInputText(InputTextRequest(inputNode, candidate, inputRequestId))
                }
                if (!inputTextSuccess) throw ActionFailedException("Input Text Failed [$inputRequestId]")

                // Optional delay after input before clicking submit? Might help target app process input.
                // delay(50)

                // 2. Click Submit
                Log.d(TAG, "performSingleAttempt: Requesting click submit [${submitRequestId}]")
                val clickSubmitSuccess = requestActionAndWait(submitRequestId, 5000L) {
                    commsManager.requestClickNode(ClickNodeRequest(submitNode, submitRequestId))
                }
                if (!clickSubmitSuccess) throw ActionFailedException("Click Submit Failed [$submitRequestId]")

                // 3. Wait for reaction (Crucial - Needs tuning)
                delay(settings.attemptPaceMillis.coerceIn(200, 5000)) // Use pace, but bounded

                // 4. Analyze Screen
                Log.d(TAG, "performSingleAttempt: Requesting screen analysis [${analysisRequestId}]")
                val analysisResult = requestAnalysisAndWait(analysisRequestId, 10000L) { // Longer timeout for analysis
                    commsManager.requestAnalyzeScreen(
                        AnalyzeScreenRequest(settings.successKeywords, settings.captchaKeywords, analysisRequestId)
                    )
                }

                // 5. Handle Result
                when (analysisResult) {
                    ScreenAnalysisResult.SuccessDetected -> if (isActive) continuation.resume(AttemptResult.SUCCESS)
                    ScreenAnalysisResult.CaptchaDetected -> if (isActive) continuation.resume(AttemptResult.CAPTCHA)
                    ScreenAnalysisResult.Unknown -> {
                        if (popupNode != null) {
                            popupRequestId = generateRequestId()
                            Log.d(TAG, "Analysis failed, attempting to click configured popup node [$popupRequestId].")
                            val clickPopupSuccess = requestActionAndWait(popupRequestId, 5000L) {
                                commsManager.requestClickNode(ClickNodeRequest(popupNode, popupRequestId!!))
                            }
                            if (clickPopupSuccess) {
                                Log.d(TAG, "Clicked popup successfully. Assuming failure for this cycle.")
                                delay(200) // Small delay after clicking popup
                                if (isActive) continuation.resume(AttemptResult.FAILURE)
                            } else {
                                Log.w(TAG, "Failed to click configured popup. Pausing.")
                                if (isActive) continuation.resume(AttemptResult.POPUP_UNHANDLED)
                            }
                        } else {
                            // No popup configured, assume standard failure
                            if (isActive) continuation.resume(AttemptResult.FAILURE)
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Attempt coroutine cancelled.")
                // Don't resume continuation if cancelled
            } catch (e: Exception) {
                Log.e(TAG, "Error within performSingleAttempt coroutine: ${e.message}")
                if (isActive) {
                    continuation.resume(AttemptResult.FAILURE) // Resume with failure on unexpected error
                }
            } finally {
                // Ensure the scope is cancelled if not already
                attemptScope.cancel()
            }
        }
    }


    // Revised helper to wait for event matching specific request ID with timeout
    private suspend inline fun <reified E: Any> requestAndWaitForEvent(
        requestId: String,
        timeoutMillis: Long,
        crossinline requestAction: suspend () -> Unit,
        eventFlow: SharedFlow<E>,
        crossinline idExtractor: (E) -> String
    ): E? = withTimeoutOrNull(timeoutMillis) {
        var result: E? = null
        // Use launch for listening, allows request to happen immediately after
        val listenerJob = launch {
            result = eventFlow
                .filter { event -> idExtractor(event) == requestId }
                .first() // Only take the first event matching the ID
        }
        // Send the request *after* starting the listener
        requestAction()

        // Wait for the listener job to complete (or timeout)
        listenerJob.join()
        listenerJob.cancel() // Ensure listener is cancelled if request completes early or times out
        result // Return the captured result
    }

    // Updated helper for ActionCompletedEvent using the generic helper
    private suspend fun requestActionAndWait(requestId: String, timeout: Long, action: suspend () -> Unit): Boolean {
        val event = requestAndWaitForEvent(
            requestId = requestId,
            timeoutMillis = timeout,
            requestAction = action,
            eventFlow = commsManager.actionCompletedEvent,
            idExtractor = { it.requestId }
        )
        if(event == null) Log.w(TAG, "Action timed out for request [$requestId]")
        return event?.success ?: false
    }

    // Updated helper for AnalysisResultEvent using the generic helper
    private suspend fun requestAnalysisAndWait(requestId: String, timeout: Long, action: suspend () -> Unit): ScreenAnalysisResult {
        val event = requestAndWaitForEvent(
            requestId = requestId,
            timeoutMillis = timeout,
            requestAction = action,
            eventFlow = commsManager.analysisResultEvent,
            idExtractor = { it.requestId }
        )
        if(event == null) Log.w(TAG, "Analysis timed out for request [$requestId]")
        return event?.result ?: ScreenAnalysisResult.Unknown
    }

    // --- Pause, Stop, Confirm, Reject Methods ---
    fun pauseBruteforce() {
        if (uiState.value.status == BruteforceStatus.RUNNING) {
            bruteforceJob?.cancel() // Cancel the current job
            bruteforceJob = null
            updateStatus(BruteforceStatus.PAUSED)
            Log.i(TAG, "Bruteforce Paused. Last attempt: ${uiState.value.currentAttempt}")
        }
    }
    fun stopBruteforce() {
        bruteforceJob?.cancel()
        bruteforceJob = null
        viewModelScope.launch { settingsRepository.updateLastAttempt(null) } // Clear last attempt on stop
        // Reset button states but keep their positions
        _uiState.update { currentState ->
            val newConfigs = currentState.buttonConfigs.mapValues { entry ->
                entry.value.copy(identifiedNodeInfo = null)
            }
            currentState.copy(
                currentAttempt = null,
                attemptCount = 0,
                dictionaryLoadProgress = 0f,
                buttonConfigs = newConfigs
            )
        }
        updateStatus(BruteforceStatus.IDLE)
        Log.i(TAG, "Bruteforce Stopped.")
    }
    fun confirmSuccess() {
        if(uiState.value.status == BruteforceStatus.SUCCESS_DETECTED) {
            Log.i(TAG, "Success confirmed by user for: ${uiState.value.successCandidate}")
            updateStatus(BruteforceStatus.IDLE) // Go back to idle
            // Optional: Clear last attempt
            // viewModelScope.launch { settingsRepository.updateLastAttempt(null) }
        }
    }
    fun rejectSuccess() {
        if(uiState.value.status == BruteforceStatus.SUCCESS_DETECTED) {
            Log.w(TAG, "Success rejected by user for: ${uiState.value.successCandidate}. Resuming might be needed.")
            // Go back to paused, user needs to manually start again
            updateStatus(BruteforceStatus.PAUSED)
            _uiState.update { it.copy(successCandidate = null) } // Clear candidate
            // lastAttempt should still be the rejected one, so start will resume after it.
        }
    }


    // Helper to update status cleanly
    private fun updateStatus(newStatus: BruteforceStatus) {
        _uiState.update { it.copy(status = newStatus) }
    }

    override fun onCleared() {
        super.onCleared()
        bruteforceJob?.cancel()
    }
}

// Private enum, only used within ViewModel logic loop
private enum class AttemptResult {
    SUCCESS,
    FAILURE,
    CAPTCHA,
    POPUP_UNHANDLED
}

// Helper function can live here or in CommsManager
fun generateRequestId(): String = UUID.randomUUID().toString()
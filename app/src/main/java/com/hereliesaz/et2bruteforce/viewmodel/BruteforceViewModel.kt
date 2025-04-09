package com.hereliesaz.et2bruteforce.viewmodel

import android.graphics.Point
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.et2bruteforce.comms.*
import com.hereliesaz.et2bruteforce.data.SettingsRepository
import com.hereliesaz.et2bruteforce.domain.BruteforceEngine
import com.hereliesaz.et2bruteforce.model.* // Includes BruteforceState, CharacterSetType, NodeType
import com.hereliesaz.et2bruteforce.services.AccessibilityInteractionManager
import com.hereliesaz.et2bruteforce.services.NodeInfo
import com.hereliesaz.et2bruteforce.services.ScreenAnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID // Import UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class BruteforceViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val bruteforceEngine: BruteforceEngine,
    private val interactionManager: AccessibilityInteractionManager,
    private val commsManager: AccessibilityCommsManager
) : ViewModel() {

    companion object {
        private const val TAG = "BruteforceViewModel"
    }

    private val _uiState = MutableStateFlow(BruteforceState())
    val uiState: StateFlow<BruteforceState> = _uiState.asStateFlow()

    private var bruteforceJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        observeAccessibilityEvents()
    }

    private fun observeAccessibilityEvents() {
        commsManager.nodeIdentifiedEvent
            .onEach { event ->
                Log.d(TAG, "Received NodeIdentifiedEvent [${event.requestId}]: ${event.nodeType}, Success: ${event.nodeInfo != null}")
                handleNodeIdentificationResult(event)
            }
            .launchIn(viewModelScope)
        // No need for active listeners for ActionCompleted or AnalysisResult here
        // as the bruteforce loop handles them via requestAndWaitForEvent
    }

    // Helper to process node identification results and update state machine
    private fun handleNodeIdentificationResult(event: NodeIdentifiedEvent) {
        val currentStatus = uiState.value.status
        if (event.nodeInfo != null) {
            // Success
            when (event.nodeType) {
                NodeType.INPUT -> {
                    if (currentStatus == BruteforceStatus.CONFIGURING_INPUT) {
                        updateStatus(BruteforceStatus.CONFIGURING_SUBMIT)
                        _uiState.update { it.copy(errorMessage = "Input identified. Now place marker over Submit.")}
                    }
                }
                NodeType.SUBMIT -> {
                    if (currentStatus == BruteforceStatus.CONFIGURING_SUBMIT) {
                        updateStatus(BruteforceStatus.READY)
                        _uiState.update { it.copy(errorMessage = "Submit identified. Ready to Start.")}
                    }
                }
                NodeType.POPUP -> {
                    if (currentStatus == BruteforceStatus.CONFIGURING_POPUP) {
                        // Go back to paused/ready, let user manually resume if needed
                        updateStatus(BruteforceStatus.PAUSED)
                        _uiState.update { it.copy(errorMessage = "Popup configured. Resume manually.")}
                    }
                }
            }
        } else {
            // Failure
            _uiState.update { it.copy(errorMessage = "Failed to identify ${event.nodeType} node.") }
            // Revert status if we were waiting for this specific node type
            if ((event.nodeType == NodeType.INPUT && currentStatus == BruteforceStatus.CONFIGURING_INPUT) ||
                (event.nodeType == NodeType.SUBMIT && currentStatus == BruteforceStatus.CONFIGURING_SUBMIT) ||
                (event.nodeType == NodeType.POPUP && currentStatus == BruteforceStatus.CONFIGURING_POPUP)) {
                // Go back to a reasonable previous state, e.g., IDLE or READY if submit failed
                updateStatus(BruteforceStatus.IDLE) // Or based on which node failed
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

    // --- Action Request Methods (Called from UI) ---
    fun requestInputConfiguration(coords: Point) {
        updateStatus(BruteforceStatus.CONFIGURING_INPUT)
        interactionManager.clearAllNodes() // Clear previous nodes before identifying new ones
        val requestId = generateRequestId()
        Log.d(TAG, "Requesting input field configuration [${requestId}] via CommsManager at $coords")
        _uiState.update { it.copy(errorMessage = "Place marker over Input field and release.")} // UI hint
        viewModelScope.launch {
            commsManager.requestNodeIdentification(NodeIdentificationRequest(coords, NodeType.INPUT, requestId))
        }
    }

    fun requestSubmitConfiguration(coords: Point) {
        if (interactionManager.identifiedInputNode.value == null) {
            Log.w(TAG, "Cannot configure submit, input not identified yet.")
            _uiState.update { it.copy(errorMessage = "Identify Input Field First") }
            return
        }
        updateStatus(BruteforceStatus.CONFIGURING_SUBMIT)
        val requestId = generateRequestId()
        Log.d(TAG, "Requesting submit button configuration [${requestId}] via CommsManager at $coords")
        _uiState.update { it.copy(errorMessage = "Place marker over Submit button and release.")} // UI hint
        viewModelScope.launch {
            commsManager.requestNodeIdentification(NodeIdentificationRequest(coords, NodeType.SUBMIT, requestId))
        }
    }

    fun requestPopupConfiguration(coords: Point) {
        // Should ideally only be callable when paused/error/captcha state requires it
        if(uiState.value.status != BruteforceStatus.PAUSED && uiState.value.status != BruteforceStatus.CAPTCHA_DETECTED) {
            Log.w(TAG, "Popup configuration requested in unexpected state: ${uiState.value.status}")
            // Optionally show error message
            // return
        }
        updateStatus(BruteforceStatus.CONFIGURING_POPUP)
        val requestId = generateRequestId()
        Log.d(TAG, "Requesting popup button configuration [${requestId}] via CommsManager at $coords")
        _uiState.update { it.copy(errorMessage = "Place marker over Popup button and release.")} // UI hint
        viewModelScope.launch {
            commsManager.requestNodeIdentification(NodeIdentificationRequest(coords, NodeType.POPUP, requestId))
        }
    }

    // --- Bruteforce Action Methods ---
    fun startBruteforce() {
        val currentUiState = uiState.value // Capture current state
        if (currentUiState.status != BruteforceStatus.READY && currentUiState.status != BruteforceStatus.PAUSED) {
            Log.w(TAG, "Cannot start, state is ${currentUiState.status}")
            _uiState.update { it.copy(errorMessage = "Configure Input/Submit first or invalid state.") }
            return
        }
        val inputNode = interactionManager.identifiedInputNode.value
        val submitNode = interactionManager.identifiedSubmitNode.value
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
                            this.coroutineContext.cancel() // Cancel the collector coroutine
                        }
                        .collect { (candidate, progress) ->
                            ensureActive() // Check if job was cancelled externally (e.g., user stop)
                            if (uiState.value.status != BruteforceStatus.RUNNING) throw CancellationException("Status changed externally.")

                            Log.v(TAG, "Attempting dictionary word: $candidate")
                            _uiState.update { it.copy(currentAttempt = candidate, dictionaryLoadProgress = progress, attemptCount = attemptCounter + 1) } // Update count immediately

                            val attemptResult = performSingleAttempt(inputNode, submitNode, candidate, currentSettings)

                            // Update last attempt *after* successful processing of the attempt
                            settingsRepository.updateLastAttempt(candidate)
                            attemptCounter++ // Increment logical counter after successful attempt cycle

                            // Handle result (cancel job if needed)
                            handleAttemptResult(attemptResult, candidate)
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
                            this.coroutineContext.cancel()
                        }
                        .collect { candidate ->
                            ensureActive()
                            if (uiState.value.status != BruteforceStatus.RUNNING) throw CancellationException("Status changed externally.")

                            Log.v(TAG, "Attempting permutation: $candidate")
                            _uiState.update { it.copy(currentAttempt = candidate, dictionaryLoadProgress = 1f, attemptCount = attemptCounter + 1)} // Update count immediately

                            val attemptResult = performSingleAttempt(inputNode, submitNode, candidate, permutationSettings)

                            settingsRepository.updateLastAttempt(candidate)
                            attemptCounter++

                            handleAttemptResult(attemptResult, candidate)
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
                coroutineContext.cancel() // Stop the current job
            }
            AttemptResult.CAPTCHA -> {
                updateStatus(BruteforceStatus.CAPTCHA_DETECTED)
                _uiState.update { it.copy(errorMessage = "CAPTCHA Detected!") }
                coroutineContext.cancel() // Stop the current job
            }
            AttemptResult.POPUP_UNHANDLED -> {
                // Pause, requiring user intervention
                updateStatus(BruteforceStatus.PAUSED)
                _uiState.update { it.copy(errorMessage = "Popup detected, configure popup button.")}
                coroutineContext.cancel() // Stop the current job
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
        candidate: String,
        settings: BruteforceSettings // Pass snapshot of settings for this attempt
    ): AttemptResult = suspendCoroutine { continuation ->
        // Use a dedicated child scope for this attempt to manage timeouts/cancellation per attempt?
        // Or rely on the main job cancellation. Sticking with main job for now.
        val attemptScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob()) // Child scope
        attemptScope.launch {
            try {
                val inputRequestId = generateRequestId()
                val submitRequestId = generateRequestId()
                val analysisRequestId = generateRequestId()
                var popupRequestId: String? = null

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
                    ScreenAnalysisResult.SuccessDetected -> if(continuation.context.isActive) continuation.resume(AttemptResult.SUCCESS)
                    ScreenAnalysisResult.CaptchaDetected -> if(continuation.context.isActive) continuation.resume(AttemptResult.CAPTCHA)
                    ScreenAnalysisResult.Unknown -> {
                        val popupNode = interactionManager.identifiedPopupNode.value // Check if popup configured
                        if (popupNode != null) {
                            popupRequestId = generateRequestId()
                            Log.d(TAG, "Coroutine: Attempting to click identified popup button [${popupRequestId}].")
                            val clickPopupSuccess = requestActionAndWait(popupRequestId, 5000L) {
                                commsManager.requestClickNode(ClickNodeRequest(popupNode, popupRequestId))
                            }
                            if (!clickPopupSuccess) {
                                Log.w(TAG,"Coroutine: Failed to click configured popup button [$popupRequestId].")
                                if(continuation.context.isActive) continuation.resume(AttemptResult.POPUP_UNHANDLED)
                            } else {
                                Log.d(TAG, "Coroutine: Clicked configured popup button [$popupRequestId].")
                                delay(200) // Small delay after clicking popup
                                if(continuation.context.isActive) continuation.resume(AttemptResult.FAILURE) // Assume failure for this cycle
                            }
                        } else {
                            // No known popup, assume standard failure
                            if(continuation.context.isActive) continuation.resume(AttemptResult.FAILURE)
                        }
                    }
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Attempt coroutine cancelled.")
                // Don't resume continuation if cancelled
            } catch (e: Exception) {
                Log.e(TAG, "Error within performSingleAttempt coroutine: ${e.message}")
                if (continuation.context.isActive) {
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
        _uiState.update { it.copy(currentAttempt = null, attemptCount = 0, dictionaryLoadProgress = 0f) }
        interactionManager.clearAllNodes()
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
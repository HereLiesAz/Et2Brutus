package com.hereliesaz.et2bruteforce.comms

import android.graphics.Point
import android.graphics.Rect
import com.hereliesaz.et2bruteforce.model.NodeType // <-- Updated Import
import android.net.Uri
import com.hereliesaz.et2bruteforce.services.NodeInfo
import com.hereliesaz.et2bruteforce.services.ScreenAnalysisResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Data classes for requests and events - requestId is now mandatory
data class InputTextRequest(val targetNodeInfo: NodeInfo, val text: String, val requestId: String)
data class ClickNodeRequest(val targetNodeInfo: NodeInfo, val requestId: String)
data class AnalyzeScreenRequest(val successKeywords: List<String>, val captchaKeywords: List<String>, val requestId: String)
data class NodeIdentificationRequest(val coordinates: Point, val nodeType: NodeType, val requestId: String)
data class HighlightNodeRequest(val coordinates: Point, val nodeType: NodeType, val requestId: String)

// Events must also carry the requestId for correlation
data class ActionCompletedEvent(val requestId: String, val success: Boolean)
data class NodeIdentifiedEvent(val requestId: String, val nodeInfo: NodeInfo?, val nodeType: NodeType) // Uses NodeType
data class AnalysisResultEvent(val requestId: String, val result: ScreenAnalysisResult)
data class NodeHighlightedEvent(val requestId: String, val bounds: Rect?, val nodeType: NodeType)


// Helper function remains the same, but caller needs to use it
fun generateRequestId(): String = UUID.randomUUID().toString()

@Singleton
class AccessibilityCommsManager @Inject constructor() {

    // --- Flows for VM -> Service Communication (Action Requests) ---
    private val _inputTextRequest = MutableSharedFlow<InputTextRequest>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val inputTextRequest: SharedFlow<InputTextRequest> = _inputTextRequest.asSharedFlow()

    private val _clickNodeRequest = MutableSharedFlow<ClickNodeRequest>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val clickNodeRequest: SharedFlow<ClickNodeRequest> = _clickNodeRequest.asSharedFlow()

    private val _analyzeScreenRequest = MutableSharedFlow<AnalyzeScreenRequest>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val analyzeScreenRequest: SharedFlow<AnalyzeScreenRequest> = _analyzeScreenRequest.asSharedFlow()

    private val _nodeIdentificationRequest = MutableSharedFlow<NodeIdentificationRequest>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nodeIdentificationRequest: SharedFlow<NodeIdentificationRequest> = _nodeIdentificationRequest.asSharedFlow()

    private val _nodeHighlightRequest = MutableSharedFlow<HighlightNodeRequest>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nodeHighlightRequest: SharedFlow<HighlightNodeRequest> = _nodeHighlightRequest.asSharedFlow()

    // --- Flows for Service -> VM Communication (Results / Events) ---
    private val _actionCompletedEvent = MutableSharedFlow<ActionCompletedEvent>(replay = 0, extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val actionCompletedEvent: SharedFlow<ActionCompletedEvent> = _actionCompletedEvent.asSharedFlow()

    private val _nodeIdentifiedEvent = MutableSharedFlow<NodeIdentifiedEvent>(replay = 0, extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nodeIdentifiedEvent: SharedFlow<NodeIdentifiedEvent> = _nodeIdentifiedEvent.asSharedFlow()

    private val _analysisResultEvent = MutableSharedFlow<AnalysisResultEvent>(replay = 0, extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val analysisResultEvent: SharedFlow<AnalysisResultEvent> = _analysisResultEvent.asSharedFlow()

    private val _nodeHighlightedEvent = MutableSharedFlow<NodeHighlightedEvent>(replay = 0, extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nodeHighlightedEvent: SharedFlow<NodeHighlightedEvent> = _nodeHighlightedEvent.asSharedFlow()

    // --- Methods for VM to Emit Requests (Now require request object) ---
    suspend fun requestInputText(request: InputTextRequest) {
        _inputTextRequest.emit(request)
    }

    suspend fun requestClickNode(request: ClickNodeRequest) {
        _clickNodeRequest.emit(request)
    }

    suspend fun requestAnalyzeScreen(request: AnalyzeScreenRequest) {
        _analyzeScreenRequest.emit(request)
    }

    suspend fun requestNodeIdentification(request: NodeIdentificationRequest) {
        _nodeIdentificationRequest.emit(request)
    }

    suspend fun requestNodeHighlight(request: HighlightNodeRequest) {
        _nodeHighlightRequest.emit(request)
    }

    // --- Methods for Service to Emit Results/Events (Require requestId) ---
    suspend fun reportActionCompleted(requestId: String, success: Boolean) {
        _actionCompletedEvent.emit(ActionCompletedEvent(requestId, success))
    }
    suspend fun reportNodeIdentified(requestId: String, nodeInfo: NodeInfo?, nodeType: NodeType) { // Uses NodeType
        _nodeIdentifiedEvent.emit(NodeIdentifiedEvent(requestId, nodeInfo, nodeType))
    }
    suspend fun reportAnalysisResult(requestId: String, result: ScreenAnalysisResult) {
        _analysisResultEvent.emit(AnalysisResultEvent(requestId, result))
    }

    suspend fun reportNodeHighlighted(requestId: String, bounds: Rect?, nodeType: NodeType) {
        _nodeHighlightedEvent.emit(NodeHighlightedEvent(requestId, bounds, nodeType))
    }

    // --- Flows and Methods for UI -> Activity Communication ---
    private val _openDictionaryPickerRequest = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val openDictionaryPickerRequest: SharedFlow<Unit> = _openDictionaryPickerRequest.asSharedFlow()

    private val _dictionaryUriResult = MutableSharedFlow<Uri>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val dictionaryUriResult: SharedFlow<Uri> = _dictionaryUriResult.asSharedFlow()

    suspend fun requestOpenDictionaryPicker() {
        _openDictionaryPickerRequest.emit(Unit)
    }

    suspend fun reportDictionaryUri(uri: Uri) {
        _dictionaryUriResult.emit(uri)
    }
}
package com.hereliesaz.et2bruteforce.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.hereliesaz.et2bruteforce.comms.AccessibilityCommsManager
import com.hereliesaz.et2bruteforce.model.NodeType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import com.hereliesaz.et2bruteforce.R
import com.hereliesaz.et2bruteforce.model.RectSerializer

/**
 * Data class holding descriptive information about a UI element found by the service.
 */
@Serializable
data class ParentInfo(
    val className: CharSequence?,
    val contentDescription: CharSequence?,
    val viewIdResourceName: String?
)

@Serializable
data class NodeInfo(
    val className: CharSequence?,
    val text: CharSequence?,
    val contentDescription: CharSequence?,
    val viewIdResourceName: String?,
    @Serializable(with = RectSerializer::class)
    val boundsInScreen: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isPassword: Boolean,
    val parentInfo: ParentInfo?
)

/**
 * Manages the state of the currently identified target nodes (Input, Submit, Popup).
 */
@Singleton
class AccessibilityInteractionManager @Inject constructor() {
    private val _identifiedInputNode = MutableStateFlow<NodeInfo?>(null)
    val identifiedInputNode: StateFlow<NodeInfo?> = _identifiedInputNode.asStateFlow()

    private val _identifiedSubmitNode = MutableStateFlow<NodeInfo?>(null)
    val identifiedSubmitNode: StateFlow<NodeInfo?> = _identifiedSubmitNode.asStateFlow()

    private val _identifiedPopupNode = MutableStateFlow<NodeInfo?>(null)
    val identifiedPopupNode: StateFlow<NodeInfo?> = _identifiedPopupNode.asStateFlow()

    fun updateInputNode(nodeInfo: NodeInfo?) {
        Log.d("InteractionManager", "Updating Input Node: ${nodeInfo?.safeIdentifier}")
        _identifiedInputNode.value = nodeInfo?.copy()
    }
    fun updateSubmitNode(nodeInfo: NodeInfo?) {
        Log.d("InteractionManager", "Updating Submit Node: ${nodeInfo?.safeIdentifier}")
        _identifiedSubmitNode.value = nodeInfo?.copy()
    }
    fun updatePopupNode(nodeInfo: NodeInfo?) {
        Log.d("InteractionManager", "Updating Popup Node: ${nodeInfo?.safeIdentifier}")
        _identifiedPopupNode.value = nodeInfo?.copy()
    }

    private val NodeInfo.safeIdentifier: String
        get() = viewIdResourceName ?: "${className ?: "Node"} $boundsInScreen"
    fun clearAllNodes() {
        Log.d("InteractionManager", "Clearing all identified nodes.")
        _identifiedInputNode.value = null
        _identifiedSubmitNode.value = null
        _identifiedPopupNode.value = null
    }
}

/**
 * The core Accessibility Service that interacts with other apps' UIs.
 */
@AndroidEntryPoint
class BruteforceAccessibilityService : AccessibilityService() {

    @Inject lateinit var interactionManager: AccessibilityInteractionManager
    @Inject lateinit var commsManager: AccessibilityCommsManager

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)

    companion object {
        private const val TAG = "BruteforceAccessService"
    }

    // --- Service Lifecycle Methods ---

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility Service Connected.")
        // Apply programmatic configuration overrides/defaults
        configureServiceInfo()
        Log.i(TAG, "Service Configured. Launching collectors.")
        launchRequestCollectors()
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service Interrupted. Cancelling jobs.")
        serviceJob.cancel()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            event.isCtrlPressed &&
            event.keyCode == KeyEvent.KEYCODE_G
        ) {
            Log.d(TAG, "Ctrl+G detected, starting floating service.")
            Toast.makeText(this, getString(R.string.accessibility_shortcut_toast), Toast.LENGTH_SHORT).show()
            val intent = Intent(this, FloatingControlService::class.java)
            startService(intent)
            return true // Event handled
        }
        return super.onKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.i(TAG, "Accessibility Service Destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Keep logging minimal unless debugging specific events
        // Log.v(TAG, "Event: ${AccessibilityEvent.eventTypeToString(event?.eventType ?: -1)}")
    }

    // --- Communication Handling ---

    private fun launchRequestCollectors() {
        Log.d(TAG, "Launching request collectors...")
        // Node identification
        commsManager.nodeIdentificationRequest
            .onEach { request ->
                Log.d(TAG, "Rcv: Identify [${request.requestId}] ${request.nodeType} @ ${request.coordinates}")
                // Launch processing in a separate job to avoid blocking collector
                serviceScope.launch { identifyAndReportNode(request.coordinates, request.nodeType, request.requestId) }
            }
            .launchIn(serviceScope)
        // Node highlight
        commsManager.nodeHighlightRequest
            .onEach { request ->
                Log.d(TAG, "Rcv: Highlight [${request.requestId}] @ ${request.coordinates}")
                serviceScope.launch { highlightAndReportNode(request.coordinates, request.nodeType, request.requestId) }
            }
            .launchIn(serviceScope)
        // Input text
        commsManager.inputTextRequest
            .onEach { request ->
                Log.d(TAG, "Rcv: Input [${request.requestId}] into ${request.targetNodeInfo.viewIdResourceName}")
                serviceScope.launch {
                    val success = performInputText(request.targetNodeInfo, request.text)
                    commsManager.reportActionCompleted(request.requestId, success)
                }
            }
            .launchIn(serviceScope)
        // Click node
        commsManager.clickNodeRequest
            .onEach { request ->
                Log.d(TAG, "Rcv: Click [${request.requestId}] on ${request.targetNodeInfo.viewIdResourceName}")
                serviceScope.launch {
                    val success = performClick(request.targetNodeInfo)
                    commsManager.reportActionCompleted(request.requestId, success)
                }
            }
            .launchIn(serviceScope)
        // Analyze screen
        commsManager.analyzeScreenRequest
            .onEach { request ->
                Log.d(TAG, "Rcv: Analyze [${request.requestId}]")
                serviceScope.launch {
                    val result = analyzeScreenContent(request.successKeywords, request.captchaKeywords)
                    commsManager.reportAnalysisResult(request.requestId, result)
                }
            }
            .launchIn(serviceScope)
        Log.d(TAG, "Request collectors launched.")
    }

    // --- Core Accessibility Actions ---

    private suspend fun identifyAndReportNode(coordinates: Point, nodeType: NodeType, requestId: String) {
        val filter: (AccessibilityNodeInfo) -> Boolean = when (nodeType) {
            NodeType.INPUT -> { node -> node.isEditable }
            NodeType.SUBMIT, NodeType.POPUP -> { node -> node.isClickable }
        }
        // Node finding can be slow, run on Default dispatcher
        val foundNodeInfo: NodeInfo? = findNodeAt(coordinates.x, coordinates.y, filter)

        // Update InteractionManager state (must happen on Main thread)
        withContext(Dispatchers.Main.immediate) {
            when (nodeType) {
                NodeType.INPUT -> interactionManager.updateInputNode(foundNodeInfo)
                NodeType.SUBMIT -> interactionManager.updateSubmitNode(foundNodeInfo)
                NodeType.POPUP -> interactionManager.updatePopupNode(foundNodeInfo)
            }
        }
        // Report result back
        commsManager.reportNodeIdentified(requestId, foundNodeInfo, nodeType)

        if (foundNodeInfo == null) Log.w(TAG, "Identify [${requestId}] FAILED for $nodeType @ $coordinates")
        else Log.i(TAG, "Identify [${requestId}] OK for $nodeType: ${foundNodeInfo.viewIdResourceName ?: "(no id)"}")
    }

    private suspend fun highlightAndReportNode(coordinates: Point, nodeType: NodeType, requestId: String) {
        val filter: (AccessibilityNodeInfo) -> Boolean = when (nodeType) {
            NodeType.INPUT -> { node -> node.isEditable }
            NodeType.SUBMIT, NodeType.POPUP -> { node -> node.isClickable }
        }
        val foundNodeInfo: NodeInfo? = findNodeAt(coordinates.x, coordinates.y, filter)
        commsManager.reportNodeHighlighted(requestId, foundNodeInfo?.boundsInScreen, nodeType)
        if (foundNodeInfo == null) Log.w(TAG, "Highlight [${requestId}] FAILED @ $coordinates")
        else Log.i(TAG, "Highlight [${requestId}] OK: ${foundNodeInfo.viewIdResourceName ?: "(no id)"}")
    }


    private suspend fun performInputText(targetNodeInfo: NodeInfo, text: String): Boolean {
        // Re-finding node runs on Default dispatcher
        val freshNode: AccessibilityNodeInfo? = findFreshNode(targetNodeInfo)

        if (freshNode == null) {
            Log.e(TAG, "InputText: Failed to RE-FIND node ${targetNodeInfo.viewIdResourceName ?: "(no id)"}")
            return false
        }
        val freshNodeCompat = AccessibilityNodeInfoCompat.wrap(freshNode)
        if (!freshNodeCompat.isEditable) {
            Log.e(TAG, "InputText: Re-found node ${targetNodeInfo.viewIdResourceName ?: "(no id)"} is NOT editable.")
            return false // Don't proceed if not editable
        }

        // Perform the action itself on the Main thread
        return withContext(Dispatchers.Main.immediate) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val success = freshNodeCompat.performAction(AccessibilityNodeInfoCompat.ACTION_SET_TEXT, arguments)
            if (!success) Log.e(TAG, "InputText: performAction FAILED for ${freshNodeCompat.viewIdResourceName ?: "(no id)"}")
            else Log.d(TAG, "InputText: performAction OK for ${freshNodeCompat.viewIdResourceName ?: "(no id)"}")
            success // Return success status
        }
    }

    private suspend fun performClick(targetNodeInfo: NodeInfo): Boolean {
        // Re-finding node runs on Default dispatcher
        val freshNode: AccessibilityNodeInfo? = findFreshNode(targetNodeInfo)

        if (freshNode == null) {
            Log.e(TAG, "ClickNode: Failed to RE-FIND node ${targetNodeInfo.viewIdResourceName ?: "(no id)"}")
            return false
        }
        if (!freshNode.isClickable) {
            Log.e(TAG, "ClickNode: Re-found node ${targetNodeInfo.viewIdResourceName ?: "(no id)"} is NOT clickable.")
            return false // Don't proceed if not clickable
        }

        // Perform the action itself on the Main thread
        return withContext(Dispatchers.Main.immediate) {
            val success = freshNode.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
            if (!success) Log.e(TAG, "ClickNode: performAction FAILED for ${freshNode.viewIdResourceName ?: "(no id)"}")
            else Log.d(TAG, "ClickNode: performAction OK for ${freshNode.viewIdResourceName ?: "(no id)"}")
            success // Return success status
        }
    }

    private suspend fun analyzeScreenContent(successKeywords: List<String>, captchaKeywords: List<String>): ScreenAnalysisResult {
        Log.d(TAG, "Analyzing screen content...")
        // Analysis runs on Default dispatcher
        return withContext(Dispatchers.Default) {
            val root = rootInActiveWindow // Get fresh root
            if (root == null) {
                Log.w(TAG, "AnalyzeScreen: Root node is null.")
                return@withContext ScreenAnalysisResult.Unknown
            }

            val captchaLower = captchaKeywords.map { it.lowercase() }
            val successLower = successKeywords.map { it.lowercase() }

            try {
                scanTreeForKeywords(root, successLower, captchaLower)
            } catch (e: Exception) {
                Log.e(TAG, "Error during screen analysis", e)
                ScreenAnalysisResult.Unknown
            } finally {
                // Recycling root is complex due to potential concurrent access. Omitted for stability.
                // root.recycle()
            }
        }
    }


    // --- Node Finding Helpers ---

    /**
     * Finds the best matching node at specific screen coordinates based on a filter.
     * Returns a NodeInfo data object. Runs on the Default dispatcher.
     */
    private suspend fun findNodeAt(screenX: Int, screenY: Int, filter: (AccessibilityNodeInfo) -> Boolean): NodeInfo? = withContext(Dispatchers.Default) {
        if (!coroutineContext.isActive) return@withContext null // Check if coroutine is active
        val root = rootInActiveWindow ?: return@withContext null // Get fresh root node

        // Optimization: Avoid allocating list for candidates. Track best match directly.
        var bestMatchNode: AccessibilityNodeInfo? = null
        var minDistanceSq = Double.MAX_VALUE

        // Reuse Rect to avoid object allocation in recursion
        val reusableRect = Rect()

        // Inner recursive function - runs within the withContext(Default) scope
        fun findRecursive(node: AccessibilityNodeInfo?) {
            if (node == null) return // Base case

            // Use native method for bounds to avoid Compat wrapper allocation
            node.getBoundsInScreen(reusableRect)

            val containsPoint = reusableRect.contains(screenX, screenY)

            // Check if node contains the point and satisfies the filter
            if (containsPoint && filter(node)) {
                val centerX = reusableRect.centerX()
                val centerY = reusableRect.centerY()
                val dx = (screenX - centerX).toDouble()
                val dy = (screenY - centerY).toDouble()
                val distanceSq = (dx * dx) + (dy * dy) // Calculate squared distance

                if (distanceSq < minDistanceSq) {
                    minDistanceSq = distanceSq
                    bestMatchNode = node
                }
            }

            // Recurse through children using native methods
            for (i in 0 until node.childCount) {
                if (!coroutineContext.isActive) break // Check cancellation before recursing further
                val child = node.getChild(i)
                findRecursive(child)
                // child?.recycle() // Simplified: Omit recycling
            }
        }

        // Start search and handle potential cleanup
        try {
            findRecursive(root)
        } finally {
            // root?.recycle() // Simplified: Omit recycling
        }

        // Convert to NodeInfo data class if a match was found
        bestMatchNode?.let { node ->
            val nodeBounds = Rect()
            node.getBoundsInScreen(nodeBounds) // Get bounds again
            val parent = node.parent
            val parentInfo = if (parent != null) {
                ParentInfo(
                    className = parent.className,
                    contentDescription = parent.contentDescription,
                    viewIdResourceName = parent.viewIdResourceName
                )
            } else {
                null
            }
            NodeInfo(
                className = node.className,
                text = if (node.isPassword) null else node.text,
                contentDescription = node.contentDescription,
                viewIdResourceName = node.viewIdResourceName,
                boundsInScreen = nodeBounds,
                isClickable = node.isClickable,
                isEditable = node.isEditable,
                isPassword = node.isPassword,
                parentInfo = parentInfo
            )
        } // Implicitly returns null if bestMatchNode is null
    }

    /**
     * Attempts to re-find a *live* AccessibilityNodeInfo object based on stored NodeInfo.
     * Runs on the Default dispatcher.
     */
    private suspend fun findFreshNode(targetNodeInfo: NodeInfo): AccessibilityNodeInfo? = withContext(Dispatchers.Default) {
        if (!coroutineContext.isActive) return@withContext null
        Log.v(TAG, "Attempting findFreshNode: ID=${targetNodeInfo.viewIdResourceName}, Class=${targetNodeInfo.className}")
        val root = rootInActiveWindow ?: return@withContext null // Need fresh root

        var foundNode: AccessibilityNodeInfo? = null
        try {
            // Strategy 1: Find by View ID (most reliable)
            if (!targetNodeInfo.viewIdResourceName.isNullOrBlank()) {
                val nodesById = root.findAccessibilityNodeInfosByViewId(targetNodeInfo.viewIdResourceName)
                if (!nodesById.isNullOrEmpty()) {
                    // Refine by class name if multiple nodes share the ID
                    foundNode = nodesById.find { node -> node.className == targetNodeInfo.className } ?: nodesById.firstOrNull()
                    Log.d(TAG, "findFreshNode: Found by ID, selected: ${foundNode != null}")
                }
                // nodesById?.filter { it != foundNode }?.forEach { it?.recycle() } // Simplified: Omit recycling
            }
            // Strategy 2: Traverse hierarchy if ID search failed
            if (foundNode == null) {
                Log.v(TAG, "findFreshNode: Traversing hierarchy...")
                foundNode = findNodeRecursiveViaProperties(root, targetNodeInfo)
            }
        } catch (e: Exception) {
            Log.e(TAG, "findFreshNode: Error during search", e)
            foundNode = null // Ensure null return on error
        } finally {
            // root?.recycle() // Simplified: Omit recycling
        }

        if (foundNode == null) Log.w(TAG,"findFreshNode: Failed to re-find node ${targetNodeInfo.viewIdResourceName}")
        foundNode // Return the live node or null
    }

    /**
     * Recursive helper for findFreshNode (Strategy 2). Runs within caller's context.
     */
    private fun findNodeRecursiveViaProperties(node: AccessibilityNodeInfo?, target: NodeInfo): AccessibilityNodeInfo? {
        if (node == null || !serviceScope.isActive) return null

        var bestNode: AccessibilityNodeInfo? = null
        var bestScore = 0
        // Reuse a single Rect to avoid N allocations
        val reusableRect = Rect()

        fun calculateScore(candidate: AccessibilityNodeInfo, parentCandidate: AccessibilityNodeInfo?, target: NodeInfo): Int {
            var score = 0
            if (!candidate.viewIdResourceName.isNullOrEmpty() && candidate.viewIdResourceName == target.viewIdResourceName) score += 10
            if (candidate.className?.equals(target.className) == true) score += 2
            if (candidate.text?.equals(target.text) == true) score += 5
            if (candidate.contentDescription?.equals(target.contentDescription) == true) score += 5
            if (candidate.isClickable == target.isClickable) score += 1
            if (candidate.isEditable == target.isEditable) score += 1
            // Use passed parent to avoid expensive getParent() call and object creation
            if (parentCandidate != null) {
                if (parentCandidate.className?.equals(target.parentInfo?.className) == true) score += 3
                if (!parentCandidate.viewIdResourceName.isNullOrEmpty() && parentCandidate.viewIdResourceName == target.parentInfo?.viewIdResourceName) score += 5
            }
            return score
        }

        fun findRecursive(currentNode: AccessibilityNodeInfo, parentNode: AccessibilityNodeInfo?) {
            if (!serviceScope.isActive) return

            currentNode.getBoundsInScreen(reusableRect)
            if (!Rect.intersects(reusableRect, target.boundsInScreen)) {
                return
            }

            val score = calculateScore(currentNode, parentNode, target)
            if (score > 0 && score > bestScore) {
                bestScore = score
                bestNode = currentNode
            }

            for (i in 0 until currentNode.childCount) {
                val child = currentNode.getChild(i)
                if (child != null) {
                    findRecursive(child, currentNode)
                }
            }
        }

        // Initial parent is null as we don't have efficient access to it and it's likely the root
        findRecursive(node, null)

        return bestNode
    }


    private fun scanTreeForKeywords(
        node: AccessibilityNodeInfo,
        successKeywords: List<String>,
        captchaKeywords: List<String>
    ): ScreenAnalysisResult {
        if (!serviceScope.isActive) return ScreenAnalysisResult.Unknown

        var successFound = false
        // Track visited text to avoid redundant checks (optimization akin to the original Set)
        val visitedText = mutableSetOf<String>()

        fun recurse(currentNode: AccessibilityNodeInfo): ScreenAnalysisResult {
            if (!serviceScope.isActive) return ScreenAnalysisResult.Unknown

            // Helper to check text against keywords
            // Optimization: Accepts CharSequence to avoid toString() allocation for blank text
            fun checkText(text: CharSequence?): ScreenAnalysisResult? {
                if (text.isNullOrBlank()) return null // Avoids allocation

                // We need String for Set, but only if it's not blank
                val textStr = text.toString()

                // Optimization: Avoid converting text to lowercase to prevent String allocation.
                // We use ignoreCase = true in contains() instead.
                // Note: visitedText deduplication is now case-sensitive ("Ok" vs "OK" checked twice),
                // but this tradeoff is acceptable to avoid allocation on every node.
                if (!visitedText.add(textStr)) return null // Skip if already checked

                if (captchaKeywords.any { text.contains(it, ignoreCase = true) }) return ScreenAnalysisResult.CaptchaDetected
                if (!successFound && successKeywords.any { text.contains(it, ignoreCase = true) }) successFound = true
                return null
            }

            // Check text properties without premature string conversion
            val resText = checkText(currentNode.text)
            if (resText != null) return resText

            val resDesc = checkText(currentNode.contentDescription)
            if (resDesc != null) return resDesc

            for (i in 0 until currentNode.childCount) {
                if (!serviceScope.isActive) break
                val child = currentNode.getChild(i)
                if (child != null) {
                    val result = recurse(child)
                    // child.recycle() // Omitted
                    if (result == ScreenAnalysisResult.CaptchaDetected) return ScreenAnalysisResult.CaptchaDetected
                }
            }
            return ScreenAnalysisResult.Unknown
        }

        val result = recurse(node)
        return if (result == ScreenAnalysisResult.CaptchaDetected) ScreenAnalysisResult.CaptchaDetected.also { Log.w(TAG,"CAPTCHA detected") }
        else if (successFound) ScreenAnalysisResult.SuccessDetected.also { Log.i(TAG,"SUCCESS detected") }
        else ScreenAnalysisResult.Unknown
    }


    // --- Service Configuration Helper ---
    private fun configureServiceInfo() {
        val currentInfo = this.serviceInfo ?: return
        currentInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        }
        this.serviceInfo = currentInfo
    }
}

// Define ScreenAnalysisResult enum (can be moved to model package)
enum class ScreenAnalysisResult {
    SuccessDetected,
    CaptchaDetected,
    Unknown
}
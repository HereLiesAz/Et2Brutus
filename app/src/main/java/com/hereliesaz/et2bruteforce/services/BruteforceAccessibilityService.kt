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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.hereliesaz.et2bruteforce.comms.AccessibilityCommsManager
import com.hereliesaz.et2bruteforce.model.NodeType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class holding descriptive information about a UI element found by the service.
 */
data class NodeInfo(
    val className: CharSequence?,
    val text: CharSequence?,
    val contentDescription: CharSequence?,
    val viewIdResourceName: String?,
    val boundsInScreen: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isPassword: Boolean,
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
        Log.d("InteractionManager", "Updating Input Node: ${nodeInfo?.viewIdResourceName ?: nodeInfo?.text}")
        _identifiedInputNode.value = nodeInfo?.copy()
    }
    fun updateSubmitNode(nodeInfo: NodeInfo?) {
        Log.d("InteractionManager", "Updating Submit Node: ${nodeInfo?.viewIdResourceName ?: nodeInfo?.text}")
        _identifiedSubmitNode.value = nodeInfo?.copy()
    }
    fun updatePopupNode(nodeInfo: NodeInfo?) {
        Log.d("InteractionManager", "Updating Popup Node: ${nodeInfo?.viewIdResourceName ?: nodeInfo?.text}")
        _identifiedPopupNode.value = nodeInfo?.copy()
    }
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
                serviceScope.launch { highlightAndReportNode(request.coordinates, request.requestId) }
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
            NodeType.INPUT -> { node -> AccessibilityNodeInfoCompat.wrap(node).isEditable }
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

    private suspend fun highlightAndReportNode(coordinates: Point, requestId: String) {
        val foundNodeInfo: NodeInfo? = findNodeAt(coordinates.x, coordinates.y) { true } // Find any node
        commsManager.reportNodeHighlighted(requestId, foundNodeInfo?.boundsInScreen)
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
            // Perform text extraction within try/finally to attempt recycling root
            val allText = try {
                getAllTextFromNode(root).joinToString(separator = " | ").lowercase()
            } catch (e: Exception) {
                Log.e(TAG, "Error during getAllTextFromNode", e)
                "" // Return empty string on error
            } finally {
                // Recycling root is complex due to potential concurrent access. Omitted for stability.
                // root.recycle()
            }

            // Log analysis text only in debug builds if necessary
            // Log.v(TAG, "Screen Text (lower): $allText")

            // Check keywords
            when {
                captchaKeywords.any { keyword -> allText.contains(keyword.lowercase()) } ->
                    ScreenAnalysisResult.CaptchaDetected.also { Log.w(TAG,"CAPTCHA detected") }
                successKeywords.any { keyword -> allText.contains(keyword.lowercase()) } ->
                    ScreenAnalysisResult.SuccessDetected.also { Log.i(TAG,"SUCCESS detected") }
                else -> ScreenAnalysisResult.Unknown
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
        val nearbyNodes = mutableListOf<Pair<AccessibilityNodeInfo, Int>>()

        // Inner recursive function - runs within the withContext(Default) scope
        fun findRecursive(node: AccessibilityNodeInfo?) {
            if (node == null) return // Base case

            // Use Compat wrapper for safe property access
            val nodeCompat = AccessibilityNodeInfoCompat.wrap(node)
            val bounds = Rect()
            nodeCompat.getBoundsInScreen(bounds) // Get screen bounds

            // Check if node contains the point and satisfies the filter
            if (bounds.contains(screenX, screenY) && filter(node)) {
                val centerX = bounds.centerX()
                val centerY = bounds.centerY()
                val dx = (screenX - centerX).toDouble()
                val dy = (screenY - centerY).toDouble()
                val distanceSq = (dx * dx) + (dy * dy) // Calculate squared distance
                nearbyNodes.add(node to distanceSq.toInt()) // Add original node
            }

            // Recurse through children
            for (i in 0 until nodeCompat.childCount) {
                if (!coroutineContext.isActive) break // Check cancellation before recursing further
                val child = nodeCompat.getChild(i)
                findRecursive(child?.unwrap()) // Recurse with original node
                // child?.recycle() // Simplified: Omit recycling
            }
        }

        // Start search and handle potential cleanup
        try {
            findRecursive(root)
        } finally {
            // root?.recycle() // Simplified: Omit recycling
        }

        // Find the closest node among matches
        val bestMatchNode: AccessibilityNodeInfo? = nearbyNodes.minByOrNull { it.second }?.first

        // Convert to NodeInfo data class if a match was found
        bestMatchNode?.let { node ->
            val nodeBounds = Rect()
            node.getBoundsInScreen(nodeBounds) // Get bounds again
            NodeInfo(
                className = node.className,
                text = node.text,
                contentDescription = node.contentDescription,
                viewIdResourceName = node.viewIdResourceName,
                boundsInScreen = nodeBounds,
                isClickable = node.isClickable,
                isEditable = node.isEditable,
                isPassword = node.isPassword
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
        if (node == null || !serviceScope.isActive) return null // Base case and cancellation check

        // Wrap node for safe property access
        val nodeCompat = AccessibilityNodeInfoCompat.wrap(node)
        val currentBounds = Rect()
        nodeCompat.getBoundsInScreen(currentBounds)

        // Define matching criteria (tune as needed)
        val classMatch = node.className == target.className
        val clickableMatch = node.isClickable == target.isClickable
        val editableMatch = node.isEditable == target.isEditable
        // Allow match if target didn't have an ID, or if IDs match
        val idMatch = target.viewIdResourceName == null || node.viewIdResourceName == target.viewIdResourceName
        // Bounds check: simple intersection is a basic check
        val boundsOverlap = Rect.intersects(currentBounds, target.boundsInScreen)

        // Check if this node is a likely match
        if (classMatch && clickableMatch && editableMatch && idMatch && boundsOverlap) {
            Log.d(TAG, "findNodeRecursiveViaProperties: Potential match: ID=${node.viewIdResourceName}")
            return node // Found a likely match
        }

        // If not this node, recurse through children
        for (i in 0 until nodeCompat.childCount) {
            if (!serviceScope.isActive) {
                break // Check cancellation before recursing
            }
            val child = nodeCompat.getChild(i)
            val foundInChild = findNodeRecursiveViaProperties(child?.unwrap(), target) // Recurse with original node
            if (foundInChild != null) {
                // child?.recycle() // Don't recycle if returning parent (foundInChild)
                return foundInChild // Return immediately if found
            }
            // child?.recycle() // Simplified: Omit recycling
        }

        // Not found in this subtree
        return null
    }


    /**
     * Recursively extracts all textual content from a node and its descendants.
     * Runs within the caller's context.
     */
    private fun getAllTextFromNode(node: AccessibilityNodeInfo?): List<String> {
        if (node == null || !serviceScope.isActive) return emptyList() // Base case and cancellation check

        val texts = mutableListOf<String>()
        try {
            // Extract text and content description safely
            val nodeText = node.text?.toString()?.trim()
            val nodeDesc = node.contentDescription?.toString()?.trim()
            if (!nodeText.isNullOrEmpty()) texts.add(nodeText)
            if (!nodeDesc.isNullOrEmpty()) texts.add(nodeDesc)

            // Recurse through children
            for (i in 0 until node.childCount) {
                if (!serviceScope.isActive) break // Check cancellation
                val child = node.getChild(i)
                texts.addAll(getAllTextFromNode(child)) // Add text from children
                // child?.recycle() // Simplified: Omit recycling
            }
        } catch(e: Exception) {
            // Log errors during text extraction
            Log.e(TAG, "Error in getAllTextFromNode for node ID: ${node.viewIdResourceName}", e)
        }
        // Return unique, non-empty text strings
        return texts.filter { it.isNotEmpty() }.distinct()
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
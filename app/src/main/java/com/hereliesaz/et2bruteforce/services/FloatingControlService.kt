package com.hereliesaz.et2bruteforce.services

import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hereliesaz.et2bruteforce.model.NodeType
import com.hereliesaz.et2bruteforce.ui.overlay.RootOverlay
import com.hereliesaz.et2bruteforce.comms.AccessibilityCommsManager
import com.hereliesaz.et2bruteforce.viewmodel.BruteforceViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FloatingControlService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "FloatingControlService"
        const val MAIN_CONTROLLER_KEY = "MAIN_CONTROLLER"
    }

    @Inject lateinit var windowManager: WindowManager
    private lateinit var viewModel: BruteforceViewModel
    @Inject lateinit var commsManager: AccessibilityCommsManager

    // --- View Management ---
    private val composeViews = mutableMapOf<Any, ComposeView>()
    private val windowLayoutParams = mutableMapOf<Any, WindowManager.LayoutParams>()

    // --- Lifecycle Management ---
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = store

    // --- Lifecycle Management for SavedStateRegistryOwner ---
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry


    // Service Coroutine Scope
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)

    override fun onCreate() {
        super.onCreate()
        viewModel = ViewModelProvider(this)[BruteforceViewModel::class.java]
        savedStateRegistryController.performRestore(null)
        Log.d(TAG, "onCreate")

        // Set the ViewModelStoreOwner for any ComposeViews created
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                composeViews.values.forEach { view ->
                    view.setViewTreeViewModelStoreOwner(this@FloatingControlService)
                    view.setViewTreeSavedStateRegistryOwner(this@FloatingControlService)
                }
            }
        })

        createOverlayViews()
    }

    private fun createOverlayViews() {
        // Create Main Controller
        createAndAddView(MAIN_CONTROLLER_KEY, Point(100, 300))

        // Create Draggable Config Buttons
        NodeType.values().forEach { nodeType ->
            val initialPos = viewModel.uiState.value.buttonConfigs[nodeType]?.position ?: Point(100, 500 + (nodeType.ordinal * 200))
            createAndAddView(nodeType, initialPos)
        }
    }

    private fun createAndAddView(viewKey: Any, initialPosition: Point) {
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingControlService)
            setViewTreeViewModelStoreOwner(this@FloatingControlService)
            setViewTreeSavedStateRegistryOwner(this@FloatingControlService)

            setContent {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                RootOverlay(
                    viewKey = viewKey,
                    uiState = uiState,
                    onDrag = { deltaX, deltaY -> updateViewPosition(viewKey, deltaX, deltaY) },
                    onDragEnd = { point ->
                        if (viewKey is NodeType) {
                            viewModel.identifyNodeAt(viewKey, point)
                        }
                    },
                    // Pass all other callbacks for the main controller
                    onStart = viewModel::startBruteforce,
                    onPause = viewModel::pauseBruteforce,
                    onStop = viewModel::stopBruteforce,
                    onSelectDictionary = {
                        serviceScope.launch {
                            commsManager.requestOpenDictionaryPicker()
                        }
                    },
                    onUpdateLength = viewModel::updateCharacterLength,
                    onUpdateCharset = viewModel::updateCharacterSet,
                    onUpdatePace = viewModel::updateAttemptPace,
                    onToggleResume = viewModel::toggleResumeFromLast,
                    onToggleSingleAttemptMode = viewModel::toggleSingleAttemptMode,
                    onUpdateSuccessKeywords = viewModel::updateSuccessKeywords,
                    onUpdateCaptchaKeywords = viewModel::updateCaptchaKeywords
                )
            }
        }
        composeViews[viewKey] = view

        val params = createLayoutParams(initialPosition)
        windowLayoutParams[viewKey] = params

        try {
            windowManager.addView(view, params)
            Log.d(TAG, "Added overlay view for key: $viewKey")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding view for key: $viewKey", e)
        }
    }

    private fun createLayoutParams(position: Point): WindowManager.LayoutParams {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = position.x
            y = position.y
        }
    }

    private fun updateViewPosition(viewKey: Any, deltaX: Float, deltaY: Float) {
        val params = windowLayoutParams[viewKey] ?: return
        val view = composeViews[viewKey] ?: return

        if (view.isAttachedToWindow) {
            params.x += deltaX.toInt()
            params.y += deltaY.toInt()
            viewModel.updateButtonPosition(viewKey, Point(params.x, params.y))
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Error updating view layout for $viewKey, view might be detached.", e)
            }
        } else {
            Log.w(TAG, "Skipping view update for $viewKey, view not attached.")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        // Perform setup for SavedStateRegistryOwner based on start command if needed
        // savedStateRegistryController.handleLifecycleEvent(Lifecycle.Event.ON_START) // Maybe needed? Check docs.
        super.onStartCommand(intent, flags, startId)
        return START_STICKY // Keep service running if killed by system
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        serviceJob.cancel()
        composeViews.keys.forEach { key ->
            composeViews[key]?.let { view ->
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing view for key: $key", e)
                }
            }
        }
        composeViews.clear()
        windowLayoutParams.clear()
        store.clear()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent) // Handle lifecycle events
        return null
    }
}
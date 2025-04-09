package com.hereliesaz.et2bruteforce.services

import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hereliesaz.et2bruteforce.comms.AccessibilityCommsManager
import com.hereliesaz.et2bruteforce.ui.overlay.OverlayUi // Import the Compose UI
import com.hereliesaz.et2bruteforce.viewmodel.BruteforceViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint // Enable Hilt injection for the Service
class FloatingControlService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "FloatingControlService"
    }

    // Hilt dependencies
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory // Use factory if ViewModel needs custom creation via Hilt
    @Inject lateinit var windowManager: WindowManager // Inject WindowManager
    @Inject lateinit var commsManager: AccessibilityCommsManager // Inject Comms Manager for coordinate reporting

    // ViewModel and UI related variables
    private lateinit var viewModel: BruteforceViewModel // Get via Hilt/ViewModelProvider
    private var composeView: ComposeView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    // --- Lifecycle Management for ViewModelStoreOwner ---
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
        // Perform the required setup for SavedStateRegistryOwner before super.onCreate()
        savedStateRegistryController.performRestore(null) // Restore state if available (null for initial create)
        super.onCreate()
        Log.d(TAG, "onCreate")

        // Set the ViewModelStoreOwner and SavedStateRegistryOwner for the ComposeView
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                composeView?.let {
                    it.setViewTreeViewModelStoreOwner(this@FloatingControlService)
                    it.setViewTreeSavedStateRegistryOwner(this@FloatingControlService)
                }
            }
        })

        // Obtain the ViewModel using Hilt's mechanisms or ViewModelProvider
        // If @Inject viewModelFactory works, use it. Otherwise, standard provider:
        // viewModel = ViewModelProvider(this, viewModelFactory)[BruteforceViewModel::class.java]
        viewModel = ViewModelProvider(this)[BruteforceViewModel::class.java] // Standard way if Hilt injects default factory


        // Create and add the floating Compose view
        addOverlayView()
    }


    private fun addOverlayView() {
        composeView = ComposeView(this).apply {
            // Dispose the Composition when the view's LifecycleOwner is destroyed
            setViewTreeLifecycleOwner(this@FloatingControlService)
            setViewTreeViewModelStoreOwner(this@FloatingControlService)
            setViewTreeSavedStateRegistryOwner(this@FloatingControlService)

            setContent {
                // Collect state and pass necessary callbacks
                val uiState by viewModel.uiState.collectAsStateWithLifecycle() // Use lifecycle-aware collector

                OverlayUi(
                    uiState = uiState,
                    onDrag = ::updateViewPosition,
                    onTap = { /* Handle tap if needed, e.g., toggle menu */ },
                    onIdentifyInput = { coords -> viewModel.requestInputConfiguration(coords) },
                    onIdentifySubmit = { coords -> viewModel.requestSubmitConfiguration(coords) },
                    onIdentifyPopup = { coords -> viewModel.requestPopupConfiguration(coords) },
                    onUpdateLength = { length -> viewModel.updateCharacterLength(length) },
                    onUpdateCharset = { type -> viewModel.updateCharacterSet(type) },
                    onUpdatePace = { pace -> viewModel.updateAttemptPace(pace) },
                    onToggleResume = { resume -> viewModel.toggleResumeFromLast(resume) },
                    onStart = { viewModel.startBruteforce() },
                    onPause = { viewModel.pauseBruteforce() },
                    onStop = { viewModel.stopBruteforce() },
                    onConfirmSuccess = { viewModel.confirmSuccess() },
                    onRejectSuccess = { viewModel.rejectSuccess() },
                    onSelectDictionary = { /* TODO: Trigger file picker via Activity */ Log.w(TAG, "Dictionary selection needs Activity interaction.") }
                )
            }
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE // Deprecated but necessary for pre-Oreo
        }

        windowLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, // Width
            WindowManager.LayoutParams.WRAP_CONTENT, // Height
            overlayType, // Allows drawing over other apps
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or // Doesn't take focus from underlying app
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // Allow dragging outside screen bounds initially
            PixelFormat.TRANSLUCENT // Allows transparent background
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100 // Initial X position
            y = 300 // Initial Y position
        }

        try {
            windowManager.addView(composeView, windowLayoutParams)
            Log.d(TAG, "ComposeView added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding view to WindowManager", e)
            // Handle error, maybe stop service
        }
    }

    // Called by the Overlay UI when dragged
    fun updateViewPosition(deltaX: Float, deltaY: Float) {
        windowLayoutParams?.let { params ->
            // Ensure view is still attached before updating
            if (composeView?.isAttachedToWindow == true) {
                params.x += deltaX.toInt()
                params.y += deltaY.toInt()
                // Optional: Clamp coordinates to screen bounds if FLAG_LAYOUT_NO_LIMITS behavior is undesirable after initial placement
                // val screenWidth = ... ; val screenHeight = ...
                // params.x = params.x.coerceIn(0, screenWidth - composeView!!.width)
                // params.y = params.y.coerceIn(0, screenHeight - composeView!!.height)
                try {
                    windowManager.updateViewLayout(composeView, params)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Error updating view layout, view might be detached.", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error updating view layout.", e)
                }
            } else {
                Log.w(TAG,"Skipping view update, view not attached.")
            }
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
        Log.d(TAG, "onDestroy")
        // Clean up coroutines
        serviceJob.cancel()
        // Remove the view from the window manager
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing view from WindowManager", e)
            }
        }
        composeView = null
        windowLayoutParams = null
        // Clean up ViewModelStore and SavedStateRegistry
        store.clear()
        // savedStateRegistryController.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY) // Maybe needed? Check docs.
        super.onDestroy()
    }

    // Not binding to this service directly
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent) // Handle lifecycle events
        return null
    }
}
package com.hereliesaz.et2bruteforce.viewmodel

import android.util.Log
import com.hereliesaz.et2bruteforce.comms.AccessibilityCommsManager
import com.hereliesaz.et2bruteforce.data.SettingsRepository
import com.hereliesaz.et2bruteforce.domain.BruteforceEngine
import com.hereliesaz.et2bruteforce.model.BruteforceSettings
import com.hereliesaz.et2bruteforce.model.BruteforceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BruteforceViewModelTest {

    private lateinit var viewModel: BruteforceViewModel
    private val settingsRepository: SettingsRepository = mock()
    private val bruteforceEngine: BruteforceEngine = mock()
    private val commsManager: AccessibilityCommsManager = mock()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var logMock: MockedStatic<Log>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        logMock = Mockito.mockStatic(Log::class.java)
        logMock.`when`<Int> { Log.d(any(), any()) }.thenReturn(0)
        logMock.`when`<Int> { Log.e(any(), any()) }.thenReturn(0)
        logMock.`when`<Int> { Log.w(any<String>(), any<String>()) }.thenReturn(0)
        logMock.`when`<Int> { Log.i(any(), any()) }.thenReturn(0)
        logMock.`when`<Int> { Log.v(any(), any()) }.thenReturn(0)

        // Mock flows
        val defaultSettings = mock<BruteforceSettings>()
        `when`(settingsRepository.settingsFlow).thenReturn(MutableStateFlow(defaultSettings))
        `when`(settingsRepository.profilesFlow).thenReturn(MutableStateFlow(emptyList()))
        `when`(commsManager.nodeIdentifiedEvent).thenReturn(MutableStateFlow(mock()))
        `when`(commsManager.nodeHighlightedEvent).thenReturn(MutableStateFlow(mock()))

        viewModel = BruteforceViewModel(settingsRepository, bruteforceEngine, commsManager)
    }

    @After
    fun tearDown() {
        logMock.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `updateHybridModeEnabled updates repository and resets attempt count`() = runTest {
        // Arrange
        val enabled = true

        // Act
        viewModel.updateHybridModeEnabled(enabled)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(settingsRepository).updateHybridModeEnabled(enabled)
        verify(settingsRepository).updateLastAttempt(null)
    }
}

// Minimal mock classes to avoid Android dependency issues during test
class MockPoint(x: Int, y: Int)

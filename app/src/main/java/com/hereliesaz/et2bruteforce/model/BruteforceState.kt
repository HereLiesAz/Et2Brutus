package com.hereliesaz.et2bruteforce.model

import android.graphics.Point
import com.hereliesaz.et2bruteforce.services.NodeInfo


enum class BruteforceStatus {
    IDLE,
    // CONFIGURING statuses are removed, as configuration is now managed per-button
    READY, // Becomes ready when INPUT and SUBMIT nodes are identified.
    RUNNING,
    PAUSED,
    CAPTCHA_DETECTED,
    SUCCESS_DETECTED,
    DICTIONARY_LOAD_FAILED,
    ERROR // General error state
}

/**
 * Represents the state of a single draggable configuration button.
 *
 * @param type The type of node this button is supposed to identify (INPUT, SUBMIT, POPUP).
 * @param position The current x,y coordinates of the button on the screen.
 * @param identifiedNodeInfo The details of the UI node identified at this button's location. Null if not yet identified or failed.
 */
data class ButtonConfig(
    val type: NodeType,
    val position: Point,
    val identifiedNodeInfo: NodeInfo? = null
)


enum class CharacterSetType {
    LETTERS,        // a-z, A-Z
    NUMBERS,        // 0-9
    LETTERS_NUMBERS, // Both
    ALPHANUMERIC_SPECIAL // Letters, numbers, and common special chars
}

data class BruteforceSettings(
    val characterLength: Int = 4,
    val characterSetType: CharacterSetType = CharacterSetType.ALPHANUMERIC_SPECIAL,
    val dictionaryUri: String? = null, // Store Uri as string for persistence
    val attemptPaceMillis: Long = 100, // Milliseconds between attempts
    val resumeFromLast: Boolean = true,
    val singleAttemptMode: Boolean = false,
    val lastAttempt: String? = null,
    // Keywords for detection need to be defined
    val successKeywords: List<String> = listOf("success", "welcome", "logged in"),
    val captchaKeywords: List<String> = listOf("captcha", "verify you", "robot"),
    val controllerPosition: Point = Point(100, 300), // Default position
    val walkthroughCompleted: Boolean = false,
    val mask: String? = null,
    val hybridModeEnabled: Boolean = false,
    val hybridSuffixes: List<String> = listOf("123", "!", "2024")
)

data class BruteforceState(
    val status: BruteforceStatus = BruteforceStatus.IDLE,
    val settings: BruteforceSettings = BruteforceSettings(),
    val buttonConfigs: Map<NodeType, ButtonConfig> = mapOf(
        NodeType.INPUT to ButtonConfig(NodeType.INPUT, Point(100, 300)),
        NodeType.SUBMIT to ButtonConfig(NodeType.SUBMIT, Point(100, 500)),
        NodeType.POPUP to ButtonConfig(NodeType.POPUP, Point(100, 700))
    ),
    val currentAttempt: String? = null,
    val attemptCount: Long = 0,
    val dictionaryLoadProgress: Float = 0f, // 0.0 to 1.0 for dictionary loading
    val errorMessage: String? = null,
    val successCandidate: String? = null, // Holds the string that triggered success detection
    val highlightedInfo: HighlightInfo? = null,
    val actionButtonsEnabled: Boolean = true
)
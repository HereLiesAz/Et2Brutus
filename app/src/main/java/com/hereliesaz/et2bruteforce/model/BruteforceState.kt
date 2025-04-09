package com.hereliesaz.et2bruteforce.model

import android.net.Uri

enum class BruteforceStatus {
    IDLE,
    CONFIGURING_INPUT, // User needs to place marker on input field
    CONFIGURING_SUBMIT, // User needs to place marker on submit button
    CONFIGURING_POPUP, // User needs to place marker on popup button
    READY, // Configured, ready to start
    RUNNING,
    PAUSED,
    CAPTCHA_DETECTED,
    SUCCESS_DETECTED,
    DICTIONARY_LOAD_FAILED,
    ERROR // General error state
}

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
    val lastAttempt: String? = null,
    // Keywords for detection need to be defined
    val successKeywords: List<String> = listOf("success", "welcome", "logged in"),
    val captchaKeywords: List<String> = listOf("captcha", "verify you", "robot")
)

data class BruteforceState(
    val status: BruteforceStatus = BruteforceStatus.IDLE,
    val settings: BruteforceSettings = BruteforceSettings(),
    val currentAttempt: String? = null,
    val attemptCount: Long = 0,
    val dictionaryLoadProgress: Float = 0f, // 0.0 to 1.0 for dictionary loading
    val errorMessage: String? = null,
    val successCandidate: String? = null // Holds the string that triggered success detection
)
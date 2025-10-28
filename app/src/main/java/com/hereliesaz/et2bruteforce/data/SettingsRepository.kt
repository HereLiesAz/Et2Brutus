package com.hereliesaz.et2bruteforce.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val AUTOMATION_CONFIG = stringPreferencesKey("automation_config")
        val CHARACTER_LENGTH = intPreferencesKey("character_length")
        val CHARACTER_SET_TYPE = stringPreferencesKey("character_set_type")
        val DICTIONARY_URI = stringPreferencesKey("dictionary_uri")
        val ATTEMPT_PACE_MILLIS = longPreferencesKey("attempt_pace_millis")
        val RESUME_FROM_LAST = booleanPreferencesKey("resume_from_last")
        val SINGLE_ATTEMPT_MODE = booleanPreferencesKey("single_attempt_mode")
        val LAST_ATTEMPT = stringPreferencesKey("last_attempt")
        val SUCCESS_KEYWORDS = stringSetPreferencesKey("success_keywords")
        val CAPTCHA_KEYWORDS = stringSetPreferencesKey("captcha_keywords")
        val CONTROLLER_POSITION_X = intPreferencesKey("controller_position_x")
        val CONTROLLER_POSITION_Y = intPreferencesKey("controller_position_y")
        val WALKTHROUGH_COMPLETED = booleanPreferencesKey("walkthrough_completed")
        val MASK = stringPreferencesKey("mask")
        val HYBRID_MODE_ENABLED = booleanPreferencesKey("hybrid_mode_enabled")
        val HYBRID_SUFFIXES = stringSetPreferencesKey("hybrid_suffixes")
    }

    val settingsFlow: Flow<BruteforceSettings> = context.dataStore.data
        .map { preferences ->
            BruteforceSettings(
                characterLength = preferences[PreferencesKeys.CHARACTER_LENGTH] ?: 4,
                characterSetType = CharacterSetType.valueOf(preferences[PreferencesKeys.CHARACTER_SET_TYPE] ?: CharacterSetType.ALPHANUMERIC_SPECIAL.name),
                dictionaryUri = preferences[PreferencesKeys.DICTIONARY_URI],
                attemptPaceMillis = preferences[PreferencesKeys.ATTEMPT_PACE_MILLIS] ?: 100,
                resumeFromLast = preferences[PreferencesKeys.RESUME_FROM_LAST] ?: true,
                singleAttemptMode = preferences[PreferencesKeys.SINGLE_ATTEMPT_MODE] ?: false,
                lastAttempt = preferences[PreferencesKeys.LAST_ATTEMPT],
                successKeywords = preferences[PreferencesKeys.SUCCESS_KEYWORDS]?.toList() ?: listOf("success", "welcome", "logged in"),
                captchaKeywords = preferences[PreferencesKeys.CAPTCHA_KEYWORDS]?.toList() ?: listOf("captcha", "verify you", "robot"),
                controllerPosition = Point(preferences[PreferencesKeys.CONTROLLER_POSITION_X] ?: 100, preferences[PreferencesKeys.CONTROLLER_POSITION_Y] ?: 300),
                walkthroughCompleted = preferences[PreferencesKeys.WALKTHROUGH_COMPLETED] ?: false,
                mask = preferences[PreferencesKeys.MASK],
                hybridModeEnabled = preferences[PreferencesKeys.HYBRID_MODE_ENABLED] ?: false,
                hybridSuffixes = preferences[PreferencesKeys.HYBRID_SUFFIXES]?.toList() ?: listOf("123", "!", "2024")
            )
        }

    suspend fun updateCharacterLength(length: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHARACTER_LENGTH] = length
        }
    }

    suspend fun updateCharacterSetType(type: CharacterSetType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHARACTER_SET_TYPE] = type.name
        }
    }

    suspend fun updateDictionaryUri(uri: Uri?) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DICTIONARY_URI] = uri.toString()
        }
    }

    suspend fun updateAttemptPace(pace: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ATTEMPT_PACE_MILLIS] = pace
        }
    }

    suspend fun updateResumeFromLast(resume: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RESUME_FROM_LAST] = resume
        }
    }

    suspend fun updateSingleAttemptMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SINGLE_ATTEMPT_MODE] = enabled
        }
    }

    suspend fun updateLastAttempt(attempt: String?) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_ATTEMPT] = attempt ?: ""
        }
    }

    suspend fun updateSuccessKeywords(keywords: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUCCESS_KEYWORDS] = keywords.toSet()
        }
    }

    suspend fun updateCaptchaKeywords(keywords: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CAPTCHA_KEYWORDS] = keywords.toSet()
        }
    }

    suspend fun updateControllerPosition(position: Point) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTROLLER_POSITION_X] = position.x
            preferences[PreferencesKeys.CONTROLLER_POSITION_Y] = position.y
        }
    }

    suspend fun updateWalkthroughCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WALKTHROUGH_COMPLETED] = completed
        }
    }

    suspend fun updateMask(mask: String?) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MASK] = mask ?: ""
        }
    }

    suspend fun updateHybridModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HYBRID_MODE_ENABLED] = enabled
        }
    }

    suspend fun updateHybridSuffixes(suffixes: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HYBRID_SUFFIXES] = suffixes.toSet()
        }
    }

    val automationConfigFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTOMATION_CONFIG]
        }

    suspend fun saveAutomationConfig(config: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOMATION_CONFIG] = config
        }
    }
}

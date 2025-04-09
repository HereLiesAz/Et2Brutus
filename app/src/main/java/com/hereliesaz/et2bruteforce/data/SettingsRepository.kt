package com.hereliesaz.et2bruteforce.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.hereliesaz.et2bruteforce.model.BruteforceSettings
import com.hereliesaz.et2bruteforce.model.CharacterSetType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


// Define DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bruteforce_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private const val TAG = "SettingsRepository"
        // Define keys for DataStore preferences
        private val KEY_CHAR_LENGTH = intPreferencesKey("char_length")
        private val KEY_CHAR_SET_TYPE = stringPreferencesKey("char_set_type")
        private val KEY_DICT_URI = stringPreferencesKey("dict_uri")
        private val KEY_ATTEMPT_PACE = longPreferencesKey("attempt_pace")
        private val KEY_RESUME_LAST = booleanPreferencesKey("resume_last")
        private val KEY_LAST_ATTEMPT = stringPreferencesKey("last_attempt")
        // Consider storing keyword lists if they need to be user-configurable
    }

    val settingsFlow: Flow<BruteforceSettings> = dataStore.data
        .catch { exception ->
            // Handle errors reading DataStore, e.g., IOException
            Log.e(TAG, "Error reading settings", exception)
            if (exception is IOException) {
                emit(emptyPreferences()) // Emit empty preferences on error
            } else {
                throw exception // Rethrow other exceptions
            }
        }
        .map { preferences ->
            // Map Preferences to BruteforceSettings data class
            val charLength = preferences[KEY_CHAR_LENGTH] ?: 4
            val charSetType = CharacterSetType.valueOf(
                preferences[KEY_CHAR_SET_TYPE] ?: CharacterSetType.ALPHANUMERIC_SPECIAL.name
            )
            val dictUri = preferences[KEY_DICT_URI]
            val attemptPace = preferences[KEY_ATTEMPT_PACE] ?: 100L
            val resumeLast = preferences[KEY_RESUME_LAST] ?: true
            val lastAttempt = preferences[KEY_LAST_ATTEMPT]

            BruteforceSettings(
                characterLength = charLength,
                characterSetType = charSetType,
                dictionaryUri = dictUri,
                attemptPaceMillis = attemptPace,
                resumeFromLast = resumeLast,
                lastAttempt = lastAttempt
                // Default keywords are used here, load from prefs if needed
            )
        }

    suspend fun updateCharacterLength(length: Int) {
        updatePreference(KEY_CHAR_LENGTH, length)
    }

    suspend fun updateCharacterSetType(type: CharacterSetType) {
        updatePreference(KEY_CHAR_SET_TYPE, type.name)
    }

    suspend fun updateDictionaryUri(uri: Uri?) {
        // Persist URI string. Need to handle permission persistence separately (ContentResolver).
        updatePreference(KEY_DICT_URI, uri?.toString())
    }

    suspend fun updateAttemptPace(paceMillis: Long) {
        updatePreference(KEY_ATTEMPT_PACE, paceMillis)
    }

    suspend fun updateResumeFromLast(resume: Boolean) {
        updatePreference(KEY_RESUME_LAST, resume)
    }

    suspend fun updateLastAttempt(attempt: String?) {
        if (attempt == null) {
            // Remove the key if the attempt is null (e.g., reset)
            dataStore.edit { settings ->
                settings.remove(KEY_LAST_ATTEMPT)
                Log.d(TAG, "Cleared last attempt")
            }
        } else {
            updatePreference(KEY_LAST_ATTEMPT, attempt)
            Log.d(TAG, "Updated last attempt to: $attempt")
        }
    }

    // Generic helper function to update a preference value
    private suspend fun <T> updatePreference(key: Preferences.Key<T>, value: T?) {
        dataStore.edit { settings ->
            if (value == null) {
                settings.remove(key)
            } else {
                settings[key] = value
            }
        }
    }

    // Function to get the current settings snapshot (not flow)
    suspend fun getSettingsSnapshot(): BruteforceSettings {
        return settingsFlow.first()
    }
}
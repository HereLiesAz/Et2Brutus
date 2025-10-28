package com.hereliesaz.et2bruteforce.domain

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hereliesaz.et2bruteforce.model.BruteforceSettings
import com.hereliesaz.et2bruteforce.model.CharacterSetType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject

class BruteforceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "BruteforceEngine"
        private const val LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val NUMBERS = "0123456789"
        private const val SPECIAL_CHARS = "!@#$%^&*()-_=+[]{};:'\",.<>/?`~"
    }

    // Flow to emit dictionary words
    fun generateDictionaryCandidates(settings: BruteforceSettings): Flow<Pair<String, Float>> = callbackFlow<Pair<String, Float>> {
        val uriString = settings.dictionaryUri ?: run {
            Log.w(TAG, "Dictionary URI is null, cannot generate dictionary candidates.")
            close(IllegalStateException("Dictionary URI is null"))
            return@callbackFlow
        }
        val dictionaryUri = Uri.parse(uriString)
        val targetLength = settings.characterLength
        val resumeFrom = if (settings.resumeFromLast) settings.lastAttempt else null
        var pastResumePoint = resumeFrom == null // Start emitting immediately if not resuming

        Log.i(TAG, "Starting dictionary generation. Target Length: $targetLength, Resume From: $resumeFrom")

        try {
            context.contentResolver.openInputStream(dictionaryUri)?.use { inputStream ->
                // Persist permission if needed (should ideally be done when URI is selected)
                // val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                // context.contentResolver.takePersistableUriPermission(dictionaryUri, takeFlags)

                val reader = BufferedReader(InputStreamReader(inputStream))
                val totalSize = context.contentResolver.openFileDescriptor(dictionaryUri, "r")?.statSize ?: -1L
                var bytesRead = 0L
                var lineCount = 0L

                reader.forEachLine { line ->
                    if (!isActive) return@forEachLine // Exit if coroutine is cancelled

                    val word = line.trim()
                    val currentLineBytes = word.length + 1 // Approximate bytes including newline

                    if (word.length == targetLength) {
                        if (!pastResumePoint && word == resumeFrom) {
                            pastResumePoint = true
                            Log.i(TAG, "Resuming dictionary from: $word")
                        }

                        if (pastResumePoint) {
                            // Calculate progress (approximation)
                            val progress = if (totalSize > 0) bytesRead.toFloat() / totalSize else 0f
                            trySend(word to progress).isSuccess // Emit word and progress
                            lineCount++
                        }
                    }
                    bytesRead += currentLineBytes
                    // Optional: Yield periodically to allow cancellation checks
                    // if (lineCount % 100 == 0L) yield()
                }
                Log.i(TAG, "Finished dictionary generation. Emitted $lineCount words.")
            } ?: run {
                Log.e(TAG, "Failed to open input stream for dictionary URI: $dictionaryUri")
                close(IOException("Failed to open dictionary file"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading dictionary file", e)
            close(e) // Close the flow with the exception
        }

        awaitClose { Log.d(TAG, "Dictionary Flow closed.") }
    }.flowOn(Dispatchers.IO) // Perform file operations on IO dispatcher


    // Flow to generate permutation candidates
    fun generatePermutationCandidates(settings: BruteforceSettings): Flow<String> = callbackFlow<String> {
        val length = settings.characterLength
        val charset = getCharacterSet(settings.characterSetType)
        val resumeFrom = if (settings.resumeFromLast) settings.lastAttempt else null
        var pastResumePoint = resumeFrom == null

        Log.i(TAG, "Starting permutation generation. Length: $length, Charset Size: ${charset.length}, Resume From: $resumeFrom")

        if (charset.isEmpty() || length <= 0) {
            Log.w(TAG, "Invalid charset or length for permutation.")
            close()
            return@callbackFlow
        }

        val indices = IntArray(length) { 0 }
        val maxIndex = charset.length - 1
        var counter = 0L

        // If resuming, fast-forward indices to the starting point
        if (!pastResumePoint && resumeFrom != null && resumeFrom.length == length) {
            var possibleResume = true
            for(i in 0 until length) {
                val charIndex = charset.indexOf(resumeFrom[i])
                if (charIndex != -1) {
                    indices[i] = charIndex
                } else {
                    Log.w(TAG,"Resume string '$resumeFrom' contains characters not in selected charset. Starting from beginning.")
                    possibleResume = false
                    break // Character not in charset, cannot resume from here
                }
            }
            if (!possibleResume) {
                indices.fill(0) // Reset indices if resume failed
            } else {
                // Successfully set initial indices, need to start from the *next* permutation
                var current = length - 1
                while (current >= 0) {
                    indices[current]++
                    if (indices[current] < charset.length) break // Found next index
                    indices[current] = 0 // Reset current index, carry over to the left
                    current--
                }
                if (current < 0) {
                    // If we carried over past the start, it means the resume string was the last possible permutation
                    Log.i(TAG, "Resume string '$resumeFrom' was the last permutation. No further permutations possible.")
                    close()
                    return@callbackFlow
                }
                pastResumePoint = true // Ready to start emitting from the calculated next point
                Log.i(TAG, "Resuming permutations after: $resumeFrom")
            }
        } else {
            pastResumePoint = true // Start emitting from the beginning
        }


        // Generation loop
        while (isActive) {
            val candidate = generateString(indices, charset)

            if (pastResumePoint) {
                trySend(candidate).isSuccess // Emit the candidate string
                counter++
                // Optional: Yield periodically for large counts
                // if (counter % 1000 == 0L) yield()
            } else if (candidate == resumeFrom) {
                // This case should ideally be handled by the initial index setup,
                // but acts as a fallback if initial setup didn't advance indices.
                pastResumePoint = true
                Log.i(TAG, "Reached resume point during generation: $resumeFrom")
                // Don't emit the resume string itself, start from the next one
            }


            // Increment indices (like odometer)
            var current = length - 1
            while (current >= 0) {
                indices[current]++
                if (indices[current] < charset.length) break // Found next index
                indices[current] = 0 // Reset current index, carry over to the left
                current--
            }

            // If current is -1, it means we have exhausted all permutations
            if (current < 0) {
                Log.i(TAG, "Finished permutation generation. Emitted $counter candidates.")
                break
            }
        }

        awaitClose { Log.d(TAG, "Permutation Flow closed.") }
    }.flowOn(Dispatchers.Default) // CPU-bound task on Default dispatcher


    private fun generateString(indices: IntArray, charset: String): String {
        return buildString(indices.size) {
            indices.forEach { index ->
                append(charset[index])
            }
        }
    }

    private fun getCharacterSet(type: CharacterSetType): String {
        return when (type) {
            CharacterSetType.LETTERS -> LETTERS
            CharacterSetType.NUMBERS -> NUMBERS
            CharacterSetType.LETTERS_NUMBERS -> LETTERS + NUMBERS
            CharacterSetType.ALPHANUMERIC_SPECIAL -> LETTERS + NUMBERS + SPECIAL_CHARS
        }
    }

    // Flow to generate mask candidates
    fun generateMaskCandidates(settings: BruteforceSettings): Flow<String> = callbackFlow<String> {
        val mask = settings.mask ?: run {
            close(IllegalStateException("Mask is null"))
            return@callbackFlow
        }
        val charset = getCharacterSet(settings.characterSetType)
        val wildcardCount = mask.count { it == '*' }
        val indices = IntArray(wildcardCount) { 0 }
        val maxIndex = charset.length - 1

        while (isActive) {
            val candidate = buildString {
                var wildcardIndex = 0
                for (char in mask) {
                    if (char == '*') {
                        append(charset[indices[wildcardIndex]])
                        wildcardIndex++
                    } else {
                        append(char)
                    }
                }
            }
            trySend(candidate).isSuccess

            var current = wildcardCount - 1
            while (current >= 0) {
                indices[current]++
                if (indices[current] < charset.length) break
                indices[current] = 0
                current--
            }

            if (current < 0) {
                break
            }
        }

        awaitClose { Log.d(TAG, "Mask Flow closed.") }
    }.flowOn(Dispatchers.Default)

    // Flow to generate hybrid candidates
    fun generateHybridCandidates(settings: BruteforceSettings): Flow<Pair<String, Float>> = callbackFlow<Pair<String, Float>> {
        generateDictionaryCandidates(settings)
            .collect { (word, progress) ->
                settings.hybridSuffixes.forEach { suffix ->
                    trySend((word + suffix) to progress).isSuccess
                }
            }
        awaitClose { Log.d(TAG, "Hybrid Flow closed.") }
    }.flowOn(Dispatchers.IO)
}
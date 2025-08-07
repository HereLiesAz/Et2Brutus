// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Make sure versions align with your Android Studio version (e.g., Hedgehog | 2023.1.1)
    // Check for latest stable versions: https://developer.android.com/build/releases/gradle-plugin
    id("com.android.application") version "8.12.0" apply false // Or latest stable AGP
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false // Match Kotlin version used by Compose/Hilt
    id("com.google.dagger.hilt.android") version "2.57" apply false // Hilt plugin version
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0" apply false // If using Kotlinx Serialization (e.g., for NodeInfo saving)
    id("org.jetbrains.kotlin.kapt") version "2.2.0" apply false // If using kapt directly (often implied)
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false // Kotlin Compose plugin

}

// Define versions in a central place (optional but good practice)
// ext {
//     hilt_version = "2.48.1"
//     // ... other versions
// }

// It's generally recommended to keep this file minimal.
// Configurations like repositories usually go into settings.gradle(.kts) now.
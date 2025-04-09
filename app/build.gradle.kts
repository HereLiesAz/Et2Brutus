plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    // id("org.jetbrains.kotlin.plugin.serialization") // Keep commented if not used
}

android {
    namespace = "com.hereliesaz.et2bruteforce"
    // *** UPDATED compileSdk ***
    compileSdk = 35 // <-- Changed from 34 to 35

    defaultConfig {
        applicationId = "com.hereliesaz.et2bruteforce"
        minSdk = 26
        // *** UPDATED targetSdk ***
        targetSdk = 35 // <-- Changed from 34 to 35 (match compileSdk)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {}
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // Check compatibility for Kotlin 1.9.21 and Compose UI libraries
        kotlinCompilerExtensionVersion = "1.5.6" // Ensure this is compatible with SDK 35 if needed
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/gradle/incremental.annotation.processors"
        }
    }
    // Optional: Add this if you encounter desugaring issues with newer SDKs/libraries
    // coreLibraryDesugaringEnabled = true
}


dependencies {

    // Core Android & Kotlin
    // *** UPDATED core-ktx version based on error message ***
    implementation("androidx.core:core-ktx:1.15.0") // <-- Use the version required
    implementation("androidx.appcompat:appcompat:1.7.0") // Keep as is unless required otherwise
    implementation("com.google.android.material:material:1.12.0") // Keep as is

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7") // Keep stable version
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")

    // Activity Compose
    // *** UPDATED activity-compose version based on error message ***
    implementation("androidx.activity:activity-compose:1.11.0-alpha02") // <-- Use the version required

    // Jetpack Compose (BOM ensures compatible versions)
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01") // Check for newer stable BOM if available
    implementation("androidx.compose:compose-bom:2025.03.01")
    androidTestImplementation("androidx.compose:compose-bom:2025.03.01")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") // Keep stable version
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.56.1") // Keep stable version
    kapt("com.google.dagger:hilt-compiler:2.56.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.4") // Keep stable version

    // SavedState
    implementation("androidx.savedstate:savedstate-ktx:1.2.1") // Keep stable version
    implementation("androidx.compose.runtime:runtime-saveable")

    // Testing Dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Optional: Add desugaring dependency if enabling coreLibraryDesugaring
    // coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}

kapt {
    correctErrorTypes = true
}
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    // id("org.jetbrains.kotlin.plugin.serialization") // Keep commented if not used
}

android {
    namespace = "com.hereliesaz.et2bruteforce"
    // *** UPDATED compileSdk ***
    compileSdk = 36 // <-- Changed from 34 to 35

    defaultConfig {
        applicationId = "com.hereliesaz.et2bruteforce"
        minSdk = 26
        // *** UPDATED targetSdk ***
        targetSdk = 36 // <-- Changed from 34 to 35 (match compileSdk)
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
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
    implementation(libs.androidx.core.ktx) // <-- Use the version required
    implementation(libs.androidx.appcompat) // Keep as is unless required otherwise
    implementation(libs.material) // Keep as is

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx) // Keep stable version
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)

    // Activity Compose
    // *** UPDATED activity-compose version based on error message ***
    implementation(libs.androidx.activity.compose) // <-- Use the version required

    // Jetpack Compose (BOM ensures compatible versions)
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core) // Keep stable version
    implementation(libs.kotlinx.coroutines.android)

    // Hilt
    implementation(libs.hilt.android) // Keep stable version
    kapt(libs.hilt.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences) // Keep stable version

    // SavedState
    implementation(libs.androidx.savedstate.ktx) // Keep stable version
    implementation(libs.androidx.compose.runtime.saveable)

    // Testing Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Optional: Add desugaring dependency if enabling coreLibraryDesugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

}

kapt {
    correctErrorTypes = true
}
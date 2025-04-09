package com.hereliesaz.et2bruteforce.core


import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application() {
    // Application-level setup can go here if needed
    override fun onCreate() {
        super.onCreate()
        // Initialization code if necessary
    }
}
package com.hereliesaz.et2bruteforce.di

import android.content.Context
import android.view.WindowManager
import com.hereliesaz.et2bruteforce.comms.AccessibilityCommsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAccessibilityCommsManager(): AccessibilityCommsManager {
        return AccessibilityCommsManager()
    }

    // Provide WindowManager system service via Hilt
    @Provides
    @Singleton
    fun provideWindowManager(@ApplicationContext context: Context): WindowManager {
        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
}
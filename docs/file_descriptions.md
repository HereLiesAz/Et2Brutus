# File Descriptions
### AGENTS.md
Provides guidance for AI agents working on the Geministrator project. It outlines the project's architecture, development conventions, and testing procedures.
### README.md
Provides an overview of the Et2Brutus app, including its purpose, setup instructions, and usage guidelines.
### app/build.gradle.kts
This file contains the build configuration for the Android application. It specifies the plugins, Android-specific settings, and dependencies required to build the app.
### app/proguard-rules.pro
This file contains ProGuard rules for the application. ProGuard is a tool that shrinks, optimizes, and obfuscates code, and these rules specify which parts of the code should not be modified.
### app/src/androidTest/java/com/hereliesaz/et2bruteforce/ExampleInstrumentedTest.kt
This file contains an example of an instrumented test that runs on an Android device. It verifies that the application's context is correctly set up.
### app/src/main/AndroidManifest.xml
This file is the Android application's manifest. It declares the application's components, such as activities and services, and specifies the permissions the app requires.
### app/src/main/java/com/hereliesaz/et2bruteforce/comms/AccessibilityCommsManager.kt
Manages communication between the ViewModel and the Accessibility Service using Kotlin Flows. It handles requests, events, and provides a singleton for communication.
### app/src/main/java/com/hereliesaz/et2bruteforce/MainActivity.kt
The main activity of the application, responsible for handling permissions, user interactions, and launching the floating control service.
### app/src/main/java/com/hereliesaz/et2bruteforce/InstructionActivity.kt
This activity displays detailed instructions for using the application.
### app/src/main/java/com/hereliesaz/et2bruteforce/domain/BruteforceEngine.kt
This file contains the core logic for the bruteforce functionality. It generates candidate strings based on different character sets and dictionary files.
### app/src/main/java/com/hereliesaz/et2bruteforce/di/AppModule.kt
This file provides dependencies for the application using Dagger Hilt. It includes a provider for the WindowManager service.
### app/src/main/java/com/hereliesaz/et2bruteforce/ui/theme/Type.kt
This file defines the typography for the application's theme, including text styles for different elements.
### app/src/main/java/com/hereliesaz/et2bruteforce/ui/theme/Color.kt
Defines the color palette for the application's theme.
### app/src/main/java/com/hereliesaz/et2bruteforce/ui/theme/Theme.kt
Defines the overall theme for the application, including the color scheme and typography.
### app/src/main/java/com/hereliesaz/et2bruteforce/ui/overlay/OverlayUI.kt
This file contains the UI components for the floating overlay, including the main controller, settings dialog, and other UI elements.
### app/src/main/java/com/hereliesaz/et2bruteforce/viewmodel/BruteforceViewModel.kt
The ViewModel for the bruteforce functionality. It manages the UI state, handles user interactions, and orchestrates the bruteforce process.
### app/src/main/java/com/hereliesaz/et2bruteforce/services/BruteforceAccessibilityService.kt
The core Accessibility Service that interacts with other apps' UIs. It performs actions like clicking, inputting text, and analyzing the screen content.
### app/src/main/java/com/hereliesaz/et2bruteforce/services/FloatingControlService.kt
This service manages the floating overlay controls. It creates and manages the Compose views for the main controller and configuration buttons.
### app/src/main/java/com/hereliesaz/et2bruteforce/data/SettingsRepository.kt
This file provides a repository for managing the application's settings. It uses DataStore to persist settings and exposes them as a Flow.
### app/src/main/java/com/hereliesaz/et2bruteforce/core/MyApp.kt
The main Application class for the app. It's annotated with @HiltAndroidApp to enable Hilt for dependency injection.
### app/src/main/java/com/hereliesaz/et2bruteforce/WalkthroughActivity.kt
This activity provides a walkthrough or tutorial for new users, explaining how to set up and use the application.
### app/src/main/java/com/hereliesaz/et2bruteforce/model/HighlightInfo.kt
A data class that holds information about a highlighted UI element, including its bounds and node type.
### app/src/main/java/com/hereliesaz/et2bruteforce/model/NodeTypeExtensions.kt
This file contains extension functions for the NodeType enum, such as a function to get the color associated with each node type.
### app/src/main/java/com/hereliesaz/et2bruteforce/model/BruteforceState.kt
This file defines the data classes and enums that represent the state of the bruteforce process, including the overall status, settings, and button configurations.
### app/src/main/java/com/hereliesaz/et2bruteforce/model/NodeType.kt
This file defines the NodeType enum, which represents the type of UI element being targeted for interaction (e.g., INPUT, SUBMIT, POPUP).

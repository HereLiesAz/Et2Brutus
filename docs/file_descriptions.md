# File Descriptions

This document provides a detailed breakdown of the files in the Et2Brutus project, explaining their purpose and how they interact with each other.

---

### AGENTS.md

**Purpose:** This file is a guide for AI agents working on the project. It provides an overview of the project's architecture, development conventions, and how to run tests.

**Key Information:**
-   **Architecture:** Describes the multi-module Gradle project structure, which is currently outdated as the project is a single-module app.
-   **Development Conventions:** Outlines procedures for adding new features and updating the `CHANGELOG.md`.
-   **Testing:** Provides the command for running unit tests: `./gradlew :app_android:test`, which is also outdated and should be `./gradlew :app:test`.

---

### README.md

**Purpose:** This is the main README file for the Et2Brutus application. It provides a high-level overview of the app, its features, and instructions for setup and usage.

**Key Information:**
-   **Functionality:** Explains that the app uses an Accessibility Service to automate on-screen tasks via a floating controller.
-   **Permissions:** Details the two required permissions: "Draw Over Other Apps" and "Accessibility Service".
-   **Usage:** Describes how to start the floating controller, both manually from the app and via a keyboard shortcut (`Ctrl + G`), and how to use it to record and replay taps.

---

### app/build.gradle.kts

**Purpose:** This is the Gradle build script for the `:app` module. It defines how the Android application is built, including its dependencies, plugins, and build configurations.

**Key Components:**
-   **Plugins:** Applies the Android application plugin, Kotlin Android plugin, Hilt for dependency injection, KSP for annotation processing, and the Kotlin Compose plugin.
-   **Android Configuration:** Sets the `namespace`, `compileSdk`, `minSdk`, `targetSdk`, and other application settings. It also configures build types (release, debug) and compile options.
-   **Dependencies:** Declares all the libraries the application depends on, such as AndroidX, Jetpack Compose, Coroutines, Hilt, and DataStore.

---

### app/proguard-rules.pro

**Purpose:** This file specifies the ProGuard rules for the application. ProGuard is a tool that shrinks, optimizes, and obfuscates the code for release builds. These rules prevent ProGuard from removing or renaming essential code that is accessed through reflection or other means.

---

### app/src/androidTest/java/com/hereliesaz/et2bruteforce/ExampleInstrumentedTest.kt

**Purpose:** This file contains an example of an instrumented test. Instrumented tests run on an Android device or emulator and are used for testing UI interactions and other functionality that depends on the Android framework.

**Key Test:**
-   `useAppContext()`: A simple test that verifies the application's context can be retrieved and that its package name is correct.

---

### app/src/main/AndroidManifest.xml

**Purpose:** This is the manifest file for the Android application. It's a crucial file that declares the app's components, required permissions, and other essential information that the Android operating system needs to know to run the app.

**Key Declarations:**
-   **Permissions:** Requests permissions for `SYSTEM_ALERT_WINDOW` (to draw overlays), `FOREGROUND_SERVICE`, and `BIND_ACCESSIBILITY_SERVICE`.
-   **Application Class:** Sets the `android:name` to `.core.MyApp`, which is the entry point of the application and where Hilt's dependency injection is initialized.
-   **Activities:** Declares the `MainActivity` (the main screen for setup) and `WalkthroughActivity`.
-   **Services:** Declares the `FloatingControlService` (for the overlay) and the `BruteforceAccessibilityService` (for interacting with other apps).

---

### app/src/main/java/com/hereliesaz/et2bruteforce/comms/AccessibilityCommsManager.kt

**Purpose:** This file acts as a central communication hub between the `BruteforceViewModel` and the `BruteforceAccessibilityService`. It uses Kotlin's `SharedFlow` to create event-driven, asynchronous communication channels.

**Key Components:**
-   **Data Classes:** Defines various data classes for requests (e.g., `InputTextRequest`, `ClickNodeRequest`) and events (e.g., `ActionCompletedEvent`, `NodeIdentifiedEvent`).
-   **SharedFlows:** Exposes `SharedFlow`s that the ViewModel can emit requests into and that the Service can listen to, and vice-versa for events.
-   **Singleton:** It is a `@Singleton`, meaning a single instance is shared across the application, ensuring consistent communication.
-   **Connects:** `BruteforceViewModel`, `BruteforceAccessibilityService`, `MainActivity`.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/MainActivity.kt

**Purpose:** This is the main entry point activity for the application. Its primary role is to guide the user through the process of granting the necessary permissions and to provide a way to start and stop the `FloatingControlService`.

**Key Responsibilities:**
-   **Permission Handling:** Contains the logic for requesting the "Draw Over Other Apps" and "Accessibility Service" permissions.
-   **UI:** Uses Jetpack Compose to build the main screen (`MainScreen` composable), which displays the status of the permissions and provides buttons to start/stop the service.
-   **Service Control:** Contains the `startFloatingService()` and `stopFloatingService()` methods.
-   **Connects:** `FloatingControlService`, `BruteforceAccessibilityService`, `SettingsRepository`, `AccessibilityCommsManager`.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/InstructionActivity.kt

**Purpose:** This activity displays a more detailed set of instructions for the user on how to use the application. It's a simple, scrollable text view.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/domain/BruteforceEngine.kt

**Purpose:** This is the core of the bruteforce logic. It's responsible for generating the candidate strings that will be tested. It can generate candidates from either a dictionary file or by creating permutations of a given character set.

**Key Functions:**
-   `generateDictionaryCandidates()`: Returns a `Flow` that emits words from a user-selected dictionary file.
-   `generatePermutationCandidates()`: Returns a `Flow` that emits all possible permutations of a given length and character set.
-   **Connects:** `BruteforceViewModel`.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/di/AppModule.kt

**Purpose:** This file sets up the dependency injection for the application using Dagger Hilt. It provides dependencies that can be injected into other classes.

**Key Provision:**
-   `provideWindowManager()`: Provides the `WindowManager` system service, which is used by the `FloatingControlService` to display the overlay.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/ui/theme/

**Purpose:** This package contains the files that define the visual theme of the application, following Material Design principles.

-   **Color.kt:** Defines the color palette for the app, including primary, secondary, and background colors.
-   **Theme.kt:** Combines the color scheme and typography to create the overall `Et2BruteForceTheme` that is applied to the composable UI.
-   **Type.kt:** Defines the typography for the app, such as font sizes and weights for different text styles.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/ui/overlay/OverlayUI.kt

**Purpose:** This file contains all the Jetpack Compose UI components for the floating overlay that is displayed on top of other apps.

**Key Composables:**
-   `RootOverlay()`: The main container for the overlay UI, which decides whether to show the main controller or a config button.
-   `MainControllerUi()`: The main floating action button menu that expands to show controls for starting, stopping, and configuring the bruteforce process.
-   `ConfigButtonUi()`: The draggable buttons that are used to identify the input field, submit button, and popup button on the target application.
-   `SettingsDialog()`: A dialog for configuring advanced settings like character length, character set, and attempt pace.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/viewmodel/BruteforceViewModel.kt

**Purpose:** This is the ViewModel that drives the bruteforce functionality. It acts as the central point of control, managing the application's state, handling user interactions from the UI, and coordinating the work of the `BruteforceEngine` and the `BruteforceAccessibilityService`.

**Key Responsibilities:**
-   **State Management:** Holds the `BruteforceState` in a `StateFlow`, which the UI observes for updates.
-   **Orchestration:** The `startBruteforce()` function launches the main coroutine that generates candidates from the `BruteforceEngine` and sends requests to the `BruteforceAccessibilityService` via the `AccessibilityCommsManager`.
-   **Event Handling:** Listens for events from the `AccessibilityCommsManager` to know when actions are completed or nodes are identified.
-   **Connects:** `BruteforceState`, `SettingsRepository`, `BruteforceEngine`, `AccessibilityCommsManager`.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/services/BruteforceAccessibilityService.kt

**Purpose:** This is the core service that performs the automated interactions with other applications' UIs. It's an `AccessibilityService`, which gives it the ability to read the screen content and perform actions like clicking and typing.

**Key Responsibilities:**
-   **Accessibility Actions:** Contains the logic to `performClick()` and `performInputText()` on UI elements.
-   **Screen Analysis:** The `analyzeScreenContent()` function reads the text on the screen to detect success or CAPTCHAs.
-   **Node Identification:** The `findNodeAt()` and `findFreshNode()` methods are used to locate the UI elements that the user has identified with the draggable config buttons.
-   **Connects:** `AccessibilityCommsManager`, `FloatingControlService`.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/services/FloatingControlService.kt

**Purpose:** This service is responsible for creating, displaying, and managing the floating overlay UI on top of other applications.

**Key Responsibilities:**
-   **Window Management:** Uses the `WindowManager` to add and remove the `ComposeView`s that contain the overlay UI.
-   **Lifecycle Management:** It's a `LifecycleService` and implements `ViewModelStoreOwner` and `SavedStateRegistryOwner` to properly manage the lifecycle of the `BruteforceViewModel` and the Compose UI.
-   **UI Creation:** Creates the `RootOverlay` composable and passes the `BruteforceViewModel` and all the necessary callbacks to it.
-   **Connects:** `WindowManager`, `BruteforceViewModel`, `RootOverlay`.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/data/SettingsRepository.kt

**Purpose:** This file provides a centralized way to manage the application's settings. It uses Android's DataStore to persist settings in a file, ensuring that the user's configurations are saved between app launches.

**Key Features:**
-   **DataStore:** Uses `preferencesDataStore` to store key-value pairs.
-   **Settings Flow:** Exposes a `Flow<BruteforceSettings>` that emits the latest settings whenever they change. The `BruteforceViewModel` collects this flow to stay up-to-date.
-   **Update Methods:** Provides `suspend` functions for updating each setting (e.g., `updateCharacterLength()`, `updateDictionaryUri()`).
-   **Connects:** `BruteforceViewModel`, `MainActivity`, `WalkthroughActivity`.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/core/MyApp.kt

**Purpose:** This is the main `Application` class for the app. It serves as the entry point for the application process.

**Key Role:**
-   **Hilt Initialization:** The `@HiltAndroidApp` annotation triggers Hilt's code generation, which sets up the dependency injection container for the entire application.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/WalkthroughActivity.kt

**Purpose:** This activity provides a guided walkthrough for first-time users, explaining the steps required to set up and use the app.

**Key Features:**
-   **Pager:** Uses a `HorizontalPager` to display a series of `WalkthroughPage`s.
-   **Settings Update:** When the user finishes the walkthrough, it updates the `SettingsRepository` to mark the walkthrough as completed, so it doesn't show up again.
-   **Connects:** `SettingsRepository`.

---

### app/src/main/java/com/hereliesaz/et2bruteforce/model/

**Purpose:** This package contains the data classes and enums that define the data model for the application. These classes are used to represent the state of the application and the different types of UI elements and settings.

-   **BruteforceState.kt:** Defines the main state class for the application, `BruteforceState`, which includes the `BruteforceStatus`, `BruteforceSettings`, and `ButtonConfig`. It also defines the `BruteforceStatus` and `CharacterSetType` enums.
-   **HighlightInfo.kt:** A simple data class to hold information about a UI element that is being highlighted on the screen.
-   **NodeType.kt:** Defines the `NodeType` enum, which is used to differentiate between the different types of UI elements that the user can identify (INPUT, SUBMIT, POPUP).
-   **NodeTypeExtensions.kt:** Contains an extension function `getColor()` for the `NodeType` enum, which returns a color for each node type, used for the highlight and the config buttons.

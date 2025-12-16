# Et2Brutus Agent Guide

This document provides guidance for AI agents working on the Et2Brutus project.

## Project Overview

Et2Brutus is an Android application that uses an Accessibility Service to automate tasks on the screen.

## Architecture

The project is an Android application built with Gradle. It has a single sub-project named `:app`.

The application is written in Kotlin and uses the following technologies:

-   **Jetpack Compose**: For building the user interface.
-   **Dagger Hilt**: For dependency injection.
-   **DataStore**: For persisting user settings and profiles.
-   **Kotlinx Serialization**: For serializing and deserializing data.
-   **AzNavRail**: The floating controller UI is built using the `AzNavRail` library (integrated locally in `com.hereliesaz.et2bruteforce.ui.aznavrail`).

## Development Conventions

### Building the Project

To build the project, you will need to create a `local.properties` file in the `app` directory with the following content:

```
sdk.dir=<path to your Android SDK>
```

### Running Tests

The project has unit tests in the `app` module. To run them, use the following command from the root of the project:

```bash
./gradlew :app:test
```

### Version Catalogs

The project uses a Gradle Version Catalog (`gradle/libs.versions.toml`) to manage dependencies. All dependencies should be added to this file.

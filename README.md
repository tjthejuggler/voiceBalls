# VoiceBalls - Voice-Controlled Juggling Balls

*Last Updated: 2025-09-03*

This Android application allows users to control the color of network-connected juggling balls using voice commands. It features a background service for continuous voice recognition, dynamic ball discovery, and a simple UI for manual control and monitoring.

## Features

*   **Offline Voice Control:** Uses Picovoice (Porcupine for wake word, Cheetah for speech-to-text) for a completely offline voice experience.
*   **Background Service:** The voice recognition runs as a foreground service, allowing the app to listen for commands even when it's not in the foreground.
*   **Dynamic Ball Discovery:** Scans the local network to discover compatible juggling balls.
*   **Custom Voice Commands:** (Functionality to add/edit commands is not yet implemented in the UI) The app supports custom phrases to trigger specific color changes.
*   **Manual Control:** The UI allows users to manually test ball colors and see their connection status.

## Getting Started

### Prerequisites

*   Android Studio
*   An Android device (or emulator) with API level 26 or higher.
*   Network-connected juggling balls that respond to UDP commands on port `41412`.

### Setup

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    ```
2.  **Picovoice Access Key:**
    *   Sign up for a free Picovoice account at the [Picovoice Console](https://console.picovoice.ai/).
    *   Open `app/src/main/java/com/example/voiceballs/VoiceControlService.kt`.
    *   Replace `"YOUR_PICOVOICE_ACCESS_KEY_HERE"` with your actual Picovoice Access Key.

3.  **Custom Wake Word:**
    *   Create a custom wake word on the Picovoice Console and download the `.ppn` file.
    *   In Android Studio, right-click the `app` module, then select `New > Folder > Assets Folder`.
    *   Drag your downloaded `.ppn` file into the newly created `assets` directory.
    *   In `VoiceControlService.kt`, change the `keywordPath` variable from `"your_custom_keyword.ppn"` to the filename of your wake word file.

4.  **Build and Run:**
    *   Open the project in Android Studio.
    *   Build and run the app on your connected device or emulator.

## How to Use the App

1.  **Scan for Balls:**
    *   Ensure your Android device is on the same Wi-Fi network as your juggling balls.
    *   Tap the **"Scan for Balls"** button. The app will search the network and display any discovered balls in a list.

2.  **Start Voice Control:**
    *   Tap the **"Start Listening"** button. The app will ask for Microphone and (on Android 13+) Notification permissions.
    *   Once granted, a persistent notification will appear, indicating that the service is running.

3.  **Give a Command:**
    *   Say your wake word (e.g., "Hey Computer"). The notification will update to "Listening for command..."
    *   Speak a command, such as **"ball one red"** or **"rainbow pattern"**. The app will execute the command, and the service will go back to listening for the wake word.

4.  **Stop Voice Control:**
    *   Tap the **"Stop Listening"** button to stop the background service.

## Project Structure

*   `MainActivity.kt`: The main entry point of the app. Handles UI, permissions, and observing data from the `MainViewModel`.
*   `VoiceControlService.kt`: A foreground service that manages the Picovoice engines (Porcupine and Cheetah) for continuous, low-power voice recognition.
*   `BallController.kt`: A singleton object that handles all UDP communication for discovering and controlling the juggling balls.
*   `CommandManager.kt`: A singleton object that saves and retrieves voice commands using `SharedPreferences`.
*   `MainViewModel.kt`: A ViewModel that exposes `LiveData` from the `BallController` to the UI, following modern Android architecture patterns.
*   `BallAdapter.kt`: A `RecyclerView.Adapter` for displaying the list of discovered balls.

## Key Dependencies

*   **Picovoice:**
    *   `porcupine-android`: For wake word detection.
    *   `cheetah-android`: For streaming speech-to-text.
*   **AndroidX:**
    *   `lifecycle-viewmodel-ktx` & `lifecycle-livedata-ktx`: For ViewModel and LiveData.
    *   `recyclerview`: For displaying lists.
    *   `preference-ktx`: For easy `SharedPreferences` access.
*   **Kotlin Coroutines:** For handling background tasks like network scanning.

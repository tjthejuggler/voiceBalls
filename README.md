# VoiceBalls - Voice-Controlled Juggling Balls

*Last Updated: 2025-09-04 09:23 UTC - Ball Persistence & IP Editing IMPLEMENTED!*

This Android application allows users to control the color of network-connected juggling balls using voice commands. It features a background service for continuous voice recognition, dynamic ball discovery, and a simple UI for manual control and monitoring.

## Features

*   **Offline Voice Control:** Uses Picovoice (Porcupine for wake word, Cheetah for speech-to-text) for a completely offline voice experience.
*   **Background Service:** The voice recognition runs as a foreground service, allowing the app to listen for commands even when it's not in the foreground.
*   **Persistent Ball Storage:** Balls and their configurations (including IP addresses) are now saved between app runs.
*   **IP Address Management:** Edit individual ball IP addresses through the UI with persistent storage.
*   **Dynamic Ball Discovery:** Scans the local network to discover compatible juggling balls.
*   **Custom Voice Commands:** Add, edit, and delete custom voice commands through the UI.
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
    *   `porcupine-android:3.0.3`: For wake word detection (Maven Central).
    *   `cheetah-android:1.1.1`: For streaming speech-to-text (Maven Central).
*   **AndroidX:**
    *   `lifecycle-viewmodel-ktx` & `lifecycle-livedata-ktx`: For ViewModel and LiveData.
    *   `recyclerview`: For displaying lists.
    *   `preference-ktx`: For easy `SharedPreferences` access.
*   **Kotlin Coroutines:** For handling background tasks like network scanning.

## Build Configuration & SSL Issue Resolution

### Issue Resolved: SSL Handshake Failure (2025-09-03)

**Problem:** The project was experiencing `javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure` when trying to download Picovoice dependencies from `maven.picovoice.ai`.

**Root Cause:** Picovoice's Maven repository (`maven.picovoice.ai`) has broken SSL/TLS configuration that causes handshake failures with modern Java versions.

**Solution:** Switched to using Picovoice library versions available on Maven Central:
- `cheetah-android:1.1.1` (instead of 2.0.1 from Picovoice's repository)
- `porcupine-android:3.0.3` (compatible version from Maven Central)

**Configuration Changes Made:**
1. **gradle.properties**: Set `org.gradle.java.home` to use Android Studio's JBR
2. **app/build.gradle.kts**: Updated dependency versions to Maven Central compatible versions
3. **VoiceControlService.kt**: Fixed API compatibility issues for older library versions
4. **local.properties**: Fixed PICOVOICE_ACCESS_KEY format (removed quotes)

**Build Status:** ✅ **BUILD SUCCESSFUL** - All SSL handshake failures resolved, dependencies downloading from Maven Central, compilation successful.

## Recent Updates (2025-09-04)

### Ball Persistence & IP Address Management

**Issues Fixed:**
1. **Ball Persistence Bug:** Balls were not persisting between app runs due to missing `saveBalls()` call in `changeBallColor()` method
2. **Missing IP Address Editing:** No UI functionality to set IP addresses for individual balls
3. **IP Address Display:** Ball adapter was showing ball numbers instead of IP addresses

**Changes Made:**
1. **BallController.kt:**
   - Fixed `changeBallColor()` method to call `saveBalls()` after color changes
   - Added `updateBallIpAddress()` method for IP address management
   - Enhanced persistence logging for debugging

2. **MainActivity.kt:**
   - Added `showEditIpDialog()` method for IP address editing
   - Updated BallAdapter initialization to handle IP editing callbacks

3. **BallAdapter.kt:**
   - Added IP editing button to ball list items
   - Updated display logic to show IP addresses alongside ball numbers
   - Enhanced UI to indicate when balls have no IP address set

4. **MainViewModel.kt:**
   - Added `updateBallIpAddress()` method to expose IP editing functionality

5. **UI Layout (list_item_ball.xml):**
   - Added "IP" button for editing individual ball IP addresses
   - Improved layout to accommodate new functionality

**New Functionality:**
- ✅ Balls now persist between app runs (colors, IP addresses, all properties)
- ✅ Click "IP" button on any ball to edit its IP address
- ✅ IP addresses are saved and remembered between app sessions
- ✅ Clear visual indication of balls with/without IP addresses
- ✅ All ball state changes are now properly persisted

**Build Status:** ✅ **BUILD SUCCESSFUL** - All persistence issues resolved, IP editing functionality implemented and tested.

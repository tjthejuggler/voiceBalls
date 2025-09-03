package com.example.voiceballs

import ai.picovoice.cheetah.Cheetah
import ai.picovoice.cheetah.CheetahActivationException
import ai.picovoice.cheetah.CheetahInvalidArgumentException
import ai.picovoice.cheetah.CheetahTranscriptCallback
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.voiceballs.BuildConfig
import java.io.File

class VoiceControlService : Service() {

    private var porcupineManager: PorcupineManager? = null
    private var cheetah: Cheetah? = null
    private var isAwake = false

    // This callback is triggered when the wake word is heard.
    private val porcupineKeywordCallback = {
        Log.d("VoiceService", "Wake Word Detected!")
        isAwake = true
        updateNotification("Listening for command...")
        // We don't need to do anything else; the audio is already being piped to Cheetah.
    }

    // This callback is triggered when Cheetah finalizes a transcription.
    private val cheetahTranscriptCallback = CheetahTranscriptCallback { transcript, isEndpoint ->
        if (isAwake && transcript.isNotBlank()) {
            if (isEndpoint) {
                val fullTranscript = transcript.trim()
                Log.d("VoiceService", "Command received: $fullTranscript")
                processCommand(fullTranscript)
                isAwake = false // Go back to sleep until the next wake word
                updateNotification("Listening for wake word...")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        CommandManager.init(this)
        startVoiceRecognition()
    }

    private fun startVoiceRecognition() {
        try {
            // --- CUSTOM WAKE WORD SETUP ---
            // 1. Create a custom keyword on the Picovoice Console
            // 2. Download the .ppn file
            // 3. In Android Studio, go to app -> New -> Folder -> Assets Folder
            // 4. Drag your .ppn file into the new 'assets' folder
            val keywordPath = "your_custom_keyword.ppn" // <-- CHANGE THIS TO YOUR FILENAME

            // Initialize Cheetah for speech-to-text
            cheetah = Cheetah.Builder()
                .setAccessKey(BuildConfig.PICOVOICE_ACCESS_KEY)
                .setEnableAutomaticPunctuation(true)
                .build(applicationContext)

            // Initialize Porcupine to listen for the wake word
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(BuildConfig.PICOVOICE_ACCESS_KEY)
                .setKeywordAsset(keywordPath) // Use the keyword from assets
                .setWakeWordCallback(porcupineKeywordCallback)
                .build(applicationContext) { audioFrame ->
                    // This is the magic: pipe the audio directly from the mic to Cheetah
                    try {
                        cheetah?.process(audioFrame)
                    } catch (e: Exception) {
                        Log.e("VoiceService", "Cheetah process error: ${e.message}")
                    }
                }

            porcupineManager?.start()
            Log.d("VoiceService", "Porcupine and Cheetah started successfully.")

        } catch (e: Exception) {
            Log.e("VoiceService", "Error initializing Picovoice: ${e.message}")
            stopSelf()
        }
    }

    private fun processCommand(commandText: String) {
        val command = CommandManager.findCommand(commandText)
        if (command != null) {
            val colorMap = command.actions.associate { it.ipAddress to it.color }
            BallController.changeMultipleBallColors(colorMap)
            Log.i("VoiceService", "Executed command for phrase: '${command.phrase}'")
        } else {
            Log.w("VoiceService", "No command found for phrase: '$commandText'")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Listening for wake word..."))
        return START_STICKY
    }

    override fun onDestroy() {
        porcupineManager?.stop()
        porcupineManager?.delete()
        cheetah?.delete()
        super.onDestroy()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(contentText: String): Notification {
        val channelId = "VoiceControlServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Voice Control Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Keeps voice control active" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Juggler Voice Control")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Default icon
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1337
    }
}
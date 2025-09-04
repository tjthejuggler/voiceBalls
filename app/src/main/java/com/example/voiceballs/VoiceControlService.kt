package com.example.voiceballs

import ai.picovoice.cheetah.Cheetah
import ai.picovoice.cheetah.CheetahActivationException
import ai.picovoice.cheetah.CheetahInvalidArgumentException
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.android.voiceprocessor.VoiceProcessor
import ai.picovoice.android.voiceprocessor.VoiceProcessorFrameListener
import kotlinx.coroutines.*
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

    private var porcupine: Porcupine? = null
    private var cheetah: Cheetah? = null
    private var voiceProcessor: VoiceProcessor? = null
    private var isAwake = false


    override fun onCreate() {
        super.onCreate()

        // Promote the service to the foreground immediately.
        val notification = createNotification("Initializing voice control...")
        startForeground(NOTIFICATION_ID, notification)

        CommandManager.init(this)
        startVoiceRecognition()
    }

    private fun startVoiceRecognition() {
        try {
            val keywordPath = "change-ta_en_android_v3_0_0.ppn"

            porcupine = Porcupine.Builder()
                .setAccessKey(BuildConfig.PICOVOICE_ACCESS_KEY)
                .setKeywordPath(keywordPath)
                .build(applicationContext)

            cheetah = Cheetah.Builder()
                .setAccessKey(BuildConfig.PICOVOICE_ACCESS_KEY)
                .setEnableAutomaticPunctuation(true)
                .build(applicationContext)

            voiceProcessor = VoiceProcessor.getInstance()
            voiceProcessor?.addFrameListener(voiceProcessorFrameListener)
            voiceProcessor?.start(porcupine!!.frameLength, porcupine!!.sampleRate)

        } catch (e: Exception) {
            Log.e("VoiceService", "Error initializing Picovoice: ${e.message}")
            stopSelf()
        }
    }

    private fun processCommand(commandText: String) {
        val command = CommandManager.findCommand(commandText)
        if (command != null) {
            command.actions.forEach { action ->
                BallController.changeBallColor(action.ballId, action.color)
            }
            Log.i("VoiceService", "Executed command for phrase: '${command.phrase}'")
        } else {
            Log.w("VoiceService", "No command found for phrase: '$commandText'")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The service is already in the foreground.
        // We can update the notification text if needed.
        updateNotification("Listening for wake word...")
        broadcastStatus("Listening for wake word...")
        return START_STICKY
    }

    override fun onDestroy() {
        voiceProcessor?.stop()
        voiceProcessor?.removeFrameListener(voiceProcessorFrameListener)
        porcupine?.delete()
        cheetah?.delete()
        broadcastStatus("Service stopped")
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

    private val voiceProcessorFrameListener = VoiceProcessorFrameListener { frame ->
        if (isAwake) {
            val result = cheetah?.process(frame)
            if (result != null && result.transcript.isNotBlank()) {
                if (result.isEndpoint) {
                    val fullTranscript = result.transcript.trim()
                    Log.d("VoiceService", "Command received: $fullTranscript")
                    processCommand(fullTranscript)
                    isAwake = false
                    broadcastStatus("Listening for wake word...")
                }
            }
        } else {
            val keywordIndex = porcupine?.process(frame)
            if (keywordIndex != null && keywordIndex >= 0) {
                Log.d("VoiceService", "Wake Word Detected!")
                isAwake = true
                broadcastStatus("Wake word detected")
            }
        }
    }

    private fun broadcastStatus(status: String) {
        val intent = Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_STATUS, status)
        }
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_STATUS_UPDATE = "com.example.voiceballs.ACTION_STATUS_UPDATE"
        const val EXTRA_STATUS = "com.example.voiceballs.EXTRA_STATUS"
        private const val NOTIFICATION_ID = 1337
    }
}
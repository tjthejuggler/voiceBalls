package com.example.voiceballs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import edu.cmu.pocketsphinx.Assets
import edu.cmu.pocketsphinx.Hypothesis
import edu.cmu.pocketsphinx.RecognitionListener
import edu.cmu.pocketsphinx.SpeechRecognizer
import edu.cmu.pocketsphinx.SpeechRecognizerSetup
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

class VoiceControlService : Service() {

    private var wakeWordRecognizer: SpeechRecognizer? = null
    private var commandRecognizer: SpeechRecognizer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val wakeWord = "hey computer"
    private val wakeWordSearch = "WAKE_WORD"
    private val commandSearch = "COMMAND"

    override fun onCreate() {
        super.onCreate()
        val notification = createNotification("Initializing voice control...")
        startForeground(NOTIFICATION_ID, notification)

        CommandManager.init(this)
        scope.launch {
            try {
                setupRecognizers()
            } catch (e: Exception) {
                Log.e("VoiceControlLogger", "Error setting up recognizers: ${e.message}", e)
                stopSelf()
            }
        }
    }

    private suspend fun setupRecognizers() {
        withContext(Dispatchers.IO) {
            val assets = Assets(applicationContext)
            val assetDir = assets.syncAssets()
            val commonSetup = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(File(assetDir, "en-us-ptm"))
                .setDictionary(File(assetDir, "cmudict-en-us.dict"))

            // 1. Wake Word Recognizer (less strict)
            wakeWordRecognizer = commonSetup.recognizer
            wakeWordRecognizer?.addListener(wakeWordListener)
            wakeWordRecognizer?.addKeyphraseSearch(wakeWordSearch, wakeWord)

            // 2. Command Recognizer (more strict)
            commonSetup.setFloat("-lw", 10.0)
            commonSetup.setFloat("-wip", 0.2)
            commandRecognizer = commonSetup.recognizer
            commandRecognizer?.addListener(commandListener)
            val grammarFile = createGrammarFile(CommandManager.getAllCommands())
            commandRecognizer?.addGrammarSearch(commandSearch, grammarFile)

            startWakeWordRecognition()
            Log.i("VoiceControlLogger", "Recognizers setup complete.")
        }
    }

    private fun startWakeWordRecognition() {
        commandRecognizer?.stop()
        wakeWordRecognizer?.startListening(wakeWordSearch)
        Log.i("VoiceControlLogger", "Listening for wake word...")
        updateNotification("Listening for wake word...")
        broadcastStatus("Listening for wake word...")
    }

    private fun startCommandRecognition() {
        wakeWordRecognizer?.stop()
        commandRecognizer?.startListening(commandSearch, 20000)
        Log.i("VoiceControlLogger", "Wake word detected! Listening for command...")
        updateNotification("Listening for command...")
        broadcastStatus("Wake word detected! Listening for command...")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        wakeWordRecognizer?.cancel()
        wakeWordRecognizer?.shutdown()
        commandRecognizer?.cancel()
        commandRecognizer?.shutdown()
        Log.i("VoiceControlLogger", "Service destroyed.")
        stopForeground(true)
    }

    private val wakeWordListener = object : RecognitionListener {
        override fun onBeginningOfSpeech() {}
        override fun onEndOfSpeech() {}
        override fun onPartialResult(hypothesis: Hypothesis?) {
            if (hypothesis?.hypstr == wakeWord) {
                startCommandRecognition()
            }
        }
        override fun onResult(hypothesis: Hypothesis?) {}
        override fun onError(error: Exception) {
            Log.e("VoiceControlLogger", "Wake word error", error)
        }
        override fun onTimeout() {}
    }

    private val commandListener = object : RecognitionListener {
        override fun onBeginningOfSpeech() {}
        override fun onEndOfSpeech() {
            commandRecognizer?.stop()
            broadcastStatus("Processing command...")
            updateNotification("Processing command...")
        }
        override fun onPartialResult(hypothesis: Hypothesis?) {}
        override fun onResult(hypothesis: Hypothesis?) {
            val commandText = hypothesis?.hypstr
            if (!commandText.isNullOrBlank()) {
                processCommand(commandText)
            }
            startWakeWordRecognition()
        }
        override fun onError(error: Exception) {
            Log.e("VoiceControlLogger", "Command recognition error", error)
            startWakeWordRecognition()
        }
        override fun onTimeout() {
            Log.d("VoiceControlLogger", "Command listening timeout.")
            startWakeWordRecognition()
        }
    }

    private fun processCommand(commandText: String) {
        Log.i("VoiceControlLogger", "Recognized command: '$commandText'")
        val command = CommandManager.findCommand(commandText)
        if (command != null) {
            command.actions.forEach { action ->
                BallController.changeBallColor(action.ballId, action.color)
            }
            Log.i("VoiceControlLogger", "Executed command for phrase: '${command.phrase}'")
        } else {
            Log.w("VoiceControlLogger", "No command found for phrase: '$commandText'")
        }
    }

    // --- Boilerplate ---
    override fun onBind(intent: Intent?): IBinder? = null

    private fun broadcastStatus(status: String) {
        val intent = Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_STATUS, status)
        }
        sendBroadcast(intent)
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_STATUS_UPDATE = "com.example.voiceballs.ACTION_STATUS_UPDATE"
        const val EXTRA_STATUS = "com.example.voiceballs.EXTRA_STATUS"
        private const val NOTIFICATION_ID = 1337
    }

    @Throws(IOException::class)
    private fun createGrammarFile(commands: List<VoiceCommand>): File {
        val grammarFile = File(applicationContext.filesDir, "commands.gram")
        grammarFile.writer().use { writer ->
            writer.write("#JSGF V1.0;\n\n")
            writer.write("grammar commands;\n\n")
            if (commands.isEmpty()) {
                writer.write("public <command> = no valid commands available;\n")
            } else {
                val commandPhrases = commands.joinToString(" | ") { it.phrase }
                writer.write("public <command> = ( $commandPhrases );\n")
            }
        }
        Log.d("VoiceControlLogger", "Generated grammar file with ${commands.size} commands.")
        return grammarFile
    }
}
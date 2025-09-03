package com.example.voiceballs

import android.content.Context
import android.graphics.Color
import androidx.core.content.edit
import androidx.preference.PreferenceManager

data class ColorAction(val ipAddress: String, val color: Int)
data class VoiceCommand(val phrase: String, val actions: List<ColorAction>)

object CommandManager {
    private const val COMMAND_PREFIX = "voice_command_"

    // This must be called once when the app starts.
    fun init(context: Context) {
        this.context = context.applicationContext
    }

    private lateinit var context: Context

    fun saveCommand(command: VoiceCommand) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        // Format: "ip1,color1;ip2,color2"
        val actionsString = command.actions.joinToString(";") { "${it.ipAddress},${it.color}" }
        prefs.edit {
            putString(COMMAND_PREFIX + command.phrase.lowercase(), actionsString)
        }
    }

    fun getAllCommands(): List<VoiceCommand> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.all.mapNotNull { (key, value) ->
            if (key.startsWith(COMMAND_PREFIX) && value is String) {
                val phrase = key.removePrefix(COMMAND_PREFIX)
                val actions = value.split(';').mapNotNull { actionString ->
                    val parts = actionString.split(',')
                    if (parts.size == 2) {
                        ColorAction(ipAddress = parts[0], color = parts[1].toInt())
                    } else {
                        null
                    }
                }
                VoiceCommand(phrase, actions)
            } else {
                null
            }
        }
    }

    fun findCommand(phrase: String): VoiceCommand? {
        return getAllCommands().find { it.phrase.equals(phrase, ignoreCase = true) }
    }
    
    fun deleteCommand(phrase: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            remove(COMMAND_PREFIX + phrase.lowercase())
        }
    }

    // Add some default commands for easy testing
    fun addDefaultCommands(balls: List<Ball>) {
        if (balls.isEmpty() || getAllCommands().isNotEmpty()) return

        val ball1Ip = balls.getOrNull(0)?.ipAddress ?: return
        
        saveCommand(VoiceCommand("ball one red", listOf(ColorAction(ball1Ip, Color.RED))))
        saveCommand(VoiceCommand("ball one green", listOf(ColorAction(ball1Ip, Color.GREEN))))
        saveCommand(VoiceCommand("ball one blue", listOf(ColorAction(ball1Ip, Color.BLUE))))

        if (balls.size >= 3) {
            val ball2Ip = balls[1].ipAddress
            val ball3Ip = balls[2].ipAddress
            saveCommand(VoiceCommand("rainbow pattern", listOf(
                ColorAction(ball1Ip, Color.RED),
                ColorAction(ball2Ip, Color.YELLOW),
                ColorAction(ball3Ip, Color.GREEN)
            )))
        }
    }
}
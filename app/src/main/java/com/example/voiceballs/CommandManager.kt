package com.example.voiceballs

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import androidx.core.content.edit
import androidx.preference.PreferenceManager

data class ColorAction(val ballId: String, val color: Int)
data class VoiceCommand(val phrase: String, val actions: List<ColorAction>)

@SuppressLint("StaticFieldLeak") // Using application context, which is safe.
object CommandManager {
    private const val COMMAND_PREFIX = "voice_command_"

    // This must be called once when the app starts.
    fun init(context: Context) {
        this.context = context.applicationContext
    }

    private lateinit var context: Context

    fun saveCommand(command: VoiceCommand) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        // Format: "id1,color1;id2,color2"
        val actionsString = command.actions.joinToString(";") { "${it.ballId},${it.color}" }
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
                        ColorAction(ballId = parts[0], color = parts[1].toInt())
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

        val ball1Id = balls.getOrNull(0)?.id ?: return

        saveCommand(VoiceCommand("ball one red", listOf(ColorAction(ball1Id, Color.RED))))
        saveCommand(VoiceCommand("ball one green", listOf(ColorAction(ball1Id, Color.GREEN))))
        saveCommand(VoiceCommand("ball one blue", listOf(ColorAction(ball1Id, Color.BLUE))))

        if (balls.size >= 3) {
            val ball2Id = balls[1].id
            val ball3Id = balls[2].id
            saveCommand(VoiceCommand("rainbow pattern", listOf(
                ColorAction(ball1Id, Color.RED),
                ColorAction(ball2Id, Color.YELLOW),
                ColorAction(ball3Id, Color.GREEN)
            )))
        }
    }
}
package com.example.voiceballs

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class Ball(
    val id: String,
    val ipAddress: String?,
    var color: Int = Color.BLACK,
    var isConnected: Boolean = false,
    val isVirtual: Boolean = false,
    var number: Int = 0
)

@SuppressLint("StaticFieldLeak") // Using application context, which is safe.
object BallController {

    private const val PREFS_NAME = "voice_balls_prefs"
    private const val KEY_BALLS = "virtual_balls"

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var context: android.content.Context
    private val gson = com.google.gson.Gson()

    private val _balls = MutableLiveData<List<Ball>>(emptyList())
    val balls: LiveData<List<Ball>> = _balls

    fun init(context: android.content.Context) {
        this.context = context.applicationContext
        loadBalls()
    }

    private fun saveBalls() {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val ballsJson = gson.toJson(_balls.value)
        prefs.edit {
            putString(KEY_BALLS, ballsJson)
        }
    }

    private fun loadBalls() {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val ballsJson = prefs.getString(KEY_BALLS, null)
        if (ballsJson != null) {
            val ballListType = object : com.google.gson.reflect.TypeToken<List<Ball>>() {}.type
            val loadedBalls: List<Ball> = gson.fromJson(ballsJson, ballListType)
            _balls.postValue(loadedBalls)
        }
    }

    fun addVirtualBall() {
        val currentBalls = _balls.value.orEmpty().toMutableList()
        val nextNumber = (currentBalls.maxOfOrNull { it.number } ?: 0) + 1
        val newBall = Ball(
            id = "virtual_$nextNumber",
            ipAddress = null,
            isVirtual = true,
            isConnected = true,
            number = nextNumber,
            color = Color.rgb((0..255).random(), (0..255).random(), (0..255).random())
        )
        currentBalls.add(newBall)
        _balls.postValue(currentBalls)
        saveBalls()
    }

    fun removeBall(ballId: String) {
        val currentBalls = _balls.value.orEmpty().toMutableList()
        currentBalls.removeAll { it.id == ballId }
        _balls.postValue(currentBalls)
        saveBalls()
    }

    fun changeBallColor(ballId: String, color: Int) {
        val currentBalls = _balls.value.orEmpty()
        val ball = currentBalls.find { it.id == ballId } ?: return

        if (!ball.isVirtual) {
            coroutineScope.launch {
                try {
                    // This part will be refactored later to a dedicated networking class
                    ball.ipAddress?.let { ip ->
                        DatagramSocket().use { socket ->
                            val buffer = byteArrayOf(
                                0x01, // Command: Set Color
                                Color.red(color).toByte(),
                                Color.green(color).toByte(),
                                Color.blue(color).toByte()
                            )
                            val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName(ip), 41412)
                            socket.send(packet)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Update the state
        val updatedList = currentBalls.map {
            if (it.id == ballId) it.copy(color = color) else it
        }
        _balls.postValue(updatedList)
    }

}
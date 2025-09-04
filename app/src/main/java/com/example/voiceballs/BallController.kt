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

    fun updateBallIpAddress(ballId: String, ipAddress: String) {
        val currentBalls = _balls.value.orEmpty()
        val updatedList = currentBalls.map {
            if (it.id == ballId) it.copy(ipAddress = ipAddress.takeIf { it.isNotBlank() }) else it
        }
        _balls.postValue(updatedList)
        saveBalls()
    }

    fun changeBallColor(ballId: String, color: Int) {
        val currentBalls = _balls.value.orEmpty()
        val ball = currentBalls.find { it.id == ballId } ?: return

        // Send UDP command to Lighttrix WiFi ball if IP address is set
        ball.ipAddress?.let { ip ->
            coroutineScope.launch {
                try {
                    DatagramSocket().use { socket ->
                        // Lighttrix WiFi ball protocol: struct.pack("!bIBH", 66, 0, 0, 0) + color data
                        val udpHeader = ByteArray(8)
                        udpHeader[0] = 66.toByte()  // Header byte
                        // Remaining bytes are 0 (int + byte + short = 7 bytes)
                        
                        val colorData = byteArrayOf(
                            0x0a.toByte(), // Color command
                            (Color.red(color) * 2).toByte(),   // Red * 2
                            (Color.green(color) * 2).toByte(), // Green * 2
                            (Color.blue(color) * 2).toByte()   // Blue * 2
                        )
                        val buffer = udpHeader + colorData
                        
                        val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName(ip), 41412)
                        socket.send(packet)
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
        
        // Save balls after color change to persist the change
        saveBalls()
    }

}
package com.example.voiceballs

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class Ball(
    val ipAddress: String,
    var color: Int = Color.BLACK,
    var isConnected: Boolean = false
)

object BallController {

    private const val BALL_PORT = 41412
    private const val PROBE_TIMEOUT = 1000

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _balls = MutableLiveData<List<Ball>>(emptyList())
    val balls: LiveData<List<Ball>> = _balls

    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    fun scanForBalls() {
        if (_isScanning.value == true) return
        _isScanning.postValue(true)

        coroutineScope.launch {
            val currentBalls = _balls.value?.associateBy { it.ipAddress }?.toMutableMap() ?: mutableMapOf()
            val localIp = getLocalIpAddress() ?: return@launch
            val subnet = localIp.substring(0, localIp.lastIndexOf("."))

            val jobs = (1..254).map { i ->
                async {
                    val ipAddress = "$subnet.$i"
                    if (probeBall(ipAddress)) {
                        ipAddress
                    } else {
                        null
                    }
                }
            }

            val discoveredIps = jobs.awaitAll().filterNotNull().toSet()

            // Update ball list
            discoveredIps.forEach { ip ->
                if (!currentBalls.containsKey(ip)) {
                    currentBalls[ip] = Ball(ipAddress = ip, isConnected = true)
                } else {
                    currentBalls[ip]?.isConnected = true
                }
            }
            // Mark non-discovered balls as disconnected
            currentBalls.values.forEach { ball ->
                if (ball.ipAddress !in discoveredIps) {
                    ball.isConnected = false
                }
            }
            
            _balls.postValue(currentBalls.values.sortedBy { it.ipAddress })
            _isScanning.postValue(false)
        }
    }

    fun changeBallColor(ipAddress: String, color: Int) {
        coroutineScope.launch {
            try {
                DatagramSocket().use { socket ->
                    val buffer = byteArrayOf(
                        0x01, // Command: Set Color
                        Color.red(color).toByte(),
                        Color.green(color).toByte(),
                        Color.blue(color).toByte()
                    )
                    val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName(ipAddress), BALL_PORT)
                    socket.send(packet)
                    
                    // Update the state
                    val updatedList = _balls.value?.map {
                        if (it.ipAddress == ipAddress) it.copy(color = color) else it
                    }
                    if (updatedList != null) {
                        _balls.postValue(updatedList)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun changeMultipleBallColors(colorMap: Map<String, Int>) {
        colorMap.forEach { (ip, color) ->
            changeBallColor(ip, color)
        }
    }

    private suspend fun probeBall(ipAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = PROBE_TIMEOUT
                val buffer = byteArrayOf(0x02) // Command: Probe
                val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName(ipAddress), BALL_PORT)
                socket.send(packet)

                val receiveBuffer = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(receivePacket)
                return@withContext true
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    // You'll need a utility to get the device's local IP, as this isn't built-in.
    // This is a common implementation.
    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = java.util.Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        if (sAddr != null) {
                            val isIPv4 = sAddr.indexOf(':') < 0
                            if (isIPv4) return sAddr
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            // Ignore
        }
        return null
    }
}
package com.example.voiceballs

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    val balls: LiveData<List<Ball>> = BallController.balls
    val isScanning: LiveData<Boolean> = BallController.isScanning

    fun startScan() {
        BallController.scanForBalls()
    }

    fun changeBallColor(ipAddress: String, color: Int) {
        BallController.changeBallColor(ipAddress, color)
    }
}
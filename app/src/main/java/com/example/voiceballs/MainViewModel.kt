package com.example.voiceballs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    val balls: LiveData<List<Ball>> = BallController.balls
    private val _commands = MutableLiveData<List<VoiceCommand>>()
    val commands: LiveData<List<VoiceCommand>> = _commands

    fun loadCommands() {
        _commands.value = CommandManager.getAllCommands()
    }

    fun addVirtualBall() {
        BallController.addVirtualBall()
    }

    fun removeBall(ballId: String) {
        BallController.removeBall(ballId)
    }

    fun changeBallColor(ballId: String, color: Int) {
        BallController.changeBallColor(ballId, color)
    }

    fun updateBallIpAddress(ballId: String, ipAddress: String) {
        BallController.updateBallIpAddress(ballId, ipAddress)
    }

    fun deleteCommand(phrase: String) {
        CommandManager.deleteCommand(phrase)
        loadCommands()
    }
}
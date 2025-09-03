package com.example.voiceballs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var ballAdapter: BallAdapter
    private lateinit var progressBar: ProgressBar

    // Updated: Now requests two permissions
    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isMicGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
            val isNotificationGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true // True for older APIs

            if (isMicGranted && isNotificationGranted) {
                startVoiceService()
            } else {
                Toast.makeText(this, "Microphone and Notification permissions are required.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main.xml)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        progressBar = findViewById(R.id.progress_bar)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_balls)
        val btnStart: Button = findViewById(R.id.btn_start_service)
        val btnStop: Button = findViewById(R.id.btn_stop_service)
        val btnScan: Button = findViewById(R.id.btn_scan)

        ballAdapter = BallAdapter { ip, color ->
            viewModel.changeBallColor(ip, color)
        }
        recyclerView.adapter = ballAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        btnStart.setOnClickListener { checkPermissionsAndStartService() }
        btnStop.setOnClickListener { stopVoiceService() }
        btnScan.setOnClickListener { viewModel.startScan() }
    }

    private fun observeViewModel() {
        viewModel.balls.observe(this) { balls ->
            ballAdapter.submitList(balls)
            if (balls.isNotEmpty()) {
                CommandManager.init(this)
                CommandManager.addDefaultCommands(balls)
            }
        }
        viewModel.isScanning.observe(this) { isScanning ->
            progressBar.visibility = if (isScanning) View.VISIBLE else View.GONE
            findViewById<Button>(R.id.btn_scan).isEnabled = !isScanning
        }
    }

    private fun checkPermissionsAndStartService() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        // Notification permission is only needed on Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            startVoiceService()
        } else {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startVoiceService() {
        val serviceIntent = Intent(this, VoiceControlService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        Toast.makeText(this, "Voice service started.", Toast.LENGTH_SHORT).show()
    }

    private fun stopVoiceService() {
        val serviceIntent = Intent(this, VoiceControlService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Voice service stopped.", Toast.LENGTH_SHORT).show()
    }
}
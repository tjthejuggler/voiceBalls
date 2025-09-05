package com.example.voiceballs

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var ballAdapter: BallAdapter
    private lateinit var commandAdapter: CommandAdapter
    private lateinit var statusTextView: TextView
    private lateinit var ballsRecyclerView: RecyclerView
    private lateinit var commandsRecyclerView: RecyclerView
    private lateinit var ballsCollapseIndicator: TextView
    private lateinit var commandsCollapseIndicator: TextView
    private var ballsSectionCollapsed = false
    private var commandsSectionCollapsed = false

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == VoiceControlService.ACTION_STATUS_UPDATE) {
                val status = intent.getStringExtra(VoiceControlService.EXTRA_STATUS)
                statusTextView.text = status
            }
        }
    }

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
        setContentView(R.layout.activity_main)

        CommandManager.init(this)
        BallController.init(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupUI()
        observeViewModel()
        viewModel.loadCommands()
    }

    private fun setupUI() {
        statusTextView = findViewById(R.id.text_voice_status)
        ballsRecyclerView = findViewById(R.id.recycler_view_balls)
        commandsRecyclerView = findViewById(R.id.recycler_view_commands)
        ballsCollapseIndicator = findViewById(R.id.balls_collapse_indicator)
        commandsCollapseIndicator = findViewById(R.id.commands_collapse_indicator)
        val btnStart: Button = findViewById(R.id.btn_start_service)
        val btnStop: Button = findViewById(R.id.btn_stop_service)
        val btnAddBall: Button = findViewById(R.id.btn_add_ball)
        val btnAddCommand: Button = findViewById(R.id.btn_add_command)
        val ballsHeaderContainer = findViewById<View>(R.id.balls_header_container)
        val commandsHeaderContainer = findViewById<View>(R.id.commands_header_container)

        ballAdapter = BallAdapter(
            onColorChanged = { ballId, color -> viewModel.changeBallColor(ballId, color) },
            onRemoveClicked = { ballId -> viewModel.removeBall(ballId) },
            onEditIpClicked = { ballId, currentIp -> showEditIpDialog(ballId, currentIp) }
        )
        ballsRecyclerView.adapter = ballAdapter
        ballsRecyclerView.layoutManager = LinearLayoutManager(this)

        commandAdapter = CommandAdapter(
            onDeleteClicked = { phrase -> viewModel.deleteCommand(phrase) },
            onEditClicked = { command -> showEditCommandDialog(command) },
            onPlayClicked = { command -> executeCommand(command) }
        )
        commandsRecyclerView.adapter = commandAdapter
        commandsRecyclerView.layoutManager = LinearLayoutManager(this)

        btnStart.setOnClickListener { checkPermissionsAndStartService() }
        btnStop.setOnClickListener { stopVoiceService() }
        btnAddBall.setOnClickListener { viewModel.addVirtualBall() }
        btnAddCommand.setOnClickListener { showAddCommandDialog() }

        // Setup collapsible sections
        ballsHeaderContainer.setOnClickListener { toggleBallsSection() }
        commandsHeaderContainer.setOnClickListener { toggleCommandsSection() }
    }

    private fun observeViewModel() {
        viewModel.balls.observe(this) { balls ->
            ballAdapter.submitList(balls)
            if (balls.isNotEmpty()) {
                CommandManager.addDefaultCommands(balls)
            }
        }
        viewModel.commands.observe(this) { commands ->
            commandAdapter.submitList(commands)
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

    private fun showAddCommandDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_command, null)
        val editTextCommand = dialogView.findViewById<EditText>(R.id.edit_text_command_phrase)
        val spinnerBalls = dialogView.findViewById<Spinner>(R.id.spinner_balls)
        val spinnerColors = dialogView.findViewById<Spinner>(R.id.spinner_colors)

        // Populate balls spinner
        val balls = viewModel.balls.value ?: emptyList()
        val ballNames = balls.map { "Ball ${it.number}" }
        val ballAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ballNames)
        spinnerBalls.adapter = ballAdapter

        // Populate colors spinner
        val colors = mapOf("Red" to Color.RED, "Green" to Color.GREEN, "Blue" to Color.BLUE)
        val colorNames = colors.keys.toList()
        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colorNames)
        spinnerColors.adapter = colorAdapter

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val commandPhrase = editTextCommand.text.toString()
                if (commandPhrase.isNotBlank()) {
                    val selectedBallIndex = spinnerBalls.selectedItemPosition
                    val selectedColorName = spinnerColors.selectedItem as String
                    val selectedColor = colors[selectedColorName] ?: Color.BLACK

                    if (selectedBallIndex >= 0 && selectedBallIndex < balls.size) {
                        val selectedBall = balls[selectedBallIndex]
                        val action = ColorAction(selectedBall.id, selectedColor)
                        val command = VoiceCommand(commandPhrase, listOf(action))
                        CommandManager.saveCommand(command)
                        viewModel.loadCommands() // Refresh the command list in the UI
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditCommandDialog(command: VoiceCommand) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_command, null)
        val editTextCommand = dialogView.findViewById<EditText>(R.id.edit_text_command_phrase)
        val spinnerBalls = dialogView.findViewById<Spinner>(R.id.spinner_balls)
        val spinnerColors = dialogView.findViewById<Spinner>(R.id.spinner_colors)

        // Pre-fill with existing command data
        editTextCommand.setText(command.phrase)

        // Populate balls spinner
        val balls = viewModel.balls.value ?: emptyList()
        val ballNames = balls.map { "Ball ${it.number}" }
        val ballAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ballNames)
        spinnerBalls.adapter = ballAdapter

        // Populate colors spinner
        val colors = mapOf("Red" to Color.RED, "Green" to Color.GREEN, "Blue" to Color.BLUE)
        val colorNames = colors.keys.toList()
        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colorNames)
        spinnerColors.adapter = colorAdapter

        // Pre-select the current ball and color if possible
        if (command.actions.isNotEmpty()) {
            val firstAction = command.actions[0]
            val ballIndex = balls.indexOfFirst { it.id == firstAction.ballId }
            if (ballIndex >= 0) {
                spinnerBalls.setSelection(ballIndex)
            }
            
            val colorName = colors.entries.find { it.value == firstAction.color }?.key
            val colorIndex = colorNames.indexOf(colorName)
            if (colorIndex >= 0) {
                spinnerColors.setSelection(colorIndex)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Command")
            .setView(dialogView)
            .setPositiveButton("Update") { dialog, _ ->
                val commandPhrase = editTextCommand.text.toString()
                if (commandPhrase.isNotBlank()) {
                    val selectedBallIndex = spinnerBalls.selectedItemPosition
                    val selectedColorName = spinnerColors.selectedItem as String
                    val selectedColor = colors[selectedColorName] ?: Color.BLACK

                    if (selectedBallIndex >= 0 && selectedBallIndex < balls.size) {
                        val selectedBall = balls[selectedBallIndex]
                        val action = ColorAction(selectedBall.id, selectedColor)
                        val newCommand = VoiceCommand(commandPhrase, listOf(action))
                        
                        // Delete the old command and save the new one
                        CommandManager.deleteCommand(command.phrase)
                        CommandManager.saveCommand(newCommand)
                        viewModel.loadCommands() // Refresh the command list in the UI
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditIpDialog(ballId: String, currentIp: String?) {
        val editText = EditText(this).apply {
            setText(currentIp ?: "")
            hint = "Enter IP address (e.g., 192.168.1.100)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Ball IP Address")
            .setMessage("Enter the IP address for this physical ball:")
            .setView(editText)
            .setPositiveButton("Save") { dialog, _ ->
                val newIp = editText.text.toString().trim()
                viewModel.updateBallIpAddress(ballId, newIp)
                Toast.makeText(this, "IP address updated", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleBallsSection() {
        ballsSectionCollapsed = !ballsSectionCollapsed
        if (ballsSectionCollapsed) {
            ballsRecyclerView.visibility = View.GONE
            ballsCollapseIndicator.text = "▶"
        } else {
            ballsRecyclerView.visibility = View.VISIBLE
            ballsCollapseIndicator.text = "▼"
        }
    }

    private fun toggleCommandsSection() {
        commandsSectionCollapsed = !commandsSectionCollapsed
        if (commandsSectionCollapsed) {
            commandsRecyclerView.visibility = View.GONE
            commandsCollapseIndicator.text = "▶"
        } else {
            commandsRecyclerView.visibility = View.VISIBLE
            commandsCollapseIndicator.text = "▼"
        }
    }

    private fun executeCommand(command: VoiceCommand) {
        // Execute all actions in the command
        command.actions.forEach { action ->
            viewModel.changeBallColor(action.ballId, action.color)
        }
        
        // Show feedback to user
        Toast.makeText(this, "Executed: '${command.phrase}'", Toast.LENGTH_SHORT).show()
    }

    private fun startVoiceService() {
        val serviceIntent = Intent(this, VoiceControlService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        statusTextView.text = "Voice service started"
        Toast.makeText(this, "Voice service started.", Toast.LENGTH_SHORT).show()
    }

    private fun stopVoiceService() {
        val serviceIntent = Intent(this, VoiceControlService::class.java)
        stopService(serviceIntent)
        statusTextView.text = "Voice service stopped"
        Toast.makeText(this, "Voice service stopped.", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(statusReceiver, IntentFilter(VoiceControlService.ACTION_STATUS_UPDATE), RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(statusReceiver)
    }
}
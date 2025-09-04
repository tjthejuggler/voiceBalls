package com.example.voiceballs

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class BallAdapter(
    private val onColorChanged: (id: String, color: Int) -> Unit,
    private val onRemoveClicked: (id: String) -> Unit,
    private val onEditIpClicked: (id: String, currentIp: String?) -> Unit
) : ListAdapter<Ball, BallAdapter.BallViewHolder>(BallDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BallViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_ball, parent, false)
        return BallViewHolder(view)
    }

    override fun onBindViewHolder(holder: BallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorView: View = itemView.findViewById(R.id.view_color)
        private val ipTextView: TextView = itemView.findViewById(R.id.text_ip_address)
        private val statusTextView: TextView = itemView.findViewById(R.id.text_status)
        private val redButton: Button = itemView.findViewById(R.id.btn_test_red)
        private val greenButton: Button = itemView.findViewById(R.id.btn_test_green)
        private val blueButton: Button = itemView.findViewById(R.id.btn_test_blue)
        private val editIpButton: Button = itemView.findViewById(R.id.btn_edit_ip)

        fun bind(ball: Ball) {
            // Display IP address or ball number if no IP is set
            ipTextView.text = if (ball.ipAddress.isNullOrBlank()) {
                "Ball ${ball.number} (No IP)"
            } else {
                "Ball ${ball.number}: ${ball.ipAddress}"
            }

            val background = colorView.background as GradientDrawable
            background.setColor(ball.color)

            if (ball.isConnected) {
                statusTextView.text = "Connected"
                statusTextView.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
            } else {
                statusTextView.text = "Disconnected"
                statusTextView.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
            }

            redButton.setOnClickListener { onColorChanged(ball.id, Color.RED) }
            greenButton.setOnClickListener { onColorChanged(ball.id, Color.GREEN) }
            blueButton.setOnClickListener { onColorChanged(ball.id, Color.BLUE) }

            // Handle edit IP button
            editIpButton.setOnClickListener { onEditIpClicked(ball.id, ball.ipAddress) }

            // Handle remove button
            val removeButton: Button = itemView.findViewById(R.id.btn_remove)
            removeButton.setOnClickListener { onRemoveClicked(ball.id) }
        }
    }
}

class BallDiffCallback : DiffUtil.ItemCallback<Ball>() {
    override fun areItemsTheSame(oldItem: Ball, newItem: Ball): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Ball, newItem: Ball): Boolean {
        return oldItem == newItem
    }
}
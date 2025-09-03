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
    private val onColorTestClicked: (ip: String, color: Int) -> Unit
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

        fun bind(ball: Ball) {
            ipTextView.text = "Ball: ${ball.ipAddress}"

            val background = colorView.background as GradientDrawable
            background.setColor(ball.color)

            if (ball.isConnected) {
                statusTextView.text = "Connected"
                statusTextView.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
            } else {
                statusTextView.text = "Disconnected"
                statusTextView.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
            }

            redButton.setOnClickListener { onColorTestClicked(ball.ipAddress, Color.RED) }
            greenButton.setOnClickListener { onColorTestClicked(ball.ipAddress, Color.GREEN) }
            blueButton.setOnClickListener { onColorTestClicked(ball.ipAddress, Color.BLUE) }
        }
    }
}

class BallDiffCallback : DiffUtil.ItemCallback<Ball>() {
    override fun areItemsTheSame(oldItem: Ball, newItem: Ball): Boolean {
        return oldItem.ipAddress == newItem.ipAddress
    }
    override fun areContentsTheSame(oldItem: Ball, newItem: Ball): Boolean {
        return oldItem == newItem
    }
}
package com.example.voiceballs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class CommandAdapter(
    private val onDeleteClicked: (String) -> Unit
) : ListAdapter<VoiceCommand, CommandAdapter.CommandViewHolder>(CommandDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_command, parent, false)
        return CommandViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommandViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val phraseTextView: TextView = itemView.findViewById(R.id.text_command_phrase)
        private val deleteButton: Button = itemView.findViewById(R.id.btn_delete_command)

        fun bind(command: VoiceCommand) {
            phraseTextView.text = "'${command.phrase}'"
            deleteButton.setOnClickListener { onDeleteClicked(command.phrase) }
        }
    }
}

class CommandDiffCallback : DiffUtil.ItemCallback<VoiceCommand>() {
    override fun areItemsTheSame(oldItem: VoiceCommand, newItem: VoiceCommand): Boolean {
        return oldItem.phrase == newItem.phrase
    }

    override fun areContentsTheSame(oldItem: VoiceCommand, newItem: VoiceCommand): Boolean {
        return oldItem == newItem
    }
}
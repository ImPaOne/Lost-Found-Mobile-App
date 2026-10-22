package com.group3.lostandfound.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.group3.lostandfound.ChatMessage
import com.group3.lostandfound.databinding.ItemChatMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView Adapter for real-time chat messages between item finders and owners.
 */
class ChatAdapter(
    private val currentUserId: String
) : ListAdapter<ChatMessage, ChatAdapter.ChatMessageViewHolder>(DiffCallback) {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatMessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatMessageViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatMessage: ChatMessage) {
            val formattedTime = if (chatMessage.messageSentTimestamp > 0L) {
                timeFormat.format(Date(chatMessage.messageSentTimestamp))
            } else {
                ""
            }

            val hasImage = chatMessage.imageUrl.isNotBlank()
            val hasText = chatMessage.chatMessageTextContent.isNotBlank()

            if (chatMessage.senderUserId == currentUserId) {
                // Outgoing message
                binding.layoutOutgoingMessage.visibility = View.VISIBLE
                binding.layoutIncomingMessage.visibility = View.GONE

                if (hasImage) {
                    binding.cardOutgoingImage.visibility = View.VISIBLE
                    com.group3.lostandfound.util.ImageLoader.load(
                        binding.root.context,
                        chatMessage.imageUrl,
                        binding.imgOutgoingPhoto
                    )
                } else {
                    binding.cardOutgoingImage.visibility = View.GONE
                }

                binding.tvOutgoingText.visibility = if (hasText) View.VISIBLE else View.GONE
                binding.tvOutgoingText.text = chatMessage.chatMessageTextContent
                binding.tvOutgoingTime.text = formattedTime
            } else {
                // Incoming message
                binding.layoutOutgoingMessage.visibility = View.GONE
                binding.layoutIncomingMessage.visibility = View.VISIBLE

                if (hasImage) {
                    binding.cardIncomingImage.visibility = View.VISIBLE
                    com.group3.lostandfound.util.ImageLoader.load(
                        binding.root.context,
                        chatMessage.imageUrl,
                        binding.imgIncomingPhoto
                    )
                } else {
                    binding.cardIncomingImage.visibility = View.GONE
                }

                binding.tvIncomingText.visibility = if (hasText) View.VISIBLE else View.GONE
                binding.tvIncomingText.text = chatMessage.chatMessageTextContent
                binding.tvIncomingTime.text = formattedTime
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.chatMessageId == newItem.chatMessageId
            }

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem == newItem
            }
        }
    }
}

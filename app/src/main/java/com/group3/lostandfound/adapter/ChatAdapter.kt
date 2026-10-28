package com.group3.lostandfound.adapter

import android.graphics.Typeface
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.group3.lostandfound.ChatMessage
import com.group3.lostandfound.R
import com.group3.lostandfound.databinding.ItemChatMessageBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * RecyclerView Adapter for real-time chat messages between item finders and owners.
 * Supports WhatsApp-style swipe/click reply, delete for me / everyone, and date headers.
 */
class ChatAdapter(
    private val currentUserId: String,
    private val onMessageLongClicked: ((ChatMessage) -> Unit)? = null,
    private val onReplyClicked: ((ChatMessage) -> Unit)? = null
) : ListAdapter<ChatMessage, ChatAdapter.ChatMessageViewHolder>(DiffCallback) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatMessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
        val currentMessage = getItem(position)
        val previousMessage = if (position > 0) getItem(position - 1) else null
        holder.bind(currentMessage, previousMessage)
    }

    inner class ChatMessageViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatMessage: ChatMessage, previousMessage: ChatMessage?) {
            // Check if hidden for current user (Delete for me)
            if (chatMessage.deletedForUserIds.contains(currentUserId)) {
                binding.root.visibility = View.GONE
                binding.root.layoutParams = RecyclerView.LayoutParams(0, 0)
                return
            } else {
                binding.root.visibility = View.VISIBLE
                binding.root.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            // Calendar Date Header Check
            val showDateHeader = shouldDisplayDateHeader(chatMessage, previousMessage)
            if (showDateHeader) {
                binding.layoutDateHeader.visibility = View.VISIBLE
                binding.tvDateHeader.text = formatCalendarHeaderDate(chatMessage.messageSentTimestamp)
            } else {
                binding.layoutDateHeader.visibility = View.GONE
            }

            val formattedTime = if (chatMessage.messageSentTimestamp > 0L) {
                timeFormat.format(Date(chatMessage.messageSentTimestamp))
            } else {
                ""
            }

            val isDeleted = chatMessage.isDeletedForEveryone
            val hasImage = chatMessage.imageUrl.isNotBlank() && !isDeleted
            val isOutgoing = chatMessage.senderUserId == currentUserId

            val displayContent = if (isDeleted) {
                "This message was deleted"
            } else {
                chatMessage.chatMessageTextContent
            }

            if (isOutgoing) {
                // Outgoing message
                binding.layoutOutgoingMessage.visibility = View.VISIBLE
                binding.layoutIncomingMessage.visibility = View.GONE

                // Image Handling
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

                // Reply Quoted Box
                if (chatMessage.replyToText.isNotBlank() && !isDeleted) {
                    binding.layoutOutgoingReplyBox.visibility = View.VISIBLE
                    binding.tvOutgoingReplyName.text = chatMessage.replyToSenderName.ifEmpty { "Reply" }
                    binding.tvOutgoingReplyText.text = chatMessage.replyToText
                } else {
                    binding.layoutOutgoingReplyBox.visibility = View.GONE
                }

                binding.tvOutgoingText.visibility = if (displayContent.isNotBlank() || isDeleted) View.VISIBLE else View.GONE
                binding.tvOutgoingText.text = displayContent
                binding.tvOutgoingText.setTypeface(null, if (isDeleted) Typeface.ITALIC else Typeface.NORMAL)
                binding.tvOutgoingText.setTextColor(
                    if (isDeleted) ContextCompat.getColor(binding.root.context, R.color.nude_cream)
                    else ContextCompat.getColor(binding.root.context, R.color.white)
                )

                binding.tvOutgoingTime.text = if (formattedTime.isNotEmpty()) "$formattedTime · Read \u2713\u2713" else ""

                binding.cardOutgoingBubble.setOnLongClickListener {
                    onMessageLongClicked?.invoke(chatMessage)
                    true
                }
                binding.cardOutgoingBubble.setOnClickListener {
                    onReplyClicked?.invoke(chatMessage)
                }
            } else {
                // Incoming message
                binding.layoutOutgoingMessage.visibility = View.GONE
                binding.layoutIncomingMessage.visibility = View.VISIBLE

                // Image Handling
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

                // Reply Quoted Box
                if (chatMessage.replyToText.isNotBlank() && !isDeleted) {
                    binding.layoutIncomingReplyBox.visibility = View.VISIBLE
                    binding.tvIncomingReplyName.text = chatMessage.replyToSenderName.ifEmpty { "Reply" }
                    binding.tvIncomingReplyText.text = chatMessage.replyToText
                } else {
                    binding.layoutIncomingReplyBox.visibility = View.GONE
                }

                binding.tvIncomingText.visibility = if (displayContent.isNotBlank() || isDeleted) View.VISIBLE else View.GONE
                binding.tvIncomingText.text = displayContent
                binding.tvIncomingText.setTypeface(null, if (isDeleted) Typeface.ITALIC else Typeface.NORMAL)
                binding.tvIncomingText.setTextColor(
                    if (isDeleted) ContextCompat.getColor(binding.root.context, R.color.text_secondary)
                    else ContextCompat.getColor(binding.root.context, R.color.text_primary)
                )

                binding.tvIncomingTime.text = formattedTime

                binding.cardIncomingBubble.setOnLongClickListener {
                    onMessageLongClicked?.invoke(chatMessage)
                    true
                }
                binding.cardIncomingBubble.setOnClickListener {
                    onReplyClicked?.invoke(chatMessage)
                }
            }
        }

        private fun shouldDisplayDateHeader(current: ChatMessage, previous: ChatMessage?): Boolean {
            if (previous == null) return current.messageSentTimestamp > 0L
            val cal1 = Calendar.getInstance().apply { timeInMillis = current.messageSentTimestamp }
            val cal2 = Calendar.getInstance().apply { timeInMillis = previous.messageSentTimestamp }
            return cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR) ||
                    cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR)
        }

        private val dayMonthFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)

        private fun formatCalendarHeaderDate(timestamp: Long): String {
            if (timestamp <= 0L) return "TODAY"
            val dayMonth = dayMonthFormat.format(Date(timestamp)).uppercase()
            return if (DateUtils.isToday(timestamp)) {
                "TODAY · $dayMonth"
            } else if (DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS)) {
                "YESTERDAY · $dayMonth"
            } else {
                SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(Date(timestamp)).uppercase()
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

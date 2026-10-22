package com.group3.lostandfound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group3.lostandfound.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing Real-Time Messaging state.
 * Streams messages via Coroutines Flow and handles asynchronous message dispatching.
 */
class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _chatMessagesListState = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessagesListState: StateFlow<List<ChatMessage>> = _chatMessagesListState.asStateFlow()

    private val _sendMessageOperationStatus = MutableStateFlow<ResultState<ChatMessage>?>(null)
    val sendMessageOperationStatus: StateFlow<ResultState<ChatMessage>?> = _sendMessageOperationStatus.asStateFlow()

    private var activeConversationId: String = ""

    /**
     * Initializes the real-time message stream for the specified chatConversationId.
     * Uses descriptive variable names and avoids duplicate message entries.
     */
    fun initializeChatConversation(chatConversationId: String) {
        if (activeConversationId == chatConversationId && _chatMessagesListState.value.isNotEmpty()) {
            return
        }
        activeConversationId = chatConversationId
        _chatMessagesListState.value = emptyList()

        viewModelScope.launch {
            chatRepository.streamAllChatMessages(chatConversationId)
                .collect { messageList ->
                    _chatMessagesListState.value = messageList
                }
        }
    }

    /**
     * Dispatches a message to the Firebase Realtime Database.
     */
    fun sendUserChatMessage(
        chatConversationId: String,
        senderUserId: String,
        recipientUserId: String,
        chatMessageTextContent: String,
        relatedItemId: String = "",
        otherUserName: String = "",
        imageUrl: String = ""
    ) {
        if (chatMessageTextContent.isBlank() && imageUrl.isBlank()) return

        viewModelScope.launch {
            _sendMessageOperationStatus.value = ResultState.Loading
            val operationResult = chatRepository.sendChatMessage(
                chatConversationId = chatConversationId,
                senderUserId = senderUserId,
                recipientUserId = recipientUserId,
                chatMessageTextContent = chatMessageTextContent.trim(),
                relatedItemId = relatedItemId,
                otherUserName = otherUserName,
                imageUrl = imageUrl
            )
            _sendMessageOperationStatus.value = operationResult
        }
    }

    /**
     * Resets the send status state after being consumed by the UI.
     */
    fun resetSendMessageStatus() {
        _sendMessageOperationStatus.value = null
    }
}

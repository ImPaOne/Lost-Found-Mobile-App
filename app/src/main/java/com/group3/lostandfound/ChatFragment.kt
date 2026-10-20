package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.group3.lostandfound.adapter.ChatAdapter
import com.group3.lostandfound.databinding.FragmentChatBinding
import com.group3.lostandfound.repository.ChatRepository
import kotlinx.coroutines.launch

/**
 * Fragment implementing real-time messaging between item finders and owners.
 * Connects to ChatViewModel and ChatRepository with Firebase Realtime Database.
 */
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatViewModel: ChatViewModel
    private val chatRepository = ChatRepository()

    private lateinit var chatAdapter: ChatAdapter

    private var chatConversationId: String = ""
    private var recipientUserId: String = ""
    private var recipientUserName: String = "Item Owner / Finder"
    private var relatedItemId: String = ""
    private var relatedItemTitle: String = ""

    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "default_user_101"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            recipientUserId = bundle.getString(ARG_RECIPIENT_USER_ID, "user_perera")
            recipientUserName = bundle.getString(ARG_RECIPIENT_NAME, "Item Contact")
            relatedItemId = bundle.getString(ARG_ITEM_ID, "")
            relatedItemTitle = bundle.getString(ARG_ITEM_TITLE, "Item Discussion")

            val passedConversationId = bundle.getString(ARG_CONVERSATION_ID, "")
            chatConversationId = if (passedConversationId.isNotEmpty()) {
                passedConversationId
            } else {
                chatRepository.generateChatConversationId(
                    firstUserId = currentUserId,
                    secondUserId = recipientUserId,
                    relatedItemId = relatedItemId
                )
            }
        } ?: run {
            recipientUserId = "user_perera"
            recipientUserName = "Dilshan Perera"
            chatConversationId = chatRepository.generateChatConversationId(currentUserId, recipientUserId)
        }

        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        chatViewModel.initializeChatConversation(chatConversationId)
    }

    private fun setupHeader() {
        binding.tvChatTitle.text = recipientUserName
        binding.tvChatSubtitle.text = if (relatedItemTitle.isNotEmpty()) {
            "Regarding: $relatedItemTitle"
        } else {
            "Direct Discussion"
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(currentUserId = currentUserId)
        val linearLayoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.apply {
            layoutManager = linearLayoutManager
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSendChatMessage.setOnClickListener {
            val chatMessageTextContent = binding.etChatMessageInput.text.toString()
            if (chatMessageTextContent.isNotBlank()) {
                chatViewModel.sendUserChatMessage(
                    chatConversationId = chatConversationId,
                    senderUserId = currentUserId,
                    recipientUserId = recipientUserId,
                    chatMessageTextContent = chatMessageTextContent,
                    relatedItemId = relatedItemId,
                    otherUserName = recipientUserName
                )
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    chatViewModel.chatMessagesListState.collect { messageList ->
                        chatAdapter.submitList(messageList) {
                            if (messageList.isNotEmpty()) {
                                binding.rvChatMessages.scrollToPosition(messageList.size - 1)
                            }
                        }
                        binding.layoutChatEmptyState.visibility =
                            if (messageList.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    chatViewModel.sendMessageOperationStatus.collect { operationStatus ->
                        when (operationStatus) {
                            is ResultState.Success -> {
                                binding.etChatMessageInput.text.clear()
                                chatViewModel.resetSendMessageStatus()
                            }
                            is ResultState.Error -> {
                                com.group3.lostandfound.util.ModernToast.show(
                                    requireContext(),
                                    "Error sending: ${operationStatus.message}",
                                    binding.root
                                )
                                chatViewModel.resetSendMessageStatus()
                            }
                            is ResultState.Loading -> {
                                // Sending in progress
                            }
                            null -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_CONVERSATION_ID = "chat_conversation_id"
        const val ARG_RECIPIENT_USER_ID = "recipient_user_id"
        const val ARG_RECIPIENT_NAME = "recipient_name"
        const val ARG_ITEM_ID = "item_id"
        const val ARG_ITEM_TITLE = "item_title"

        fun newInstance(
            recipientUserId: String,
            recipientName: String,
            itemId: String,
            itemTitle: String
        ): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_RECIPIENT_USER_ID, recipientUserId)
                    putString(ARG_RECIPIENT_NAME, recipientName)
                    putString(ARG_ITEM_ID, itemId)
                    putString(ARG_ITEM_TITLE, itemTitle)
                }
            }
        }
    }
}

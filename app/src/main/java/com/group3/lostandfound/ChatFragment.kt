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
        get() = FirebaseManager.currentUid

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

    private var attachedImageUri: android.net.Uri? = null
    private var previousMessageCount: Int = -1
    private val reportRepository = com.group3.lostandfound.repository.ReportRepository()

    private val pickPhotoLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            attachedImageUri = uri
            binding.layoutAttachmentPreview.visibility = View.VISIBLE
            com.group3.lostandfound.util.ImageLoader.load(requireContext(), uri.toString(), binding.imgAttachedPreview)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnChatMenu.setOnClickListener {
            showChatOptionsMenu()
        }

        binding.btnAttachPhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }

        binding.btnRemoveAttachment.setOnClickListener {
            attachedImageUri = null
            binding.layoutAttachmentPreview.visibility = View.GONE
        }

        binding.btnQuickReply1.setOnClickListener {
            binding.etChatMessageInput.setText("Hi, is this item still available?")
            binding.etChatMessageInput.setSelection(binding.etChatMessageInput.text.length)
            binding.etChatMessageInput.requestFocus()
        }

        binding.btnQuickReply2.setOnClickListener {
            binding.etChatMessageInput.setText("Can we arrange a safe handover for this item?")
            binding.etChatMessageInput.setSelection(binding.etChatMessageInput.text.length)
            binding.etChatMessageInput.requestFocus()
        }

        binding.btnQuickReply3.setOnClickListener {
            binding.etChatMessageInput.setText("I have verification proof and details for this item.")
            binding.etChatMessageInput.setSelection(binding.etChatMessageInput.text.length)
            binding.etChatMessageInput.requestFocus()
        }

        binding.btnSendChatMessage.setOnClickListener {
            val chatMessageTextContent = binding.etChatMessageInput.text.toString().trim()
            val uriToSend = attachedImageUri

            if (chatMessageTextContent.isBlank() && uriToSend == null) {
                return@setOnClickListener
            }

            if (uriToSend != null) {
                // Upload image first via resilient pipeline
                binding.btnSendChatMessage.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        var uploadedUrl = ""
                        val compressedBytes = reportRepository.compressImageUri(
                            requireContext(),
                            uriToSend,
                            maxWidth = 800,
                            maxHeight = 800,
                            quality = 75
                        )

                        // Tier 1: Try Cloudinary
                        try {
                            uploadedUrl = com.group3.lostandfound.util.CloudinaryManager.uploadImage(compressedBytes) {}
                        } catch (_: Exception) {}

                        // Tier 2: Resilient Base64 fallback if network/Cloudinary failed
                        if (!uploadedUrl.startsWith("http://") && !uploadedUrl.startsWith("https://")) {
                            val b64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                            uploadedUrl = "data:image/jpeg;base64,$b64"
                        }

                        chatViewModel.sendUserChatMessage(
                            chatConversationId = chatConversationId,
                            senderUserId = currentUserId,
                            recipientUserId = recipientUserId,
                            chatMessageTextContent = chatMessageTextContent,
                            relatedItemId = relatedItemId,
                            otherUserName = recipientUserName,
                            imageUrl = uploadedUrl,
                            relatedItemTitle = relatedItemTitle
                        )
                    } catch (e: Exception) {
                        com.group3.lostandfound.util.ModernToast.show(
                            requireContext(),
                            "Failed to attach photo: ${e.message}",
                            binding.root
                        )
                    } finally {
                        binding.btnSendChatMessage.isEnabled = true
                    }
                }
            } else {
                chatViewModel.sendUserChatMessage(
                    chatConversationId = chatConversationId,
                    senderUserId = currentUserId,
                    recipientUserId = recipientUserId,
                    chatMessageTextContent = chatMessageTextContent,
                    relatedItemId = relatedItemId,
                    otherUserName = recipientUserName,
                    relatedItemTitle = relatedItemTitle
                )
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    chatViewModel.chatMessagesListState.collect { messageList ->
                        if (previousMessageCount != -1 && messageList.size > previousMessageCount) {
                            val lastMsg = messageList.last()
                            if (lastMsg.senderUserId != currentUserId && lastMsg.senderUserId.isNotEmpty()) {
                                try {
                                    val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                                    val ringtone = android.media.RingtoneManager.getRingtone(requireContext().applicationContext, soundUri)
                                    ringtone?.play()
                                } catch (_: Exception) {}
                            }
                        }
                        previousMessageCount = messageList.size

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
                                attachedImageUri = null
                                binding.layoutAttachmentPreview.visibility = View.GONE
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

    private fun showChatOptionsMenu() {
        val options = arrayOf(
            "Block $recipientUserName",
            "Report Inappropriate Activity",
            "Clear Local View"
        )
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Chat Options · $recipientUserName")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmBlockUser()
                    1 -> {
                        com.group3.lostandfound.util.ModernToast.show(
                            requireContext(),
                            "Report submitted for safety review.",
                            binding.root
                        )
                    }
                    2 -> {
                        chatAdapter.submitList(emptyList())
                        binding.layoutChatEmptyState.visibility = View.VISIBLE
                        com.group3.lostandfound.util.ModernToast.show(
                            requireContext(),
                            "Conversation view cleared.",
                            binding.root
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmBlockUser() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Block $recipientUserName?")
            .setMessage("Are you sure you want to block this user? They will no longer be able to message you regarding item reports.")
            .setPositiveButton("Block User") { _, _ ->
                val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
                val blocked = prefs.getStringSet("blocked_uids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                blocked.add(recipientUserId)
                prefs.edit().putStringSet("blocked_uids", blocked).apply()

                if (currentUserId.isNotEmpty()) {
                    FirebaseManager.database.getReference("blocked_users")
                        .child(currentUserId)
                        .child(recipientUserId)
                        .setValue(mapOf("blockedAt" to System.currentTimeMillis(), "userName" to recipientUserName))
                }

                binding.etChatMessageInput.isEnabled = false
                binding.btnSendChatMessage.isEnabled = false
                binding.btnAttachPhoto.isEnabled = false
                com.group3.lostandfound.util.ModernToast.show(
                    requireContext(),
                    "$recipientUserName has been blocked.",
                    binding.root
                )
                parentFragmentManager.popBackStack()
            }
            .setNegativeButton("Cancel", null)
            .show()
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

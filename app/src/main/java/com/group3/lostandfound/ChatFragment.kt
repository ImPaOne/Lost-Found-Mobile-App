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
        chatAdapter = ChatAdapter(
            currentUserId = currentUserId,
            onMessageLongClicked = { message -> handleMessageLongClick(message) },
            onReplyClicked = { message -> startReply(message) }
        )
        val linearLayoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.apply {
            layoutManager = linearLayoutManager
            adapter = chatAdapter
        }
    }

    private var attachedImageUri: android.net.Uri? = null
    private var selectedReplyMessage: ChatMessage? = null
    private var previousMessageCount: Int = -1
    private var isRecipientTyping: Boolean = false
    private var recipientPresence: Pair<Boolean, Long> = Pair(false, 0L)
    private var typingDebounceJob: kotlinx.coroutines.Job? = null
    private val reportRepository = com.group3.lostandfound.repository.ReportRepository()

    private val pickPhotoLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            attachedImageUri = uri
            binding.layoutAttachmentPreview.visibility = View.VISIBLE
            com.group3.lostandfound.util.ImageLoader.load(requireContext(), uri.toString(), binding.imgAttachedPreview)
        }
    }

    private val takeCameraLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToTempUri(bitmap)
            if (uri != null) {
                attachedImageUri = uri
                binding.layoutAttachmentPreview.visibility = View.VISIBLE
                com.group3.lostandfound.util.ImageLoader.load(requireContext(), uri.toString(), binding.imgAttachedPreview)
            }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takeCameraLauncher.launch(null)
        } else {
            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Camera permission is required to capture photos", binding.root)
        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            pickPhotoLauncher.launch("image/*")
        } else {
            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Storage permission is required to select photos", binding.root)
        }
    }

    private fun saveBitmapToTempUri(bitmap: android.graphics.Bitmap): android.net.Uri? {
        return try {
            val cachePath = java.io.File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val file = java.io.File(cachePath, "chat_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { stream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                stream.flush()
            }
            androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun showAttachmentOptions() {
        val options = arrayOf("Take Photo with Camera", "Choose from Gallery")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Attach Photo")
            .setItems(options) { _, which ->
                if (which == 0) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            requireContext(),
                            android.Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        takeCameraLauncher.launch(null)
                    } else {
                        requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                } else {
                    val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            requireContext(),
                            storagePermission
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        pickPhotoLauncher.launch("image/*")
                    } else {
                        requestStoragePermissionLauncher.launch(storagePermission)
                    }
                }
            }
            .show()
    }

    private fun handleMessageLongClick(message: ChatMessage) {
        if (message.isDeletedForEveryone) return
        val isSender = message.senderUserId == currentUserId
        val options = if (isSender) {
            arrayOf("Delete for everyone", "Delete for me", "Reply")
        } else {
            arrayOf("Delete for me", "Reply")
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Message Options")
            .setItems(options) { _, which ->
                val selected = options[which]
                when (selected) {
                    "Delete for everyone" -> {
                        chatViewModel.deleteMessageForEveryone(chatConversationId, message.chatMessageId)
                        com.group3.lostandfound.util.ModernToast.show(requireContext(), "Message deleted for everyone", binding.root)
                    }
                    "Delete for me" -> {
                        chatViewModel.deleteMessageForMe(chatConversationId, message.chatMessageId, currentUserId)
                        com.group3.lostandfound.util.ModernToast.show(requireContext(), "Message removed from your view", binding.root)
                    }
                    "Reply" -> {
                        startReply(message)
                    }
                }
            }
            .show()
    }

    private fun startReply(message: ChatMessage) {
        if (message.isDeletedForEveryone) return
        selectedReplyMessage = message
        binding.layoutReplyPreview.visibility = View.VISIBLE
        binding.tvReplyToName.text = if (message.senderUserId == currentUserId) "You" else recipientUserName
        val preview = if (message.chatMessageTextContent.isNotBlank()) message.chatMessageTextContent else "[Photo]"
        binding.tvReplyToText.text = preview
        binding.etChatMessageInput.requestFocus()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnChatMenu.setOnClickListener {
            showChatOptionsMenu()
        }

        binding.btnAttachPhoto.setOnClickListener {
            showAttachmentOptions()
        }

        binding.btnRemoveAttachment.setOnClickListener {
            attachedImageUri = null
            binding.layoutAttachmentPreview.visibility = View.GONE
        }

        binding.btnCloseReply.setOnClickListener {
            selectedReplyMessage = null
            binding.layoutReplyPreview.visibility = View.GONE
        }

        binding.etChatMessageInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                if (hasText) {
                    chatViewModel.setTyping(chatConversationId, currentUserId, true)
                    typingDebounceJob?.cancel()
                    typingDebounceJob = viewLifecycleOwner.lifecycleScope.launch {
                        kotlinx.coroutines.delay(2000)
                        chatViewModel.setTyping(chatConversationId, currentUserId, false)
                    }
                } else {
                    chatViewModel.setTyping(chatConversationId, currentUserId, false)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

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
            val replyMsg = selectedReplyMessage

            if (chatMessageTextContent.isBlank() && uriToSend == null) {
                return@setOnClickListener
            }

            val replyId = replyMsg?.chatMessageId ?: ""
            val replyName = if (replyMsg != null) {
                if (replyMsg.senderUserId == currentUserId) "You" else recipientUserName
            } else ""
            val replyText = replyMsg?.chatMessageTextContent?.ifEmpty { "[Photo]" } ?: ""

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
                            relatedItemTitle = relatedItemTitle,
                            replyToMessageId = replyId,
                            replyToSenderName = replyName,
                            replyToText = replyText
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
                    relatedItemTitle = relatedItemTitle,
                    replyToMessageId = replyId,
                    replyToSenderName = replyName,
                    replyToText = replyText
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
                                    val mediaPlayer = android.media.MediaPlayer.create(
                                        requireContext().applicationContext,
                                        R.raw.apple_chime
                                    )
                                    mediaPlayer?.start()
                                    mediaPlayer?.setOnCompletionListener { it.release() }
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
                    chatViewModel.getTypingFlow(chatConversationId, recipientUserId).collect { typing ->
                        isRecipientTyping = typing
                        updateChatSubtitle()
                    }
                }

                launch {
                    chatViewModel.getUserPresenceFlow(recipientUserId).collect { presence ->
                        recipientPresence = presence
                        updateChatSubtitle()
                    }
                }

                launch {
                    chatViewModel.sendMessageOperationStatus.collect { operationStatus ->
                        when (operationStatus) {
                            is ResultState.Success -> {
                                binding.etChatMessageInput.text.clear()
                                attachedImageUri = null
                                selectedReplyMessage = null
                                binding.layoutReplyPreview.visibility = View.GONE
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

    private fun updateChatSubtitle() {
        if (_binding == null) return
        if (isRecipientTyping) {
            binding.tvChatSubtitle.text = "typing..."
            binding.tvChatSubtitle.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary)
            )
            binding.tvChatSubtitle.setTypeface(null, android.graphics.Typeface.BOLD_ITALIC)
        } else {
            binding.tvChatSubtitle.setTypeface(null, android.graphics.Typeface.NORMAL)
            binding.tvChatSubtitle.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
            if (recipientPresence.first) {
                binding.tvChatSubtitle.text = "Online"
            } else if (recipientPresence.second > 0L) {
                val formatted = android.text.format.DateUtils.getRelativeTimeSpanString(
                    recipientPresence.second,
                    System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS
                )
                binding.tvChatSubtitle.text = "Last seen $formatted"
            } else if (relatedItemTitle.isNotEmpty()) {
                binding.tvChatSubtitle.text = "Regarding: $relatedItemTitle"
            } else {
                binding.tvChatSubtitle.text = "Direct Discussion"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentUserId.isNotEmpty()) {
            chatRepository.setUserPresence(currentUserId, true)
        }
    }

    override fun onPause() {
        super.onPause()
        typingDebounceJob?.cancel()
        if (currentUserId.isNotEmpty() && chatConversationId.isNotEmpty()) {
            chatViewModel.setTyping(chatConversationId, currentUserId, false)
            chatRepository.setUserPresence(currentUserId, false)
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

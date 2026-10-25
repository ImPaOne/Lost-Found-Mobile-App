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

    private var isCurrentUserReporter: Boolean = false
    private var isItemAlreadyResolved: Boolean = false

    private val currentUserId: String
        get() = FirebaseManager.currentUid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            recipientUserId = bundle.getString(ARG_RECIPIENT_USER_ID, "user_kasun")
            recipientUserName = bundle.getString(ARG_RECIPIENT_NAME, "Kasun Jayasinghe")
            relatedItemId = bundle.getString(ARG_ITEM_ID, "item_wallet")
            relatedItemTitle = bundle.getString(ARG_ITEM_TITLE, "Black leather wallet")

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
            recipientUserId = "user_kasun"
            recipientUserName = "Kasun Jayasinghe"
            relatedItemId = "item_wallet"
            relatedItemTitle = "Black leather wallet"
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
        checkReportResolutionStatus()
        NavigationHelper.setup(this, binding.root, NavigationHelper.Tab.MESSAGES)

        chatViewModel.initializeChatConversation(chatConversationId)
    }

    private fun setupHeader() {
        val displayName = recipientUserName.ifEmpty { "Kasun Jayasinghe" }
        binding.tvChatTitle.text = displayName
        updateChatSubtitle()
        setupPinnedReport()
    }

    private fun setupPinnedReport() {
        val defaultTitle = if (relatedItemTitle.isNotBlank()) relatedItemTitle else "Black leather wallet"
        binding.tvPinnedItemTitle.text = defaultTitle
        binding.tvPinnedItemSubtitle.text = "Colombo 03 · 12 Sep"
        binding.tvReportTag.text = "YOUR LOST REPORT"

        if (relatedItemId.isNotBlank()) {
            FirebaseManager.itemsRef.child(relatedItemId).get().addOnSuccessListener { snapshot ->
                if (_binding == null || !isAdded) return@addOnSuccessListener
                val title = snapshot.child("title").getValue(String::class.java) ?: defaultTitle
                val location = snapshot.child("location").getValue(String::class.java) ?: "Colombo 03"
                val type = snapshot.child("type").getValue(String::class.java) ?: "LOST"
                val photoUrl = snapshot.child("imageUrl").getValue(String::class.java) ?: ""

                binding.tvPinnedItemTitle.text = title.ifEmpty { defaultTitle }
                binding.tvReportTag.text = if (type.equals("LOST", ignoreCase = true)) "YOUR LOST REPORT" else "FOUND REPORT"
                binding.tvPinnedItemSubtitle.text = "$location · 12 Sep"

                if (photoUrl.isNotEmpty()) {
                    com.group3.lostandfound.util.ImageLoader.load(requireContext(), photoUrl, binding.imgPinnedReport)
                }
            }

            val openDetails = View.OnClickListener {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.screenBox, DetailsFragment.newInstance(relatedItemId))
                    .addToBackStack(null)
                    .commit()
            }
            binding.cardPinnedItem.setOnClickListener(openDetails)
            binding.btnViewItemDetails.setOnClickListener(openDetails)
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
        val displayName = if (message.senderUserId == currentUserId) {
            "You"
        } else if (message.replyToSenderName.isNotBlank() && message.replyToSenderName != "You") {
            message.replyToSenderName
        } else {
            recipientUserName
        }
        binding.tvReplyToName.text = "Replying to $displayName"
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

        binding.rvChatMessages.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom && chatAdapter.itemCount > 0) {
                binding.rvChatMessages.post {
                    binding.rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            }
        }

        binding.etChatMessageInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && chatAdapter.itemCount > 0) {
                binding.rvChatMessages.postDelayed({
                    binding.rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }, 100)
            }
        }

        binding.etChatMessageInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                if (hasText) {
                    if (chatAdapter.itemCount > 0) {
                        binding.rvChatMessages.post {
                            binding.rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
                        }
                    }
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

        binding.btnStartOwnershipCheck.setOnClickListener {
            val ownershipPrompt = "Could you describe a detail that isn't in the report? You can use Foundly's secure ownership check."
            binding.etChatMessageInput.setText(ownershipPrompt)
            binding.etChatMessageInput.setSelection(ownershipPrompt.length)
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
            binding.dotActiveStatus.visibility = View.VISIBLE
        } else {
            binding.tvChatSubtitle.setTypeface(null, android.graphics.Typeface.NORMAL)
            if (recipientPresence.first) {
                binding.tvChatSubtitle.text = "Active now"
                binding.tvChatSubtitle.setTextColor(android.graphics.Color.parseColor("#0D9488"))
                binding.dotActiveStatus.visibility = View.VISIBLE
            } else if (recipientPresence.second > 0L) {
                val formatted = android.text.format.DateUtils.getRelativeTimeSpanString(
                    recipientPresence.second,
                    System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS
                )
                binding.tvChatSubtitle.text = "Last seen $formatted"
                binding.tvChatSubtitle.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
                binding.dotActiveStatus.visibility = View.GONE
            } else {
                binding.tvChatSubtitle.text = "Active now"
                binding.tvChatSubtitle.setTextColor(android.graphics.Color.parseColor("#0D9488"))
                binding.dotActiveStatus.visibility = View.VISIBLE
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

    private fun checkReportResolutionStatus() {
        if (relatedItemId.isBlank()) {
            binding.layoutResolveReportBanner.visibility = View.GONE
            return
        }

        FirebaseManager.itemsRef.child(relatedItemId)
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (_binding == null || !isAdded) return
                    val reporterId = snapshot.child("userId").getValue(String::class.java) ?: ""
                    val status = snapshot.child("status").getValue(String::class.java) ?: ""
                    isCurrentUserReporter = reporterId.isNotEmpty() && reporterId == currentUserId
                    isItemAlreadyResolved = status.equals("RESOLVED", ignoreCase = true)

                    if (isCurrentUserReporter && !isItemAlreadyResolved) {
                        binding.layoutResolveReportBanner.visibility = View.VISIBLE
                        binding.tvResolveBannerTitle.text = "Reunited with $recipientUserName?"
                        binding.tvResolveBannerSub.text = "Select this user to resolve & award achievement"
                        binding.btnResolveWithChatUser.visibility = View.VISIBLE
                        binding.btnResolveWithChatUser.text = "Resolve Case"
                        binding.btnResolveWithChatUser.setOnClickListener {
                            confirmResolveReportWithUser(recipientUserId, recipientUserName)
                        }
                    } else if (isItemAlreadyResolved) {
                        val resolvedWithName = snapshot.child("resolvedWithUserName").getValue(String::class.java)
                            ?: recipientUserName
                        binding.layoutResolveReportBanner.visibility = View.VISIBLE
                        binding.tvResolveBannerTitle.text = "Report RESOLVED & Reunited"
                        binding.tvResolveBannerSub.text = "Resolved with $resolvedWithName"
                        binding.btnResolveWithChatUser.visibility = View.GONE
                    } else {
                        binding.layoutResolveReportBanner.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }

    private fun confirmResolveReportWithUser(targetUserId: String, targetUserName: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Resolve Case with $targetUserName")
            .setMessage(
                "Did $targetUserName help safely return or reunite '$relatedItemTitle'?\n\n" +
                "Recognition & Rewards:\n" +
                "• $targetUserName will earn the 'Reunited Hero' Achievement Badge\n" +
                "• +20 Trust Score points for $targetUserName\n" +
                "• +15 Trust Score points for you\n" +
                "• Case status will be permanently marked as RESOLVED"
            )
            .setPositiveButton("Yes, Resolve & Award!") { _, _ ->
                performReportResolution(targetUserId, targetUserName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performReportResolution(targetUserId: String, targetUserName: String) {
        if (relatedItemId.isBlank()) return
        val currentContext = context ?: return
        val itemTitle = relatedItemTitle.ifEmpty { "Reported Item" }
        val reporterName = FirebaseManager.currentUserEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

        // 1. Update Item in Firebase Realtime Database
        val itemUpdate = mapOf(
            "status" to "RESOLVED",
            "resolvedWithUserId" to targetUserId,
            "resolvedWithUserName" to targetUserName,
            "resolvedByUserId" to currentUserId,
            "resolvedTimestamp" to System.currentTimeMillis()
        )
        FirebaseManager.itemsRef.child(relatedItemId).updateChildren(itemUpdate)

        // 2. Award Achievement and Trust Score to Chatted User (targetUserId)
        if (targetUserId.isNotEmpty()) {
            val recipientDbRef = FirebaseManager.usersRef.child(targetUserId)
            recipientDbRef.child("itemsReturned").setValue(com.google.firebase.database.ServerValue.increment(1))
            recipientDbRef.child("trustScore").setValue(com.google.firebase.database.ServerValue.increment(20))

            val achievementKey = "reunited_hero_${System.currentTimeMillis()}"
            val recipientAchievement = mapOf(
                "id" to achievementKey,
                "title" to "Reunited Hero",
                "description" to "Helped resolve '$itemTitle' with $reporterName! Safely returned to owner.",
                "timestamp" to System.currentTimeMillis(),
                "badge" to "HERO_REUNITED",
                "xpAwarded" to 20
            )
            recipientDbRef.child("achievements").child(achievementKey).setValue(recipientAchievement)

            // Notification for recipient
            val notif = mapOf(
                "id" to "notif_${System.currentTimeMillis()}",
                "title" to "Achievement Unlocked: Reunited Hero!",
                "message" to "$reporterName confirmed you helped reunite '$itemTitle'. You earned +20 Trust Points!",
                "timestamp" to System.currentTimeMillis(),
                "type" to "ACHIEVEMENT",
                "itemId" to relatedItemId,
                "senderUserId" to currentUserId,
                "read" to false
            )
            FirebaseManager.database.getReference("notifications").child(targetUserId).push().setValue(notif)
        }

        // 3. Award Trust Score and Achievement to Reporting User (currentUserId)
        if (currentUserId.isNotEmpty()) {
            val currentReporterRef = FirebaseManager.usersRef.child(currentUserId)
            currentReporterRef.child("trustScore").setValue(com.google.firebase.database.ServerValue.increment(15))
            currentReporterRef.child("resolvedReports").setValue(com.google.firebase.database.ServerValue.increment(1))

            val reporterAchievement = mapOf(
                "id" to "case_resolved_${System.currentTimeMillis()}",
                "title" to "Case Resolved",
                "description" to "Successfully resolved '$itemTitle' with $targetUserName!",
                "timestamp" to System.currentTimeMillis()
            )
            currentReporterRef.child("achievements").push().setValue(reporterAchievement)
        }

        // 4. Send celebratory announcement message in chat
        chatViewModel.sendUserChatMessage(
            chatConversationId = chatConversationId,
            senderUserId = currentUserId,
            recipientUserId = targetUserId,
            chatMessageTextContent = "[Case Resolved] '$itemTitle' has been marked as RESOLVED! Congratulations to $targetUserName for safely reuniting this item! (+20 Trust XP & Reunited Hero badge awarded)",
            relatedItemId = relatedItemId,
            otherUserName = targetUserName,
            relatedItemTitle = itemTitle
        )

        // 5. Trigger local push notification & update banner
        com.group3.lostandfound.service.FCMService.triggerPushNotification(
            currentContext,
            "Item Reunited & Resolved!",
            "'$itemTitle' marked RESOLVED with $targetUserName. Achievements awarded!",
            relatedItemId
        )

        isItemAlreadyResolved = true
        binding.tvResolveBannerTitle.text = "Report RESOLVED & Reunited"
        binding.tvResolveBannerSub.text = "Resolved with $targetUserName"
        binding.btnResolveWithChatUser.visibility = View.GONE

        androidx.appcompat.app.AlertDialog.Builder(currentContext)
            .setTitle("Congratulations!")
            .setMessage("'$itemTitle' has been marked as RESOLVED!\n\n$targetUserName was awarded the 'Reunited Hero' achievement and +20 Trust Score points.\nYou received +15 Trust Score points.")
            .setPositiveButton("Awesome!", null)
            .show()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        if (com.group3.lostandfound.service.FloatingOverlayService.canDrawOverlays(requireContext())) {
            com.group3.lostandfound.service.FloatingOverlayService.startService(requireContext())
            com.group3.lostandfound.util.ModernToast.show(
                requireContext(),
                "Floating chat head launched! You can minimize Foundly and keep multitasking.",
                binding.root
            )
        }
    }

    private fun launchFloatingBubble() {
        if (com.group3.lostandfound.service.FloatingOverlayService.canDrawOverlays(requireContext())) {
            com.group3.lostandfound.service.FloatingOverlayService.startService(requireContext())
            com.group3.lostandfound.util.ModernToast.show(
                requireContext(),
                "Floating chat head launched! You can minimize Foundly and keep multitasking.",
                binding.root
            )
        } else {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Allow Draw Over Other Apps")
                .setMessage("Foundly requires permission to display over other apps to show floating chat bubbles while you use other apps.")
                .setPositiveButton("Open Settings") { _, _ ->
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${requireContext().packageName}")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showChatOptionsMenu() {
        val optionsList = mutableListOf<String>()
        if (isCurrentUserReporter && !isItemAlreadyResolved && relatedItemId.isNotBlank()) {
            optionsList.add("Mark RESOLVED with $recipientUserName")
        }
        optionsList.add("Pop out Floating Chat Bubble")
        optionsList.add("Block $recipientUserName")
        optionsList.add("Report Inappropriate Activity")
        optionsList.add("Clear Local View")

        val options = optionsList.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Chat Options · $recipientUserName")
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Mark RESOLVED with $recipientUserName" -> {
                        confirmResolveReportWithUser(recipientUserId, recipientUserName)
                    }
                    "Pop out Floating Chat Bubble" -> {
                        launchFloatingBubble()
                    }
                    "Block $recipientUserName" -> confirmBlockUser()
                    "Report Inappropriate Activity" -> {
                        com.group3.lostandfound.util.ModernToast.show(
                            requireContext(),
                            "Report submitted for safety review.",
                            binding.root
                        )
                    }
                    "Clear Local View" -> {
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

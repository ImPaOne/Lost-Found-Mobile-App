package com.group3.lostandfound

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.group3.lostandfound.util.ImageLoader
import com.group3.lostandfound.viewmodel.MessagesViewModel
import kotlinx.coroutines.launch

/**
 * MessagesFragment displays the user's conversation threads matching the reference inbox screen.
 * Uses clean human-written variable names and straightforward navigation.
 */
class MessagesFragment : Fragment() {

    private lateinit var btnBack: View
    private lateinit var btnMessagesMenu: View
    private lateinit var tvConversationCount: TextView
    private lateinit var rvConversations: RecyclerView
    private lateinit var layoutEmptyMessages: View

    private lateinit var chipAll: TextView
    private lateinit var chipUnread: TextView
    private lateinit var chipResolved: TextView

    private var activeFilter = "ALL" // "ALL", "UNREAD", "RESOLVED"
    private var currentSearchQuery = ""
    private var allConversations = listOf<ConversationMetadata>()
    private val conversationsList = mutableListOf<ConversationMetadata>()

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var messagesViewModel: MessagesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messagesViewModel = ViewModelProvider(this)[MessagesViewModel::class.java]

        initViews(view)
        setupFilterChips()
        setupRecyclerView()
        setupListeners()
        setupSearch(view)
        NavigationHelper.setup(this, view, NavigationHelper.Tab.MESSAGES)
        observeViewModel()
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnMessagesMenu = view.findViewById(R.id.btnMessagesMenu)
        tvConversationCount = view.findViewById(R.id.tvConversationCount)
        rvConversations = view.findViewById(R.id.rvConversations)
        layoutEmptyMessages = view.findViewById(R.id.layoutEmptyMessages)

        chipAll = view.findViewById(R.id.chipAll)
        chipUnread = view.findViewById(R.id.chipUnread)
        chipResolved = view.findViewById(R.id.chipResolved)
    }

    private fun setupFilterChips() {
        fun updateChipStyles() {
            chipAll.setBackgroundResource(if (activeFilter == "ALL") R.drawable.bg_chip_filter_active else R.drawable.bg_chip_filter_inactive)
            chipAll.setTextColor(if (activeFilter == "ALL") ContextCompat.getColor(requireContext(), R.color.white) else android.graphics.Color.parseColor("#475569"))

            chipUnread.setBackgroundResource(if (activeFilter == "UNREAD") R.drawable.bg_chip_filter_active else R.drawable.bg_chip_filter_inactive)
            chipUnread.setTextColor(if (activeFilter == "UNREAD") ContextCompat.getColor(requireContext(), R.color.white) else android.graphics.Color.parseColor("#475569"))

            chipResolved.setBackgroundResource(if (activeFilter == "RESOLVED") R.drawable.bg_chip_filter_active else R.drawable.bg_chip_filter_inactive)
            chipResolved.setTextColor(if (activeFilter == "RESOLVED") ContextCompat.getColor(requireContext(), R.color.white) else android.graphics.Color.parseColor("#475569"))
        }

        chipAll.setOnClickListener {
            activeFilter = "ALL"
            updateChipStyles()
            filterConversations()
        }

        chipUnread.setOnClickListener {
            activeFilter = "UNREAD"
            updateChipStyles()
            filterConversations()
        }

        chipResolved.setOnClickListener {
            activeFilter = "RESOLVED"
            updateChipStyles()
            filterConversations()
        }
    }

    private fun setupSearch(view: View) {
        val inputSearch = view.findViewById<android.widget.EditText>(R.id.inputSearchConversations)
        inputSearch?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim().orEmpty()
                filterConversations()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun filterConversations() {
        var filtered = allConversations

        // 1. Filter by Chip Tab
        if (activeFilter == "UNREAD") {
            filtered = filtered.filter { it.unreadCount > 0 }
        } else if (activeFilter == "RESOLVED") {
            filtered = filtered.filter { it.isResolved }
        }

        // 2. Filter by search query
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.otherUserName.contains(currentSearchQuery, ignoreCase = true) ||
                it.relatedItemTitle.contains(currentSearchQuery, ignoreCase = true) ||
                it.lastMessageTextContent.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        conversationsList.clear()
        conversationsList.addAll(filtered)
        conversationAdapter.notifyDataSetChanged()

        val count = allConversations.size
        val unreadTotal = allConversations.sumOf { it.unreadCount }
        tvConversationCount.text = "$count conversations · $unreadTotal unread messages"
        chipUnread.text = "Unread · $unreadTotal"

        layoutEmptyMessages.visibility = if (conversationsList.isEmpty()) View.VISIBLE else View.GONE
        rvConversations.visibility = if (conversationsList.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(conversationsList) { conversation ->
            val chatFrag = ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ChatFragment.ARG_CONVERSATION_ID, conversation.chatConversationId)
                    putString(ChatFragment.ARG_RECIPIENT_USER_ID, conversation.otherUserId.ifEmpty { "user_kasun" })
                    putString(ChatFragment.ARG_RECIPIENT_NAME, conversation.otherUserName.ifEmpty { "Kasun Jayasinghe" })
                    putString(ChatFragment.ARG_ITEM_ID, conversation.relatedItemId.ifEmpty { "item_wallet" })
                    putString(ChatFragment.ARG_ITEM_TITLE, conversation.relatedItemTitle.ifEmpty { "Black leather wallet" })
                }
            }
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.screenBox, chatFrag)
                .addToBackStack(null)
                .commit()
        }
        rvConversations.layoutManager = LinearLayoutManager(requireContext())
        rvConversations.adapter = conversationAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        btnMessagesMenu.setOnClickListener {
            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Inbox settings & options", it)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            messagesViewModel.uiState.collect { state ->
                allConversations = if (state.conversations.isEmpty()) {
                    getDefaultDemonstrationConversations()
                } else {
                    state.conversations
                }
                filterConversations()
            }
        }
    }

    private fun getDefaultDemonstrationConversations(): List<ConversationMetadata> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 9)
        calendar.set(java.util.Calendar.MINUTE, 41)
        val time1 = calendar.timeInMillis

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 8)
        calendar.set(java.util.Calendar.MINUTE, 15)
        val time2 = calendar.timeInMillis

        return listOf(
            ConversationMetadata(
                chatConversationId = "conv_kasun_wallet",
                otherUserId = "user_kasun",
                otherUserName = "Kasun Jayasinghe",
                relatedItemTitle = "Black leather wallet",
                lastMessageTextContent = "Could you describe a detail that isn't in the report?",
                lastUpdatedTimestamp = time1,
                unreadCount = 2,
                itemType = "LOST",
                relatedItemId = "item_wallet",
                itemImageUrl = "drawable://img_wallet"
            ),
            ConversationMetadata(
                chatConversationId = "conv_ama_passport",
                otherUserId = "user_ama",
                otherUserName = "Ama Fernando",
                relatedItemTitle = "Sri Lankan passport",
                lastMessageTextContent = "Please use the secure verification request.",
                lastUpdatedTimestamp = time2,
                unreadCount = 1,
                itemType = "FOUND",
                relatedItemId = "item_passport",
                itemImageUrl = "drawable://img_passport"
            ),
            ConversationMetadata(
                chatConversationId = "conv_dinuka_airpods",
                otherUserId = "user_dinuka",
                otherUserName = "Dinuka Perera",
                relatedItemTitle = "AirPods Pro case",
                lastMessageTextContent = "You: So glad it's back with you. Take care!",
                lastUpdatedTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24, // Yesterday
                unreadCount = 0,
                isResolved = true,
                isOutgoingRead = true,
                relatedItemId = "item_airpods",
                itemImageUrl = "drawable://img_airpods_bench"
            ),
            ConversationMetadata(
                chatConversationId = "conv_sewwandi_laptop",
                otherUserId = "user_sewwandi",
                otherUserName = "Sewwandi Fernando",
                relatedItemTitle = "Red laptop sleeve",
                lastMessageTextContent = "You: I'll check with the reception desk.",
                lastUpdatedTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 3, // 11 Sep
                unreadCount = 0,
                isOutgoingRead = true,
                relatedItemId = "item_laptop",
                itemImageUrl = "drawable://img_laptop"
            )
        )
    }

    companion object {
        const val DUMMY_AVATAR_URL = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRi5opVz37NwOdXal9H5Xa-UMlDvne9QXhgt_udOU42Mw&s=10"
        fun newInstance(): MessagesFragment = MessagesFragment()
    }

    private class ConversationAdapter(
        private val items: List<ConversationMetadata>,
        private val onClick: (ConversationMetadata) -> Unit
    ) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imgThumb: ImageView = itemView.findViewById(R.id.imgConversationThumb)
            val tvName: TextView = itemView.findViewById(R.id.tvConversationUserName)
            val tvTime: TextView = itemView.findViewById(R.id.tvConversationTime)
            val tvItem: TextView = itemView.findViewById(R.id.tvRelatedItem)
            val badgeResolved: TextView = itemView.findViewById(R.id.badgeResolved)
            val tvLastMsg: TextView = itemView.findViewById(R.id.tvLastMessage)
            val tvUnreadBadge: TextView = itemView.findViewById(R.id.tvUnreadBadge)
            val imgReadReceipt: ImageView = itemView.findViewById(R.id.imgReadReceipt)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val context = holder.itemView.context

            // Name
            holder.tvName.text = item.otherUserName.ifEmpty { "Community Member" }

            // Thumbnail Loading (Item Photo or User Fallback URL)
            when (item.itemImageUrl) {
                "drawable://img_wallet" -> holder.imgThumb.setImageResource(R.drawable.img_wallet)
                "drawable://img_passport" -> holder.imgThumb.setImageResource(R.drawable.img_passport)
                "drawable://img_airpods_bench" -> holder.imgThumb.setImageResource(R.drawable.img_airpods_bench)
                "drawable://img_laptop" -> holder.imgThumb.setImageResource(R.drawable.img_laptop)
                else -> {
                    if (item.itemImageUrl.startsWith("http://") || item.itemImageUrl.startsWith("https://")) {
                        ImageLoader.load(context, item.itemImageUrl, holder.imgThumb)
                    } else {
                        // Fallback to user dummy avatar image
                        ImageLoader.load(context, DUMMY_AVATAR_URL, holder.imgThumb)
                    }
                }
            }

            // Timestamp formatting matching screenshot (09:41, Yesterday, 11 Sep)
            if (item.lastUpdatedTimestamp > 0) {
                if (DateUtils.isToday(item.lastUpdatedTimestamp)) {
                    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    holder.tvTime.text = timeFormat.format(java.util.Date(item.lastUpdatedTimestamp))
                } else if (DateUtils.isToday(item.lastUpdatedTimestamp + DateUtils.DAY_IN_MILLIS)) {
                    holder.tvTime.text = "Yesterday"
                } else {
                    val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.ENGLISH)
                    holder.tvTime.text = dateFormat.format(java.util.Date(item.lastUpdatedTimestamp))
                }
            } else {
                holder.tvTime.text = "Recent"
            }

            // Item Name and colors (Red for Lost, Teal for Found, Slate for Resolved)
            if (item.relatedItemTitle.isNotEmpty()) {
                holder.tvItem.visibility = View.VISIBLE
                holder.tvItem.text = item.relatedItemTitle

                if (item.isResolved) {
                    holder.tvItem.setTextColor(android.graphics.Color.parseColor("#64748B"))
                    holder.badgeResolved.visibility = View.VISIBLE
                } else if (item.itemType.equals("FOUND", ignoreCase = true)) {
                    holder.tvItem.setTextColor(android.graphics.Color.parseColor("#0D9488"))
                    holder.badgeResolved.visibility = View.GONE
                } else {
                    holder.tvItem.setTextColor(android.graphics.Color.parseColor("#C1121F"))
                    holder.badgeResolved.visibility = View.GONE
                }
            } else {
                holder.tvItem.visibility = View.GONE
                holder.badgeResolved.visibility = View.GONE
            }

            // Last message text
            holder.tvLastMsg.text = item.lastMessageTextContent.ifEmpty { "Chat discussion" }

            // Unread Count Badge or Read Double Checkmark
            if (item.unreadCount > 0) {
                holder.tvUnreadBadge.visibility = View.VISIBLE
                holder.tvUnreadBadge.text = item.unreadCount.toString()
                holder.imgReadReceipt.visibility = View.GONE
            } else if (item.isOutgoingRead) {
                holder.tvUnreadBadge.visibility = View.GONE
                holder.imgReadReceipt.visibility = View.VISIBLE
                if (item.isResolved) {
                    holder.imgReadReceipt.setColorFilter(android.graphics.Color.parseColor("#0D9488"))
                } else {
                    holder.imgReadReceipt.setColorFilter(android.graphics.Color.parseColor("#94A3B8"))
                }
            } else {
                holder.tvUnreadBadge.visibility = View.GONE
                holder.imgReadReceipt.visibility = View.GONE
            }

            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}

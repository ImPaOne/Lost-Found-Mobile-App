package com.group3.lostandfound

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.group3.lostandfound.viewmodel.MessagesViewModel
import kotlinx.coroutines.launch

/**
 * MessagesFragment displays the user's active messaging rooms / conversations in real-time
 * following MVVM architecture.
 * Delegating state management and conversation streaming to MessagesViewModel.
 */
class MessagesFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var tvConversationCount: TextView
    private lateinit var rvConversations: RecyclerView
    private lateinit var layoutEmptyMessages: View

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
        setupRecyclerView()
        setupListeners()
        NavigationHelper.setup(this, view, NavigationHelper.Tab.MESSAGES)
        observeViewModel()
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        tvConversationCount = view.findViewById(R.id.tvConversationCount)
        rvConversations = view.findViewById(R.id.rvConversations)
        layoutEmptyMessages = view.findViewById(R.id.layoutEmptyMessages)
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(conversationsList) { conversation ->
            val chatFrag = ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ChatFragment.ARG_CONVERSATION_ID, conversation.chatConversationId)
                    putString(ChatFragment.ARG_RECIPIENT_USER_ID, conversation.otherUserId)
                    putString(ChatFragment.ARG_RECIPIENT_NAME, conversation.otherUserName.ifEmpty { "User #${conversation.otherUserId.takeLast(4)}" })
                    putString(ChatFragment.ARG_ITEM_ID, conversation.relatedItemId)
                    putString(ChatFragment.ARG_ITEM_TITLE, "Discussion")
                }
            }
            parentFragmentManager.beginTransaction()
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
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            messagesViewModel.uiState.collect { state ->
                conversationsList.clear()
                conversationsList.addAll(state.conversations)
                conversationAdapter.notifyDataSetChanged()

                val count = conversationsList.size
                tvConversationCount.text = "$count Active"
                layoutEmptyMessages.visibility = if (count == 0) View.VISIBLE else View.GONE
                rvConversations.visibility = if (count > 0) View.VISIBLE else View.GONE
            }
        }
    }

    companion object {
        fun newInstance(): MessagesFragment = MessagesFragment()
    }

    private class ConversationAdapter(
        private val items: List<ConversationMetadata>,
        private val onClick: (ConversationMetadata) -> Unit
    ) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvConversationUserName)
            val tvTime: TextView = itemView.findViewById(R.id.tvConversationTime)
            val tvItem: TextView = itemView.findViewById(R.id.tvRelatedItem)
            val tvLastMsg: TextView = itemView.findViewById(R.id.tvLastMessage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val displayName = if (item.otherUserName.isNotEmpty()) {
                item.otherUserName
            } else if (item.otherUserId.isNotEmpty()) {
                "User #${item.otherUserId.takeLast(4)}"
            } else {
                "Community Member"
            }
            holder.tvName.text = displayName

            if (item.lastUpdatedTimestamp > 0) {
                val timeSpan = DateUtils.getRelativeTimeSpanString(
                    item.lastUpdatedTimestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
                holder.tvTime.text = timeSpan
            } else {
                holder.tvTime.text = "Recent"
            }

            if (item.relatedItemId.isNotEmpty()) {
                holder.tvItem.visibility = View.VISIBLE
                holder.tvItem.text = "Item Discussion"
            } else {
                holder.tvItem.visibility = View.GONE
            }

            holder.tvLastMsg.text = item.lastMessageTextContent.ifEmpty { "Chat started" }

            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}

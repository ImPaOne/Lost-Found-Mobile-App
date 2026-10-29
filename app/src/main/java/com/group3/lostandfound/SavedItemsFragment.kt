package com.group3.lostandfound

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.util.ImageLoader

class SavedItemsFragment : Fragment() {

    private lateinit var btnBack: View
    private lateinit var btnMore: View
    private lateinit var searchField: EditText

    private lateinit var chipAll: TextView
    private lateinit var chipLost: TextView
    private lateinit var chipFound: TextView

    private lateinit var tvSavedCount: TextView
    private lateinit var btnSort: TextView

    private lateinit var rvSavedItems: RecyclerView
    private lateinit var layoutEmptySaved: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView

    private lateinit var adapter: SavedItemsAdapter

    private var selectedCategory = "ALL" // "ALL", "LOST", "FOUND"
    private var searchQuery = ""
    private var isSortAlphabetical = false

    private val allSavedItems = mutableListOf<ItemReport>()
    private val displayedItems = mutableListOf<ItemReport>()
    private val savedIdsSet = mutableSetOf<String>()

    private var savedItemsListener: ValueEventListener? = null
    private var allItemsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_saved_items, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        loadSavedItemsLive()
        NavigationHelper.setup(this, view, NavigationHelper.Tab.PROFILE)
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnMore = view.findViewById(R.id.btnMore)
        searchField = view.findViewById(R.id.searchField)

        chipAll = view.findViewById(R.id.chipAll)
        chipLost = view.findViewById(R.id.chipLost)
        chipFound = view.findViewById(R.id.chipFound)

        tvSavedCount = view.findViewById(R.id.tvSavedCount)
        btnSort = view.findViewById(R.id.btnSort)

        rvSavedItems = view.findViewById(R.id.rvSavedItems)
        layoutEmptySaved = view.findViewById(R.id.layoutEmptySaved)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle)
    }

    private fun setupRecyclerView() {
        adapter = SavedItemsAdapter(
            items = displayedItems,
            onItemClick = { report ->
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.screenBox,
                        DetailsFragment.newInstance(
                            itemId = report.itemId,
                            title = report.title,
                            desc = report.description,
                            status = report.status,
                            location = report.location,
                            imageUrl = report.imageUrl,
                            reporterUserId = report.userId,
                            authorName = report.authorName,
                            latitude = report.latitude,
                            longitude = report.longitude
                        )
                    )
                    .addToBackStack("saved_details")
                    .commit()
            },
            onRemoveBookmark = { report ->
                removeSavedItem(report)
            }
        )
        rvSavedItems.layoutManager = LinearLayoutManager(requireContext())
        rvSavedItems.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnMore.setOnClickListener {
            val options = arrayOf("Clear All Saved Items", "Refresh")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Saved Items Options")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> confirmClearAllSaved()
                        1 -> loadSavedItemsLive()
                    }
                }
                .show()
        }

        btnSort.setOnClickListener {
            isSortAlphabetical = !isSortAlphabetical
            btnSort.text = if (isSortAlphabetical) "Title (A-Z)" else "Recently saved"
            applyFilterAndSort()
        }

        chipAll.setOnClickListener {
            selectedCategory = "ALL"
            updateChipStyles()
            applyFilterAndSort()
        }

        chipLost.setOnClickListener {
            selectedCategory = "LOST"
            updateChipStyles()
            applyFilterAndSort()
        }

        chipFound.setOnClickListener {
            selectedCategory = "FOUND"
            updateChipStyles()
            applyFilterAndSort()
        }

        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                applyFilterAndSort()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateChipStyles() {
        if (!isAdded) return
        val darkBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_dark)
        val outlineBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_outline)

        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val lostColor = 0xFFDC2626.toInt()
        val foundColor = 0xFF059669.toInt()
        val darkNavy = 0xFF0B2238.toInt()

        if (selectedCategory == "ALL") {
            chipAll.background = darkBg
            chipAll.setTextColor(whiteColor)
        } else {
            chipAll.background = outlineBg
            chipAll.setTextColor(darkNavy)
        }

        if (selectedCategory == "LOST") {
            chipLost.background = darkBg
            chipLost.setTextColor(whiteColor)
        } else {
            chipLost.background = outlineBg
            chipLost.setTextColor(lostColor)
        }

        if (selectedCategory == "FOUND") {
            chipFound.background = darkBg
            chipFound.setTextColor(whiteColor)
        } else {
            chipFound.background = outlineBg
            chipFound.setTextColor(foundColor)
        }
    }

    private fun loadSavedItemsLive() {
        val currentUid = FirebaseManager.currentUid
        val prefs = requireContext().getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
        val localBookmarked = prefs.getStringSet("bookmarked_ids", emptySet()) ?: emptySet()
        savedIdsSet.clear()
        savedIdsSet.addAll(localBookmarked)

        if (currentUid.isNotEmpty()) {
            val userSavedRef = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                .getReference("users")
                .child(currentUid)
                .child("saved_items")

            savedItemsListener?.let { userSavedRef.removeEventListener(it) }
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    savedIdsSet.clear()
                    savedIdsSet.addAll(localBookmarked)
                    for (child in snapshot.children) {
                        val itemId = child.key ?: ""
                        val isSaved = child.getValue(Boolean::class.java) ?: true
                        if (itemId.isNotEmpty() && isSaved) {
                            savedIdsSet.add(itemId)
                        }
                    }
                    fetchItemsDetails()
                }

                override fun onCancelled(error: DatabaseError) {
                    fetchItemsDetails()
                }
            }
            savedItemsListener = listener
            userSavedRef.addValueEventListener(listener)
        } else {
            fetchItemsDetails()
        }
    }

    private fun fetchItemsDetails() {
        val itemsRef = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("items")

        allItemsListener?.let { itemsRef.removeEventListener(it) }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                allSavedItems.clear()
                for (child in snapshot.children) {
                    val key = child.key ?: ""
                    val reportId = child.child("itemId").getValue(String::class.java) ?: key
                    if (savedIdsSet.contains(reportId) || savedIdsSet.contains(key)) {
                        val report = child.getValue(ItemReport::class.java)
                        if (report != null) {
                            allSavedItems.add(report.copy(itemId = if (report.itemId.isNotEmpty()) report.itemId else key))
                        }
                    }
                }
                applyFilterAndSort()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Failed to sync saved items: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        allItemsListener = listener
        itemsRef.addValueEventListener(listener)
    }

    private fun applyFilterAndSort() {
        if (!isAdded) return

        val totalAll = allSavedItems.size
        val totalLost = allSavedItems.count { it.status.equals("LOST", ignoreCase = true) }
        val totalFound = allSavedItems.count { it.status.equals("FOUND", ignoreCase = true) }

        chipAll.text = "All · $totalAll"
        chipLost.text = "Lost · $totalLost"
        chipFound.text = "Found · $totalFound"

        displayedItems.clear()
        val filtered = allSavedItems.filter { item ->
            val matchesCategory = when (selectedCategory) {
                "LOST" -> item.status.equals("LOST", ignoreCase = true)
                "FOUND" -> item.status.equals("FOUND", ignoreCase = true)
                else -> true
            }

            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                item.title.lowercase().contains(searchQuery) ||
                        item.location.lowercase().contains(searchQuery) ||
                        item.description.lowercase().contains(searchQuery)
            }

            matchesCategory && matchesSearch
        }

        val sorted = if (isSortAlphabetical) {
            filtered.sortedBy { it.title.lowercase() }
        } else {
            filtered.reversed()
        }

        displayedItems.addAll(sorted)
        adapter.notifyDataSetChanged()

        val count = displayedItems.size
        tvSavedCount.text = "$count saved item${if (count == 1) "" else "s"}"

        if (displayedItems.isEmpty()) {
            rvSavedItems.visibility = View.GONE
            layoutEmptySaved.visibility = View.VISIBLE
            if (allSavedItems.isEmpty()) {
                tvEmptyTitle.text = "No saved items yet"
                tvEmptySubtitle.text = "Items you bookmark while browsing reports will be saved here for instant access."
            } else {
                tvEmptyTitle.text = "No matching saved items"
                tvEmptySubtitle.text = "Try switching category filters or clearing your search query."
            }
        } else {
            rvSavedItems.visibility = View.VISIBLE
            layoutEmptySaved.visibility = View.GONE
        }
    }

    private fun removeSavedItem(report: ItemReport) {
        val currentUid = FirebaseManager.currentUid
        val prefs = requireContext().getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)

        // Update local SharedPreferences
        val localSet = prefs.getStringSet("bookmarked_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        localSet.remove(report.itemId)
        prefs.edit().putStringSet("bookmarked_ids", localSet).apply()

        savedIdsSet.remove(report.itemId)

        // Remove from Firebase Realtime Database
        if (currentUid.isNotEmpty()) {
            FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                .getReference("users")
                .child(currentUid)
                .child("saved_items")
                .child(report.itemId)
                .removeValue()
        }

        allSavedItems.removeAll { it.itemId == report.itemId }
        applyFilterAndSort()

        Toast.makeText(requireContext(), "Removed '${report.title}' from saved items", Toast.LENGTH_SHORT).show()
    }

    private fun confirmClearAllSaved() {
        if (allSavedItems.isEmpty()) {
            Toast.makeText(requireContext(), "No saved items to clear", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Clear All Saved Items")
            .setMessage("Are you sure you want to remove all saved items from your bookmarks?")
            .setPositiveButton("Clear All") { _, _ ->
                val currentUid = FirebaseManager.currentUid
                val prefs = requireContext().getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
                prefs.edit().remove("bookmarked_ids").apply()

                if (currentUid.isNotEmpty()) {
                    FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                        .getReference("users")
                        .child(currentUid)
                        .child("saved_items")
                        .removeValue()
                }

                savedIdsSet.clear()
                allSavedItems.clear()
                applyFilterAndSort()
                Toast.makeText(requireContext(), "All saved items cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val currentUid = FirebaseManager.currentUid
        if (currentUid.isNotEmpty() && savedItemsListener != null) {
            FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                .getReference("users")
                .child(currentUid)
                .child("saved_items")
                .removeEventListener(savedItemsListener!!)
        }
        if (allItemsListener != null) {
            FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                .getReference("items")
                .removeEventListener(allItemsListener!!)
        }
    }

    companion object {
        fun newInstance(): SavedItemsFragment = SavedItemsFragment()
    }

    private class SavedItemsAdapter(
        private val items: List<ItemReport>,
        private val onItemClick: (ItemReport) -> Unit,
        private val onRemoveBookmark: (ItemReport) -> Unit
    ) : RecyclerView.Adapter<SavedItemsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgItemThumb: ImageView = view.findViewById(R.id.imgItemThumb)
            val tagStatus: TextView = view.findViewById(R.id.tagStatus)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvLocation: TextView = view.findViewById(R.id.tvLocation)
            val btnRemoveBookmark: ImageView = view.findViewById(R.id.btnRemoveBookmark)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_saved_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvLocation.text = if (item.location.isNotEmpty()) item.location else "Sri Lanka"

            val statusUpper = item.status.uppercase()
            holder.tagStatus.text = statusUpper

            val context = holder.itemView.context
            when (statusUpper) {
                "LOST" -> {
                    holder.tagStatus.setBackgroundResource(R.drawable.bg_tag_lost_outlined)
                    holder.tagStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_lost_color))
                }
                "FOUND" -> {
                    holder.tagStatus.setBackgroundResource(R.drawable.bg_tag_found)
                    holder.tagStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_found_color))
                }
                else -> {
                    holder.tagStatus.setBackgroundResource(R.drawable.bg_pill_dark)
                    holder.tagStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
                }
            }

            if (item.imageUrl.isNotEmpty()) {
                ImageLoader.load(holder.imgItemThumb, item.imageUrl, R.drawable.ic_item_placeholder)
            } else {
                holder.imgItemThumb.setImageResource(R.drawable.ic_item_placeholder)
            }

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }

            holder.btnRemoveBookmark.setOnClickListener {
                onRemoveBookmark(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}

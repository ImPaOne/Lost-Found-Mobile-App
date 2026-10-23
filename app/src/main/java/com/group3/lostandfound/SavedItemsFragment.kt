package com.group3.lostandfound

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.util.ImageLoader
import com.group3.lostandfound.util.ModernToast

/**
 * SavedItemsFragment provides a dedicated, modern screen displaying bookmarked items.
 */
class SavedItemsFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnClearAll: ImageView
    private lateinit var tvSavedCount: TextView
    private lateinit var rvSavedItems: RecyclerView
    private lateinit var layoutEmpty: LinearLayout

    private val savedItemList = mutableListOf<ItemReport>()
    private lateinit var adapter: SavedItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_saved_items, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
        btnClearAll = view.findViewById(R.id.btnClearAll)
        tvSavedCount = view.findViewById(R.id.tvSavedCount)
        rvSavedItems = view.findViewById(R.id.rvSavedItems)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnClearAll.setOnClickListener {
            if (savedItemList.isEmpty()) return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setTitle("Clear All Saved Items?")
                .setMessage("Remove all saved bookmarks from your personal list?")
                .setPositiveButton("Clear All") { _, _ ->
                    val prefs = requireContext().getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
                    prefs.edit().remove("bookmarked_ids").apply()
                    savedItemList.clear()
                    adapter.notifyDataSetChanged()
                    updateUiState()
                    ModernToast.show(requireContext(), "Saved items cleared", view)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        adapter = SavedItemsAdapter(savedItemList,
            onItemClicked = { item ->
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(
                        R.id.screenBox,
                        DetailsFragment.newInstance(
                            title = item.title,
                            desc = item.description,
                            status = item.status,
                            location = item.location,
                            imageResId = R.drawable.ic_item_placeholder,
                            itemId = item.itemId,
                            reporterUserId = item.userId,
                            authorName = item.authorName
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            },
            onRemoveClicked = { item, position ->
                val prefs = requireContext().getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
                val bookmarkedSet = prefs.getStringSet("bookmarked_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                bookmarkedSet.remove(item.itemId)
                prefs.edit().putStringSet("bookmarked_ids", bookmarkedSet).apply()

                val uid = FirebaseManager.currentUid
                if (uid.isNotEmpty()) {
                    FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                        .getReference("users")
                        .child(uid)
                        .child("saved_items")
                        .child(item.itemId)
                        .removeValue()
                }

                if (position in savedItemList.indices) {
                    savedItemList.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    updateUiState()
                }
                ModernToast.show(requireContext(), "'${item.title}' removed from saved", view)
            }
        )

        rvSavedItems.layoutManager = LinearLayoutManager(requireContext())
        rvSavedItems.adapter = adapter

        NavigationHelper.setup(this, view, NavigationHelper.Tab.PROFILE)
        loadSavedItems()
    }

    private fun loadSavedItems() {
        val prefs = requireContext().getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
        val localBookmarkedSet = prefs.getStringSet("bookmarked_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        val uid = FirebaseManager.currentUid

        if (uid.isNotEmpty()) {
            FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                .getReference("users")
                .child(uid)
                .child("saved_items")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            child.key?.let { localBookmarkedSet.add(it) }
                        }
                        prefs.edit().putStringSet("bookmarked_ids", localBookmarkedSet).apply()
                        fetchItemsForSavedKeys(localBookmarkedSet)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        fetchItemsForSavedKeys(localBookmarkedSet)
                    }
                })
        } else {
            fetchItemsForSavedKeys(localBookmarkedSet)
        }
    }

    private fun fetchItemsForSavedKeys(bookmarkedSet: Set<String>) {
        if (bookmarkedSet.isEmpty()) {
            savedItemList.clear()
            adapter.notifyDataSetChanged()
            updateUiState()
            return
        }

        FirebaseManager.itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                savedItemList.clear()
                for (child in snapshot.children) {
                    val report = ItemReport.fromSnapshot(child)
                    if (bookmarkedSet.contains(report.itemId)) {
                        savedItemList.add(report)
                    }
                }
                adapter.notifyDataSetChanged()
                updateUiState()
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) updateUiState()
            }
        })
    }

    private fun updateUiState() {
        tvSavedCount.text = "${savedItemList.size} item${if (savedItemList.size == 1) "" else "s"} bookmarked"
        layoutEmpty.visibility = if (savedItemList.isEmpty()) View.VISIBLE else View.GONE
        rvSavedItems.visibility = if (savedItemList.isEmpty()) View.GONE else View.VISIBLE
    }

    inner class SavedItemsAdapter(
        private val items: List<ItemReport>,
        private val onItemClicked: (ItemReport) -> Unit,
        private val onRemoveClicked: (ItemReport, Int) -> Unit
    ) : RecyclerView.Adapter<SavedItemsAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val imgItemThumb: com.google.android.material.imageview.ShapeableImageView = v.findViewById(R.id.imgItemThumb)
            val tagStatus: TextView = v.findViewById(R.id.tagStatus)
            val tvTitle: TextView = v.findViewById(R.id.tvTitle)
            val tvLocation: TextView = v.findViewById(R.id.tvLocation)
            val btnRemoveBookmark: ImageView = v.findViewById(R.id.btnRemoveBookmark)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvLocation.text = "${item.location.ifEmpty { "Colombo" }} · Live Post"
            holder.tagStatus.text = item.status.uppercase()

            val isLost = item.status.equals("LOST", ignoreCase = true)
            holder.tagStatus.setTextColor(
                ContextCompat.getColor(holder.itemView.context, if (isLost) R.color.tag_lost_color else R.color.tag_found_color)
            )

            ImageLoader.load(holder.imgItemThumb, item.imageUrl, R.drawable.ic_item_placeholder, viewLifecycleOwner.lifecycleScope)

            holder.itemView.setOnClickListener { onItemClicked(item) }
            holder.btnRemoveBookmark.setOnClickListener { onRemoveClicked(item, holder.adapterPosition) }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        fun newInstance(): SavedItemsFragment = SavedItemsFragment()
    }
}

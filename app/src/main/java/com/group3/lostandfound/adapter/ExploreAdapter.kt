package com.group3.lostandfound.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.group3.lostandfound.R
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.util.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * Adapter for Explore Search Screen, rendering live items with asynchronous image loading,
 * status badge overlay on image, and click handling.
 */
class ExploreAdapter(
    private var items: List<ItemReport> = emptyList(),
    private val scope: CoroutineScope,
    private val onItemClick: (ItemReport) -> Unit = {}
) : RecyclerView.Adapter<ExploreAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgItem: ImageView = view.findViewById(R.id.imgItem)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        var imageJob: Job? = null
    }

    // Creates a new item card using the XML layout.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_card, parent, false)
        return ViewHolder(view)
    }

    // Connects the item's data to the views inside the card.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.tvTitle.text = if (item.title.isNotBlank()) item.title else "Untitled item"

        // Build "STATUS · Xh" badge text
        val time = timeAgo(item.timestamp)
        holder.tvStatus.text = if (time.isNotEmpty()) "${item.status.uppercase()} · $time"
                                else item.status.uppercase()

        // Use filled pill background: red for LOST, green for FOUND
        if (item.status.equals("LOST", ignoreCase = true)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_lost_pill)
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_lost_color))
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_found_pill)
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_found_color))
        }

        // Show location name; fall back to Colombo if empty
        holder.tvLocation.text = item.location.ifEmpty { "Colombo" }

        // Cancel previous image load job if cell was recycled
        holder.imageJob?.cancel()
        holder.imageJob = ImageLoader.load(
            imageView = holder.imgItem,
            url = item.imageUrl,
            placeholderRes = R.drawable.ic_item_placeholder,
            scope = scope
        )

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ItemReport>) {
        items = newItems
        notifyDataSetChanged()
    }

    /** Returns a human-readable time string like "2h", "30m", "3d", or "" if timestamp is invalid. */
    private fun timeAgo(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60_000L
        val hours = diff / 3_600_000L
        val days = diff / 86_400_000L
        return when {
            days >= 1   -> "${days}d"
            hours >= 1  -> "${hours}h"
            minutes >= 1 -> "${minutes}m"
            else        -> "now"
        }
    }
}


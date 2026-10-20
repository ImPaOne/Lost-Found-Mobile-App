package com.group3.lostandfound.adapter

import android.graphics.Color
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
 * status badge styling, and click handling.
 */
class ExploreAdapter(
    private var items: List<ItemReport>,
    private val scope: CoroutineScope,
    private val onItemClick: (ItemReport) -> Unit
) : RecyclerView.Adapter<ExploreAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgItem: ImageView = view.findViewById(R.id.imgItem)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        var imageJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.tvTitle.text = if (item.title.isNotBlank()) item.title else "Untitled item"
        holder.tvStatus.text = item.status.uppercase()

        if (item.status.equals("LOST", ignoreCase = true)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_lost_color))
        } else if (item.status.equals("FOUND", ignoreCase = true)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_found_color))
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#4A7C59"))
        }

        val locationText = if (item.category.isNotBlank()) {
            "${item.category} · ${item.latitude.toString().take(6)}, ${item.longitude.toString().take(6)}"
        } else {
            "Colombo · ${item.latitude.toString().take(6)}, ${item.longitude.toString().take(6)}"
        }
        holder.tvLocation.text = locationText

        // Cancel previous image load job if recycled
        holder.imageJob?.cancel()
        holder.imageJob = ImageLoader.load(
            imageView = holder.imgItem,
            url = item.imageUrl,
            placeholderRes = if (item.status.equals("LOST", ignoreCase = true)) R.drawable.img_wallet else R.drawable.img_samsung_phone,
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
}

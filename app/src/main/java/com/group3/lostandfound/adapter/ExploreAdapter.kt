package com.group3.lostandfound.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.group3.lostandfound.R
import com.group3.lostandfound.model.ItemReport

class ExploreAdapter(
    private var items: List<ItemReport>,
    private val onItemClick: (ItemReport) -> Unit
) : RecyclerView.Adapter<ExploreAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgItem: ImageView = view.findViewById(R.id.imgItem)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvStatus.text = item.status
        holder.tvLocation.text = "${item.latitude}, ${item.longitude}" // Normally reverse geocoded
        // Normally load image with Glide or Coil here
        
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<ItemReport>) {
        items = newItems
        notifyDataSetChanged()
    }
}

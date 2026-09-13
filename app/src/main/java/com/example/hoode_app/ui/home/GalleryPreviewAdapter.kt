package com.example.hoode_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hoode_app.R
import com.example.hoode_app.data.model.GalleryItem

class GalleryPreviewAdapter(
    private val items: List<GalleryItem>,
    private val onItemClick: (GalleryItem) -> Unit
) : RecyclerView.Adapter<GalleryPreviewAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_gallery_item_title)
        val tvOrder: TextView = itemView.findViewById(R.id.tv_gallery_item_order)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_thumbnail, parent, false)
        // Ensure preview cards have explicit width for horizontal list
        view.layoutParams = ViewGroup.LayoutParams(
            (160 * parent.context.resources.displayMetrics.density).toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.caption
        holder.tvOrder.text = "${position + 1} of ${items.size}"

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

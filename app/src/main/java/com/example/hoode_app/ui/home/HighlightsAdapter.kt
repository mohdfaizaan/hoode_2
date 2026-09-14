package com.example.hoode_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hoode_app.R

class HighlightsAdapter(
    private val items: List<HighlightItem>,
    private val onItemClick: ((HighlightItem) -> Unit)? = null
) : RecyclerView.Adapter<HighlightsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.iv_highlight_image)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_highlight_title)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tv_highlight_subtitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_highlight_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.subtitle
        holder.tvCategory.text = item.category

        if (!item.imageUrl.isNullOrBlank()) {
            holder.ivImage.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_gallery_luxury_gradient)
                error(R.drawable.bg_gallery_luxury_gradient)
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

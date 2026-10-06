package com.example.hoode_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hoode_app.R
import com.example.hoode_app.data.model.GalleryItem

class GalleryPreviewAdapter(
    private val items: List<GalleryItem>,
    private val onItemClick: (GalleryItem) -> Unit
) : RecyclerView.Adapter<GalleryPreviewAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.iv_gallery_image)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_gallery_item_title)
        val tvPhotographer: TextView? = itemView.findViewById(R.id.tv_gallery_item_photographer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_gallery, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvPhotographer?.text = item.photographer
        holder.tvPhotographer?.visibility = if (item.photographer.isBlank()) View.GONE else View.VISIBLE

        holder.ivImage.load(item.imageUrl.takeIf { it.isNotBlank() }) {
            crossfade(true)
            placeholder(R.drawable.bg_dashboard_icon)
            fallback(R.drawable.ic_gallery)
            error(R.drawable.ic_gallery)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

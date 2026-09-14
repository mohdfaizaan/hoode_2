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
        val tvOrder: TextView = itemView.findViewById(R.id.tv_gallery_item_order)
        val tvPhotographer: TextView? = itemView.findViewById(R.id.tv_gallery_item_photographer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_thumbnail, parent, false)
        val density = parent.context.resources.displayMetrics.density
        view.layoutParams = ViewGroup.MarginLayoutParams(
            (160 * density).toInt(),
            (148 * density).toInt()
        ).apply {
            marginEnd = (12 * density).toInt()
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvOrder.text = "${position + 1} of ${items.size}"
        holder.tvPhotographer?.text = "📸 ${item.photographer}"

        if (item.imageUrl.isNotBlank()) {
            holder.ivImage.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_gallery_luxury_gradient)
                error(R.drawable.bg_gallery_luxury_gradient)
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

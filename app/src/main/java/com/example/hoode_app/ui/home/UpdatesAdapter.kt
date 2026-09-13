package com.example.hoode_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hoode_app.R

data class HomeUpdateItem(
    val category: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val destinationId: Int
)

class UpdatesAdapter(
    private val items: List<HomeUpdateItem>,
    private val onItemClick: (HomeUpdateItem) -> Unit
) : RecyclerView.Adapter<UpdatesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tv_update_category)
        val tvTime: TextView = itemView.findViewById(R.id.tv_update_time)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_update_title)
        val tvDesc: TextView = itemView.findViewById(R.id.tv_update_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_update, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvCategory.text = item.category
        holder.tvTime.text = item.timestamp
        holder.tvTitle.text = item.title
        holder.tvDesc.text = item.description

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

package com.example.hoode_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hoode_app.R

class CarouselAdapter(
    private val slides: List<CarouselSlide>,
    private val onSlideClick: ((CarouselSlide) -> Unit)? = null
) : RecyclerView.Adapter<CarouselAdapter.SlideViewHolder>() {

    inner class SlideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flSlideBg: View = itemView.findViewById(R.id.fl_slide_bg)
        val ivCarouselBg: ImageView = itemView.findViewById(R.id.iv_carousel_bg)
        val tvHeadline: TextView = itemView.findViewById(R.id.tv_headline)
        val tvSubheadline: TextView = itemView.findViewById(R.id.tv_subheadline)
        val tvAdvertiser: TextView = itemView.findViewById(R.id.tv_advertiser)
        val btnCta: View = itemView.findViewById(R.id.btn_cta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel_slide, parent, false)
        return SlideViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        val slide = slides[position]
        holder.tvHeadline.text = slide.headline
        holder.tvSubheadline.text = slide.subheadline
        holder.tvAdvertiser.text = holder.itemView.context.getString(R.string.dashboard_sponsored_by, slide.advertiser)

        if (!slide.imageUrl.isNullOrBlank()) {
            holder.ivCarouselBg.visibility = View.VISIBLE
            holder.ivCarouselBg.load(slide.imageUrl) {
                crossfade(true)
            }
        } else {
            holder.ivCarouselBg.visibility = View.GONE
        }

        holder.btnCta.visibility = View.VISIBLE
        (holder.btnCta as? TextView)?.text = slide.ctaLabel?.takeIf { it.isNotBlank() }
            ?: holder.itemView.context.getString(R.string.dashboard_view_details)

        holder.flSlideBg.setBackgroundResource(R.drawable.bg_dashboard_hero)
        holder.itemView.contentDescription="Sponsored by ${slide.advertiser}: ${slide.headline}"
        holder.itemView.isFocusable=true
        // Slide click listener
        holder.itemView.setOnClickListener {
            onSlideClick?.invoke(slide)
        }
        holder.btnCta.setOnClickListener {
            onSlideClick?.invoke(slide)
        }
    }

    override fun getItemCount(): Int = slides.size
}

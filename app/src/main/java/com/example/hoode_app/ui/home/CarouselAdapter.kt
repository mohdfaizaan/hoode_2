package com.example.hoode_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hoode_app.R

class CarouselAdapter(
    private val slides: List<CarouselSlide>,
    private val onSlideClick: ((CarouselSlide) -> Unit)? = null
) : RecyclerView.Adapter<CarouselAdapter.SlideViewHolder>() {

    inner class SlideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
        holder.tvAdvertiser.text = slide.advertiser

        if (slide.ctaLabel != null) {
            holder.btnCta.visibility = View.VISIBLE
            (holder.btnCta as? TextView)?.text = slide.ctaLabel
        } else {
            holder.btnCta.visibility = View.GONE
        }

        // Vary gradient colors per slide for visual variety
        val gradients = intArrayOf(
            R.drawable.bg_carousel_gradient,
            R.drawable.bg_carousel_slide_2,
            R.drawable.bg_carousel_slide_3,
            R.drawable.bg_carousel_slide_4,
            R.drawable.bg_carousel_slide_5
        )
        val gradientRes = gradients.getOrElse(position) { R.drawable.bg_carousel_gradient }
        holder.itemView.setBackgroundResource(gradientRes)

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

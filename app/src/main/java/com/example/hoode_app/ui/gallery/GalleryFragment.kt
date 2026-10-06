package com.example.hoode_app.ui.gallery

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hoode_app.R
import com.example.hoode_app.data.model.GalleryItem
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentGalleryBinding
import com.example.hoode_app.databinding.ItemGalleryThumbnailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAddPhoto.setOnClickListener {
            showAddPhotoDialog()
        }

        binding.rvGalleryGrid.layoutManager = GridLayoutManager(requireContext(), 2)

        binding.btnGalleryRefresh.setOnClickListener { loadPhotos(false) }
        binding.btnGalleryMore.setOnClickListener { loadPhotos(true) }
        binding.btnMyPhotos.setOnClickListener { findNavController().navigate(R.id.profileFragment) }
        loadPhotos(false)
    }

    private val photos=mutableListOf<GalleryItem>()
    private var loadJob:kotlinx.coroutines.Job?=null
    private fun showAddPhotoDialog() {
        com.example.hoode_app.ui.common.CommunityPostForm.showGallery(this) { loadPhotos(false) }
    }
    private fun loadPhotos(append:Boolean) {
        loadJob?.cancel()
        binding.tvGalleryStatus.text="Loading photos…"
        binding.btnGalleryMore.isEnabled=false
        if(!append)photos.clear()
        loadJob=viewLifecycleOwner.lifecycleScope.launch {
            com.hoodeconnect.backend.CommunityApi.entries("gallery",offset=if(append)photos.size else 0).onSuccess { rows->
                photos.addAll(rows.map { GalleryItem(id=it.id,title=it.title,caption=it.text("caption"),photographer=it.text("photographer"),imageUrl=it.imageUrl) })
                binding.tvGalleryCount.text="${photos.size} photos"
                binding.tvGalleryStatus.text=if(photos.isEmpty())"No approved photos yet. Share the first view of Hoode." else "Tap a photo to open it. New photos appear after approval."
                binding.rvGalleryGrid.adapter=GalleryAdapter(photos.toList()) { item,pos,total->showFullscreenViewer(item,pos+1,total) }
                binding.btnGalleryMore.visibility=if(rows.size==50)View.VISIBLE else View.GONE
            }.onFailure { binding.tvGalleryStatus.text=it.message ?: "Could not load photos. Tap Refresh." }
            binding.btnGalleryMore.isEnabled=true
        }
    }

    private fun showFullscreenViewer(item: GalleryItem, currentPos: Int, total: Int) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image)

        val ivImage = dialog.findViewById<ImageView>(R.id.ivFullscreenImage)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvFullscreenTitle)
        val tvCaption = dialog.findViewById<TextView>(R.id.tvFullscreenCaption)
        val tvUploader = dialog.findViewById<TextView>(R.id.tvFullscreenUploader)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btnCloseFullscreen)

        tvTitle.text = item.title
        tvCaption.text = item.caption
        tvUploader.text = "Uploaded by: ${item.photographer}"

        if (item.imageUrl.isNotBlank()) {
            ivImage.load(item.imageUrl) {
                crossfade(true)
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        tvUploader.setOnClickListener { com.example.hoode_app.ui.common.PostDiscussion.show(this,"community_content",item.id,item.title) }
        tvUploader.text="By ${item.photographer} · Likes & comments"
        tvUploader.contentDescription="Open photo likes and comments"
        tvUploader.isFocusable=true
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadJob?.cancel()
        _binding = null
    }

    class GalleryAdapter(
        private val items: List<GalleryItem>,
        private val onClick: (GalleryItem, Int, Int) -> Unit
    ) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemGalleryThumbnailBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemGalleryThumbnailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            b.root.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (210 * parent.resources.displayMetrics.density).toInt())
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvGalleryItemTitle.text = item.title
            holder.binding.tvGalleryItemPhotographer.text = "📸 ${item.photographer}"

            if (item.imageUrl.isNotBlank()) {
                holder.binding.ivGalleryImage.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_gallery_luxury_gradient)
                    error(R.drawable.bg_gallery_luxury_gradient)
                }
            }

            holder.binding.root.setOnClickListener {
                onClick(item, position, items.size)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}

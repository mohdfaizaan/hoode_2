package com.example.hoode_app.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hoode_app.data.model.GalleryItem
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentGalleryBinding
import com.example.hoode_app.databinding.ItemGalleryThumbnailBinding
import coil.load
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

        binding.rvGalleryGrid.layoutManager = GridLayoutManager(requireContext(), 2)

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.galleryItems.collectLatest { items ->
                binding.tvGalleryCount.text = "${items.size} Photos (Max 25)"
                binding.rvGalleryGrid.adapter = GalleryAdapter(items) { item, pos, total ->
                    showFullscreenViewer(item, pos + 1, total)
                }
            }
        }
    }

    private fun showFullscreenViewer(item: GalleryItem, currentPos: Int, total: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("${item.title} ($currentPos of $total)")
            .setMessage("${item.caption}\n\nPhoto Credit: ${item.photographer}\nLocation: Hoode Community Heritage")
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class GalleryAdapter(
        private val items: List<GalleryItem>,
        private val onClick: (GalleryItem, Int, Int) -> Unit
    ) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemGalleryThumbnailBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemGalleryThumbnailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvGalleryItemTitle.text = item.title
            holder.binding.tvGalleryItemOrder.text = "${position + 1} of ${items.size}"
            holder.binding.tvGalleryItemPhotographer.text = "📸 ${item.photographer}"

            if (item.imageUrl.isNotBlank()) {
                holder.binding.ivGalleryImage.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(com.example.hoode_app.R.drawable.bg_gallery_luxury_gradient)
                    error(com.example.hoode_app.R.drawable.bg_gallery_luxury_gradient)
                }
            }

            holder.binding.root.setOnClickListener {
                onClick(item, position, items.size)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}

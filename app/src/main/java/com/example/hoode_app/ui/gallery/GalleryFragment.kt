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

    private var onPhotoPicked: ((android.net.Uri) -> Unit)? = null
    private val pickPhotoLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        uri?.let { onPhotoPicked?.invoke(it) }
    }

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

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.galleryItems.collectLatest { items ->
                binding.tvGalleryCount.text = "${items.size} Photos"
                binding.rvGalleryGrid.adapter = GalleryAdapter(items) { item, pos, total ->
                    showFullscreenViewer(item, pos + 1, total)
                }
            }
        }
    }

    private fun showAddPhotoDialog() {
        val user = HoodeRepository.currentUser.value
        val defaultName = user?.displayName ?: "Verified Resident"

        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormAddPhotoBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        formBinding.etPhotographer.setText(defaultName)

        var selectedTag = "Beach & Nature"
        val tagChips = listOf(
            formBinding.chipGalNature to "Beach & Nature",
            formBinding.chipGalSports to "Sports",
            formBinding.chipGalMosques to "Mosques",
            formBinding.chipGalCommunity to "Community"
        )

        for ((view, tag) in tagChips) {
            view.setOnClickListener {
                selectedTag = tag
                for ((v, t) in tagChips) {
                    if (t == selectedTag) {
                        v.setBackgroundResource(R.drawable.bg_chip_black_border)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        fun updatePreview(url: String) {
            if (url.isNotBlank()) {
                formBinding.ivPhotoPreview.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.bg_gallery_luxury_gradient)
                    error(R.drawable.bg_gallery_luxury_gradient)
                }
            } else {
                formBinding.ivPhotoPreview.setImageResource(R.drawable.bg_gallery_luxury_gradient)
            }
        }

        updatePreview(formBinding.etPhotoUrl.text.toString())
        formBinding.etPhotoUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Pick Photo from Gallery
        val launchPicker = View.OnClickListener {
            onPhotoPicked = { uri ->
                formBinding.etPhotoUrl.setText(uri.toString())
                updatePreview(uri.toString())
                Toast.makeText(requireContext(), "Photo selected from gallery", Toast.LENGTH_SHORT).show()
            }
            pickPhotoLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
        formBinding.btnUploadPhoto.setOnClickListener(launchPicker)
        formBinding.flPhotoPreviewContainer.setOnClickListener(launchPicker)

        formBinding.btnClosePhoto.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitPhoto.setOnClickListener {
            val url = formBinding.etPhotoUrl.text.toString().trim()
            val title = formBinding.etPhotoTitle.text.toString().trim()
            val photoName = formBinding.etPhotographer.text.toString().trim().ifBlank { defaultName }
            val caption = formBinding.etPhotoCaption.text.toString().trim()

            if (url.isBlank()) {
                formBinding.tilPhotoUrl.error = "Please enter an image URL"
                return@setOnClickListener
            }
            formBinding.tilPhotoUrl.error = null

            if (title.isBlank()) {
                formBinding.tilPhotoTitle.error = "Please enter a photo title"
                return@setOnClickListener
            }
            formBinding.tilPhotoTitle.error = null

            val newItem = GalleryItem(
                id = "gal_${System.currentTimeMillis()}",
                title = title,
                imageUrl = url,
                photographer = photoName,
                caption = if (caption.isNotBlank()) "$caption • $selectedTag" else selectedTag,
                sortOrder = 0
            )

            HoodeRepository.addGalleryItem(newItem)
            Toast.makeText(requireContext(), "Photo published to Community Gallery!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
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
        dialog.show()
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

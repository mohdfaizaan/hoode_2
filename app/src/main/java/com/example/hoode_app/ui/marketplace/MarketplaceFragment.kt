package com.example.hoode_app.ui.marketplace

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.hoode_app.R
import com.example.hoode_app.data.model.ClassifiedItem
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.DialogMarketplaceDetailBinding
import com.example.hoode_app.databinding.FragmentMarketplaceBinding
import com.example.hoode_app.databinding.ItemClassifiedCardBinding
import com.example.hoode_app.databinding.ItemDetailImageSlideBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MarketplaceFragment : Fragment() {

    private var _binding: FragmentMarketplaceBinding? = null
    private val binding get() = _binding!!

    private var selectedCategory: String = "All"
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketplaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPostListing.setOnClickListener {
            showPostListingDialog()
        }

        setupChips()

        binding.etSearchMarketplace.doAfterTextChanged { text ->
            searchQuery = text?.toString()?.trim()?.lowercase() ?: ""
            renderListings()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.classifieds.collectLatest {
                renderListings()
            }
        }
    }

    private fun setupChips() {
        val chips = listOf(
            binding.chipMarketAll to "All",
            binding.chipMarketVehicles to "Vehicles",
            binding.chipMarketFurniture to "Furniture",
            binding.chipMarketElectronics to "Electronics",
            binding.chipMarketProperty to "Property",
            binding.chipMarketServices to "Fishery"
        )

        for ((chipView, category) in chips) {
            chipView.setOnClickListener {
                selectedCategory = category
                for ((v, c) in chips) {
                    if (c == selectedCategory) {
                        // Highlighted with black border as requested
                        v.setBackgroundResource(R.drawable.bg_chip_black_border)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
                renderListings()
            }
        }

        // Set default selected style on All chip with black border
        binding.chipMarketAll.setBackgroundResource(R.drawable.bg_chip_black_border)
        binding.chipMarketAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
    }

    private fun renderListings() {
        val all = HoodeRepository.classifieds.value
        val filtered = all.filter { item ->
            val matchesCategory = if (selectedCategory == "All") true
            else item.category.contains(selectedCategory, ignoreCase = true)

            val matchesSearch = if (searchQuery.isBlank()) true
            else item.title.lowercase().contains(searchQuery) ||
                    item.description.lowercase().contains(searchQuery) ||
                    item.area.lowercase().contains(searchQuery)

            matchesCategory && matchesSearch
        }

        binding.llMarketplaceContainer.removeAllViews()

        if (filtered.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "No listings found matching '$searchQuery'.\nBe the first to post in this category!"
                textSize = 14f
                setPadding(32, 64, 32, 32)
                gravity = android.view.Gravity.CENTER
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            }
            binding.llMarketplaceContainer.addView(emptyTv)
            return
        }

        for (item in filtered) {
            val itemBinding = ItemClassifiedCardBinding.inflate(layoutInflater, binding.llMarketplaceContainer, false)
            itemBinding.tvClassifiedType.text = item.type.uppercase()
            itemBinding.tvClassifiedCategory.text = item.category
            itemBinding.tvClassifiedPrice.text = item.price
            itemBinding.tvClassifiedTitle.text = item.title
            itemBinding.tvClassifiedAreaDate.text = "${item.area} • ${item.date}"

            val firstImg = item.images.firstOrNull() ?: ""
            if (firstImg.isNotBlank()) {
                itemBinding.ivClassifiedImage.load(firstImg) {
                    crossfade(true)
                    placeholder(R.drawable.bg_gallery_luxury_gradient)
                    error(R.drawable.bg_gallery_luxury_gradient)
                }
            }

            if (item.images.size > 1) {
                itemBinding.tvImageCountBadge.visibility = View.VISIBLE
                itemBinding.tvImageCountBadge.text = "📷 ${item.images.size}"
            } else {
                itemBinding.tvImageCountBadge.visibility = View.GONE
            }

            // Clicking opens the complete OLX-style detail page with multi-image swipe
            itemBinding.root.setOnClickListener {
                showMarketplaceDetailDialog(item)
            }

            binding.llMarketplaceContainer.addView(itemBinding.root)
        }
    }

    private fun showMarketplaceDetailDialog(item: ClassifiedItem) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogMarketplaceDetailBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.tvDetailPrice.text = item.price
        dialogBinding.tvDetailType.text = item.type.uppercase()
        dialogBinding.tvDetailTitle.text = item.title
        dialogBinding.tvDetailCategory.text = item.category
        dialogBinding.tvDetailAreaDate.text = "${item.area} • ${item.date}"
        dialogBinding.tvDetailSeller.text = "Sold by ${item.sellerName}"
        dialogBinding.tvDetailDescription.text = item.description

        val images = if (item.images.isNotEmpty()) item.images else listOf("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800")
        val imageAdapter = DetailImageSliderAdapter(images)
        dialogBinding.vpDetailImages.adapter = imageAdapter

        if (images.size > 1) {
            dialogBinding.tvDetailImageIndex.visibility = View.VISIBLE
            dialogBinding.tvDetailImageIndex.text = "1 / ${images.size}"
            dialogBinding.vpDetailImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    dialogBinding.tvDetailImageIndex.text = "${position + 1} / ${images.size}"
                }
            })
        } else {
            dialogBinding.tvDetailImageIndex.visibility = View.GONE
        }

        dialogBinding.btnCloseDetail.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnCallSeller.setOnClickListener {
            Toast.makeText(requireContext(), "Connecting phone call to ${item.sellerName} (${item.phone})...", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.btnChatSeller.setOnClickListener {
            dialog.dismiss()
            showInquiryDialog(item)
        }

        dialog.show()
    }

    private fun showInquiryDialog(item: ClassifiedItem) {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormInquireBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        formBinding.tvInquireItemInfo.text = "${item.title} • ₹${item.price} • Seller: ${item.sellerName}"

        formBinding.btnCloseInquire.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSendInquiry.setOnClickListener {
            val msg = formBinding.etInquireMessage.text.toString().trim()
            if (msg.isBlank()) {
                formBinding.tilInquireMessage.error = "Please enter your question or message"
                return@setOnClickListener
            }
            formBinding.tilInquireMessage.error = null

            Toast.makeText(requireContext(), "Inquiry sent to ${item.sellerName}. Check your Inbox for replies.", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPostListingDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormPostMarketplaceBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedFormCategory = "Vehicles"

        // Category selection chips in form
        val catChips = listOf(
            formBinding.chipCatVehicles to "Vehicles",
            formBinding.chipCatFurniture to "Furniture",
            formBinding.chipCatElectronics to "Electronics",
            formBinding.chipCatBooks to "Books",
            formBinding.chipCatHome to "Home & Garden"
        )

        for ((view, cat) in catChips) {
            view.setOnClickListener {
                selectedFormCategory = cat
                for ((v, c) in catChips) {
                    if (c == selectedFormCategory) {
                        v.setBackgroundResource(R.drawable.bg_chip_black_border)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        // Live preview of first image URL
        fun updatePreview(url: String) {
            val first = url.split(",").firstOrNull()?.trim() ?: ""
            if (first.isNotBlank()) {
                formBinding.ivImagePreview.load(first) {
                    crossfade(true)
                    placeholder(R.drawable.bg_gallery_luxury_gradient)
                    error(R.drawable.bg_gallery_luxury_gradient)
                }
            } else {
                formBinding.ivImagePreview.setImageResource(R.drawable.bg_gallery_luxury_gradient)
            }
        }

        updatePreview(formBinding.etItemImages.text.toString())
        formBinding.etItemImages.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        formBinding.btnClosePost.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitListing.setOnClickListener {
            val title = formBinding.etItemTitle.text.toString().trim()
            val rawPrice = formBinding.etItemPrice.text.toString().trim()
            val type = formBinding.etItemType.text.toString().trim().ifBlank { "Sell" }
            val desc = formBinding.etItemDescription.text.toString().trim()
            val rawImages = formBinding.etItemImages.text.toString().trim()
            val area = formBinding.etItemArea.text.toString().trim().ifBlank { "Hoode" }

            if (title.isBlank()) {
                formBinding.tilItemTitle.error = "Please enter an item title"
                return@setOnClickListener
            }
            formBinding.tilItemTitle.error = null

            if (rawPrice.isBlank()) {
                formBinding.tilItemPrice.error = "Please specify a price"
                return@setOnClickListener
            }
            formBinding.tilItemPrice.error = null

            val priceFormatted = if (rawPrice.startsWith("₹")) rawPrice else "₹$rawPrice"
            val imageList = rawImages.split(",").map { it.trim() }.filter { it.isNotBlank() }

            val user = HoodeRepository.currentUser.value
            val newItem = ClassifiedItem(
                title = title,
                price = priceFormatted,
                type = type,
                category = selectedFormCategory,
                area = area,
                description = desc,
                sellerName = user?.displayName ?: "Verified Resident",
                date = "Today",
                images = if (imageList.isNotEmpty()) imageList else listOf("https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=800")
            )

            val current = HoodeRepository.classifieds.value.toMutableList()
            current.add(0, newItem)
            Toast.makeText(requireContext(), "Listing posted successfully to Marketplace!", Toast.LENGTH_SHORT).show()
            renderListings()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class DetailImageSliderAdapter(private val images: List<String>) :
        RecyclerView.Adapter<DetailImageSliderAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemDetailImageSlideBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemDetailImageSlideBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val url = images[position]
            holder.binding.ivSlideImage.load(url) {
                crossfade(true)
                placeholder(R.drawable.bg_gallery_luxury_gradient)
                error(R.drawable.bg_gallery_luxury_gradient)
            }
        }

        override fun getItemCount(): Int = images.size
    }
}

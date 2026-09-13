package com.example.hoode_app.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.model.ClassifiedItem
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentMarketplaceBinding
import com.example.hoode_app.databinding.ItemClassifiedCardBinding
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
                        v.setBackgroundResource(R.drawable.bg_chip_selected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.border_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
                renderListings()
            }
        }
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
            itemBinding.tvClassifiedAreaDate.text = "${item.area} • Seller: ${item.sellerName} • ${item.date}"
            itemBinding.tvClassifiedDescription.text = item.description

            itemBinding.btnSendInquiry.setOnClickListener {
                showInquiryDialog(item)
            }

            binding.llMarketplaceContainer.addView(itemBinding.root)
        }
    }

    private fun showInquiryDialog(item: ClassifiedItem) {
        val input = EditText(requireContext()).apply {
            hint = "Ask a question about availability or condition"
            setBackgroundResource(R.drawable.bg_search_bar)
            setPadding(36, 28, 36, 28)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Inquire: ${item.title}")
            .setMessage("Your private inquiry will be sent to ${item.sellerName}. Replies appear in your Inbox without sharing your phone number.")
            .setView(input)
            .setPositiveButton("Send Inquiry") { _, _ ->
                val msg = input.text.toString().trim()
                if (msg.isNotBlank()) {
                    Toast.makeText(requireContext(), "Inquiry sent to ${item.sellerName}. Check your Inbox for replies.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPostListingDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val etTitle = EditText(requireContext()).apply { hint = "Item Title (e.g. Hero Cycle)" }
        val etPrice = EditText(requireContext()).apply { hint = "Price in INR (e.g. ₹2,500 or Free)" }
        val etCat = EditText(requireContext()).apply { hint = "Category (Vehicles, Furniture, Electronics)" }
        val etArea = EditText(requireContext()).apply { hint = "Area (e.g. Bengre, Hoode Beach)" }
        val etDesc = EditText(requireContext()).apply { hint = "Condition & specifications" }

        layout.addView(etTitle)
        layout.addView(etPrice)
        layout.addView(etCat)
        layout.addView(etArea)
        layout.addView(etDesc)

        AlertDialog.Builder(requireContext())
            .setTitle("List an Item in Hoode")
            .setView(layout)
            .setPositiveButton("Post Item") { _, _ ->
                val title = etTitle.text.toString().trim()
                val price = etPrice.text.toString().trim()
                val cat = etCat.text.toString().trim()
                val area = etArea.text.toString().trim()
                val desc = etDesc.text.toString().trim()

                if (title.isNotBlank()) {
                    val newItem = ClassifiedItem(
                        title = title,
                        price = if (price.isNotBlank()) price else "Free",
                        type = "Sell",
                        category = if (cat.isNotBlank()) cat else "General",
                        area = if (area.isNotBlank()) area else "Hoode",
                        description = desc,
                        sellerName = HoodeRepository.currentUser.value?.displayName ?: "Faizan",
                        date = "Today"
                    )
                    HoodeRepository.postClassified(newItem)
                    Toast.makeText(requireContext(), "Listing posted to community marketplace!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

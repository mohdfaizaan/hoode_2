package com.example.hoode_app.ui.marketplace

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import com.example.hoode_app.ui.common.submitForReview
import com.example.hoode_app.ui.common.attachClearAction
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.hoode_app.databinding.DialogBookProductSpinnyBinding
import com.example.hoode_app.databinding.DialogMarketplaceDetailBinding
import com.example.hoode_app.databinding.FragmentMarketplaceBinding
import com.example.hoode_app.databinding.ItemClassifiedGridBinding
import com.example.hoode_app.databinding.ItemDetailImageSlideBinding
import com.example.hoode_app.databinding.ItemImageFormationChipBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MarketplaceFragment : Fragment() {

    private var _binding: FragmentMarketplaceBinding? = null
    private val binding get() = _binding!!

    private var selectedCategory: String = "All"
    private var searchQuery: String = ""

    private var onMultipleImagesPicked: ((List<Uri>) -> Unit)? = null
    private val pickMultipleImagesLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(8)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onMultipleImagesPicked?.invoke(uris)
        }
    }

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
        binding.etSearchMarketplace.attachClearAction()

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
                        v.setBackgroundResource(R.drawable.bg_chip_accent_filled)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
                renderListings()
            }
        }

        binding.chipMarketAll.setBackgroundResource(R.drawable.bg_chip_black_border)
        binding.chipMarketAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
    }

    private fun renderListings() {
        HoodeRepository.checkAndExpireBookings()

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
            val card = ItemClassifiedGridBinding.inflate(layoutInflater, binding.llMarketplaceContainer, false)

            card.ivGridImage.load(item.images.firstOrNull()) {
                crossfade(true)
                placeholder(R.drawable.bg_gallery_luxury_gradient)
                error(R.drawable.bg_gallery_luxury_gradient)
            }

            when {
                item.isSold -> {
                    card.tvGridStatus.text = "SOLD"
                    card.tvGridStatus.setBackgroundResource(R.drawable.bg_pill_neutral)
                    card.tvGridStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                }
                HoodeRepository.isBookingActive(item) -> {
                    card.tvGridStatus.text = "RESERVED"
                    card.tvGridStatus.setBackgroundColor(requireContext().getColor(R.color.warning))
                    card.tvGridStatus.setTextColor(Color.WHITE)
                }
                else -> {
                    card.tvGridStatus.text = "AVAILABLE"
                    card.tvGridStatus.setBackgroundResource(R.drawable.bg_pill_accent)
                    card.tvGridStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_ink))
                }
            }

            card.tvGridType.text = item.type.uppercase()
            card.tvGridPrice.text = item.price
            card.tvGridTitle.text = item.title.uppercase()
            card.tvGridLocation.text = "${item.area} • ${item.date}"

            if (item.images.size > 1) {
                card.tvGridImageCount.visibility = View.VISIBLE
                card.tvGridImageCount.text = "1 / ${item.images.size}"
            } else {
                card.tvGridImageCount.visibility = View.GONE
            }

            card.root.setOnClickListener { showMarketplaceDetailDialog(item) }

            binding.llMarketplaceContainer.addView(card.root)
        }
    }

    private fun showSpinnyBookingDialog(item: ClassifiedItem) {
        val user = HoodeRepository.currentUser.value
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to reserve a product.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check 1-product-per-user rule
        val activeBooking = HoodeRepository.getActiveBookingForUser(user.id)
        if (activeBooking != null && activeBooking.id != item.id) {
            AlertDialog.Builder(requireContext())
                .setTitle("Active Reservation Exists 📌")
                .setMessage("To keep reservations fair, you can only hold 1 product at a time.\n\nYou currently have a 24-hour hold on:\n'${activeBooking.title}'\n\nPlease finalize or cancel that reservation first before holding another product.")
                .setPositiveButton("I Understand", null)
                .show()
            return
        }

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val b = DialogBookProductSpinnyBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        b.tvBookingItemTitle.text = item.title
        b.tvBookingItemPrice.text = item.price
        b.tvBookingItemSeller.text = "• Seller: ${item.sellerName} (${item.area})"

        b.etBookerName.setText(user.displayName)
        b.etBookerPhone.setText(user.phone.orEmpty())

        b.btnCloseBookingDialog.setOnClickListener {
            dialog.dismiss()
        }

        b.btnConfirmBooking.setOnClickListener {
            val name = b.etBookerName.text.toString().trim()
            val phone = b.etBookerPhone.text.toString().trim()
            val note = b.etBookingNote.text.toString().trim()

            if (name.isBlank() || phone.isBlank()) {
                Toast.makeText(requireContext(), "Please enter your name and phone number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val result = HoodeRepository.bookClassified(item.id, user, phone, note)
                result.onSuccess {
                    Toast.makeText(requireContext(), "⚡ 24-Hour Hold Confirmed! You have 24 hours to inspect & purchase from ${item.sellerName}.", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    renderListings()
                }.onFailure { err ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("Reservation Notice")
                        .setMessage(err.message ?: "Could not reserve product.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }

        dialog.show()
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

        val currentUser = HoodeRepository.currentUser.value
        val currentUserId = currentUser?.id ?: ""

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

        val isBookingActive = HoodeRepository.isBookingActive(item)
        val isSeller = item.sellerUserId == currentUserId && currentUserId.isNotBlank()

        if (item.isSold) {
            dialogBinding.cardDetailBookingInfo.visibility = View.GONE
            dialogBinding.btnDetailBook.visibility = View.GONE
            dialogBinding.btnDetailCancelBooking.visibility = View.GONE
        } else if (isBookingActive) {
            dialogBinding.cardDetailBookingInfo.visibility = View.VISIBLE
            val remainingMillis = (item.bookedAtTimestamp + 24 * 60 * 60 * 1000L) - System.currentTimeMillis()
            val hours = (remainingMillis / (1000 * 60 * 60)).coerceAtLeast(0)
            val mins = ((remainingMillis / (1000 * 60)) % 60).coerceAtLeast(0)
            dialogBinding.tvDetailBookingStatus.text = "🔒 Reserved: ${hours}h ${mins}m left"

            if (item.bookedByUserId == currentUserId) {
                dialogBinding.tvDetailBookingSubtext.text = "You reserved this product. Tap below to cancel or release."
                dialogBinding.btnDetailBook.visibility = View.GONE
                dialogBinding.btnDetailCancelBooking.visibility = View.VISIBLE
                dialogBinding.btnDetailCancelBooking.setOnClickListener {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val actionResult = HoodeRepository.cancelBooking(item.id, currentUserId)
                        if (actionResult.isFailure) {
                            Toast.makeText(requireContext(), actionResult.exceptionOrNull()?.message ?: "Could not save. Retry.", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        Toast.makeText(requireContext(), "Reservation cancelled.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        renderListings()
                    }
                }
            } else {
                dialogBinding.tvDetailBookingSubtext.text = "Reserved exclusively by resident ${item.bookedByName ?: ""}. Goes live after 24h."
                dialogBinding.btnDetailBook.visibility = View.VISIBLE
                dialogBinding.btnDetailBook.isEnabled = false
                dialogBinding.btnDetailBook.text = "🔒 Currently Reserved"
                dialogBinding.btnDetailBook.setBackgroundColor(requireContext().getColor(R.color.text_secondary))
                dialogBinding.btnDetailCancelBooking.visibility = View.GONE
            }
        } else {
            dialogBinding.cardDetailBookingInfo.visibility = View.GONE
            dialogBinding.btnDetailCancelBooking.visibility = View.GONE
            dialogBinding.btnDetailBook.visibility = View.VISIBLE
            dialogBinding.btnDetailBook.isEnabled = true
            dialogBinding.btnDetailBook.text = "⚡ Reserve for 24 hours"
            dialogBinding.btnDetailBook.setOnClickListener {
                dialog.dismiss()
                showSpinnyBookingDialog(item)
            }
        }

        // Seller controls in detail
        if (isSeller) {
            dialogBinding.llDetailSellerControls.visibility = View.VISIBLE
            dialogBinding.btnDetailMarkSold.visibility = if (!item.isSold) View.VISIBLE else View.GONE
            dialogBinding.btnDetailMarkSold.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    val actionResult = HoodeRepository.markClassifiedSold(item.id, currentUserId)
                    if (actionResult.isFailure) {
                        Toast.makeText(requireContext(), actionResult.exceptionOrNull()?.message ?: "Could not save. Retry.", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    Toast.makeText(requireContext(), "Item marked as sold!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    renderListings()
                }
            }
            dialogBinding.btnDetailDelete.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("Withdraw ${item.title}?").setMessage("The listing will leave the marketplace and its reservation will be cancelled.").setNegativeButton("Keep listing",null).setPositiveButton("Withdraw") { _,_->
                viewLifecycleOwner.lifecycleScope.launch {
                    val actionResult = HoodeRepository.removeClassified(item.id)
                    if (actionResult.isFailure) {
                        Toast.makeText(requireContext(), actionResult.exceptionOrNull()?.message ?: "Could not save. Retry.", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    Toast.makeText(requireContext(), "Listing removed.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    renderListings()
                }
                }.show()
            }
        } else {
            dialogBinding.llDetailSellerControls.visibility = View.GONE
        }

        dialogBinding.btnCloseDetail.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnCallSeller.setOnClickListener {
            try { startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL, Uri.parse("tel:${item.phone}"))) } catch (_: Exception) { Toast.makeText(requireContext(), "No phone app is available.", Toast.LENGTH_SHORT).show() }
        }

        dialogBinding.btnChatSeller.setOnClickListener {
            dialog.dismiss()
            showInquiryDialog(item)
        }

        dialog.show()
    }

    private fun showInquiryDialog(item: ClassifiedItem) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormInquireBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        formBinding.tvInquireItemInfo.text = "${item.title} • ${item.price} • Seller: ${item.sellerName}"

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

            formBinding.btnSendInquiry.isEnabled=false
            viewLifecycleOwner.lifecycleScope.launch {
                val result=com.hoodeconnect.backend.CommunityApi.safely {
                    check(!item.sellerUserId.isNullOrBlank()) { "Seller contact is unavailable." }
                    com.hoodeconnect.backend.CommunityApi.request("marketplace_messages","POST",org.json.JSONObject()
                        .put("item_id",item.id).put("sender_id",com.hoodeconnect.backend.BackendSession.userId)
                        .put("recipient_id",item.sellerUserId).put("body",msg))
                }
                result.onSuccess { Toast.makeText(requireContext(),"Inquiry sent. Replies appear in Inbox.",Toast.LENGTH_LONG).show();dialog.dismiss() }
                    .onFailure { formBinding.tilInquireMessage.error=it.message;formBinding.btnSendInquiry.isEnabled=true }
            }
        }

        dialog.show()
    }

    private fun showPostListingDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormPostMarketplaceBinding.inflate(layoutInflater)
        formBinding.etItemPhone.setText(HoodeRepository.currentUser.value?.phone.orEmpty())
        dialog.setContentView(formBinding.root)

        // OLX-style: sheet anchored to the bottom of the screen
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedFormCategory = "Vehicles"

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
                        v.setBackgroundResource(R.drawable.bg_chip_accent_filled)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        val orderedImages = mutableListOf<String>()
        val defaultInitial = formBinding.etItemImages.text.toString().trim()
        if (defaultInitial.isNotBlank()) {
            orderedImages.addAll(defaultInitial.split(",").map { it.trim() }.filter { it.isNotBlank() })
        }

        fun refreshFormationUI() {
            formBinding.llFormationImagesContainer.removeAllViews()

            if (orderedImages.isEmpty()) {
                formBinding.tvFormationInstruction.text = "No images selected yet. Tap above to pick photos."
                formBinding.ivImagePreview.setImageResource(R.drawable.bg_gallery_luxury_gradient)
                formBinding.tvPreviewBadge.text = "No Cover Image"
                formBinding.etItemImages.setText("")
                return
            }

            formBinding.tvFormationInstruction.text = "Formation & Order (◀ / ▶ to move. Photo #1 is your Cover):"
            val coverUrl = orderedImages.firstOrNull() ?: ""
            if (coverUrl.isNotBlank()) {
                formBinding.ivImagePreview.load(coverUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_gallery_luxury_gradient)
                    error(R.drawable.bg_gallery_luxury_gradient)
                }
                formBinding.tvPreviewBadge.text = "#1 Cover Photo Selected"
            }

            formBinding.etItemImages.setText(orderedImages.joinToString(", "))

            for (idx in orderedImages.indices) {
                val imgUrl = orderedImages[idx]
                val chipBinding = ItemImageFormationChipBinding.inflate(layoutInflater, formBinding.llFormationImagesContainer, false)

                chipBinding.ivChipThumb.load(imgUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_gallery_luxury_gradient)
                    error(R.drawable.bg_gallery_luxury_gradient)
                }

                if (idx == 0) {
                    chipBinding.tvChipPosition.text = "⭐ #1 COVER"
                    chipBinding.tvChipPosition.setBackgroundResource(R.drawable.bg_pill_accent)
                    chipBinding.btnChipMoveLeft.visibility = View.INVISIBLE
                } else {
                    chipBinding.tvChipPosition.text = "#${idx + 1}"
                    chipBinding.tvChipPosition.setBackgroundResource(R.drawable.bg_gallery_pill)
                    chipBinding.btnChipMoveLeft.visibility = View.VISIBLE
                }

                if (idx == orderedImages.size - 1) {
                    chipBinding.btnChipMoveRight.visibility = View.INVISIBLE
                } else {
                    chipBinding.btnChipMoveRight.visibility = View.VISIBLE
                }

                chipBinding.btnChipMoveLeft.setOnClickListener {
                    if (idx > 0) {
                        val temp = orderedImages[idx]
                        orderedImages[idx] = orderedImages[idx - 1]
                        orderedImages[idx - 1] = temp
                        refreshFormationUI()
                    }
                }

                chipBinding.btnChipMoveRight.setOnClickListener {
                    if (idx < orderedImages.size - 1) {
                        val temp = orderedImages[idx]
                        orderedImages[idx] = orderedImages[idx + 1]
                        orderedImages[idx + 1] = temp
                        refreshFormationUI()
                    }
                }

                chipBinding.btnChipRemove.setOnClickListener {
                    orderedImages.removeAt(idx)
                    refreshFormationUI()
                }

                formBinding.llFormationImagesContainer.addView(chipBinding.root)
            }
        }

        refreshFormationUI()

        val launchMarketplacePicker = View.OnClickListener {
            onMultipleImagesPicked = { uris ->
                for (u in uris) {
                    val str = u.toString()
                    if (!orderedImages.contains(str)) {
                        orderedImages.add(str)
                    }
                }
                refreshFormationUI()
                Toast.makeText(requireContext(), "${uris.size} photos added! Rearrange using ◀ / ▶ to pick cover.", Toast.LENGTH_SHORT).show()
            }
            pickMultipleImagesLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        formBinding.btnUploadImage.setOnClickListener(launchMarketplacePicker)
        formBinding.flImagePreviewContainer.setOnClickListener(launchMarketplacePicker)

        formBinding.btnClosePost.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitListing.setOnClickListener {
            val title = formBinding.etItemTitle.text.toString().trim()
            val sellerPhone=formBinding.etItemPhone.text.toString().trim()
            if(sellerPhone.isBlank()){formBinding.tilItemPhone.error="Enter your contact number";return@setOnClickListener}
            formBinding.tilItemPhone.error=null
            val rawPrice = formBinding.etItemPrice.text.toString().trim()
            val type = formBinding.etItemType.text.toString().trim().ifBlank { "Sell" }
            val desc = formBinding.etItemDescription.text.toString().trim()
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
            val finalImages = if (orderedImages.isNotEmpty()) {
                orderedImages.toList()
            } else {
                val typed = formBinding.etItemImages.text.toString().trim()
                if (typed.isNotBlank()) typed.split(",").map { it.trim() }.filter { it.isNotBlank() }
                else emptyList()
            }

            val user = HoodeRepository.currentUser.value
            val newItem = ClassifiedItem(
                title = title,
                phone = sellerPhone,
                price = priceFormatted,
                type = type,
                category = selectedFormCategory,
                area = area,
                description = desc,
                sellerName = user?.displayName ?: "Verified Resident",
                sellerUserId = user?.id,
                date = "Today",
                images = finalImages
            )

            submitForReview(formBinding.btnSubmitListing,dialog) { HoodeRepository.postClassified(newItem) }
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

package com.example.hoode_app.ui.create

import android.app.Dialog
import android.net.Uri
import com.example.hoode_app.ui.common.submitForReview
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.hoode_app.R
import com.example.hoode_app.data.model.BloodRequest
import com.example.hoode_app.data.model.ClassifiedItem
import com.example.hoode_app.data.model.CommunityEvent
import com.example.hoode_app.data.model.JobPosting
import com.example.hoode_app.data.model.LostFoundItem
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.DialogFormBloodRequestBinding
import com.example.hoode_app.databinding.DialogFormPostEventBinding
import com.example.hoode_app.databinding.DialogFormPostJobBinding
import com.example.hoode_app.databinding.DialogFormPostMarketplaceBinding
import com.example.hoode_app.databinding.DialogFormReportCivicBinding
import com.example.hoode_app.databinding.DialogFormReportLostFoundBinding
import com.example.hoode_app.databinding.FragmentCreateBinding
import com.example.hoode_app.databinding.ItemImageFormationChipBinding
import kotlinx.coroutines.launch

class CreateFragment : Fragment() {

    private var _binding: FragmentCreateBinding? = null
    private val binding get() = _binding!!

    private var onImagePicked: ((Uri) -> Unit)? = null
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onImagePicked?.invoke(it) }
    }

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
        _binding = FragmentCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCreateButtons()
    }

    private fun setupCreateButtons() {
        binding.cardCreateClassified.setOnClickListener {
            showPostMarketplaceDialog()
        }
        binding.cardCreateEvent.setOnClickListener {
            showPostEventDialog()
        }
        binding.cardCreateJob.setOnClickListener {
            showPostJobDialog()
        }
        binding.cardCreateLostFound.setOnClickListener {
            showReportLostFoundDialog()
        }
        binding.cardCreateBlood.setOnClickListener {
            showPostBloodDialog()
        }
        binding.cardCreateCivic.setOnClickListener {
            showReportCivicDialog()
        }
    }

    private fun showPostMarketplaceDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = DialogFormPostMarketplaceBinding.inflate(layoutInflater)
        formBinding.etItemPhone.setText(HoodeRepository.currentUser.value?.phone.orEmpty())
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedFormCategory = "Vehicles"

        val catChips: List<Pair<TextView, String>> = listOf(
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

                // Move Left in Formation
                chipBinding.btnChipMoveLeft.setOnClickListener {
                    if (idx > 0) {
                        val temp = orderedImages[idx]
                        orderedImages[idx] = orderedImages[idx - 1]
                        orderedImages[idx - 1] = temp
                        refreshFormationUI()
                    }
                }

                // Move Right in Formation
                chipBinding.btnChipMoveRight.setOnClickListener {
                    if (idx < orderedImages.size - 1) {
                        val temp = orderedImages[idx]
                        orderedImages[idx] = orderedImages[idx + 1]
                        orderedImages[idx + 1] = temp
                        refreshFormationUI()
                    }
                }

                // Remove from Formation
                chipBinding.btnChipRemove.setOnClickListener {
                    orderedImages.removeAt(idx)
                    refreshFormationUI()
                }

                formBinding.llFormationImagesContainer.addView(chipBinding.root)
            }
        }

        refreshFormationUI()

        // Pick Multiple Images from Gallery
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

    private fun showPostEventDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = DialogFormPostEventBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedCategory = "Community"
        val categoryChips: List<Pair<TextView, String>> = listOf(
            formBinding.chipEvCommunity to "Community",
            formBinding.chipEvMajlis to "Majlis / Dars",
            formBinding.chipEvSports to "Sports Meet",
            formBinding.chipEvWedding to "Wedding / Nikah",
            formBinding.chipEvCivic to "Civic Drive"
        )

        for ((chipView, categoryName) in categoryChips) {
            chipView.setOnClickListener {
                selectedCategory = categoryName
                for ((v, name) in categoryChips) {
                    if (name == selectedCategory) {
                        v.setBackgroundResource(R.drawable.bg_chip_accent_filled)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        formBinding.btnClosePostEvent.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitEvent.setOnClickListener {
            val title = formBinding.etEventTitle.text.toString().trim()
            val date = formBinding.etEventDate.text.toString().trim()
            val venue = formBinding.etEventVenue.text.toString().trim()

            if (title.isBlank()) {
                formBinding.tilEventTitle.error = "Please enter event title"
                return@setOnClickListener
            }
            formBinding.tilEventTitle.error = null

            if (date.isBlank()) {
                formBinding.tilEventDate.error = "Please enter date and time"
                return@setOnClickListener
            }
            formBinding.tilEventDate.error = null

            if (venue.isBlank()) {
                formBinding.tilEventVenue.error = "Please enter venue"
                return@setOnClickListener
            }
            formBinding.tilEventVenue.error = null

            val user = HoodeRepository.currentUser.value
            val newEvent = CommunityEvent(
                title = title,
                organizer = formBinding.etEventOrganizer.text.toString().trim().ifBlank { user?.displayName ?: "Hoode Resident" },
                category = selectedCategory,
                date = date,
                time = "",
                venue = venue,
                description = formBinding.etEventDescription.text.toString().trim()
            )
            submitForReview(formBinding.btnSubmitEvent,dialog) { HoodeRepository.postEvent(newEvent) }
        }

        dialog.show()
    }

    private fun showPostJobDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = DialogFormPostJobBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedType = "Full-time"
        val typeChips: List<Pair<TextView, String>> = listOf(
            formBinding.chipJobFulltime to "Full-time",
            formBinding.chipJobParttime to "Part-time",
            formBinding.chipJobGig to "Gig / Contract",
            formBinding.chipJobApprentice to "Apprentice"
        )

        for ((chipView, typeName) in typeChips) {
            chipView.setOnClickListener {
                selectedType = typeName
                for ((v, name) in typeChips) {
                    if (name == selectedType) {
                        v.setBackgroundResource(R.drawable.bg_chip_accent_filled)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        formBinding.btnClosePostJob.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitJob.setOnClickListener {
            val title = formBinding.etJobTitle.text.toString().trim()
            val employer = formBinding.etJobEmployer.text.toString().trim()
            val pay = formBinding.etJobPay.text.toString().trim()
            val loc = formBinding.etJobLocation.text.toString().trim()
            val desc = formBinding.etJobDescription.text.toString().trim()
            val phone = formBinding.etJobPhone.text.toString().trim()

            if (title.isBlank()) {
                formBinding.tilJobTitle.error = "Please enter job title"
                return@setOnClickListener
            }
            formBinding.tilJobTitle.error = null

            if (employer.isBlank()) {
                formBinding.tilJobEmployer.error = "Please enter business/employer name"
                return@setOnClickListener
            }
            formBinding.tilJobEmployer.error = null

            if (phone.isBlank()) {
                formBinding.tilJobPhone.error = "Please enter contact phone"
                return@setOnClickListener
            }
            formBinding.tilJobPhone.error = null

            val newJob = JobPosting(
                title = title,
                employer = employer,
                type = selectedType,
                pay = if (pay.isNotBlank()) pay else "Competitive",
                location = if (loc.isNotBlank()) loc else "Hoode",
                description = if (desc.isNotBlank()) desc else "Contact $phone for full details and schedule.",
                deadline = "Contact employer"
            )
            submitForReview(formBinding.btnSubmitJob,dialog) { HoodeRepository.postJob(newJob,phone) }
        }

        dialog.show()
    }

    private fun showReportLostFoundDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = DialogFormReportLostFoundBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var isLostSelection = true
        var selectedCategory = "Keys"

        formBinding.btnTypeLost.setOnClickListener {
            isLostSelection = true
            formBinding.btnTypeLost.setBackgroundResource(R.drawable.bg_pill_danger)
            formBinding.btnTypeLost.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
            formBinding.btnTypeFound.background = null
            formBinding.btnTypeFound.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        formBinding.btnTypeFound.setOnClickListener {
            isLostSelection = false
            formBinding.btnTypeFound.setBackgroundResource(R.drawable.bg_pill_accent)
            formBinding.btnTypeFound.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_ink))
            formBinding.btnTypeLost.background = null
            formBinding.btnTypeLost.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        val catChips: List<Pair<TextView, String>> = listOf(
            formBinding.chipLfKeys to "Keys",
            formBinding.chipLfElectronics to "Electronics",
            formBinding.chipLfWallet to "Wallet / ID",
            formBinding.chipLfBags to "Bags",
            formBinding.chipLfOther to "Other"
        )

        for ((view, cat) in catChips) {
            view.setOnClickListener {
                selectedCategory = cat
                for ((v, c) in catChips) {
                    if (c == selectedCategory) {
                        v.setBackgroundResource(R.drawable.bg_chip_accent_filled)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        fun updatePreview(url: String) {
            if (url.isNotBlank()) {
                formBinding.flLfPreviewContainer.visibility = View.VISIBLE
                formBinding.ivLfPreview.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.bg_gallery_luxury_gradient)
                    error(R.drawable.bg_gallery_luxury_gradient)
                }
            } else {
                formBinding.flLfPreviewContainer.visibility = View.GONE
            }
        }

        updatePreview(formBinding.etLfImage.text.toString())
        formBinding.etLfImage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Pick / Upload from Gallery
        val launchLfPicker = View.OnClickListener {
            onImagePicked = { uri ->
                formBinding.etLfImage.setText(uri.toString())
                updatePreview(uri.toString())
                Toast.makeText(requireContext(), "Image selected from gallery", Toast.LENGTH_SHORT).show()
            }
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        formBinding.btnLfUploadImage.setOnClickListener(launchLfPicker)
        formBinding.flLfPreviewContainer.setOnClickListener(launchLfPicker)

        formBinding.btnCloseReport.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitReport.setOnClickListener {
            val title = formBinding.etLfTitle.text.toString().trim()
            val area = formBinding.etLfArea.text.toString().trim()
            val phone = formBinding.etLfPhone.text.toString().trim()
            val desc = formBinding.etLfDescription.text.toString().trim()
            val imgUrl = formBinding.etLfImage.text.toString().trim()

            if (title.isBlank()) {
                formBinding.tilLfTitle.error = "Please enter item title"
                return@setOnClickListener
            }
            formBinding.tilLfTitle.error = null

            if (area.isBlank()) {
                formBinding.tilLfArea.error = "Please enter area"
                return@setOnClickListener
            }
            formBinding.tilLfArea.error = null

            if(phone.isBlank()) { formBinding.tilLfPhone.error="Enter a contact phone number";return@setOnClickListener }
            val newItem = LostFoundItem(
                title = title,
                isLost = isLostSelection,
                category = selectedCategory,
                area = area,
                date = "Today",
                description = if (desc.isNotBlank()) desc else "Reported in $area.",
                status = "open",
                images = if (imgUrl.isNotBlank()) listOf(imgUrl) else emptyList(),
                contactPhone = phone
            )

            submitForReview(formBinding.btnSubmitReport,dialog) { HoodeRepository.postLostFound(newItem) }
        }

        dialog.show()
    }

    private fun showPostBloodDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = DialogFormBloodRequestBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedBlood = "O+"
        val groupChips: List<Pair<TextView, String>> = listOf(
            formBinding.chipBgOpos to "O+",
            formBinding.chipBgApos to "A+",
            formBinding.chipBgBpos to "B+",
            formBinding.chipBgAbpos to "AB+",
            formBinding.chipBgOneg to "O-",
            formBinding.chipBgAneg to "A-",
            formBinding.chipBgBneg to "B-",
            formBinding.chipBgAbneg to "AB-"
        )

        for ((chipView, groupName) in groupChips) {
            chipView.setOnClickListener {
                selectedBlood = groupName
                for ((v, name) in groupChips) {
                    if (name == selectedBlood) {
                        v.setBackgroundResource(R.drawable.bg_chip_accent_filled)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        formBinding.btnCloseBloodReq.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitBloodReq.setOnClickListener {
            val hospital = formBinding.etBloodHospital.text.toString().trim()
            val units = formBinding.etBloodUnits.text.toString().trim().toIntOrNull() ?: 1
            val phone = formBinding.etBloodPhone.text.toString().trim()

            if (hospital.isBlank()) {
                formBinding.tilBloodHospital.error = "Please enter hospital name"
                return@setOnClickListener
            }
            formBinding.tilBloodHospital.error = null

            if (phone.isBlank()) {
                formBinding.tilBloodPhone.error = "Please enter emergency phone"
                return@setOnClickListener
            }
            formBinding.tilBloodPhone.error = null

            val newReq = BloodRequest(
                bloodGroup = selectedBlood,
                patientNamePlaceholder = formBinding.etBloodPatient.text.toString().trim().ifBlank { "Patient in need" },
                hospital = hospital,
                unitsNeeded = units,
                neededBy = "Immediate Emergency",
                coordinatorPhone = phone
            )
            submitForReview(formBinding.btnSubmitBloodReq,dialog) { HoodeRepository.postBloodRequest(newReq) }
        }

        dialog.show()
    }

    private fun showReportCivicDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = DialogFormReportCivicBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedCategory = "Streetlights"
        val categoryChips: List<Pair<TextView, String>> = listOf(
            formBinding.chipCivicLights to "Streetlights",
            formBinding.chipCivicRoads to "Roads & Potholes",
            formBinding.chipCivicWaste to "Waste / Trash",
            formBinding.chipCivicDrainage to "Drainage & Water",
            formBinding.chipCivicBeach to "Beach Cleanliness"
        )

        for ((chipView, catName) in categoryChips) {
            chipView.setOnClickListener {
                selectedCategory = catName
                for ((v, name) in categoryChips) {
                    if (name == selectedCategory) {
                        v.setBackgroundResource(R.drawable.bg_chip_accent_filled)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        formBinding.btnCloseReportCivic.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitCivic.setOnClickListener {
            val title = formBinding.etCivicTitle.text.toString().trim()
            val loc = formBinding.etCivicLocation.text.toString().trim()

            if (title.isBlank()) {
                formBinding.tilCivicTitle.error = "Please enter issue summary"
                return@setOnClickListener
            }
            formBinding.tilCivicTitle.error = null

            if (loc.isBlank()) {
                formBinding.tilCivicLocation.error = "Please enter location"
                return@setOnClickListener
            }
            formBinding.tilCivicLocation.error = null

            submitForReview(formBinding.btnSubmitCivic,dialog) { HoodeRepository.submitCivicIssue(title,selectedCategory,loc,formBinding.etCivicDetails.text.toString().trim()) }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

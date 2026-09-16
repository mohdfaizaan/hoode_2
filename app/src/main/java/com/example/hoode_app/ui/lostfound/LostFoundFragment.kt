package com.example.hoode_app.ui.lostfound

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.hoode_app.R
import com.example.hoode_app.data.model.LostFoundItem
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentLostFoundBinding
import com.example.hoode_app.databinding.ItemLostFoundCardBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LostFoundFragment : Fragment() {

    private var _binding: FragmentLostFoundBinding? = null
    private val binding get() = _binding!!
    private var activeFilter = "ALL" // ALL, LOST, FOUND

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLostFoundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.tabAll.setOnClickListener { setTab("ALL") }
        binding.tabLost.setOnClickListener { setTab("LOST") }
        binding.tabFound.setOnClickListener { setTab("FOUND") }

        binding.btnReportItem.setOnClickListener {
            showReportItemDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.lostFound.collectLatest { list ->
                renderList(list)
            }
        }
    }

    private fun setTab(tab: String) {
        activeFilter = tab
        binding.tabAll.setBackgroundResource(if (tab == "ALL") R.drawable.bg_chip_black_border else 0)
        binding.tabAll.setTextColor(ContextCompat.getColor(requireContext(), if (tab == "ALL") R.color.text_primary else R.color.text_secondary))

        binding.tabLost.setBackgroundResource(if (tab == "LOST") R.drawable.bg_chip_black_border else 0)
        binding.tabLost.setTextColor(ContextCompat.getColor(requireContext(), if (tab == "LOST") R.color.text_primary else R.color.text_secondary))

        binding.tabFound.setBackgroundResource(if (tab == "FOUND") R.drawable.bg_chip_black_border else 0)
        binding.tabFound.setTextColor(ContextCompat.getColor(requireContext(), if (tab == "FOUND") R.color.text_primary else R.color.text_secondary))

        renderList(HoodeRepository.lostFound.value)
    }

    private fun renderList(all: List<LostFoundItem>) {
        val filtered = when (activeFilter) {
            "LOST" -> all.filter { it.isLost }
            "FOUND" -> all.filter { !it.isLost }
            else -> all
        }

        binding.llLostFoundContainer.removeAllViews()
        for (item in filtered) {
            val itemBinding = ItemLostFoundCardBinding.inflate(layoutInflater, binding.llLostFoundContainer, false)

            // Image loading
            val firstImage = item.images.firstOrNull()
            if (!firstImage.isNullOrBlank()) {
                itemBinding.ivItemImage.load(firstImage) {
                    crossfade(true)
                    placeholder(R.drawable.bg_gallery_luxury_gradient)
                    error(R.drawable.bg_gallery_luxury_gradient)
                }
            } else {
                itemBinding.ivItemImage.setImageResource(R.drawable.bg_gallery_luxury_gradient)
            }

            // Multi-image count indicator
            if (item.images.size > 1) {
                itemBinding.tvItemImagesCount.visibility = View.VISIBLE
                itemBinding.tvItemImagesCount.text = "📷 ${item.images.size}"
            } else {
                itemBinding.tvItemImagesCount.visibility = View.GONE
            }

            // Type badge
            itemBinding.tvItemTypeBadge.text = if (item.isLost) "LOST" else "FOUND"
            itemBinding.tvItemTypeBadge.setBackgroundResource(if (item.isLost) R.drawable.bg_pill_danger else R.drawable.bg_pill_accent)
            itemBinding.tvItemTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), if (item.isLost) R.color.danger else R.color.accent_ink))

            // Badges & details
            itemBinding.tvItemCategory.text = item.category
            itemBinding.tvItemStatus.text = item.status.replace("_", " ").uppercase()
            itemBinding.tvItemTitle.text = item.title
            itemBinding.tvItemDateArea.text = "${item.date} • ${item.area}"
            itemBinding.tvItemDescription.text = item.description

            // Tapping card opens the rich detail modal with multi-image carousel
            itemBinding.root.setOnClickListener {
                showItemDetailModal(item)
            }

            binding.llLostFoundContainer.addView(itemBinding.root)
        }
    }

    private fun showItemDetailModal(item: LostFoundItem) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        dialog.setContentView(R.layout.dialog_lost_found_detail)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Close button
        dialog.findViewById<View>(R.id.btn_close_detail)?.setOnClickListener {
            dialog.dismiss()
        }

        // Image ViewPager2 Carousel
        val vpImages = dialog.findViewById<ViewPager2>(R.id.vp_detail_images)
        val tvIndex = dialog.findViewById<TextView>(R.id.tv_detail_image_index)

        val imageList = if (item.images.isNotEmpty()) item.images else listOf("")
        vpImages?.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_detail_image_slide, parent, false)
                return object : RecyclerView.ViewHolder(v) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val iv = holder.itemView.findViewById<ImageView>(R.id.iv_slide_image)
                val url = imageList[position]
                if (url.isNotBlank()) {
                    iv.load(url) {
                        crossfade(true)
                        placeholder(R.drawable.bg_gallery_luxury_gradient)
                        error(R.drawable.bg_gallery_luxury_gradient)
                    }
                } else {
                    iv.setImageResource(R.drawable.bg_gallery_luxury_gradient)
                }
            }
            override fun getItemCount(): Int = imageList.size
        }

        if (imageList.size > 1) {
            tvIndex?.visibility = View.VISIBLE
            tvIndex?.text = "1 / ${imageList.size}"
            vpImages?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    tvIndex?.text = "${position + 1} / ${imageList.size}"
                }
            })
        } else {
            tvIndex?.visibility = View.GONE
        }

        // Fill Details
        val tvTypeBadge = dialog.findViewById<TextView>(R.id.tv_detail_type_badge)
        tvTypeBadge?.text = if (item.isLost) "LOST ITEM" else "FOUND ITEM"
        tvTypeBadge?.setBackgroundResource(if (item.isLost) R.drawable.bg_pill_danger else R.drawable.bg_pill_accent)
        tvTypeBadge?.setTextColor(ContextCompat.getColor(requireContext(), if (item.isLost) R.color.danger else R.color.accent_ink))

        val tvStatusBadge = dialog.findViewById<TextView>(R.id.tv_detail_status_badge)
        tvStatusBadge?.text = item.status.replace("_", " ").uppercase()

        dialog.findViewById<TextView>(R.id.tv_detail_category)?.text = item.category
        dialog.findViewById<TextView>(R.id.tv_detail_title)?.text = item.title
        dialog.findViewById<TextView>(R.id.tv_detail_location)?.text = item.area
        dialog.findViewById<TextView>(R.id.tv_detail_date)?.text = item.date
        dialog.findViewById<TextView>(R.id.tv_detail_description)?.text = item.description

        // Buttons
        dialog.findViewById<MaterialButton>(R.id.btn_detail_claim)?.setOnClickListener {
            dialog.dismiss()
            showClaimDialog(item)
        }

        dialog.findViewById<MaterialButton>(R.id.btn_detail_call)?.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.contactPhone}"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Calling ${item.contactPhone}", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showClaimDialog(item: LostFoundItem) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormClaimItemBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        formBinding.tvClaimItemInfo.text = "${item.title} • Area: ${item.area} • Status: ${if (item.isLost) "Lost" else "Found"}"

        formBinding.btnCloseClaim.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitClaim.setOnClickListener {
            val proof = formBinding.etClaimProof.text.toString().trim()
            if (proof.isBlank()) {
                formBinding.tilClaimProof.error = "Please provide identifying ownership details"
                return@setOnClickListener
            }
            formBinding.tilClaimProof.error = null

            HoodeRepository.claimLostFound(item.id)
            Toast.makeText(requireContext(), "Private claim submitted. You will be notified once verified.", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showReportItemDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormReportLostFoundBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var isLostSelection = true
        var selectedCategory = "Keys"

        // Type selection toggle
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

        // Category chips
        val catChips = listOf(
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
                        v.setBackgroundResource(R.drawable.bg_chip_black_border)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        // Live photo preview
        formBinding.etLfImage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val url = s?.toString()?.trim() ?: ""
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
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        val currentUser = HoodeRepository.currentUser.value
        currentUser?.let {
            formBinding.etLfPhone.setText(it.phone ?: "")
        }

        formBinding.btnCloseReport.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitReport.setOnClickListener {
            val title = formBinding.etLfTitle.text.toString().trim()
            val area = formBinding.etLfArea.text.toString().trim()
            val desc = formBinding.etLfDescription.text.toString().trim()
            val img = formBinding.etLfImage.text.toString().trim()
            val phone = formBinding.etLfPhone.text.toString().trim()

            if (title.isBlank()) {
                formBinding.tilLfTitle.error = "Please enter item name"
                return@setOnClickListener
            }
            formBinding.tilLfTitle.error = null

            if (area.isBlank()) {
                formBinding.tilLfArea.error = "Please specify location/area"
                return@setOnClickListener
            }
            formBinding.tilLfArea.error = null

            if (desc.isBlank()) {
                formBinding.tilLfDescription.error = "Please describe the item"
                return@setOnClickListener
            }
            formBinding.tilLfDescription.error = null

            val imagesList = if (img.isNotBlank()) listOf(img) else emptyList()
            val newItem = LostFoundItem(
                title = title,
                isLost = isLostSelection,
                category = selectedCategory,
                area = area,
                date = "Today",
                description = desc,
                images = imagesList,
                contactPhone = phone.ifBlank { "+91 820 252 0100" }
            )

            HoodeRepository.postLostFound(newItem)
            Toast.makeText(requireContext(), "Community report submitted successfully!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

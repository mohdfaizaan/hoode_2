package com.example.hoode_app.ui.activities

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.example.hoode_app.ui.common.showEmptyContent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.model.CommunityActivity
import com.example.hoode_app.data.model.DuaItem
import com.example.hoode_app.data.model.EssentialDuas
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.DialogDuaDetailBinding
import com.example.hoode_app.databinding.FragmentActivitiesBinding
import com.example.hoode_app.databinding.ItemActivityCardBinding
import com.example.hoode_app.databinding.ItemDuaCardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ActivitiesFragment : Fragment() {

    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!
    private var selectedCategory = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupCategoryFilters()

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.activities.collectLatest { list ->
                filterAndDisplay(list)
            }
        }
    }

    private fun setupCategoryFilters() {
        val filters = listOf(
            binding.filterActAll to "All",
            binding.filterActReligious to "Religious",
            binding.filterActCommunity to "Community",
            binding.filterActSocial to "Social",
            binding.filterActSports to "Sports",
            binding.filterActCondolences to "Condolences",
            binding.filterActDua to "Dua Request"
        )

        val targetCat = HoodeRepository.initialActivityCategory
        if (targetCat.isNotBlank() && targetCat != "All") {
            selectedCategory = targetCat
            HoodeRepository.initialActivityCategory = "All" // reset
        }

        val padH = (16 * resources.displayMetrics.density).toInt()
        val padV = (8 * resources.displayMetrics.density).toInt()

        fun updateUI() {
            for ((v, c) in filters) {
                if (c.equals(selectedCategory, ignoreCase = true)) {
                    v.setBackgroundResource(R.drawable.bg_chip_selected)
                    v.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_ink))
                } else {
                    v.setBackgroundResource(R.drawable.bg_chip_unselected)
                    v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                }
                v.setPadding(padH, padV, padH, padV)
            }
        }

        updateUI()

        for ((view, catName) in filters) {
            view.setOnClickListener {
                selectedCategory = catName
                updateUI()
                filterAndDisplay(HoodeRepository.activities.value)
            }
        }
    }

    private fun filterAndDisplay(all: List<CommunityActivity>) {
        binding.llActivitiesContainer.removeAllViews()

        if (selectedCategory.equals("Dua Request", ignoreCase = true)) {
            // Render Essential Memorizable Duas List
            for (dua in EssentialDuas.list) {
                val itemBinding = ItemDuaCardBinding.inflate(layoutInflater, binding.llActivitiesContainer, false)
                itemBinding.tvDuaItemTitle.text = dua.title
                itemBinding.tvDuaItemCategory.text = dua.category
                itemBinding.root.setOnClickListener {
                    showDuaDetailDialog(dua)
                }
                binding.llActivitiesContainer.addView(itemBinding.root)
            }
        } else {
            val filtered = if (selectedCategory == "All") all else all.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            if (filtered.isEmpty()) binding.llActivitiesContainer.showEmptyContent("No activities to show")
            for (act in filtered) {
                val itemBinding = ItemActivityCardBinding.inflate(layoutInflater, binding.llActivitiesContainer, false)
                itemBinding.tvActCategory.text = act.category
                itemBinding.tvActStatus.text = act.status.replaceFirstChar { it.uppercase() }
                itemBinding.tvActTitle.text = act.title
                itemBinding.tvActSchedule.text = "${act.schedule} • ${act.venue}"
                itemBinding.tvActOrganizer.text = "Organizer: ${act.organizer}"
                itemBinding.tvActDescription.text = act.description
                binding.llActivitiesContainer.addView(itemBinding.root)
            }
        }
    }

    private fun showDuaDetailDialog(dua: DuaItem) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogDuaDetailBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.tvDuaTitle.text = dua.title
        dialogBinding.tvDuaCategory.text = dua.category
        dialogBinding.tvDuaArabic.text = dua.arabic
        dialogBinding.tvDuaTransliteration.text = dua.transliteration
        dialogBinding.tvDuaTranslation.text = dua.translation
        dialogBinding.tvDuaReference.text = "Reference: ${dua.reference}"

        dialogBinding.btnCloseDua.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.btnDoneDua.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnCopyDua.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                dua.title,
                "${dua.title}\n\n${dua.arabic}\n\n${dua.transliteration}\n\n${dua.translation}\n(${dua.reference})"
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Dua copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

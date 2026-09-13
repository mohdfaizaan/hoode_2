package com.example.hoode_app.ui.lostfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.model.LostFoundItem
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentLostFoundBinding
import com.example.hoode_app.databinding.ItemLostFoundCardBinding
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
        binding.tabAll.setBackgroundResource(if (tab == "ALL") R.drawable.bg_pill_accent else 0)
        binding.tabAll.setTextColor(ContextCompat.getColor(requireContext(), if (tab == "ALL") R.color.border_primary else R.color.text_secondary))

        binding.tabLost.setBackgroundResource(if (tab == "LOST") R.drawable.bg_pill_accent else 0)
        binding.tabLost.setTextColor(ContextCompat.getColor(requireContext(), if (tab == "LOST") R.color.border_primary else R.color.text_secondary))

        binding.tabFound.setBackgroundResource(if (tab == "FOUND") R.drawable.bg_pill_accent else 0)
        binding.tabFound.setTextColor(ContextCompat.getColor(requireContext(), if (tab == "FOUND") R.color.border_primary else R.color.text_secondary))

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
            itemBinding.tvItemTypeBadge.text = if (item.isLost) "LOST" else "FOUND"
            itemBinding.tvItemTypeBadge.setBackgroundResource(if (item.isLost) R.drawable.bg_pill_danger else R.drawable.bg_pill_accent)
            itemBinding.tvItemTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), if (item.isLost) R.color.danger else R.color.accent_ink))

            itemBinding.tvItemCategory.text = item.category
            itemBinding.tvItemStatus.text = item.status.replace("_", " ").uppercase()
            itemBinding.tvItemTitle.text = item.title
            itemBinding.tvItemDateArea.text = "${item.date} • ${item.area}"
            itemBinding.tvItemDescription.text = item.description

            itemBinding.btnClaimItem.setOnClickListener {
                showClaimDialog(item)
            }

            binding.llLostFoundContainer.addView(itemBinding.root)
        }
    }

    private fun showClaimDialog(item: LostFoundItem) {
        val input = EditText(requireContext()).apply {
            hint = "Describe identifying details / proof of ownership"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Claim: ${item.title}")
            .setMessage("Your identifying answer will be privately verified by the coordinator or finder.")
            .setView(input)
            .setPositiveButton("Submit Claim") { _, _ ->
                val proof = input.text.toString().trim()
                if (proof.isNotBlank()) {
                    HoodeRepository.claimLostFound(item.id)
                    Toast.makeText(requireContext(), "Private claim submitted. You will be notified once verified.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReportItemDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val etTitle = EditText(requireContext()).apply { hint = "Item Name" }
        val etCategory = EditText(requireContext()).apply { hint = "Category (Keys, Electronics, Bags, etc.)" }
        val etArea = EditText(requireContext()).apply { hint = "Approximate Area (e.g. Hoode Beach)" }
        val etDesc = EditText(requireContext()).apply { hint = "Description (without private identifying numbers)" }
        layout.addView(etTitle)
        layout.addView(etCategory)
        layout.addView(etArea)
        layout.addView(etDesc)

        AlertDialog.Builder(requireContext())
            .setTitle("Report Lost / Found Item")
            .setView(layout)
            .setPositiveButton("Submit") { _, _ ->
                val title = etTitle.text.toString().trim()
                val cat = etCategory.text.toString().trim()
                val area = etArea.text.toString().trim()
                val desc = etDesc.text.toString().trim()

                if (title.isNotBlank()) {
                    val newItem = LostFoundItem(
                        title = title,
                        isLost = true,
                        category = if (cat.isNotBlank()) cat else "General",
                        area = if (area.isNotBlank()) area else "Hoode",
                        date = "Today",
                        description = desc
                    )
                    HoodeRepository.postLostFound(newItem)
                    Toast.makeText(requireContext(), "Report submitted to community board!", Toast.LENGTH_SHORT).show()
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

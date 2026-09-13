package com.example.hoode_app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentAdminCarouselBinding

class AdminCarouselFragment : Fragment() {

    private var _binding: FragmentAdminCarouselBinding? = null
    private val binding get() = _binding!!
    private var selectedSlot = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminCarouselBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupSlotButtons()
        loadSlot(1)

        binding.btnSaveAdSlide.setOnClickListener {
            val headline = binding.etAdHeadline.text.toString().trim()
            val sub = binding.etAdSubheadline.text.toString().trim()
            val adv = binding.etAdAdvertiser.text.toString().trim()

            if (headline.isNotBlank() && adv.isNotBlank()) {
                HoodeRepository.updateAdSlide(selectedSlot, headline, sub, adv)
                Toast.makeText(requireContext(), "Slide slot $selectedSlot updated! Home carousel reflects changes.", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "Please enter headline and advertiser name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSlotButtons() {
        val slots = listOf(
            binding.btnSlot1 to 1,
            binding.btnSlot2 to 2,
            binding.btnSlot3 to 3,
            binding.btnSlot4 to 4,
            binding.btnSlot5 to 5
        )

        for ((view, slotNum) in slots) {
            view.setOnClickListener {
                selectedSlot = slotNum
                for ((v, s) in slots) {
                    if (s == selectedSlot) {
                        v.setBackgroundResource(R.drawable.bg_chip_selected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.border_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
                loadSlot(selectedSlot)
            }
        }
    }

    private fun loadSlot(slot: Int) {
        val slide = HoodeRepository.adSlides.value.find { it.slotIndex == slot }
        if (slide != null) {
            binding.etAdHeadline.setText(slide.headline)
            binding.etAdSubheadline.setText(slide.subheadline)
            binding.etAdAdvertiser.setText(slide.advertiser)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

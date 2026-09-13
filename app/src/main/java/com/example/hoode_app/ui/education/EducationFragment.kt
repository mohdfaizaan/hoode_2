package com.example.hoode_app.ui.education

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentEducationBinding
import com.example.hoode_app.databinding.ItemEducationCardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EducationFragment : Fragment() {

    private var _binding: FragmentEducationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEducationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.educationOfferings.collectLatest { list ->
                binding.llEducationContainer.removeAllViews()
                for (item in list) {
                    val cardBinding = ItemEducationCardBinding.inflate(layoutInflater, binding.llEducationContainer, false)
                    cardBinding.tvEduCategory.text = item.category
                    cardBinding.tvEduFee.text = item.feeDescription
                    cardBinding.tvEduTitle.text = item.title
                    cardBinding.tvEduProviderAudience.text = "${item.provider} • ${item.audience}"
                    cardBinding.tvEduSchedule.text = "${item.schedule} • Contact: ${item.contact}"
                    cardBinding.tvEduDescription.text = item.description
                    binding.llEducationContainer.addView(cardBinding.root)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

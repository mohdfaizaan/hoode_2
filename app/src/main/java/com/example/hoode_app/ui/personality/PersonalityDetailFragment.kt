package com.example.hoode_app.ui.personality

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentPersonalityDetailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PersonalityDetailFragment : Fragment() {

    private var _binding: FragmentPersonalityDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.dailyPersonality.collectLatest { p ->
                binding.tvFeaturedDate.text = p.featuredDate
                binding.tvPersonName.text = p.name
                binding.tvPersonRole.text = p.role
                binding.tvPersonQuote.text = p.quote
                binding.tvPersonBiography.text = p.fullBiography

                if (p.imageUrl.isNotBlank()) {
                    binding.ivPersonAvatar.load(p.imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.bg_circle_lavender)
                        error(R.drawable.bg_circle_lavender)
                    }
                }

                binding.llContributionsList.removeAllViews()
                for (item in p.contributions) {
                    val tv = TextView(requireContext()).apply {
                        text = "• $item"
                        textSize = 14f
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        setPadding(0, 8, 0, 8)
                    }
                    binding.llContributionsList.addView(tv)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

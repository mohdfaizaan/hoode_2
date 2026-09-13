package com.example.hoode_app.ui.badges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentBadgesBinding
import com.example.hoode_app.databinding.ItemBadgeCardBinding
import com.example.hoode_app.databinding.ItemContributionRowBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BadgesFragment : Fragment() {

    private var _binding: FragmentBadgesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBadgesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.totalPoints.collectLatest { points ->
                binding.tvTotalPoints.text = points.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.badges.collectLatest { badgeList ->
                binding.llBadgesContainer.removeAllViews()
                for (badge in badgeList) {
                    val cardBinding = ItemBadgeCardBinding.inflate(layoutInflater, binding.llBadgesContainer, false)
                    cardBinding.tvBadgeTitle.text = badge.title
                    cardBinding.tvBadgeDescription.text = badge.description
                    if (badge.isUnlocked) {
                        cardBinding.tvBadgeStatus.text = "✓ UNLOCKED"
                        cardBinding.tvBadgeStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                    } else {
                        cardBinding.tvBadgeStatus.text = "LOCKED"
                        cardBinding.tvBadgeStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                    }
                    binding.llBadgesContainer.addView(cardBinding.root)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.contributions.collectLatest { list ->
                binding.llContributionsContainer.removeAllViews()
                for (record in list) {
                    val rowBinding = ItemContributionRowBinding.inflate(layoutInflater, binding.llContributionsContainer, false)
                    rowBinding.tvContribAction.text = record.action
                    rowBinding.tvContribDate.text = record.date
                    rowBinding.tvContribPoints.text = "+${record.points} pts"
                    binding.llContributionsContainer.addView(rowBinding.root)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

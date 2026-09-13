package com.example.hoode_app.ui.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.databinding.FragmentCreateBinding

class CreateFragment : Fragment() {

    private var _binding: FragmentCreateBinding? = null
    private val binding get() = _binding!!

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
            findNavController().navigate(R.id.marketplaceFragment)
        }
        binding.cardCreateEvent.setOnClickListener {
            findNavController().navigate(R.id.eventsFragment)
        }
        binding.cardCreateJob.setOnClickListener {
            findNavController().navigate(R.id.jobsFragment)
        }
        binding.cardCreateLostFound.setOnClickListener {
            findNavController().navigate(R.id.lostFoundFragment)
        }
        binding.cardCreateBlood.setOnClickListener {
            findNavController().navigate(R.id.bloodNetworkFragment)
        }
        binding.cardCreateCivic.setOnClickListener {
            findNavController().navigate(R.id.pollsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.hoode_app.ui.ramadan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentRamadanBinding
import com.example.hoode_app.databinding.ItemRamadanRowBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RamadanFragment : Fragment() {

    private var _binding: FragmentRamadanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRamadanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.ramadanTimetable.collectLatest { list ->
                binding.llRamadanTableContainer.removeAllViews()
                for (item in list) {
                    val rowBinding = ItemRamadanRowBinding.inflate(layoutInflater, binding.llRamadanTableContainer, false)
                    rowBinding.tvRamadanDay.text = "Day ${item.day}"
                    rowBinding.tvRamadanSuhoor.text = "Suhoor: ${item.suhoorEnd}"
                    rowBinding.tvRamadanIftar.text = "Iftar: ${item.iftarTime}"
                    binding.llRamadanTableContainer.addView(rowBinding.root)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

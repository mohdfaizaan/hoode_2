package com.example.hoode_app.ui.huffaz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentHuffazBinding
import com.example.hoode_app.databinding.ItemHuffazCardBinding
import coil.load
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HuffazFragment : Fragment() {

    private var _binding: FragmentHuffazBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHuffazBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.huffazList.collectLatest { list ->
                binding.llHuffazContainer.removeAllViews()
                for (huffaz in list) {
                    val cardBinding = ItemHuffazCardBinding.inflate(layoutInflater, binding.llHuffazContainer, false)
                    cardBinding.tvHuffazName.text = huffaz.name
                    cardBinding.tvHuffazYear.text = "Completed ${huffaz.completionYear}"
                    cardBinding.tvHuffazInstitutionTeacher.text = "${huffaz.institution} • Ustad: ${huffaz.teacher}"
                    cardBinding.tvHuffazBio.text = huffaz.biography
                    if (huffaz.imageUrl.isNotBlank()) {
                        cardBinding.ivHafizAvatar.load(huffaz.imageUrl) {
                            crossfade(true)
                            placeholder(com.example.hoode_app.R.drawable.bg_circle_lavender)
                            error(com.example.hoode_app.R.drawable.bg_circle_lavender)
                        }
                    }
                    binding.llHuffazContainer.addView(cardBinding.root)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

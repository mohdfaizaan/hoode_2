package com.example.hoode_app.ui.providers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentProvidersBinding
import com.example.hoode_app.databinding.ItemProviderCardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProvidersFragment : Fragment() {

    private var _binding: FragmentProvidersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProvidersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.providers.collectLatest { list ->
                binding.llProvidersContainer.removeAllViews()
                for (provider in list) {
                    val itemBinding = ItemProviderCardBinding.inflate(layoutInflater, binding.llProvidersContainer, false)
                    itemBinding.tvProviderCategory.text = provider.category
                    itemBinding.tvProviderRating.text = "★ ${provider.rating} (${provider.reviewCount} reviews)"
                    itemBinding.tvProviderName.text = provider.name
                    itemBinding.tvProviderAreaHours.text = "${provider.area} • ${provider.availability}"
                    itemBinding.tvProviderPhone.text = provider.phone

                    itemBinding.btnCallProvider.setOnClickListener {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone.replace(" ", "")}"))
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Phone: ${provider.phone}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    binding.llProvidersContainer.addView(itemBinding.root)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

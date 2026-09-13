package com.example.hoode_app.ui.emergency

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.model.EmergencyContact
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentEmergencyBinding
import com.example.hoode_app.databinding.ItemEmergencyContactBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EmergencyFragment : Fragment() {

    private var _binding: FragmentEmergencyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmergencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSuggestCorrection.setOnClickListener {
            val input = EditText(requireContext())
            input.hint = "Detail new number or department correction"
            AlertDialog.Builder(requireContext())
                .setTitle("Suggest Directory Correction")
                .setView(input)
                .setPositiveButton("Submit") { _, _ ->
                    val text = input.text.toString().trim()
                    if (text.isNotBlank()) {
                        Toast.makeText(requireContext(), "Thank you. Your correction was submitted for review.", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.emergencyContacts.collectLatest { contacts ->
                displayContacts(contacts)
            }
        }

        binding.etSearchContacts.doAfterTextChanged { query ->
            val q = query.toString().trim().lowercase()
            val all = HoodeRepository.emergencyContacts.value
            val filtered = if (q.isBlank()) all else all.filter {
                it.name.lowercase().contains(q) || it.category.lowercase().contains(q) || it.area.lowercase().contains(q)
            }
            displayContacts(filtered)
        }
    }

    private fun displayContacts(contacts: List<EmergencyContact>) {
        binding.llContactsContainer.removeAllViews()
        for (contact in contacts) {
            val itemBinding = ItemEmergencyContactBinding.inflate(layoutInflater, binding.llContactsContainer, false)
            itemBinding.tvContactCategory.text = contact.category
            itemBinding.tvContactName.text = contact.name
            itemBinding.tvContactArea.text = "${contact.area} • Verified ${contact.lastVerified}"
            itemBinding.tvContactPhone.text = contact.phone

            itemBinding.btnDialPhone.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone.replace(" ", "")}"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Dialer unavailable: ${contact.phone}", Toast.LENGTH_SHORT).show()
                }
            }

            binding.llContactsContainer.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

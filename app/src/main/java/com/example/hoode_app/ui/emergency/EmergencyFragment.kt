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
            val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
            val formBinding = com.example.hoode_app.databinding.DialogFormSuggestCorrectionBinding.inflate(layoutInflater)
            dialog.setContentView(formBinding.root)

            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.94).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            formBinding.btnCloseSuggestCorrection.setOnClickListener {
                dialog.dismiss()
            }

            formBinding.btnSubmitCorrection.setOnClickListener {
                val service = formBinding.etCorrectionService.text.toString().trim()
                val phone = formBinding.etCorrectionPhone.text.toString().trim()
                val details = formBinding.etCorrectionDetails.text.toString().trim()

                if (service.isBlank()) {
                    formBinding.tilCorrectionService.error = "Please enter service name"
                    return@setOnClickListener
                }
                formBinding.tilCorrectionService.error = null

                if (phone.isBlank()) {
                    formBinding.tilCorrectionPhone.error = "Please enter contact number"
                    return@setOnClickListener
                }
                formBinding.tilCorrectionPhone.error = null

                Toast.makeText(requireContext(), "Thank you! Correction for '$service' submitted for verification.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }

            dialog.show()
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

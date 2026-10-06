package com.example.hoode_app.ui.blood

import android.content.Intent
import android.net.Uri
import com.example.hoode_app.ui.common.submitForReview
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.model.BloodRequest
import com.example.hoode_app.data.model.DonorRegistration
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentBloodNetworkBinding
import com.example.hoode_app.databinding.ItemBloodRequestCardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BloodNetworkFragment : Fragment() {

    private var _binding: FragmentBloodNetworkBinding? = null
    private val binding get() = _binding!!

    private var selectedGroup: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBloodNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnJoinDonorRegistry.setOnClickListener {
            showDonorRegistrationDialog()
        }

        binding.btnRequestBlood.setOnClickListener {
            showCreateBloodRequestDialog()
        }

        setupGroupChips()

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.bloodRequests.collectLatest {
                renderRequests()
            }
        }
    }

    private fun setupGroupChips() {
        val chips = listOf(
            binding.chipBloodAll to "All",
            binding.chipBloodOPos to "O+",
            binding.chipBloodBPos to "B+",
            binding.chipBloodAPos to "A+",
            binding.chipBloodAbPos to "AB+",
            binding.chipBloodNeg to "Negative"
        )

        for ((chipView, group) in chips) {
            chipView.setOnClickListener {
                selectedGroup = group
                for ((v, g) in chips) {
                    if (g == selectedGroup) {
                        v.setBackgroundResource(R.drawable.bg_chip_selected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
                renderRequests()
            }
        }
    }

    private fun renderRequests() {
        val all = HoodeRepository.bloodRequests.value
        val filtered = all.filter { req ->
            if (selectedGroup == "All") true
            else if (selectedGroup == "Negative") req.bloodGroup.contains("-")
            else req.bloodGroup.equals(selectedGroup, ignoreCase = true)
        }

        binding.llBloodRequestsContainer.removeAllViews()

        if (filtered.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "No requests to show for this blood group. Check another group or refresh the community feed."
                textSize = 13f
                setPadding(24, 48, 24, 24)
                gravity = android.view.Gravity.CENTER
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            }
            binding.llBloodRequestsContainer.addView(emptyTv)
            return
        }

        for (request in filtered) {
            val itemBinding = ItemBloodRequestCardBinding.inflate(layoutInflater, binding.llBloodRequestsContainer, false)
            itemBinding.tvBloodGroup.text = request.bloodGroup
            itemBinding.tvBloodHospital.text = request.hospital
            itemBinding.tvBloodNeededBy.text = "Needed: ${request.neededBy} • ${request.unitsNeeded} Unit(s)"

            itemBinding.btnContactCoordinator.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${request.coordinatorPhone.replace(" ", "")}"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Coordinator phone: ${request.coordinatorPhone}", Toast.LENGTH_SHORT).show()
                }
            }

            binding.llBloodRequestsContainer.addView(itemBinding.root)
        }
    }

    private fun showCreateBloodRequestDialog() {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormBloodRequestBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedBlood = "O+"
        val groupChips = listOf(
            formBinding.chipBgOpos to "O+",
            formBinding.chipBgApos to "A+",
            formBinding.chipBgBpos to "B+",
            formBinding.chipBgAbpos to "AB+",
            formBinding.chipBgOneg to "O-",
            formBinding.chipBgAneg to "A-",
            formBinding.chipBgBneg to "B-",
            formBinding.chipBgAbneg to "AB-"
        )

        for ((chipView, groupName) in groupChips) {
            chipView.setOnClickListener {
                selectedBlood = groupName
                for ((v, name) in groupChips) {
                    if (name == selectedBlood) {
                        v.setBackgroundResource(R.drawable.bg_chip_black_border)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        formBinding.btnCloseBloodReq.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitBloodReq.setOnClickListener {
            val patient = formBinding.etBloodPatient.text.toString().trim()
            val hospital = formBinding.etBloodHospital.text.toString().trim()
            val units = formBinding.etBloodUnits.text.toString().trim().toIntOrNull() ?: 1
            val phone = formBinding.etBloodPhone.text.toString().trim()

            if (hospital.isBlank()) {
                formBinding.tilBloodHospital.error = "Please enter hospital name"
                return@setOnClickListener
            }
            formBinding.tilBloodHospital.error = null

            if (phone.isBlank()) {
                formBinding.tilBloodPhone.error = "Please enter coordinator/emergency phone"
                return@setOnClickListener
            }
            formBinding.tilBloodPhone.error = null

            val newReq = BloodRequest(
                bloodGroup = selectedBlood,
                patientNamePlaceholder=patient.ifBlank{"Patient in need"},
                hospital = hospital,
                unitsNeeded = units,
                neededBy = "Immediate Emergency",
                coordinatorPhone = phone
            )
            submitForReview(formBinding.btnSubmitBloodReq,dialog) { HoodeRepository.postBloodRequest(newReq) }
        }

        dialog.show()
    }

    private fun showDonorRegistrationDialog() {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormRegisterDonorBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val currentUser = HoodeRepository.currentUser.value
        currentUser?.let {
            formBinding.etDonorName.setText(it.displayName)
            formBinding.etDonorPhone.setText(it.phone ?: "")
            if (!it.locality.isNullOrBlank()) {
                formBinding.etDonorArea.setText(it.locality)
            }
        }

        var selectedBlood = "O+"
        val groupChips = listOf(
            formBinding.chipDonorOpos to "O+",
            formBinding.chipDonorApos to "A+",
            formBinding.chipDonorBpos to "B+",
            formBinding.chipDonorAbpos to "AB+",
            formBinding.chipDonorOneg to "O-",
            formBinding.chipDonorAneg to "A-",
            formBinding.chipDonorBneg to "B-",
            formBinding.chipDonorAbneg to "AB-"
        )

        for ((chipView, groupName) in groupChips) {
            chipView.setOnClickListener {
                selectedBlood = groupName
                for ((v, name) in groupChips) {
                    if (name == selectedBlood) {
                        v.setBackgroundResource(R.drawable.bg_chip_black_border)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        formBinding.btnCloseRegisterDonor.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitDonor.setOnClickListener {
            val name = formBinding.etDonorName.text.toString().trim()
            val area = formBinding.etDonorArea.text.toString().trim()
            val phone = formBinding.etDonorPhone.text.toString().trim()

            if (name.isBlank()) {
                formBinding.tilDonorName.error = "Please enter your name"
                return@setOnClickListener
            }
            formBinding.tilDonorName.error = null

            if (phone.isBlank()) {
                formBinding.tilDonorPhone.error = "Please enter your phone"
                return@setOnClickListener
            }
            formBinding.tilDonorPhone.error = null

            submitForReview(formBinding.btnSubmitDonor,dialog) { HoodeRepository.registerDonor(DonorRegistration(name = name, bloodGroup = selectedBlood, area = if (area.isNotBlank()) area else "Hoode", phone = phone)) }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

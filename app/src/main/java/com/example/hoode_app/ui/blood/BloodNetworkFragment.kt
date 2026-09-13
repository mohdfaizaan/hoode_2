package com.example.hoode_app.ui.blood

import android.content.Intent
import android.net.Uri
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
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.border_primary))
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
                text = "No urgent requests for $selectedGroup blood at this time.\nAlhamdulillah, supplies are stable."
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
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val etPatient = EditText(requireContext()).apply { hint = "Patient / Attender Name" }
        val etGroup = EditText(requireContext()).apply { hint = "Blood Group (e.g. O+, B-, AB+)" }
        val etHospital = EditText(requireContext()).apply { hint = "Hospital (e.g. Adarsh Hospital, Manipal)" }
        val etUnits = EditText(requireContext()).apply { hint = "Units Required (e.g. 2)" }
        val etPhone = EditText(requireContext()).apply { hint = "Emergency Contact Phone" }

        layout.addView(etPatient)
        layout.addView(etGroup)
        layout.addView(etHospital)
        layout.addView(etUnits)
        layout.addView(etPhone)

        AlertDialog.Builder(requireContext())
            .setTitle("Post Emergency Blood Request")
            .setMessage("Coordinator verification is enforced to prevent spam and duplicate calls.")
            .setView(layout)
            .setPositiveButton("Broadcast Alert") { _, _ ->
                val group = etGroup.text.toString().trim().uppercase()
                val hospital = etHospital.text.toString().trim()
                val units = etUnits.text.toString().trim().toIntOrNull() ?: 1
                val phone = etPhone.text.toString().trim()

                if (group.isNotBlank() && hospital.isNotBlank()) {
                    val newReq = BloodRequest(
                        bloodGroup = group,
                        hospital = hospital,
                        unitsNeeded = units,
                        neededBy = "Immediate Emergency",
                        coordinatorPhone = if (phone.isNotBlank()) phone else "+91 98450 99887"
                    )
                    HoodeRepository.postBloodRequest(newReq)
                    Toast.makeText(requireContext(), "Emergency blood call broadcasted to verified donors!", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDonorRegistrationDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val etName = EditText(requireContext()).apply { hint = "Full Name" }
        val etGroup = EditText(requireContext()).apply { hint = "Blood Group (e.g. O+, A+, B-, etc.)" }
        val etArea = EditText(requireContext()).apply { hint = "Area in Hoode (e.g. Beach Road, Bengre)" }
        val etPhone = EditText(requireContext()).apply { hint = "Contact Phone / WhatsApp" }
        layout.addView(etName)
        layout.addView(etGroup)
        layout.addView(etArea)
        layout.addView(etPhone)

        AlertDialog.Builder(requireContext())
            .setTitle("Volunteer as Blood Donor")
            .setMessage("Your contact information will remain private and will be used only by coordinators during urgent emergencies matching your blood group.")
            .setView(layout)
            .setPositiveButton("Confirm Opt-In") { _, _ ->
                val name = etName.text.toString().trim()
                val group = etGroup.text.toString().trim().uppercase()
                val area = etArea.text.toString().trim()
                val phone = etPhone.text.toString().trim()

                if (name.isNotBlank() && group.isNotBlank() && phone.isNotBlank()) {
                    HoodeRepository.registerDonor(DonorRegistration(name = name, bloodGroup = group, area = area, phone = phone))
                    Toast.makeText(requireContext(), "Thank you! You are now registered as a life-saving donor.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

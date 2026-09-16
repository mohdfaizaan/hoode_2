package com.example.hoode_app.ui.jobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.model.JobPosting
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentJobsBinding
import com.example.hoode_app.databinding.ItemJobCardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class JobsFragment : Fragment() {

    private var _binding: FragmentJobsBinding? = null
    private val binding get() = _binding!!
    private var selectedFilter = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupFilterTabs()

        binding.btnPostJob.setOnClickListener {
            showPostJobDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.jobs.collectLatest { jobsList ->
                filterAndDisplayJobs(jobsList)
            }
        }
    }

    private fun setupFilterTabs() {
        val filters = listOf(
            binding.filterAll to "All",
            binding.filterFullTime to "Full-time",
            binding.filterPartTime to "Part-time",
            binding.filterGig to "Gig",
            binding.filterVolunteer to "Volunteer"
        )

        for ((view, filterName) in filters) {
            view.setOnClickListener {
                selectedFilter = filterName
                for ((v, f) in filters) {
                    if (f == selectedFilter) {
                        v.setBackgroundResource(R.drawable.bg_chip_selected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.border_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
                filterAndDisplayJobs(HoodeRepository.jobs.value)
            }
        }
    }

    private fun filterAndDisplayJobs(allJobs: List<JobPosting>) {
        val filtered = if (selectedFilter == "All") allJobs else allJobs.filter { it.type == selectedFilter }
        binding.llJobsContainer.removeAllViews()

        for (job in filtered) {
            val itemBinding = ItemJobCardBinding.inflate(layoutInflater, binding.llJobsContainer, false)
            itemBinding.tvJobType.text = job.type
            itemBinding.tvJobTitle.text = job.title
            itemBinding.tvJobEmployerLocation.text = "${job.employer} • ${job.location}"
            itemBinding.tvJobDescription.text = job.description
            itemBinding.tvJobPay.text = job.pay
            itemBinding.tvJobDeadline.text = "Closes ${job.deadline}"
            itemBinding.tvJobApplicants.text = "${job.applicantsCount} applied"
            if (job.isSponsored) {
                itemBinding.tvJobSponsored.visibility = View.VISIBLE
            }

            itemBinding.btnApplyJob.setOnClickListener {
                showApplyDialog(job)
            }

            binding.llJobsContainer.addView(itemBinding.root)
        }
    }

    private fun showApplyDialog(job: JobPosting) {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormApplyJobBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        formBinding.tvApplyTitle.text = "Apply: ${job.title}"
        formBinding.tvApplyEmployer.text = "Employer: ${job.employer} • ${job.location}"

        val currentUser = HoodeRepository.currentUser.value
        currentUser?.let {
            formBinding.etApplicantName.setText(it.displayName)
            formBinding.etApplicantPhone.setText(it.phone ?: "")
        }

        formBinding.btnCloseApply.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitApplication.setOnClickListener {
            val name = formBinding.etApplicantName.text.toString().trim()
            val phone = formBinding.etApplicantPhone.text.toString().trim()
            val msg = formBinding.etApplicantMessage.text.toString().trim()

            if (name.isBlank()) {
                formBinding.tilApplicantName.error = "Please enter your name"
                return@setOnClickListener
            }
            formBinding.tilApplicantName.error = null

            if (phone.isBlank()) {
                formBinding.tilApplicantPhone.error = "Please enter your phone number"
                return@setOnClickListener
            }
            formBinding.tilApplicantPhone.error = null

            HoodeRepository.applyJob(job.id, name, phone, msg)
            Toast.makeText(requireContext(), "Application submitted directly to ${job.employer}!", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPostJobDialog() {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormPostJobBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedType = "Full-time"
        val typeChips = listOf(
            formBinding.chipJobFulltime to "Full-time",
            formBinding.chipJobParttime to "Part-time",
            formBinding.chipJobGig to "Gig / Contract",
            formBinding.chipJobApprentice to "Apprentice"
        )

        for ((chipView, typeName) in typeChips) {
            chipView.setOnClickListener {
                selectedType = typeName
                for ((v, name) in typeChips) {
                    if (name == selectedType) {
                        v.setBackgroundResource(R.drawable.bg_chip_black_border)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        formBinding.btnClosePostJob.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitJob.setOnClickListener {
            val title = formBinding.etJobTitle.text.toString().trim()
            val employer = formBinding.etJobEmployer.text.toString().trim()
            val pay = formBinding.etJobPay.text.toString().trim()
            val loc = formBinding.etJobLocation.text.toString().trim()
            val desc = formBinding.etJobDescription.text.toString().trim()
            val phone = formBinding.etJobPhone.text.toString().trim()

            if (title.isBlank()) {
                formBinding.tilJobTitle.error = "Please enter job title"
                return@setOnClickListener
            }
            formBinding.tilJobTitle.error = null

            if (employer.isBlank()) {
                formBinding.tilJobEmployer.error = "Please enter business/employer name"
                return@setOnClickListener
            }
            formBinding.tilJobEmployer.error = null

            if (phone.isBlank()) {
                formBinding.tilJobPhone.error = "Please enter contact phone"
                return@setOnClickListener
            }
            formBinding.tilJobPhone.error = null

            val newJob = JobPosting(
                title = title,
                employer = employer,
                type = selectedType,
                pay = if (pay.isNotBlank()) pay else "Competitive",
                location = if (loc.isNotBlank()) loc else "Hoode",
                description = if (desc.isNotBlank()) desc else "Contact $phone for full details and schedule.",
                deadline = "30 Sep 2026"
            )
            HoodeRepository.postJob(newJob)
            Toast.makeText(requireContext(), "Job '$title' published to Hoode Community!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

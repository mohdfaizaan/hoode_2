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
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val etName = EditText(requireContext()).apply { hint = "Full Name" }
        val etPhone = EditText(requireContext()).apply { hint = "Phone / WhatsApp" }
        val etMessage = EditText(requireContext()).apply { hint = "Brief experience / cover message" }
        layout.addView(etName)
        layout.addView(etPhone)
        layout.addView(etMessage)

        AlertDialog.Builder(requireContext())
            .setTitle("Apply for ${job.title}")
            .setMessage("Employer: ${job.employer}")
            .setView(layout)
            .setPositiveButton("Submit Application") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val msg = etMessage.text.toString().trim()

                if (name.isNotBlank() && phone.isNotBlank()) {
                    HoodeRepository.applyJob(job.id, name, phone, msg)
                    Toast.makeText(requireContext(), "Application submitted to ${job.employer}!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Please provide your name and contact phone.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPostJobDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val etTitle = EditText(requireContext()).apply { hint = "Job Title (e.g. Electrician, Sales)" }
        val etEmployer = EditText(requireContext()).apply { hint = "Employer / Business Name" }
        val etPay = EditText(requireContext()).apply { hint = "Pay (e.g. ₹15,000 / month)" }
        val etDesc = EditText(requireContext()).apply { hint = "Requirements & description" }
        layout.addView(etTitle)
        layout.addView(etEmployer)
        layout.addView(etPay)
        layout.addView(etDesc)

        AlertDialog.Builder(requireContext())
            .setTitle("Post a Community Job")
            .setView(layout)
            .setPositiveButton("Post Job") { _, _ ->
                val title = etTitle.text.toString().trim()
                val employer = etEmployer.text.toString().trim()
                val pay = etPay.text.toString().trim()
                val desc = etDesc.text.toString().trim()

                if (title.isNotBlank() && employer.isNotBlank()) {
                    val newJob = JobPosting(
                        title = title,
                        employer = employer,
                        type = "Full-time",
                        pay = if (pay.isNotBlank()) pay else "Competitive",
                        location = "Hoode",
                        description = if (desc.isNotBlank()) desc else "No additional description provided.",
                        deadline = "30 Sep 2026"
                    )
                    HoodeRepository.postJob(newJob)
                    Toast.makeText(requireContext(), "Job posted to community board!", Toast.LENGTH_SHORT).show()
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

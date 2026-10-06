package com.example.hoode_app.ui.polls

import com.example.hoode_app.ui.common.submitForReview
import com.example.hoode_app.ui.common.saveAction
import com.example.hoode_app.ui.common.showEmptyContent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentPollsBinding
import com.example.hoode_app.databinding.ItemCivicIssueCardBinding
import com.example.hoode_app.databinding.ItemPollCardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PollsFragment : Fragment() {

    private var _binding: FragmentPollsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPollsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnReportIssue.setOnClickListener {
            showReportIssueDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.polls.collectLatest { pollsList ->
                binding.llPollsContainer.removeAllViews()
        if (pollsList.isEmpty()) binding.llPollsContainer.showEmptyContent("No polls to show")
                for (poll in pollsList) {
                    val cardBinding = ItemPollCardBinding.inflate(layoutInflater, binding.llPollsContainer, false)
                    cardBinding.tvPollQuestion.text = poll.question
                    cardBinding.tvPollDescription.text = poll.description
                    cardBinding.tvPollVotesCount.text = "${poll.totalVotes} votes • Closes ${poll.closesAt}"

                    cardBinding.rgPollOptions.removeAllViews()
                    var selectedOptionId: String? = null

                    for (option in poll.options) {
                        val rb = RadioButton(requireContext()).apply {
                            val percent = if (poll.totalVotes > 0) (option.votes * 100) / poll.totalVotes else 0
                            text = if (poll.userVotedOptionId != null) "${option.label} (${percent}%)" else option.label
                            id = View.generateViewId()
                            isChecked = poll.userVotedOptionId == option.id
                            isEnabled = poll.userVotedOptionId == null
                            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                            setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) selectedOptionId = option.id
                            }
                        }
                        cardBinding.rgPollOptions.addView(rb)
                    }

                    if (poll.userVotedOptionId != null) {
                        cardBinding.btnVoteSubmit.isEnabled = false
                        cardBinding.btnVoteSubmit.text = "✓ Voted"
                    } else {
                        cardBinding.btnVoteSubmit.setOnClickListener {
                            if (selectedOptionId != null) {
                                saveAction(cardBinding.btnVoteSubmit,"Vote saved.") { HoodeRepository.castVote(poll.id, selectedOptionId!!) }
                            } else {
                                Toast.makeText(requireContext(), "Please select an option first.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    binding.llPollsContainer.addView(cardBinding.root)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.civicIssues.collectLatest { issues ->
                binding.llCivicIssuesContainer.removeAllViews()
        if (issues.isEmpty()) binding.llCivicIssuesContainer.showEmptyContent("No civic reports to show")
                for (issue in issues) {
                    val issueBinding = ItemCivicIssueCardBinding.inflate(layoutInflater, binding.llCivicIssuesContainer, false)
                    issueBinding.tvIssueCategory.text = issue.category
                    issueBinding.tvIssueStatus.text = issue.status.replace("_", " ").uppercase()
                    issueBinding.tvIssueTitle.text = issue.title
                    issueBinding.tvIssueLocation.text = "${issue.location} • Reported ${issue.reportedDate}"
                    issueBinding.tvIssueEndorsements.text = "${issue.endorsements} neighbors endorsed"

                    if (issue.userEndorsed) {
                        issueBinding.btnEndorseIssue.text = "✓ Endorsed"
                        issueBinding.btnEndorseIssue.isEnabled = false
                    } else {
                        issueBinding.btnEndorseIssue.setOnClickListener {
                            saveAction(issueBinding.btnEndorseIssue,"Endorsement saved.") { HoodeRepository.endorseCivicIssue(issue.id) }
                        }
                    }

                    binding.llCivicIssuesContainer.addView(issueBinding.root)
                }
            }
        }
    }

    private fun showReportIssueDialog() {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormReportCivicBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedCategory = "Streetlights"
        val categoryChips = listOf(
            formBinding.chipCivicLights to "Streetlights",
            formBinding.chipCivicRoads to "Roads & Potholes",
            formBinding.chipCivicWaste to "Waste / Trash",
            formBinding.chipCivicDrainage to "Drainage & Water",
            formBinding.chipCivicBeach to "Beach Cleanliness"
        )

        for ((chipView, catName) in categoryChips) {
            chipView.setOnClickListener {
                selectedCategory = catName
                for ((v, name) in categoryChips) {
                    if (name == selectedCategory) {
                        v.setBackgroundResource(R.drawable.bg_chip_black_border)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_chip_unselected)
                        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
        }

        formBinding.btnCloseReportCivic.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitCivic.setOnClickListener {
            val title = formBinding.etCivicTitle.text.toString().trim()
            val loc = formBinding.etCivicLocation.text.toString().trim()
            val details = formBinding.etCivicDetails.text.toString().trim()

            if (title.isBlank()) {
                formBinding.tilCivicTitle.error = "Please enter issue summary"
                return@setOnClickListener
            }
            formBinding.tilCivicTitle.error = null

            if (loc.isBlank()) {
                formBinding.tilCivicLocation.error = "Please enter location"
                return@setOnClickListener
            }
            formBinding.tilCivicLocation.error = null

            submitForReview(formBinding.btnSubmitCivic,dialog) { HoodeRepository.submitCivicIssue(title,selectedCategory,loc,formBinding.etCivicDetails.text.toString().trim()) }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

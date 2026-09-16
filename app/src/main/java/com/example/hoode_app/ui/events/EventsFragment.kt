package com.example.hoode_app.ui.events

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
import com.example.hoode_app.data.model.CommunityEvent
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentEventsBinding
import com.example.hoode_app.databinding.ItemEventCardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCreateEvent.setOnClickListener {
            showCreateEventDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.events.collectLatest { eventsList ->
                binding.llEventsContainer.removeAllViews()
                for (event in eventsList) {
                    val itemBinding = ItemEventCardBinding.inflate(layoutInflater, binding.llEventsContainer, false)
                    itemBinding.tvEventCategory.text = event.category
                    itemBinding.tvEventPrivacy.text = if (event.isPrivate) "Private Invitation" else "Public Community"
                    itemBinding.tvEventPrivacy.setBackgroundResource(if (event.isPrivate) R.drawable.bg_pill_neutral else R.drawable.bg_pill_accent)
                    itemBinding.tvEventTitle.text = event.title
                    itemBinding.tvEventDatetime.text = "${event.date} • ${event.time}"
                    itemBinding.tvEventVenue.text = "${event.venue} • Organizer: ${event.organizer}"
                    itemBinding.tvEventRsvpCount.text = "${event.rsvpGoing} / ${event.rsvpTotalCapacity} Attending"

                    if (event.userRsvp == true) {
                        itemBinding.btnRsvpGoing.text = "✓ Going"
                        itemBinding.btnRsvpGoing.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.accent))
                    } else {
                        itemBinding.btnRsvpGoing.text = "RSVP: Going"
                    }

                    itemBinding.btnRsvpGoing.setOnClickListener {
                        HoodeRepository.rsvpEvent(event.id, true)
                        Toast.makeText(requireContext(), "RSVP confirmed for ${event.title}!", Toast.LENGTH_SHORT).show()
                    }

                    itemBinding.btnRsvpNotGoing.setOnClickListener {
                        HoodeRepository.rsvpEvent(event.id, false)
                        Toast.makeText(requireContext(), "Marked as not attending", Toast.LENGTH_SHORT).show()
                    }

                    binding.llEventsContainer.addView(itemBinding.root)
                }
            }
        }
    }

    private fun showCreateEventDialog() {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val formBinding = com.example.hoode_app.databinding.DialogFormPostEventBinding.inflate(layoutInflater)
        dialog.setContentView(formBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedCategory = "Community"
        val categoryChips = listOf(
            formBinding.chipEvCommunity to "Community",
            formBinding.chipEvMajlis to "Majlis / Dars",
            formBinding.chipEvSports to "Sports Meet",
            formBinding.chipEvWedding to "Wedding / Nikah",
            formBinding.chipEvCivic to "Civic Drive"
        )

        for ((chipView, categoryName) in categoryChips) {
            chipView.setOnClickListener {
                selectedCategory = categoryName
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

        formBinding.btnClosePostEvent.setOnClickListener {
            dialog.dismiss()
        }

        formBinding.btnSubmitEvent.setOnClickListener {
            val title = formBinding.etEventTitle.text.toString().trim()
            val date = formBinding.etEventDate.text.toString().trim()
            val venue = formBinding.etEventVenue.text.toString().trim()
            val desc = formBinding.etEventDescription.text.toString().trim()
            val organizer = formBinding.etEventOrganizer.text.toString().trim()

            if (title.isBlank()) {
                formBinding.tilEventTitle.error = "Please enter event title"
                return@setOnClickListener
            }
            formBinding.tilEventTitle.error = null

            if (date.isBlank()) {
                formBinding.tilEventDate.error = "Please enter date and time"
                return@setOnClickListener
            }
            formBinding.tilEventDate.error = null

            if (venue.isBlank()) {
                formBinding.tilEventVenue.error = "Please enter venue"
                return@setOnClickListener
            }
            formBinding.tilEventVenue.error = null

            Toast.makeText(requireContext(), "Event '$title' ($selectedCategory) submitted for community review!", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

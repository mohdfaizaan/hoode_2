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
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val etTitle = EditText(requireContext()).apply { hint = "Event Title" }
        val etDate = EditText(requireContext()).apply { hint = "Date & Time (e.g. Sunday 5:00 PM)" }
        val etVenue = EditText(requireContext()).apply { hint = "Venue (e.g. Community Hall)" }
        val etCategory = EditText(requireContext()).apply { hint = "Category (Majlis, Wedding, Meeting)" }
        layout.addView(etTitle)
        layout.addView(etDate)
        layout.addView(etVenue)
        layout.addView(etCategory)

        AlertDialog.Builder(requireContext())
            .setTitle("Post Community Event")
            .setView(layout)
            .setPositiveButton("Submit for Approval") { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isNotBlank()) {
                    Toast.makeText(requireContext(), "Event '$title' submitted for moderator review.", Toast.LENGTH_LONG).show()
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

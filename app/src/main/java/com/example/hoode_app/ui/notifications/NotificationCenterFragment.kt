package com.example.hoode_app.ui.notifications

import com.example.hoode_app.ui.common.saveAction
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.model.NotificationItem
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentNotificationCenterBinding
import com.example.hoode_app.databinding.ItemNotificationCardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationCenterFragment : Fragment() {

    private var _binding: FragmentNotificationCenterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationCenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnMarkAllRead.setOnClickListener {
            saveAction(binding.btnMarkAllRead,"Shown notifications marked as read.") { HoodeRepository.markNotificationsRead(HoodeRepository.notifications.value.map{it.id}) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.notifications.collectLatest { list ->
                binding.llNotificationsContainer.removeAllViews()
                if(list.isEmpty())binding.llNotificationsContainer.addView(android.widget.TextView(requireContext()).apply{text="Review updates will appear here."})
                for (notif in list) {
                    val itemBinding = ItemNotificationCardBinding.inflate(layoutInflater, binding.llNotificationsContainer, false)
                    itemBinding.tvNotifCategory.text = if(notif.isRead)"Read review" else "New review"
                    itemBinding.tvNotifTimestamp.text = notif.timestamp
                    itemBinding.tvNotifTitle.text = notif.title
                    itemBinding.tvNotifBody.text = notif.body

                    itemBinding.root.setOnClickListener {
                        handleNotificationClick(notif)
                    }

                    binding.llNotificationsContainer.addView(itemBinding.root)
                }
            }
        }
    }

    private fun handleNotificationClick(notif: NotificationItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.markNotificationsRead(listOf(notif.id)).onSuccess { findNavController().navigate(R.id.profileFragment) }
                .onFailure { Toast.makeText(requireContext(),it.message,Toast.LENGTH_LONG).show() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

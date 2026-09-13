package com.example.hoode_app.ui.notifications

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
            Toast.makeText(requireContext(), "All notifications marked as read.", Toast.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.notifications.collectLatest { list ->
                binding.llNotificationsContainer.removeAllViews()
                for (notif in list) {
                    val itemBinding = ItemNotificationCardBinding.inflate(layoutInflater, binding.llNotificationsContainer, false)
                    itemBinding.tvNotifCategory.text = notif.category
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
        val cat = notif.category.lowercase()
        val title = notif.title.lowercase()

        when {
            cat.contains("prayer") || title.contains("iqamah") || title.contains("adhan") -> {
                findNavController().navigate(R.id.prayerDetailFragment)
            }
            cat.contains("blood") || title.contains("blood") || title.contains("donor") -> {
                findNavController().navigate(R.id.bloodNetworkFragment)
            }
            cat.contains("sports") || cat.contains("tournament") || title.contains("hpl") || title.contains("cricket") -> {
                findNavController().navigate(R.id.tournamentsFragment)
            }
            cat.contains("news") || title.contains("update") || title.contains("official") -> {
                findNavController().navigate(R.id.newsFragment)
            }
            cat.contains("classified") || cat.contains("marketplace") -> {
                findNavController().navigate(R.id.marketplaceFragment)
            }
            cat.contains("job") -> {
                findNavController().navigate(R.id.jobsFragment)
            }
            cat.contains("emergency") -> {
                findNavController().navigate(R.id.emergencyFragment)
            }
            cat.contains("poll") -> {
                findNavController().navigate(R.id.pollsFragment)
            }
            else -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(notif.title)
                    .setMessage("${notif.body}\n\nReceived: ${notif.timestamp} • Category: ${notif.category}")
                    .setPositiveButton("Dismiss", null)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

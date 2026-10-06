package com.example.hoode_app.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentSettingsBinding
import com.example.hoode_app.ui.common.CommunityFeaturesHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Live User Sync in Account Card
        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.currentUser.collectLatest { user ->
                binding.tvSettingsName.text = user?.displayName ?: "Hoode Resident"
                binding.tvSettingsEmail.text = user?.email ?: ""

                if (!user?.profilePicUri.isNullOrBlank()) {
                    binding.ivSettingsAvatar.load(user?.profilePicUri) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                        placeholder(R.drawable.profile_placeholder)
                        error(R.drawable.profile_placeholder)
                    }
                }
            }
        }

        // Tapping user card opens Edit Profile
        binding.profileSettingsCard.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }

        setupGeneralSettings()
        setupSupportSettings()
    }

    private fun info(title: String, message: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(title).setMessage(message).setPositiveButton("Close", null).show()
    }

    private fun setupGeneralSettings() {
        val container = binding.generalSettingsContainer
        container.removeAllViews()
        addSettingRow(container, R.drawable.ic_person, "Personal details", "Edit") {
            findNavController().navigate(R.id.editProfileFragment)
        }
        addSettingRow(container, R.drawable.ic_notification_bell, "Notifications", "Open") {
            findNavController().navigate(R.id.notificationCenterFragment)
        }
        addSettingRow(container, R.drawable.ic_category_services, "Appearance", "Coastal light") {
            info("Coastal light", "Hoode uses a light coastal theme. Text follows your phone’s font size, and the opening animation respects your device’s animation settings.")
        }
        addSettingRow(container, R.drawable.ic_community, "Opening animation", "Replay") {
            val intro = com.example.hoode_app.ui.common.LaunchIntro(requireActivity(), true)
            viewLifecycleOwner.lifecycleScope.launch {
                try { intro.reveal(binding.root) } finally { intro.remove() }
            }
        }
        addSettingRow(container, R.drawable.ic_blood, "Blood group & contact", "Edit") {
            findNavController().navigate(R.id.editProfileFragment)
        }
    }

    private fun setupSupportSettings() {
        val container = binding.supportSettingsContainer
        container.removeAllViews()
        addSettingRow(container, R.drawable.ic_news, "How submissions work") {
            info("Sharing with Hoode", "Submit photos, listings, events or requests from the app. They stay pending until the community team reviews them. Open Profile → Activity to see the status and any review notes. Public posts become visible after approval; private applications and claims stay private.")
        }
        addSettingRow(container, R.drawable.ic_blood, "Community contributions") {
            CommunityFeaturesHelper.showDonationDialog(requireContext(), layoutInflater)
        }
        addSettingRow(container, R.drawable.ic_community, "Our projects") {
            CommunityFeaturesHelper.showOurWorkDialog(requireContext(), layoutInflater)
        }
        addSettingRow(container, R.drawable.ic_community, "About Hoode", "1.0.0") {
            info("Hoode Connect", "A place for our community’s news, services, gatherings and everyday moments. Find local help, share something useful, and stay connected to Hoode.")
        }
        addSettingRow(container, R.drawable.ic_close, "Sign out", isDestructive = true) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign out of Hoode?")
                .setMessage("You’ll need your email and password to sign in again.")
                .setNegativeButton("Stay signed in", null)
                .setPositiveButton("Sign out") { _, _ ->
                    HoodeRepository.signOut()
                    findNavController().navigate(R.id.signInFragment, null,
                        androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build())
                }.show()
        }
    }

    private fun addSettingRow(
        parent: ViewGroup,
        iconRes: Int,
        title: String,
        value: String? = null,
        isDestructive: Boolean = false,
        onClick: () -> Unit
    ) {
        val row = layoutInflater.inflate(R.layout.item_settings_row, parent, false)
        val ivIcon = row.findViewById<ImageView>(R.id.ivIcon)
        val tvTitle = row.findViewById<TextView>(R.id.tvTitle)
        val tvValue = row.findViewById<TextView>(R.id.tvValue)

        ivIcon.setImageResource(iconRes)
        tvTitle.text = title

        if (isDestructive) {
            tvTitle.setTextColor(resources.getColor(R.color.danger, null))
            ivIcon.setColorFilter(resources.getColor(R.color.danger, null))
        }

        if (!value.isNullOrBlank()) {
            tvValue.visibility = View.VISIBLE
            tvValue.text = value
        } else {
            tvValue.visibility = View.GONE
        }

        row.setOnClickListener { onClick() }
        parent.addView(row)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.hoode_app.ui.profile

import android.app.AlertDialog
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
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.model.User
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentProfileBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import androidx.activity.result.contract.ActivityResultContracts
import coil.load

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // Update UI
            binding.ivAvatar.setImageURI(uri)
            // Save to repository
            val current = HoodeRepository.currentUser.value
            if (current != null) {
                HoodeRepository.updateUserProfile(
                    displayName = current.displayName,
                    phone = current.phone ?: "",
                    age = current.age ?: "",
                    dob = current.dob ?: "",
                    fatherName = current.fatherName ?: "",
                    bloodGroup = current.bloodGroup ?: "",
                    profession = current.profession ?: "",
                    locality = current.locality ?: "",
                    profilePicUri = uri.toString()
                )
                Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProfile()
        setupMenuActions()
        setupStatsInteractions()
    }

    private fun setupProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.currentUser.collectLatest { user ->
                if (user != null) {
                    binding.tvDisplayName.text = user.displayName
                    val isAdmin = user.roles.contains("community_admin")
                    val roleLabel = if (isAdmin) "Community Admin" else "Resident"
                    binding.tvCommunityBadge.text = "Hoode • $roleLabel"
                    binding.adminCard.visibility = if (isAdmin) View.VISIBLE else View.GONE
                    
                    if (user.profilePicUri != null) {
                        try {
                            binding.ivAvatar.setImageURI(Uri.parse(user.profilePicUri))
                        } catch (e: Exception) {
                            binding.ivAvatar.load("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80") { crossfade(true) }
                        }
                    } else {
                        binding.ivAvatar.load("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80") { crossfade(true) }
                    }
                } else {
                    binding.tvDisplayName.text = "Guest"
                    binding.tvCommunityBadge.text = "Not logged in"
                    binding.adminCard.visibility = View.GONE
                }
            }
        }

        binding.ivAvatar.setOnClickListener {
            showAvatarSelectionDialog()
        }
        binding.btnEditAvatar.setOnClickListener {
            showAvatarSelectionDialog()
        }
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun setupStatsInteractions() {
        binding.statPostsContainer.setOnClickListener {
            showMyListingsDialog()
        }
        binding.statBookmarksContainer.setOnClickListener {
            showBookmarksDialog()
        }
    }

    private fun setupMenuActions() {
        // ── SECTION 1: COMMUNITY & CONTENT ─────────────────────────────

        // My Posts & Listings
        binding.menuMyPosts.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_nav_create)
            tvMenuLabel.text = "My Listings & Activity"
            tvMenuSubtitle.text = "Classifieds, civic reports & blood requests"
            tvMenuSubtitle.visibility = View.VISIBLE
            tvMenuBadge.text = "4 Active"
            tvMenuBadge.visibility = View.VISIBLE
            root.setOnClickListener {
                showMyListingsDialog()
            }
        }

        // Bookmarks
        binding.menuBookmarks.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_nav_explore)
            tvMenuLabel.text = "Saved Bookmarks"
            tvMenuSubtitle.text = "Timetables, emergency hotlines & fixtures"
            tvMenuSubtitle.visibility = View.VISIBLE
            tvMenuBadge.text = "5 Saved"
            tvMenuBadge.visibility = View.VISIBLE
            root.setOnClickListener {
                showBookmarksDialog()
            }
        }

        // Notifications
        binding.menuNotifications.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_notification_bell)
            tvMenuLabel.text = "Notification Center"
            tvMenuSubtitle.text = "Adhan reminders, emergency alerts & notices"
            tvMenuSubtitle.visibility = View.VISIBLE
            root.setOnClickListener {
                findNavController().navigate(R.id.notificationCenterFragment)
            }
        }

        // ── SECTION 2: RESIDENT SERVICES & HEALTH ─────────────────────

        // Emergency Medical Profile
        binding.menuMedical.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_blood)
            tvMenuLabel.text = "Emergency Medical Profile"
            tvMenuSubtitle.text = "Blood Group O+ • ICE contact registered"
            tvMenuSubtitle.visibility = View.VISIBLE
            tvMenuBadge.text = "O+ Donor"
            tvMenuBadge.visibility = View.VISIBLE
            root.setOnClickListener {
                showMedicalProfileDialog()
            }
        }

        // Help Desk & Secretariat
        binding.menuHelp.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_phone)
            tvMenuLabel.text = getString(R.string.profile_help)
            tvMenuSubtitle.text = "24/7 hotline, WhatsApp desk & resident FAQ"
            tvMenuSubtitle.visibility = View.VISIBLE
            root.setOnClickListener {
                showHelpDeskDialog()
            }
        }

        // ── SECTION 3: PREFERENCES & SECURITY ─────────────────────────

        // Interface Language
        binding.menuLanguage.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_nav_explore)
            tvMenuLabel.text = getString(R.string.profile_language)
            tvMenuSubtitle.text = "English, ಕನ್ನಡ (Kannada), اردو (Urdu)"
            tvMenuSubtitle.visibility = View.VISIBLE
            tvMenuBadge.text = "English"
            tvMenuBadge.visibility = View.VISIBLE
            root.setOnClickListener {
                showLanguageDialog()
            }
        }

        // Privacy Policy & Terms
        binding.menuPrivacy.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_check)
            tvMenuLabel.text = "Privacy & Row-Level Security"
            tvMenuSubtitle.text = "Zero phone leaks • PostgreSQL data isolation"
            tvMenuSubtitle.visibility = View.VISIBLE
            root.setOnClickListener {
                showPrivacyDialog()
            }
        }

        // Sync & Offline Storage
        binding.menuSettings.apply {
            ivMenuIcon.setImageResource(R.drawable.ic_admin)
            tvMenuLabel.text = "Offline Sync & Settings"
            tvMenuSubtitle.text = "35 Supabase tables synced locally"
            tvMenuSubtitle.visibility = View.VISIBLE
            tvMenuBadge.text = "Synced"
            tvMenuBadge.visibility = View.VISIBLE
            root.setOnClickListener {
                showSettingsDialog()
            }
        }

        // Admin Console
        binding.adminCard.setOnClickListener {
            findNavController().navigate(R.id.adminDashboardFragment)
        }

        // Sign Out
        binding.btnSignOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.auth_sign_out)
                .setMessage("Are you sure you want to sign out from Hoode Connect?")
                .setPositiveButton("Sign Out") { _, _ ->
                    HoodeRepository.signOut()
                    findNavController().navigate(R.id.welcomeFragment)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showMyListingsDialog() {
        val items = arrayOf(
            "📦 Classified: Teakwood Study Table (₹3,200) — Active",
            "📢 Civic Report: Bengre Beach Streetlight Outage — In Progress",
            "🩸 Blood Registry: O+ Emergency Donor — Available",
            "🏏 HPL 2026: Team Hoode Coastal Strikers — Registered"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("My Active Submissions (4)")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Teakwood Study Table")
                            .setMessage("Listing Status: Active in Hoode Marketplace\nInquiries Received: 3 resident messages")
                            .setPositiveButton("Mark as Sold") { _, _ ->
                                Toast.makeText(requireContext(), "Item marked as sold!", Toast.LENGTH_SHORT).show()
                            }
                            .setNeutralButton("View Inquiries") { _, _ ->
                                findNavController().navigate(R.id.inboxFragment)
                            }
                            .setNegativeButton("Close", null)
                            .show()
                    }
                    1 -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Bengre Beach Streetlight Outage")
                            .setMessage("Status: Under Review by Ward Councillor\nAssigned Contractor: Coastal Energy Dept\nEstimated Resolution: 12 Sep 2026")
                            .setPositiveButton("Understood", null)
                            .show()
                    }
                    2 -> {
                        findNavController().navigate(R.id.bloodNetworkFragment)
                    }
                    3 -> {
                        findNavController().navigate(R.id.tournamentsFragment)
                    }
                }
            }
            .setPositiveButton("+ Create New") { _, _ ->
                findNavController().navigate(R.id.createFragment)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showMedicalProfileDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 24)
        }

        val tvBloodGroup = TextView(requireContext()).apply {
            text = "Blood Group: O Positive (Universal Red Cell Donor)"
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }

        val etIce = EditText(requireContext()).apply {
            hint = "Emergency Contact (ICE Name & Phone)"
            setText("Arshad (Brother) — +91 98450 67890")
            setBackgroundResource(R.drawable.bg_search_bar)
            setPadding(36, 24, 36, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }

        val etAllergies = EditText(requireContext()).apply {
            hint = "Known Medical Conditions / Allergies"
            setText("Penicillin allergy • Nil chronic ailments")
            setBackgroundResource(R.drawable.bg_search_bar)
            setPadding(36, 24, 36, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }

        val swDonorPledge = SwitchCompat(requireContext()).apply {
            text = "Active in Hoode Emergency Blood Network"
            isChecked = true
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 24 }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }

        layout.addView(tvBloodGroup)
        layout.addView(etIce)
        layout.addView(etAllergies)
        layout.addView(swDonorPledge)

        AlertDialog.Builder(requireContext())
            .setTitle("Emergency Medical Profile")
            .setView(layout)
            .setPositiveButton("Save Profile") { _, _ ->
                Toast.makeText(requireContext(), "Emergency medical profile saved securely.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPrivacyDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Privacy & Community Protection")
            .setMessage(
                "1. Verified Community Scope: Your telephone number and address are never publicly exposed or listed in open directories.\n\n" +
                "2. Row Level Security: End-to-end PostgreSQL RLS guarantees total data isolation between communities.\n\n" +
                "3. Masked Blood Calls: Volunteer donor contacts remain concealed until explicit mutual consent is given for urgent emergencies.\n\n" +
                "4. Right to Erasure: Export your data ledger or delete your community account anytime from settings."
            )
            .setPositiveButton("Understood", null)
            .show()
    }

    private fun showEditProfileDialog() {
        val user = HoodeRepository.currentUser.value
        val view = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        
        val etName = view.findViewById<EditText>(R.id.et_display_name)
        val etPhone = view.findViewById<EditText>(R.id.et_phone)
        val etFatherName = view.findViewById<EditText>(R.id.et_father_name)
        val etAge = view.findViewById<EditText>(R.id.et_age)
        val etDob = view.findViewById<EditText>(R.id.et_dob)
        val etBloodGroup = view.findViewById<EditText>(R.id.et_blood_group)
        val etProfession = view.findViewById<EditText>(R.id.et_profession)
        val etLocality = view.findViewById<EditText>(R.id.et_locality)

        // Pre-fill
        etName.setText(user?.displayName ?: "")
        etPhone.setText(user?.phone ?: "")
        etFatherName.setText(user?.fatherName ?: "")
        etAge.setText(user?.age ?: "")
        etDob.setText(user?.dob ?: "")
        etBloodGroup.setText(user?.bloodGroup ?: "")
        etProfession.setText(user?.profession ?: "")
        etLocality.setText(user?.locality ?: "")

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isNotBlank()) {
                HoodeRepository.updateUserProfile(
                    displayName = name,
                    phone = etPhone.text.toString().trim(),
                    age = etAge.text.toString().trim(),
                    dob = etDob.text.toString().trim(),
                    fatherName = etFatherName.text.toString().trim(),
                    bloodGroup = etBloodGroup.text.toString().trim(),
                    profession = etProfession.text.toString().trim(),
                    locality = etLocality.text.toString().trim()
                )
                Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Display Name cannot be empty.", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showAvatarSelectionDialog() {
        pickMediaLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showBookmarksDialog() {
        val bookmarks = arrayOf(
            "🕌 Hoode Juma Masjid — Today's Prayer Timetable",
            "🚑 Hoode Emergency Ambulance (+91 820 252 0108)",
            "🏏 HPL 2026 — Tournament Schedule & Standings",
            "🐟 Fresh Catch of the Day — Coastal Fisheries",
            "📢 Coastal Seawall Project Approval — Verified News"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Saved Bookmarks (${bookmarks.size})")
            .setItems(bookmarks) { _, which ->
                when (which) {
                    0 -> findNavController().navigate(R.id.prayerDetailFragment)
                    1 -> findNavController().navigate(R.id.emergencyFragment)
                    2 -> findNavController().navigate(R.id.tournamentsFragment)
                    3 -> findNavController().navigate(R.id.marketplaceFragment)
                    4 -> findNavController().navigate(R.id.newsFragment)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showSettingsDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 24)
        }

        val swPrayer = SwitchCompat(requireContext()).apply {
            text = "Adhan & Iqamah Notifications"
            isChecked = true
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
        val swBlood = SwitchCompat(requireContext()).apply {
            text = "Urgent Blood Emergency Alerts"
            isChecked = true
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 24 }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
        val swDigest = SwitchCompat(requireContext()).apply {
            text = "Daily Personality & Digest"
            isChecked = true
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 24 }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }

        val tvCache = TextView(requireContext()).apply {
            text = "Offline Database Sync:\n✓ 35 Supabase tables cached locally\n✓ Realtime sync operational\n✓ Storage used: 1.4 MB"
            textSize = 12f
            setLineSpacing(3f, 1f)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 32 }
        }

        container.addView(swPrayer)
        container.addView(swBlood)
        container.addView(swDigest)
        container.addView(tvCache)

        AlertDialog.Builder(requireContext())
            .setTitle("App & Offline Sync Settings")
            .setView(container)
            .setPositiveButton("Done") { _, _ ->
                Toast.makeText(requireContext(), "Preferences saved", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showHelpDeskDialog() {
        val options = arrayOf(
            "📞 Call Hoode Coordinator (+91 820 252 0001)",
            "💬 WhatsApp Support Desk",
            "📧 Email Community Secretariat",
            "❓ Frequently Asked Questions"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Hoode Support Desk")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+918202520001"))
                        startActivity(dialIntent)
                    }
                    1 -> {
                        Toast.makeText(requireContext(), "Opening Hoode WhatsApp Desk...", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@hoode.community"))
                        startActivity(mailIntent)
                    }
                    3 -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("FAQ — Hoode Connect")
                            .setMessage("Q: Who can register?\nA: Any resident of Hoode, Bengre, or Kodi.\n\nQ: How are namaz timings verified?\nA: Updated daily by designated mosque caretakers.\n\nQ: How to post a classified or job?\nA: Use the central '+' Create tab.")
                            .setPositiveButton("Close", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English (Selected)", "ಕನ್ನಡ (Kannada)", "اردو (Urdu)")
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.profile_language)
            .setItems(languages) { _, which ->
                val selected = when (which) {
                    0 -> "English"
                    1 -> "ಕನ್ನಡ"
                    else -> "اردو"
                }
                binding.menuLanguage.tvMenuBadge.text = selected
                Toast.makeText(requireContext(), "Interface language set to: $selected", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.hoode_app.ui.profile

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import com.example.hoode_app.R
import com.example.hoode_app.data.model.User
import com.example.hoode_app.data.model.UserPostItem
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentProfileBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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

        // Live User Sync & Profile Details
        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.currentUser.collectLatest { user ->
                bindUserProfile(user)
                updateProfileCompletion(user)
                refreshCurrentTab()
            }
        }

        // Edit Profile Navigation
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }
        binding.btnCompleteProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }
        binding.cardResidentDetails.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }

        // Settings Navigation
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        // Setup Tabs
        setupProfileTabs()

        // Populate initial feed
        showPostsFeed()
    }

    private fun bindUserProfile(user: User?) {
        val displayName = user?.displayName?.ifBlank { "Hoode Resident" } ?: "Hoode Resident"
        val username = user?.username?.ifBlank { displayName.lowercase().replace(" ", "") }
            ?: displayName.lowercase().replace(" ", "")
        val bio = user?.bio?.ifBlank { "Connecting with the Hoode community." }
            ?: "Connecting with the Hoode community."
        val locality = user?.locality?.ifBlank { "Hoode" } ?: "Hoode"

        binding.tvName.text = displayName
        binding.tvUsername.text = "@$username"
        binding.tvBio.text = bio
        binding.tvLocalityBadge.text = "$locality • Verified Resident"

        // Avatar
        if (!user?.profilePicUri.isNullOrBlank()) {
            binding.ivProfile.load(user?.profilePicUri) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.profile_placeholder)
                error(R.drawable.profile_placeholder)
            }
        } else {
            binding.ivProfile.setImageResource(R.drawable.profile_placeholder)
        }

        // Cover
        if (!user?.coverPicUri.isNullOrBlank()) {
            binding.ivCover.load(user?.coverPicUri) {
                crossfade(true)
                placeholder(R.drawable.profile_cover_placeholder)
                error(R.drawable.profile_cover_placeholder)
            }
        }

        // Resident Details Card
        binding.tvDetailPhone.text = user?.phone?.takeIf { it.isNotBlank() } ?: "Not provided"
        binding.tvDetailEmail.text = user?.email?.takeIf { it.isNotBlank() } ?: "Not provided"
        binding.tvDetailLocality.text = user?.locality?.takeIf { it.isNotBlank() } ?: "Hoode"
        binding.tvDetailBloodGroup.text = user?.bloodGroup?.takeIf { it.isNotBlank() } ?: "Not provided"
        binding.tvDetailProfession.text = user?.profession?.takeIf { it.isNotBlank() } ?: "Not provided"
        binding.tvDetailFatherName.text = user?.fatherName?.takeIf { it.isNotBlank() } ?: "Not provided"
        binding.tvDetailStatus.text = "Verified Resident • Active"

        // Dynamic Post Count from genuine items
        val genuinePosts = HoodeRepository.getUserPosts(user)
        binding.tvStatPosts.text = genuinePosts.size.toString()
    }

    private fun updateProfileCompletion(user: User?) {
        if (user == null) {
            binding.pbProfileCompletion.progress = 0
            binding.tvCompletionPercentage.text = "0%"
            binding.tvCompletionTip.text = "Please complete your resident details."
            return
        }

        var filledCount = 0
        val totalFields = 8
        val missingFields = mutableListOf<String>()

        if (!user.displayName.isNullOrBlank()) filledCount++ else missingFields.add("Full Name")
        if (!user.email.isNullOrBlank()) filledCount++ else missingFields.add("Email")
        if (!user.phone.isNullOrBlank()) filledCount++ else missingFields.add("Phone")
        if (!user.locality.isNullOrBlank()) filledCount++ else missingFields.add("Locality")
        if (!user.bloodGroup.isNullOrBlank()) filledCount++ else missingFields.add("Blood Group")
        if (!user.profession.isNullOrBlank()) filledCount++ else missingFields.add("Profession")
        if (!user.fatherName.isNullOrBlank()) filledCount++ else missingFields.add("Father's Name")
        if (!user.profilePicUri.isNullOrBlank()) filledCount++ else missingFields.add("Profile Picture")

        val percentage = (filledCount * 100) / totalFields
        binding.pbProfileCompletion.progress = percentage
        binding.tvCompletionPercentage.text = "$percentage%"

        if (percentage == 100) {
            binding.tvCompletionTip.text = "All set! Your profile is 100% complete & verified."
            binding.btnCompleteProfile.text = "Edit Details"
        } else {
            val tip = when {
                missingFields.size == 1 -> "Add your ${missingFields[0]} to reach 100%!"
                missingFields.size >= 2 -> "Add your ${missingFields[0]} and ${missingFields[1]} to reach 100%!"
                else -> "Complete your profile to unlock all resident features."
            }
            binding.tvCompletionTip.text = tip
            binding.btnCompleteProfile.text = "Complete Profile →"
        }
    }

    private fun setupProfileTabs() {
        val tabs = binding.profileTabs
        tabs.removeAllTabs()

        tabs.addTab(tabs.newTab().setText("Posts"), true)
        tabs.addTab(tabs.newTab().setText("Community"))
        tabs.addTab(tabs.newTab().setText("Activity"))

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showPostsFeed()
                    1 -> showCommunityFeed()
                    2 -> showActivityFeed()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun refreshCurrentTab() {
        when (binding.profileTabs.selectedTabPosition) {
            0 -> showPostsFeed()
            1 -> showCommunityFeed()
            2 -> showActivityFeed()
            else -> showPostsFeed()
        }
    }

    // ── Genuine Posts Tab ────────────────────────────────────
    private fun showPostsFeed() {
        val container = binding.feedContainer
        container.removeAllViews()

        val currentUser = HoodeRepository.currentUser.value
        val genuinePosts = HoodeRepository.getUserPosts(currentUser)
        binding.tvStatPosts.text = genuinePosts.size.toString()

        if (genuinePosts.isEmpty()) {
            showEmptyPostsState(container)
        } else {
            for (post in genuinePosts) {
                addUserPostCard(container, post)
            }
        }
    }

    private fun showEmptyPostsState(container: LinearLayout) {
        val emptyCard = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            background = resources.getDrawable(R.drawable.bg_profile_card, null)
            gravity = Gravity.CENTER
            setPadding(48, 54, 48, 54)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 24)
            }
        }

        val icon = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(54, 54)
            setImageResource(R.drawable.ic_nav_create)
            setColorFilter(resources.getColor(R.color.profile_text_secondary, null))
        }

        val tvTitle = TextView(requireContext()).apply {
            text = "No Genuine Posts Yet"
            textSize = 17f
            setTextColor(resources.getColor(R.color.profile_text_main, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 6)
        }

        val tvSubtitle = TextView(requireContext()).apply {
            text = "You haven't posted any marketplace listings, lost & found reports, or event notices yet. Everything you publish will appear here."
            textSize = 13f
            setTextColor(resources.getColor(R.color.profile_text_secondary, null))
            gravity = Gravity.CENTER
            setLineSpacing(4f, 1f)
            setPadding(0, 0, 0, 20)
        }

        val btnCreate = MaterialButton(requireContext()).apply {
            text = "Create Your First Post"
            setBackgroundColor(resources.getColor(R.color.accent_ink, null))
            setTextColor(resources.getColor(R.color.white, null))
            cornerRadius = 24
            setOnClickListener {
                findNavController().navigate(R.id.createFragment)
            }
        }

        emptyCard.addView(icon)
        emptyCard.addView(tvTitle)
        emptyCard.addView(tvSubtitle)
        emptyCard.addView(btnCreate)
        container.addView(emptyCard)
    }

    private fun addUserPostCard(container: LinearLayout, post: UserPostItem) {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            background = resources.getDrawable(R.drawable.bg_profile_card, null)
            setPadding(36, 32, 36, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
        }

        // Header Row: Type Badge + Date
        val topRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 8)
        }

        val tvType = TextView(requireContext()).apply {
            text = post.type
            textSize = 11f
            setTextColor(resources.getColor(R.color.text_primary, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = resources.getDrawable(R.drawable.bg_chip_black_border, null)
            setPadding(20, 6, 20, 6)
        }

        val tvDate = TextView(requireContext()).apply {
            text = post.date
            textSize = 12f
            setTextColor(resources.getColor(R.color.profile_text_secondary, null))
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        topRow.addView(tvType)
        topRow.addView(tvDate)

        // Post Title
        val tvTitle = TextView(requireContext()).apply {
            text = post.title
            textSize = 16f
            setTextColor(resources.getColor(R.color.profile_text_main, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 6, 0, 6)
        }

        // Post Content
        val tvContent = TextView(requireContext()).apply {
            text = post.content
            textSize = 13.5f
            setTextColor(resources.getColor(R.color.profile_text_main, null))
            setLineSpacing(4f, 1f)
            setPadding(0, 0, 0, 10)
        }

        card.addView(topRow)
        card.addView(tvTitle)
        card.addView(tvContent)

        // Post Image Preview (if present)
        if (!post.imageUrl.isNullOrBlank()) {
            val ivImage = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    360
                ).apply {
                    setMargins(0, 6, 0, 12)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                load(post.imageUrl) {
                    crossfade(true)
                    transformations(RoundedCornersTransformation(16f))
                    placeholder(R.drawable.bg_gallery_luxury_gradient)
                    error(R.drawable.bg_gallery_luxury_gradient)
                }
            }
            card.addView(ivImage)
        }

        // Engagement Footer Row
        val bottomRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 0)
        }

        val tvLikes = TextView(requireContext()).apply {
            text = "♥ ${post.likes}"
            textSize = 12.5f
            setTextColor(resources.getColor(R.color.profile_text_secondary, null))
        }

        val tvComments = TextView(requireContext()).apply {
            text = "💬 ${post.comments}"
            textSize = 12.5f
            setPadding(32, 0, 0, 0)
            setTextColor(resources.getColor(R.color.profile_text_secondary, null))
        }

        bottomRow.addView(tvLikes)
        bottomRow.addView(tvComments)
        card.addView(bottomRow)

        container.addView(card)
    }

    // ── Community Tab ────────────────────────────────────────
    private fun showCommunityFeed() {
        val container = binding.feedContainer
        container.removeAllViews()

        val user = HoodeRepository.currentUser.value

        // Card 1: Verified Resident Locality Status
        val locality = user?.locality?.ifBlank { "Hoode" } ?: "Hoode"
        addFeedCard(
            title = "Verified Community Resident",
            time = "Verified • Ward Community",
            content = "Active resident of $locality, Udupi District. Connected to Hoode municipal & community alert channels.",
            likes = 32,
            comments = 4
        )

        // Card 2: Emergency Blood Network Status
        val bloodGroup = user?.bloodGroup
        if (!bloodGroup.isNullOrBlank()) {
            addFeedCard(
                title = "Emergency Blood Donor: $bloodGroup",
                time = "Ready to Donate",
                content = "Registered in the Hoode Emergency Blood Network. Available for emergency requests across Udupi, Kundapura, & Manipal.",
                likes = 28,
                comments = 5
            )
        } else {
            addFeedCard(
                title = "Emergency Blood Network",
                time = "Registration Pending",
                content = "You haven't listed your blood group yet. Edit your profile to register as a local donor and save lives.",
                likes = 10,
                comments = 1
            )
        }

        // Card 3: Profession / Service
        val profession = user?.profession
        if (!profession.isNullOrBlank()) {
            addFeedCard(
                title = "Community Member: $profession",
                time = "Active Profession",
                content = "Contributing professional skills and services to the Hoode community development network.",
                likes = 19,
                comments = 2
            )
        }
    }

    // ── Activity Tab ─────────────────────────────────────────
    private fun showActivityFeed() {
        val container = binding.feedContainer
        container.removeAllViews()

        val records = HoodeRepository.contributions.value

        if (records.isEmpty()) {
            addFeedCard(
                title = "Resident Community Contributions",
                time = "Getting Started",
                content = "Participate in civic polls, report community issues, register as a blood donor, or list items in the marketplace to earn contribution badges!",
                likes = 12,
                comments = 0
            )
        } else {
            for (record in records.take(6)) {
                addFeedCard(
                    title = record.action,
                    time = "${record.date} • +${record.points} Points",
                    content = "Verified community contribution logged to your resident profile scorecard.",
                    likes = record.points / 3 + 4,
                    comments = 1
                )
            }
        }
    }

    private fun addFeedCard(
        title: String,
        time: String,
        content: String,
        likes: Int,
        comments: Int
    ) {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            background = resources.getDrawable(R.drawable.bg_profile_card, null)
            setPadding(36, 32, 36, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
        }

        val tvCardTitle = TextView(requireContext()).apply {
            text = title
            textSize = 15f
            setTextColor(resources.getColor(R.color.profile_text_main, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val tvCardTime = TextView(requireContext()).apply {
            text = time
            textSize = 12f
            setTextColor(resources.getColor(R.color.profile_text_secondary, null))
            setPadding(0, 4, 0, 12)
        }

        val tvCardContent = TextView(requireContext()).apply {
            text = content
            textSize = 13.5f
            setTextColor(resources.getColor(R.color.profile_text_main, null))
            setLineSpacing(4f, 1f)
        }

        val bottomRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        val tvLikes = TextView(requireContext()).apply {
            text = "♥ $likes"
            textSize = 12.5f
            setTextColor(resources.getColor(R.color.profile_text_secondary, null))
        }

        val tvComments = TextView(requireContext()).apply {
            text = "💬 $comments"
            textSize = 12.5f
            setPadding(32, 0, 0, 0)
            setTextColor(resources.getColor(R.color.profile_text_secondary, null))
        }

        bottomRow.addView(tvLikes)
        bottomRow.addView(tvComments)

        card.addView(tvCardTitle)
        card.addView(tvCardTime)
        card.addView(tvCardContent)
        card.addView(bottomRow)

        binding.feedContainer.addView(card)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

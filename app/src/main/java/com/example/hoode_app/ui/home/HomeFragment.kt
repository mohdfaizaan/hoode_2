package com.example.hoode_app.ui.home

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.hoode_app.MainActivity
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.DialogSponsoredDetailBinding
import com.example.hoode_app.databinding.FragmentHomeBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var currentSlides: List<CarouselSlide> = emptyList()
    private var noticeboard: List<HomeUpdateItem> = emptyList()
    private var selectedFilter = R.id.chip_all
    private var refreshJob: Job? = null
    private var activeDialog: Dialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedFilter = savedInstanceState?.getInt("home_filter", R.id.chip_all) ?: R.id.chip_all
        setupNavigation()
        setupHeader()
        setupPrayer()
        setupSponsors()
        setupNoticeboard()
        setupHighlights()
        setupPersonality()
        setupGallery()
        binding.dashboardRefresh.setColorSchemeResources(R.color.dashboard_primary)
        binding.dashboardRefresh.setProgressBackgroundColorSchemeResource(R.color.dashboard_surface)
        binding.dashboardRefresh.setOnRefreshListener { refreshDashboard() }
        refreshDashboard()
        val accessibility = requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (savedInstanceState == null && ValueAnimator.areAnimatorsEnabled() && !accessibility.isTouchExplorationEnabled) {
            binding.dashboardContent.alpha = 0f
            binding.dashboardContent.translationY = 8 * resources.displayMetrics.density
            binding.dashboardContent.animate().alpha(1f).translationY(0f).setDuration(280).start()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("home_filter", selectedFilter)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        refreshJob?.cancel()
        activeDialog?.dismiss()
        activeDialog = null
        binding.dashboardContent.animate().cancel()
        super.onDestroyView()
        _binding = null
    }

    private fun <T> observe(flow: Flow<T>, render: (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect { render(it) } }
        }
    }

    private fun navigate(destination: Int) {
        findNavController().navigate(destination)
    }

    private fun setupNavigation() {
        binding.btnMenuDrawer.setOnClickListener { (activity as? MainActivity)?.openDrawer() }
        binding.ivHomeAvatar.setOnClickListener { navigate(R.id.profileFragment) }
        binding.btnNotification.setOnClickListener { navigate(R.id.notificationCenterFragment) }
        binding.prayerHeroCard.setOnClickListener { navigate(R.id.prayerDetailFragment) }
        binding.personalityCard.setOnClickListener { navigate(R.id.personalityDetailFragment) }
        binding.btnGallerySeeAll.setOnClickListener { navigate(R.id.galleryFragment) }
        val destinations = listOf(
            binding.actionServices to R.id.providersFragment,
            binding.actionMarket to R.id.marketplaceFragment,
            binding.actionEmergency to R.id.emergencyFragment,
            binding.actionBlood to R.id.bloodNetworkFragment,
            binding.actionEvents to R.id.eventsFragment,
            binding.actionJobs to R.id.jobsFragment,
            binding.actionLostFound to R.id.lostFoundFragment,
            binding.actionHuffaz to R.id.huffazFragment,
            binding.actionLocalNews to R.id.newsFragment
        )
        destinations.forEach { (control, destination) -> control.setOnClickListener { navigate(destination) } }
        binding.actionActivities.setOnClickListener {
            HoodeRepository.initialActivityCategory = "All"
            navigate(R.id.activitiesFragment)
        }
    }

    private fun setupHeader() {
        binding.tvDate.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
        observe(HoodeRepository.currentUser) { user ->
            val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 0..11 -> R.string.home_greeting_morning
                in 12..16 -> R.string.home_greeting_afternoon
                else -> R.string.home_greeting_evening
            }
            binding.tvGreeting.text = getString(R.string.dashboard_greeting, getString(greeting), user?.displayName ?: "Resident")
            binding.ivHomeAvatar.load(user?.profilePicUri) {
                placeholder(R.drawable.profile_placeholder)
                fallback(R.drawable.profile_placeholder)
                error(R.drawable.profile_placeholder)
            }
        }
        observe(HoodeRepository.notifications) { rows ->
            val unread = rows.count { !it.isRead }
            binding.tvNotificationBadge.isVisible = unread > 0
            binding.btnNotification.contentDescription = if (unread == 0) getString(R.string.cd_notification_bell)
                else resources.getQuantityString(R.plurals.dashboard_unread_notifications, unread, unread)
        }
    }

    private fun setupPrayer() {
        observe(HoodeRepository.prayerTimings) { timings ->
            val next = timings.find { it.isNext }
            binding.tvPrayerName.text = next?.name ?: getString(R.string.dashboard_prayer_unavailable)
            binding.tvPrayerTime.text = next?.adhanTime ?: "—"
            val hasIqamah = next?.iqamahTime?.contains(":") == true
            binding.tvPrayerSub.text = next?.iqamahTime?.takeIf { hasIqamah }
                ?.let { getString(R.string.dashboard_iqamah, it) } ?: getString(R.string.dashboard_prayer_hint)
            val parts = next?.timeRemaining?.split(":")?.mapNotNull { it.toIntOrNull() }.orEmpty()
            val remaining = if (parts.size == 3) {
                when {
                    parts[0] > 0 -> getString(R.string.dashboard_hours_minutes, parts[0], parts[1])
                    parts[1] > 0 -> getString(R.string.dashboard_minutes, parts[1])
                    else -> getString(R.string.dashboard_seconds, parts[2])
                }
            } else ""
            binding.tvCountdown.isVisible = remaining.isNotBlank()
            binding.tvCountdown.text = getString(
                if (hasIqamah) R.string.dashboard_iqamah_countdown else R.string.dashboard_countdown, remaining
            )
        }
        observe(HoodeRepository.activeMosque) { binding.tvMosqueName.text = it.name }
    }

    private fun setupSponsors() {
        binding.cardCarousel.layoutParams = binding.cardCarousel.layoutParams.apply {
            height = ((270 + 160 * (resources.configuration.fontScale - 1).coerceAtLeast(0f)) * resources.displayMetrics.density).toInt()
        }
        observe(HoodeRepository.adSlides) { slides ->
            val items = slides.filter { it.isEnabled }.map {
                CarouselSlide(it.headline, it.subheadline, it.advertiser, it.ctaLabel, it.ctaUrl, it.imageUrl, it.description)
            }
            binding.partnersSection.isVisible = items.isNotEmpty()
            if (items != currentSlides || binding.carouselPager.adapter == null) {
                val position = binding.carouselPager.currentItem
                currentSlides = items
                binding.carouselPager.adapter = CarouselAdapter(items, ::showSponsoredDetailDialog)
                if (items.isNotEmpty()) binding.carouselPager.setCurrentItem(position.coerceAtMost(items.lastIndex), false)
            }
            updatePartnerPosition()
        }
        binding.carouselPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { updatePartnerPosition() }
        })
        binding.btnCarouselNext.setOnClickListener {
            if (currentSlides.size > 1) binding.carouselPager.setCurrentItem((binding.carouselPager.currentItem + 1) % currentSlides.size, ValueAnimator.areAnimatorsEnabled())
        }
    }

    private fun updatePartnerPosition() {
        if (_binding == null) return
        binding.tvPartnerCount.text = if (currentSlides.isEmpty()) "" else
            getString(R.string.dashboard_partner_count, binding.carouselPager.currentItem + 1, currentSlides.size)
        binding.btnCarouselNext.isVisible = currentSlides.size > 1
    }

    private fun setupNoticeboard() {
        binding.rvUpdates.layoutManager = LinearLayoutManager(requireContext())
        binding.chipGroupUpdates.check(selectedFilter)
        binding.chipGroupUpdates.setOnCheckedStateChangeListener { _, ids ->
            selectedFilter = ids.firstOrNull() ?: R.id.chip_all
            renderNoticeboard()
        }
        binding.btnUpdatesSeeAll.setOnClickListener {
            navigate(when (selectedFilter) {
                R.id.chip_events -> R.id.eventsFragment
                R.id.chip_polls -> R.id.pollsFragment
                R.id.chip_community -> R.id.activitiesFragment
                else -> R.id.newsFragment
            })
        }
        // Repository feeds contain the published records fetched by the existing backend client.
        observe(combine(HoodeRepository.newsArticles, HoodeRepository.events, HoodeRepository.polls, HoodeRepository.activities) { news, events, polls, activities ->
            val groups = listOf(
                news.map { HomeUpdateItem(getString(R.string.dashboard_news), it.title, it.summary, it.verifiedDate, R.id.newsFragment) },
                events.map { HomeUpdateItem(getString(R.string.dashboard_events), it.title, listOf(it.venue, it.time).filter(String::isNotBlank).joinToString(" · "), it.date, R.id.eventsFragment) },
                polls.map { HomeUpdateItem(getString(R.string.dashboard_polls), it.question, it.description, "", R.id.pollsFragment) },
                activities.map { HomeUpdateItem(getString(R.string.dashboard_activities), it.title, it.description, it.schedule, R.id.activitiesFragment) }
            )
            // Interleave categories so the overview does not bury a poll under a long news feed.
            buildList { for (index in 0 until (groups.maxOfOrNull { it.size } ?: 0)) groups.forEach { it.getOrNull(index)?.let(::add) } }
        }) {
            noticeboard = it
            renderNoticeboard()
        }
    }

    private fun renderNoticeboard() {
        val destination = when (selectedFilter) {
            R.id.chip_news -> R.id.newsFragment
            R.id.chip_events -> R.id.eventsFragment
            R.id.chip_polls -> R.id.pollsFragment
            R.id.chip_community -> R.id.activitiesFragment
            else -> null
        }
        val entries = noticeboard.filter { destination == null || it.destinationId == destination }.take(4)
        binding.rvUpdates.isVisible = entries.isNotEmpty()
        binding.emptyUpdates.isVisible = entries.isEmpty()
        binding.rvUpdates.adapter = UpdatesAdapter(entries) { navigate(it.destinationId) }
    }

    private fun setupHighlights() {
        binding.rvTodayHighlights.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        observe(HoodeRepository.highlights) { rows ->
            binding.highlightsSection.isVisible = rows.isNotEmpty()
            binding.rvTodayHighlights.adapter = HighlightsAdapter(rows.take(6).map {
                HighlightItem(it.title, it.text("subtitle"), it.text("category"), it.imageUrl, it.text("description"))
            }) { item ->
                activeDialog = AlertDialog.Builder(requireContext()).setTitle(item.title)
                    .setMessage(item.description.ifBlank { item.subtitle }).setPositiveButton(R.string.close, null).show()
            }
        }
    }

    private fun setupPersonality() {
        observe(HoodeRepository.dailyPersonality) { profile ->
            binding.personalityCard.isVisible = profile.name.isNotBlank()
            binding.tvPersonalityName.text = profile.name
            binding.tvPersonalityIntro.text = profile.intro
            binding.ivPersonalityPhoto.load(profile.imageUrl.takeIf(String::isNotBlank)) {
                placeholder(R.drawable.profile_placeholder)
                fallback(R.drawable.profile_placeholder)
                error(R.drawable.profile_placeholder)
            }
        }
    }

    private fun setupGallery() {
        binding.rvGalleryPreview.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        observe(HoodeRepository.galleryItems) { photos ->
            binding.rvGalleryPreview.isVisible = photos.isNotEmpty()
            binding.emptyGallery.isVisible = photos.isEmpty()
            binding.rvGalleryPreview.adapter = GalleryPreviewAdapter(photos.take(6)) { navigate(R.id.galleryFragment) }
        }
    }

    private fun refreshDashboard() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.dashboardRefresh.isRefreshing = true
            binding.tvRefreshMessage.isVisible = false
            try {
                val finished = withTimeoutOrNull(60_000) { HoodeRepository.syncWithCloud().join(); true } ?: false
                _binding?.let {
                    it.tvRefreshMessage.isVisible = !finished
                    it.tvRefreshMessage.setText(R.string.dashboard_refresh_timeout)
                }
            } finally { _binding?.dashboardRefresh?.isRefreshing = false }
        }
    }

    private fun showSponsoredDetailDialog(slide: CarouselSlide) {

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogSponsoredDetailBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.tvModalHeadline.text = slide.headline
        dialogBinding.tvModalSubheadline.text = slide.subheadline
        dialogBinding.tvModalDescription.text = slide.description
        dialogBinding.tvModalDescription.visibility = if(slide.description.isBlank())View.GONE else View.VISIBLE
        dialogBinding.tvModalAdvertiser.text = slide.advertiser
        if (!slide.imageUrl.isNullOrBlank()) {
            dialogBinding.ivModalBanner.load(slide.imageUrl) {
                crossfade(true)
            }
        }
        if (!slide.ctaLabel.isNullOrBlank()) {
            dialogBinding.btnModalLearnMore.text = slide.ctaLabel
        }

        dialogBinding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.btnModalClose.setOnClickListener {
            dialog.dismiss()
        }

        val partnerUri = slide.ctaUrl?.takeIf { it.isNotBlank() }?.let(android.net.Uri::parse)
        dialogBinding.btnModalLearnMore.visibility =
            if (partnerUri?.scheme in setOf("https", "http")) View.VISIBLE else View.GONE
        dialogBinding.btnModalLearnMore.setOnClickListener {
            try {
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, partnerUri))
                dialog.dismiss()
            } catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(requireContext(), "No browser is available to open this link.", Toast.LENGTH_LONG).show()
            }
        }

        activeDialog = dialog
        dialog.setOnDismissListener { activeDialog = null }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels*0.92).toInt(), (resources.displayMetrics.heightPixels*0.85).toInt())
    }

}

data class CarouselSlide(
    val headline: String,
    val subheadline: String,
    val advertiser: String,
    val ctaLabel: String? = null,
    val ctaUrl: String? = null,
    val imageUrl: String? = null,
    val description: String = ""
)

data class HighlightItem(
    val title: String,
    val subtitle: String,
    val category: String,
    val imageUrl: String? = null,
    val description: String = ""
)

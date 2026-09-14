package com.example.hoode_app.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.hoode_app.R
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import coil.load
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.DialogSponsoredDetailBinding
import com.example.hoode_app.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var currentSlides: List<CarouselSlide> = emptyList()

    // Carousel auto-advance
    private val autoAdvanceHandler = Handler(Looper.getMainLooper())
    private val autoAdvanceRunnable = object : Runnable {
        override fun run() {
            val currentItem = binding.carouselPager.currentItem
            val count = binding.carouselPager.adapter?.itemCount ?: CAROUSEL_SLIDE_COUNT
            val nextItem = if (currentItem >= count - 1) 0 else currentItem + 1
            binding.carouselPager.setCurrentItem(nextItem, true)
            autoAdvanceHandler.postDelayed(this, CAROUSEL_INTERVAL_MS)
        }
    }

    companion object {
        private const val CAROUSEL_SLIDE_COUNT = 5
        private const val CAROUSEL_INTERVAL_MS = 3000L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGreeting()
        setupDate()
        setupAirtelNavigation()
        setupActivitiesBoxes()
        setupCarousel()
        setupPrayerHero()
        setupQuickActions()
        setupTodayHighlights()
        setupPersonality()
        setupUpdatesSection()
        setupGallerySection()

        binding.btnNotification.setOnClickListener {
            findNavController().navigate(R.id.notificationCenterFragment)
        }

        binding.btnCarouselSeeAll.setOnClickListener {
            findNavController().navigate(R.id.exploreFragment)
        }

        binding.btnCarouselMenu.setOnClickListener {
            Toast.makeText(requireContext(), "Featured community announcements & sponsorships", Toast.LENGTH_SHORT).show()
        }

        binding.communitySelector.setOnClickListener {
            val communities = arrayOf(
                "🌊 Hoode Community (Coastal & Beach Ward • 6 Masjids • 4,200 Residents)"
            )
            AlertDialog.Builder(requireContext())
                .setTitle("Active Community")
                .setItems(communities) { _, _ ->
                    binding.tvCommunityName.text = "Hoode"
                    Toast.makeText(requireContext(), "Community: Hoode active", Toast.LENGTH_SHORT).show()
                }
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        startCarouselAutoAdvance()
    }

    override fun onPause() {
        super.onPause()
        stopCarouselAutoAdvance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCarouselAutoAdvance()
        _binding = null
    }

    // ── Greeting ──────────────────────────────────────────────

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> getString(R.string.home_greeting_morning)
            hour < 17 -> getString(R.string.home_greeting_afternoon)
            else -> getString(R.string.home_greeting_evening)
        }
        val user = HoodeRepository.currentUser.value
        val name = user?.displayName ?: "Resident"
        binding.tvGreeting.text = "$greeting, $name"
    }

    private fun setupDate() {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date())
    }

    // ── Carousel ──────────────────────────────────────────────

    private fun setupCarousel() {
        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.adSlides.collectLatest { slides ->
                val carouselSlides = slides.map {
                    CarouselSlide(
                        headline = it.headline,
                        subheadline = it.subheadline,
                        advertiser = it.advertiser,
                        ctaLabel = "Know More",
                        imageUrl = it.imageUrl
                    )
                }

                currentSlides = carouselSlides
                val adapter = CarouselAdapter(carouselSlides) { slide ->
                    showSponsoredDetailDialog(slide)
                }
                binding.carouselPager.adapter = adapter
                binding.carouselPager.offscreenPageLimit = 1
                binding.cardCarousel.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                binding.cardCarousel.clipToOutline = true
                binding.carouselPager.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                binding.carouselPager.clipToOutline = true
                binding.carouselPager.clipChildren = true
                (binding.carouselPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.apply {
                    clipToPadding = false
                    clipChildren = true
                }

                setupCarouselIndicators(carouselSlides.size)
                updateCarouselIndicators(0)
            }
        }

        binding.carouselPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateCarouselIndicators(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    stopCarouselAutoAdvance()
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    startCarouselAutoAdvance()
                }
            }
        })
    }

    private fun setupCarouselIndicators(count: Int) {
        binding.carouselIndicators.removeAllViews()
        for (i in 0 until count) {
            val dot = ImageView(requireContext())
            val params = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.indicator_dot),
                resources.getDimensionPixelSize(R.dimen.indicator_dot)
            )
            params.marginStart = if (i == 0) 0 else resources.getDimensionPixelSize(R.dimen.spacing_xs)
            params.marginEnd = if (i == count - 1) 0 else resources.getDimensionPixelSize(R.dimen.spacing_xs)
            dot.layoutParams = params
            dot.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.indicator_dot_inactive))
            binding.carouselIndicators.addView(dot)
        }
    }

    private fun updateCarouselIndicators(activePosition: Int) {
        for (i in 0 until binding.carouselIndicators.childCount) {
            val dot = binding.carouselIndicators.getChildAt(i) as ImageView
            val drawableRes = if (i == activePosition) {
                R.drawable.indicator_dot_active
            } else {
                R.drawable.indicator_dot_inactive
            }
            dot.setImageDrawable(ContextCompat.getDrawable(requireContext(), drawableRes))
        }
    }

    private fun startCarouselAutoAdvance() {
        autoAdvanceHandler.removeCallbacks(autoAdvanceRunnable)
        autoAdvanceHandler.postDelayed(autoAdvanceRunnable, CAROUSEL_INTERVAL_MS)
    }

    private fun stopCarouselAutoAdvance() {
        autoAdvanceHandler.removeCallbacks(autoAdvanceRunnable)
    }

    private fun showSponsoredDetailDialog(slide: CarouselSlide) {
        stopCarouselAutoAdvance()

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

        dialogBinding.btnModalLearnMore.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(
                requireContext(),
                "Opening partner link: ${slide.advertiser}",
                Toast.LENGTH_SHORT
            ).show()
        }

        dialog.setOnDismissListener {
            startCarouselAutoAdvance()
        }

        dialog.show()
    }

    // ── Airtel-Style Navigation & Actions ─────────────────────

    private fun setupAirtelNavigation() {
        // Drawer Menu Button (Top Bar)
        binding.btnMenuDrawer.setOnClickListener {
            showSideDrawerDialog()
        }
    }

    private fun showSideDrawerDialog() {
        val user = HoodeRepository.currentUser.value
        val name = user?.displayName ?: "Resident"
        val items = arrayOf(
            "👤 Profile & Account ($name)",
            "🕌 Mosque Timings & Iqamah",
            "🩸 Blood Donor Network",
            "🚨 24/7 Emergency Helplines",
            "🌊 Active Ward: Hoode",
            "📢 Community Notices & News"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Community Menu")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> findNavController().navigate(R.id.profileFragment)
                    1 -> findNavController().navigate(R.id.prayerDetailFragment)
                    2 -> findNavController().navigate(R.id.bloodNetworkFragment)
                    3 -> findNavController().navigate(R.id.emergencyFragment)
                    4 -> Toast.makeText(requireContext(), "Ward: Hoode active", Toast.LENGTH_SHORT).show()
                    5 -> findNavController().navigate(R.id.newsFragment)
                }
            }
            .setPositiveButton("Close", null)
            .show()
    }

    // ── Prayer Hero ───────────────────────────────────────────

    private fun setupPrayerHero() {
        binding.prayerHeroCard.setOnClickListener {
            findNavController().navigate(R.id.prayerDetailFragment)
        }

        binding.btnManageReminders.setOnClickListener {
            showQuickPrayerRemindersDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.prayerTimings.collectLatest { timings ->
                val next = timings.find { it.isNext } ?: timings.firstOrNull()
                if (next != null) {
                    binding.tvPrayerName.text = "Next: ${next.name}"
                    binding.tvPrayerTime.text = "• ${next.adhanTime}"
                    binding.tvPrayerSub.text = "Iqamah ${next.iqamahTime}"

                    if (next.timeRemaining.isNotBlank()) {
                        val parts = next.timeRemaining.split(":")
                        if (parts.size == 3) {
                            val h = parts[0].toIntOrNull() ?: 0
                            val m = parts[1].toIntOrNull() ?: 0
                            val s = parts[2].toIntOrNull() ?: 0
                            val totalSecs = h * 3600 + m * 60 + s

                            binding.tvCountdown.text = when {
                                h > 0 -> "${h}h ${m}m"
                                m > 0 -> "${m}m"
                                else -> "${s}s"
                            }

                            // Calculate countdown circle progress (e.g. 2.5 hour window)
                            val progressPct = ((totalSecs.coerceIn(0, 9000) / 9000f) * 100).toInt()
                            binding.prayerCountdownRing.setProgressCompat(progressPct.coerceIn(5, 100), true)
                        } else {
                            binding.tvCountdown.text = next.timeRemaining
                        }
                    } else {
                        binding.tvCountdown.text = "—"
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.activeMosque.collectLatest { mosque ->
                binding.tvMosqueName.text = mosque.name
            }
        }
    }

    // ── Activities (3x3 Grid of 9 Cards) ─────────────────────

    private fun setupActivitiesBoxes() {
        binding.boxActReligious.setOnClickListener {
            HoodeRepository.initialActivityCategory = "Religious"
            findNavController().navigate(R.id.activitiesFragment)
        }
        binding.boxActCommunity.setOnClickListener {
            HoodeRepository.initialActivityCategory = "Community"
            findNavController().navigate(R.id.activitiesFragment)
        }
        binding.boxActSocial.setOnClickListener {
            HoodeRepository.initialActivityCategory = "Social"
            findNavController().navigate(R.id.activitiesFragment)
        }
        binding.boxActSports.setOnClickListener {
            HoodeRepository.initialActivityCategory = "Sports"
            findNavController().navigate(R.id.activitiesFragment)
        }
        binding.boxActCondolences.setOnClickListener {
            HoodeRepository.initialActivityCategory = "Condolences"
            findNavController().navigate(R.id.activitiesFragment)
        }
        binding.boxActDua.setOnClickListener {
            HoodeRepository.initialActivityCategory = "Dua Request"
            findNavController().navigate(R.id.activitiesFragment)
        }
        binding.boxActBlood.setOnClickListener {
            findNavController().navigate(R.id.bloodNetworkFragment)
        }
        binding.boxActMosques.setOnClickListener {
            findNavController().navigate(R.id.prayerDetailFragment)
        }
        binding.boxActEmergency.setOnClickListener {
            findNavController().navigate(R.id.emergencyFragment)
        }
        binding.btnActivitiesSeeAll.setOnClickListener {
            HoodeRepository.initialActivityCategory = "All"
            findNavController().navigate(R.id.activitiesFragment)
        }
    }

    // ── Quick Actions ─────────────────────────────────────────

    private fun setupQuickActions() {
        binding.actionEmergency.setOnClickListener {
            findNavController().navigate(R.id.emergencyFragment)
        }
        binding.actionBlood.setOnClickListener {
            findNavController().navigate(R.id.bloodNetworkFragment)
        }
        binding.actionPrayer.setOnClickListener {
            findNavController().navigate(R.id.prayerDetailFragment)
        }
        binding.actionJobs.setOnClickListener {
            findNavController().navigate(R.id.jobsFragment)
        }
        binding.actionTournaments.setOnClickListener {
            findNavController().navigate(R.id.tournamentsFragment)
        }
        binding.actionEvents.setOnClickListener {
            findNavController().navigate(R.id.eventsFragment)
        }
        binding.actionServices.setOnClickListener {
            findNavController().navigate(R.id.providersFragment)
        }
        binding.actionNews.setOnClickListener {
            findNavController().navigate(R.id.newsFragment)
        }
        binding.actionPolls.setOnClickListener {
            findNavController().navigate(R.id.pollsFragment)
        }
        binding.actionHuffaz.setOnClickListener {
            findNavController().navigate(R.id.huffazFragment)
        }
    }

    // ── Today's Highlights ────────────────────────────────────

    private fun setupTodayHighlights() {
        val highlights = listOf(
            HighlightItem(
                "Community Iftar & Dua",
                "Today, 6:15 PM",
                "Religious",
                "https://images.unsplash.com/photo-1542838132-92c53300491e?w=500&auto=format&fit=crop&q=80"
            ),
            HighlightItem(
                "HPL Cricket Tournament",
                "18–21 Sep 2026",
                "Sports",
                "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?w=500&auto=format&fit=crop&q=80"
            ),
            HighlightItem(
                "Qur'an Study Circle",
                "Friday post-Maghrib",
                "Religious",
                "https://images.unsplash.com/photo-1609599006353-e629aaabfeae?w=500&auto=format&fit=crop&q=80"
            ),
            HighlightItem(
                "Free Health Checkup Camp",
                "Sunday, 9:00 AM",
                "Community",
                "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=500&auto=format&fit=crop&q=80"
            )
        )

        binding.rvTodayHighlights.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvTodayHighlights.adapter = HighlightsAdapter(highlights) { item ->
            when (item.category) {
                "Sports" -> findNavController().navigate(R.id.tournamentsFragment)
                "Religious" -> findNavController().navigate(R.id.prayerDetailFragment)
                else -> findNavController().navigate(R.id.eventsFragment)
            }
        }
    }

    // ── Personality ───────────────────────────────────────────

    private fun setupPersonality() {
        binding.personalityCard.setOnClickListener {
            findNavController().navigate(R.id.personalityDetailFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.dailyPersonality.collectLatest { profile ->
                binding.tvPersonalityName.text = profile.name
                binding.tvPersonalityIntro.text = profile.intro
                if (profile.imageUrl.isNotBlank()) {
                    binding.ivPersonalityPhoto.load(profile.imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.bg_circle_lavender)
                        error(R.drawable.bg_circle_lavender)
                    }
                }
            }
        }
    }

    // ── Updates Section ───────────────────────────────────────

    private fun setupUpdatesSection() {
        val allUpdates = listOf(
            HomeUpdateItem("News", "New Bengre–Hoode Coastal Seawall Approved", "State fisheries department sanctions ₹4.2 crore for sea erosion prevention.", "Today", R.id.newsFragment),
            HomeUpdateItem("Poll", "Community Poll: Cleanliness Drive Sunday", "Should Hoode Beach Cleanliness drive commence at 6:30 AM or 7:30 AM?", "Live", R.id.pollsFragment),
            HomeUpdateItem("Activity", "Inter-Madrasa Qur'an & Tajweed Competition", "Annual recitation competition at Hoode Juma Masjid hall on Sep 20.", "Upcoming", R.id.activitiesFragment),
            HomeUpdateItem("News", "Baitul Mal Scholarship 2026 Phase 2", "Applications open for deserving youth pursuing higher education in Udupi.", "Sep 08", R.id.newsFragment)
        )

        fun updateList(categoryFilter: String) {
            val filtered = if (categoryFilter == "All") {
                allUpdates
            } else {
                allUpdates.filter { it.category.equals(categoryFilter, ignoreCase = true) }
            }
            binding.rvUpdates.adapter = UpdatesAdapter(filtered) { update ->
                findNavController().navigate(update.destinationId)
            }
        }

        binding.rvUpdates.layoutManager = LinearLayoutManager(requireContext())
        updateList("All")

        binding.chipGroupUpdates.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chip_news) -> updateList("News")
                checkedIds.contains(R.id.chip_polls) -> updateList("Poll")
                checkedIds.contains(R.id.chip_community) -> updateList("Activity")
                else -> updateList("All")
            }
        }

        binding.btnUpdatesSeeAll.setOnClickListener {
            findNavController().navigate(R.id.newsFragment)
        }
    }

    // ── Gallery Preview ───────────────────────────────────────

    private fun setupGallerySection() {
        binding.rvGalleryPreview.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.galleryItems.collectLatest { photos ->
                val previewPhotos = photos.take(6)
                binding.rvGalleryPreview.adapter = GalleryPreviewAdapter(previewPhotos) {
                    findNavController().navigate(R.id.galleryFragment)
                }
            }
        }

        binding.btnGallerySeeAll.setOnClickListener {
            findNavController().navigate(R.id.galleryFragment)
        }
    }

    private fun showQuickPrayerRemindersDialog() {
        val options = arrayOf(
            "🔔 Enable Adhan Audio (All 5 Prayers)",
            "⏱️ 10-Minute Iqamah Warning Alert",
            "🕌 Switch Primary Mosque"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Manage Prayer Reminders")
            .setItems(options) { _, which ->
                when (which) {
                    0, 1 -> Toast.makeText(requireContext(), "Prayer reminder settings saved.", Toast.LENGTH_SHORT).show()
                    2 -> findNavController().navigate(R.id.prayerDetailFragment)
                }
            }
            .setPositiveButton("Full Timetable") { _, _ ->
                findNavController().navigate(R.id.prayerDetailFragment)
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }
}

// ── Data Classes ──────────────────────────────────────────────

data class CarouselSlide(
    val headline: String,
    val subheadline: String,
    val advertiser: String,
    val ctaLabel: String? = null,
    val ctaUrl: String? = null,
    val imageUrl: String? = null
)

data class HighlightItem(
    val title: String,
    val subtitle: String,
    val category: String,
    val imageUrl: String? = null
)

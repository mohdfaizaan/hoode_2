package com.example.hoode_app.ui.explore

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.databinding.FragmentExploreBinding
import com.example.hoode_app.databinding.ItemExploreFeatureBinding
import com.example.hoode_app.databinding.ItemExploreHeroBinding

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val allCards = mutableListOf<Pair<View, String>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        allCards.clear()
        setupHeroCards()
        setupFeatureTiles()
        setupInteractiveSearch()
    }

    private fun setupHeroCards() {
        configureHero(
            binding.featurePrayer,
            R.drawable.ic_prayer,
            getString(R.string.feature_prayer),
            "Adhan & Iqamah at your mosque",
            R.drawable.bg_hero_prayer,
            R.id.prayerDetailFragment,
            "prayer namaz adhan iqamah mosque fajr dhuhr asr maghrib isha"
        )

        configureHero(
            binding.featureEmergency,
            R.drawable.ic_phone,
            getString(R.string.feature_emergency),
            "Ambulance, police, clinic",
            R.drawable.bg_hero_emergency,
            R.id.emergencyFragment,
            "emergency ambulance police fire clinic hospital doctor call"
        )
    }

    private fun setupFeatureTiles() {
        // ── Faith & Time ────────────────────────────────────────────
        configureTile(
            binding.featureRamadan,
            R.drawable.ic_ramadan,
            getString(R.string.feature_ramadan),
            R.id.ramadanFragment,
            "ramadan suhoor sehri iftar fasting timetable roza",
            R.color.warning_soft,
            R.color.warning
        )

        configureTile(
            binding.featureCalendar,
            R.drawable.ic_calendar,
            "Community Calendar",
            R.id.calendarFragment,
            "calendar event holiday annual month date schedule",
            R.color.surface_dim,
            R.color.accent
        )

        // ── Community ───────────────────────────────────────────────
        configureTile(
            binding.featureEvents,
            R.drawable.ic_events,
            getString(R.string.feature_events),
            R.id.eventsFragment,
            "events wedding nikah reception meeting festival gathering",
            R.color.navy_soft,
            R.color.navy
        )

        configureTile(
            binding.featureActivities,
            R.drawable.ic_activities,
            getString(R.string.feature_activities),
            R.id.activitiesFragment,
            "activities sports cricket football badminton youth celebration",
            R.color.success_soft,
            R.color.success
        )

        configureTile(
            binding.featureTournaments,
            R.drawable.ic_tournament,
            "Tournaments",
            R.id.tournamentsFragment,
            "tournament sports cricket match fixtures score table standings",
            R.color.warning_soft,
            R.color.warning
        )

        configureTile(
            binding.featureNews,
            R.drawable.ic_news,
            getString(R.string.feature_news),
            R.id.newsFragment,
            "news verified rumor announcement council update official",
            R.color.surface_dim,
            R.color.accent
        )

        // ── Learning & People ───────────────────────────────────────
        configureTile(
            binding.featureEducation,
            R.drawable.ic_education,
            getString(R.string.feature_education),
            R.id.educationFragment,
            "education madrasa school tuition tutoring learning class",
            R.color.surface_dim,
            R.color.accent
        )

        configureTile(
            binding.featureHuffaz,
            R.drawable.ic_huffaz,
            getString(R.string.feature_huffaz),
            R.id.huffazFragment,
            "huffaz hafiz quran tajweed memorizer honor ustad",
            R.color.success_soft,
            R.color.success
        )

        configureTile(
            binding.featurePersonality,
            R.drawable.ic_personality,
            "Daily Personality",
            R.id.personalityDetailFragment,
            "personality person patriarch biography profile history leader",
            R.color.warning_soft,
            R.color.warning
        )

        configureTile(
            binding.featureGallery,
            R.drawable.ic_gallery,
            getString(R.string.feature_gallery),
            R.id.galleryFragment,
            "gallery photo picture beach delta sunset boat hoode scenic",
            R.color.navy_soft,
            R.color.navy
        )

        // ── Work & Services ─────────────────────────────────────────
        configureTile(
            binding.featureJobs,
            R.drawable.ic_jobs,
            getString(R.string.feature_jobs),
            R.id.jobsFragment,
            "jobs employment vacancy work driver shop hire gig volunteer",
            R.color.success_soft,
            R.color.success
        )

        configureTile(
            binding.featureMarketplace,
            R.drawable.ic_classified,
            getString(R.string.feature_classifieds),
            R.id.marketplaceFragment,
            "classifieds marketplace buy sell teak furniture vehicle used",
            R.color.warning_soft,
            R.color.warning
        )

        configureTile(
            binding.featureProviders,
            R.drawable.ic_services,
            getString(R.string.feature_providers),
            R.id.providersFragment,
            "providers handyman plumber electrician carpenter repair service",
            R.color.surface_dim,
            R.color.accent
        )

        configureTile(
            binding.featureBlood,
            R.drawable.ic_blood,
            getString(R.string.feature_blood),
            R.id.bloodNetworkFragment,
            "blood donor emergency hospital donation ABO platelet",
            R.color.danger_soft,
            R.color.danger
        )

        configureTile(
            binding.featureLostFound,
            R.drawable.ic_search,
            getString(R.string.feature_lost_found),
            R.id.lostFoundFragment,
            "lost found wallet keys document phone return search",
            R.color.navy_soft,
            R.color.navy
        )

        // ── Civic Voice & Rewards ───────────────────────────────────
        configureTile(
            binding.featurePolls,
            R.drawable.ic_poll,
            getString(R.string.feature_polls),
            R.id.pollsFragment,
            "polls vote civic issue road streetlight water election feedback",
            R.color.surface_dim,
            R.color.accent
        )

        configureTile(
            binding.featureBadges,
            R.drawable.ic_badge,
            getString(R.string.feature_badges),
            R.id.badgesFragment,
            "contributions badges rewards points ledger honors",
            R.color.warning_soft,
            R.color.warning
        )
    }

    private fun configureHero(
        heroBinding: ItemExploreHeroBinding,
        iconRes: Int,
        title: String,
        subtitle: String,
        backgroundRes: Int,
        destinationId: Int,
        keywords: String
    ) {
        heroBinding.apply {
            llHeroContent.setBackgroundResource(backgroundRes)
            ivHeroIcon.setImageResource(iconRes)
            ivHeroIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
            tvHeroTitle.text = title
            tvHeroSubtitle.text = subtitle
            root.setOnClickListener {
                findNavController().navigate(destinationId)
            }
        }
        allCards.add(heroBinding.root to "$title $subtitle $keywords".lowercase())
    }

    private fun configureTile(
        tileBinding: ItemExploreFeatureBinding,
        iconRes: Int,
        title: String,
        destinationId: Int,
        keywords: String,
        chipBackgroundRes: Int,
        iconTintRes: Int
    ) {
        val chipColor = ContextCompat.getColor(requireContext(), chipBackgroundRes)
        val tintColor = ContextCompat.getColor(requireContext(), iconTintRes)
        tileBinding.apply {
            flIconChip.backgroundTintList = ColorStateList.valueOf(chipColor)
            ivFeatureIcon.setImageResource(iconRes)
            ivFeatureIcon.setColorFilter(tintColor)
            tvFeatureName.text = title
            root.setOnClickListener {
                findNavController().navigate(destinationId)
            }
        }
        allCards.add(tileBinding.root to "$title $keywords".lowercase())
    }

    private fun setupInteractiveSearch() {
        binding.etExploreSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterFeatures(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etExploreSearch.setText("")
            binding.etExploreSearch.requestFocus()
        }
    }

    private fun filterFeatures(query: String) {
        binding.tvSearchEmpty.visibility =
            if (query.isNotEmpty() && allCards.none { it.second.contains(query) }) View.VISIBLE else View.GONE
        if (query.isEmpty()) {
            showAllHeaders(View.VISIBLE)
            for ((root, _) in allCards) {
                root.visibility = View.VISIBLE
            }
        } else {
            showAllHeaders(View.GONE)
            for ((root, searchable) in allCards) {
                root.visibility = if (searchable.contains(query)) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAllHeaders(visibility: Int) {
        binding.headerEssentials.visibility = visibility
        binding.headerFaith.visibility = visibility
        binding.headerCommunity.visibility = visibility
        binding.headerLearning.visibility = visibility
        binding.headerOpportunities.visibility = visibility
        binding.headerCivic.visibility = visibility
    }

    override fun onDestroyView() {
        super.onDestroyView()
        allCards.clear()
        _binding = null
    }
}

package com.example.hoode_app.ui.explore

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.databinding.FragmentExploreBinding
import com.example.hoode_app.databinding.ItemExploreFeatureBinding

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val allCards = mutableListOf<Pair<ItemExploreFeatureBinding, String>>()

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
        setupFeatureCards()
        setupInteractiveSearch()
    }

    private fun setupFeatureCards() {
        // ── 1. Faith & Time ──────────────────────────────────────────
        configureCard(
            binding.featurePrayer,
            R.drawable.ic_prayer,
            getString(R.string.feature_prayer),
            "Live timetable & Iqamah ring",
            R.id.prayerDetailFragment,
            "prayer namaz adhan iqamah mosque fajr dhuhr asr maghrib isha"
        )

        configureCard(
            binding.featureRamadan,
            R.drawable.ic_ramadan,
            getString(R.string.feature_ramadan),
            "Suhoor & Iftar countdown",
            R.id.ramadanFragment,
            "ramadan suhoor sehri iftar fasting timetable roza"
        )

        configureCard(
            binding.featureCalendar,
            R.drawable.ic_calendar,
            "Community Calendar",
            "Annual events & local holidays",
            R.id.calendarFragment,
            "calendar event holiday annual month date schedule"
        )

        // ── 2. Help & Emergency ──────────────────────────────────────
        configureCard(
            binding.featureEmergency,
            R.drawable.ic_phone,
            getString(R.string.feature_emergency),
            "Ambulance, police, clinic",
            R.id.emergencyFragment,
            "emergency ambulance police fire clinic hospital doctor call"
        )

        configureCard(
            binding.featureBlood,
            R.drawable.ic_blood,
            getString(R.string.feature_blood),
            "Urgent donors & requests",
            R.id.bloodNetworkFragment,
            "blood donor emergency hospital donation ABO platelet"
        )

        // ── 3. Community Gatherings & Sports ─────────────────────────
        configureCard(
            binding.featureEvents,
            R.drawable.ic_events,
            getString(R.string.feature_events),
            "Weddings & gatherings",
            R.id.eventsFragment,
            "events wedding nikah reception meeting festival gathering"
        )

        configureCard(
            binding.featureActivities,
            R.drawable.ic_activities,
            getString(R.string.feature_activities),
            "Sports & celebrations",
            R.id.activitiesFragment,
            "activities sports cricket football badminton youth celebration"
        )

        configureCard(
            binding.featureTournaments,
            R.drawable.ic_tournament,
            "HPL Tournaments",
            "Fixtures, points & tables",
            R.id.tournamentsFragment,
            "tournament hpl cricket match fixtures score table standings"
        )

        configureCard(
            binding.featureLostFound,
            R.drawable.ic_search,
            getString(R.string.feature_lost_found),
            "Claims & reports hub",
            R.id.lostFoundFragment,
            "lost found wallet keys document phone return search"
        )

        // ── 4. Heritage & People ─────────────────────────────────────
        configureCard(
            binding.featureEducation,
            R.drawable.ic_education,
            getString(R.string.feature_education),
            "Madrasas & tutoring",
            R.id.educationFragment,
            "education madrasa school tuition tutoring learning class"
        )

        configureCard(
            binding.featureHuffaz,
            R.drawable.ic_huffaz,
            getString(R.string.feature_huffaz),
            "Qur'an memorizers roll",
            R.id.huffazFragment,
            "huffaz hafiz quran tajweed memorizer honor ustad"
        )

        configureCard(
            binding.featurePersonality,
            R.drawable.ic_personality,
            "Daily Personality",
            "Community patriarch spotlight",
            R.id.personalityDetailFragment,
            "personality person patriarch biography profile history leader"
        )

        configureCard(
            binding.featureGallery,
            R.drawable.ic_gallery,
            getString(R.string.feature_gallery),
            "Coastal & scenic 25 photos",
            R.id.galleryFragment,
            "gallery photo picture beach delta sunset boat hoode scenic"
        )

        // ── 5. Opportunities & Commerce ──────────────────────────────
        configureCard(
            binding.featureJobs,
            R.drawable.ic_jobs,
            getString(R.string.feature_jobs),
            "Local jobs & gigs",
            R.id.jobsFragment,
            "jobs employment vacancy work driver shop hire gig volunteer"
        )

        configureCard(
            binding.featureMarketplace,
            R.drawable.ic_classified,
            getString(R.string.feature_classifieds),
            "Buy, sell & donate",
            R.id.marketplaceFragment,
            "classifieds marketplace buy sell teak furniture vehicle used"
        )

        configureCard(
            binding.featureProviders,
            R.drawable.ic_services,
            getString(R.string.feature_providers),
            "Electrician, plumber, tutor",
            R.id.providersFragment,
            "providers handyman plumber electrician carpenter repair service"
        )

        configureCard(
            binding.featureNews,
            R.drawable.ic_news,
            getString(R.string.feature_news),
            "Official & rumor updates",
            R.id.newsFragment,
            "news verified rumor announcement council update official"
        )

        // ── 6. Civic Voice & Rewards ─────────────────────────────────
        configureCard(
            binding.featurePolls,
            R.drawable.ic_poll,
            getString(R.string.feature_polls),
            "Civic issues & voting",
            R.id.pollsFragment,
            "polls vote civic issue road streetlight water election feedback"
        )

        configureCard(
            binding.featureBadges,
            R.drawable.ic_badge,
            getString(R.string.feature_badges),
            "Points & rewards ledger",
            R.id.badgesFragment,
            "badges rewards points contribution pillar volunteer honors"
        )
    }

    private fun configureCard(
        cardBinding: ItemExploreFeatureBinding,
        iconRes: Int,
        title: String,
        desc: String,
        destinationId: Int,
        keywords: String
    ) {
        cardBinding.apply {
            ivFeatureIcon.setImageResource(iconRes)
            tvFeatureName.text = title
            tvFeatureDesc.text = desc
            root.setOnClickListener {
                findNavController().navigate(destinationId)
            }
        }
        allCards.add(cardBinding to "$title $desc $keywords".lowercase())
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
        }
    }

    private fun filterFeatures(query: String) {
        if (query.isEmpty()) {
            for ((cardBinding, _) in allCards) {
                cardBinding.root.visibility = View.VISIBLE
            }
            showAllHeaders(View.VISIBLE)
        } else {
            for ((cardBinding, searchContent) in allCards) {
                val match = searchContent.contains(query)
                cardBinding.root.visibility = if (match) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAllHeaders(visibility: Int) {
        binding.headerFaith.visibility = visibility
        binding.headerHelp.visibility = visibility
        binding.headerCommunity.visibility = visibility
        binding.headerLearning.visibility = visibility
        binding.headerOpportunities.visibility = visibility
        binding.headerCivic.visibility = visibility
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

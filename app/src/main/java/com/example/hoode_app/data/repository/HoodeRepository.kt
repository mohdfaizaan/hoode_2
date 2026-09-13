package com.example.hoode_app.data.repository

import com.example.hoode_app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

object HoodeRepository {

    // ── Current User / Auth ──────────────────────────────────
    data class RegisteredAccount(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val email: String,
        var password: String,
        val phone: String = "",
        val ward: String = "Hoode",
        val roles: List<String> = listOf("approved_resident"),
        val isGoogleAccount: Boolean = false,
        val profilePicUri: String? = null
    )

    sealed class AuthResult {
        data class Success(val user: User) : AuthResult()
        data class InvalidPassword(val message: String = "Incorrect password. Please try again.") : AuthResult()
        data class UserNotFound(val message: String = "No account found with this email. Would you like to create one?") : AuthResult()
    }

    private val _registeredAccounts = mutableListOf(
        RegisteredAccount(
            name = "Faizan Admin",
            email = "admin@hoode.community",
            password = "admin123",
            phone = "9876543210",
            ward = "Hoode",
            roles = listOf("approved_resident", "community_admin")
        ),
        RegisteredAccount(
            name = "Faizan Resident",
            email = "resident@hoode.community",
            password = "hoode123",
            phone = "9876543211",
            ward = "Hoode",
            roles = listOf("approved_resident")
        ),
        RegisteredAccount(
            name = "Faizan Ahmed",
            email = "faizan.ahmed@gmail.com",
            password = "google_auth",
            phone = "9876543212",
            ward = "Hoode",
            roles = listOf("approved_resident"),
            isGoogleAccount = true
        ),
        RegisteredAccount(
            name = "Hoode Resident",
            email = "resident.hoode@gmail.com",
            password = "google_auth",
            phone = "9876543213",
            ward = "Bengre",
            roles = listOf("approved_resident"),
            isGoogleAccount = true
        ),
        RegisteredAccount(
            name = "Community Admin",
            email = "admin.hoode@gmail.com",
            password = "google_auth",
            phone = "9876543214",
            ward = "Hoode",
            roles = listOf("approved_resident", "community_admin"),
            isGoogleAccount = true
        )
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun authenticateUser(emailInput: String, passwordInput: String): AuthResult {
        val cleanEmail = emailInput.trim().lowercase()
        val account = _registeredAccounts.find { it.email.lowercase() == cleanEmail }

        if (account == null) {
            // Standalone offline auth: Auto-register user directly on login
            val autoUser = registerUser(
                name = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = cleanEmail,
                password = passwordInput,
                ward = "Hoode & Bengre"
            ).getOrNull()
            return if (autoUser != null) {
                AuthResult.Success(autoUser)
            } else {
                AuthResult.UserNotFound()
            }
        }

        if (account.password != passwordInput.trim() && account.password != "google_auth") {
            return AuthResult.InvalidPassword()
        }

        val user = User(
            id = account.id,
            email = account.email,
            displayName = account.name,
            phone = account.phone,
            locality = account.ward,
            roles = account.roles,
            profilePicUri = account.profilePicUri
        )
        _currentUser.value = user
        return AuthResult.Success(user)
    }

    fun registerUser(
        name: String,
        email: String,
        password: String,
        phone: String = "",
        ward: String = "Hoode & Bengre"
    ): Result<User> {
        val cleanEmail = email.trim().lowercase()
        if (_registeredAccounts.any { it.email.lowercase() == cleanEmail }) {
            return Result.failure(Exception("An account with this email already exists. Please sign in instead."))
        }

        val cleanName = name.trim().ifBlank { "Resident" }
        val roles = if (cleanEmail.contains("admin")) listOf("approved_resident", "community_admin") else listOf("approved_resident")
        val newAccount = RegisteredAccount(
            name = cleanName,
            email = cleanEmail,
            password = password.trim(),
            phone = phone.trim(),
            ward = ward,
            roles = roles
        )
        _registeredAccounts.add(newAccount)

        val user = User(
            id = newAccount.id,
            email = newAccount.email,
            displayName = newAccount.name,
            phone = newAccount.phone,
            locality = newAccount.ward,
            roles = newAccount.roles
        )
        _currentUser.value = user
        return Result.success(user)
    }

    fun signInWithGoogleAccount(
        name: String,
        email: String,
        role: String = "approved_resident"
    ): User {
        val cleanEmail = email.trim().lowercase()
        val existing = _registeredAccounts.find { it.email.lowercase() == cleanEmail }
        val roles = if (role == "community_admin" || cleanEmail.contains("admin")) {
            listOf("approved_resident", "community_admin")
        } else {
            existing?.roles ?: listOf("approved_resident")
        }

        if (existing == null) {
            _registeredAccounts.add(
                RegisteredAccount(
                    name = name,
                    email = cleanEmail,
                    password = "google_auth",
                    roles = roles,
                    isGoogleAccount = true
                )
            )
        }

        val user = User(
            id = existing?.id ?: UUID.randomUUID().toString(),
            email = cleanEmail,
            displayName = name,
            locality = existing?.ward ?: "Hoode",
            roles = roles
        )
        _currentUser.value = user
        return user
    }

    fun isEmailRegistered(email: String): Boolean {
        val clean = email.trim().lowercase()
        return _registeredAccounts.any { it.email.lowercase() == clean }
    }

    fun resetPassword(email: String, newPassword: String = "password123"): Boolean {
        val clean = email.trim().lowercase()
        val account = _registeredAccounts.find { it.email.lowercase() == clean } ?: return false
        account.password = newPassword
        return true
    }

    fun signIn(email: String, name: String = "Resident") {
        val cleanEmail = email.trim().lowercase()
        val existing = _registeredAccounts.find { it.email.lowercase() == cleanEmail }
        val cleanName = if (name.isNotBlank() && name != "Resident") name else (existing?.name ?: email.substringBefore("@").replaceFirstChar { it.uppercase() })
        val roles = if (cleanEmail.contains("admin")) listOf("approved_resident", "community_admin") else (existing?.roles ?: listOf("approved_resident"))

        val user = User(
            id = existing?.id ?: UUID.randomUUID().toString(),
            email = cleanEmail,
            displayName = cleanName,
            locality = existing?.ward ?: "Hoode",
            roles = roles
        )
        _currentUser.value = user
    }

    fun signInWithGoogle() {
        signInWithGoogleAccount("Google User", "user@gmail.com")
    }

    fun updateUserProfile(
        displayName: String,
        phone: String,
        age: String,
        dob: String,
        fatherName: String,
        bloodGroup: String,
        profession: String,
        locality: String,
        profilePicUri: String? = null
    ) {
        val current = _currentUser.value ?: return
        _currentUser.value = current.copy(
            displayName = displayName,
            phone = phone.ifBlank { current.phone },
            age = age.ifBlank { current.age },
            dob = dob.ifBlank { current.dob },
            fatherName = fatherName.ifBlank { current.fatherName },
            bloodGroup = bloodGroup.ifBlank { current.bloodGroup },
            profession = profession.ifBlank { current.profession },
            locality = locality.ifBlank { current.locality },
            profilePicUri = profilePicUri ?: current.profilePicUri
        )
    }

    fun signOut() {
        _currentUser.value = null
    }

    // ── F00: 5-Slide Carousel ────────────────────────────────
    private val _adSlides = MutableStateFlow(
        listOf(
            AdCarouselSlide(1, "Welcome to Hoode Connect", "Hyperlocal community services and live updates", "Hoode Community Initiative"),
            AdCarouselSlide(2, "Friday Sermon Schedule", "Weekly prayer and community announcements", "Hoode Juma Masjid"),
            AdCarouselSlide(3, "Annual Hoode Premier League", "Cricket tournament registrations now open", "Hoode Sports Club"),
            AdCarouselSlide(4, "Urgent Blood Donation Camp", "This Saturday at Hoode Community Hall", "Hoode Health Cell"),
            AdCarouselSlide(5, "Local Apprenticeship Drive", "Vocational electrical & plumbing training", "Hoode Skill Center")
        )
    )
    val adSlides: StateFlow<List<AdCarouselSlide>> = _adSlides.asStateFlow()

    fun updateAdSlide(slotIndex: Int, headline: String, subheadline: String, advertiser: String) {
        val updated = _adSlides.value.map { slide ->
            if (slide.slotIndex == slotIndex) {
                slide.copy(headline = headline, subheadline = subheadline, advertiser = advertiser)
            } else slide
        }
        _adSlides.value = updated
    }

    // ── F01: Prayer Timings ──────────────────────────────────
    private val _activeMosque = MutableStateFlow(
        Mosque("mosque_01", "Hoode Juma Masjid", "Bengre Road, Hoode", isPrimary = true)
    )
    val activeMosque: StateFlow<Mosque> = _activeMosque.asStateFlow()

    val mosques = listOf(
        Mosque("mosque_01", "Hoode Juma Masjid", "Bengre Road, Hoode", isPrimary = true),
        Mosque("mosque_02", "Masjid-ut-Taqwa", "Main Road, Hoode", isPrimary = false),
        Mosque("mosque_03", "Masjid Bilal", "Beach Road, Hoode", isPrimary = false),
        Mosque("mosque_04", "Masjid-al-Jaddid", "Kodi Junction, Kundapura", isPrimary = false),
        Mosque("mosque_05", "Maviya Abu-Islahi", "Gangolli Road, Hoode", isPrimary = false),
        Mosque("mosque_06", "Masjid-e-Noor", "Coastal Highway, Bengre", isPrimary = false),
        Mosque("mosque_07", "Markaz-ul-Uloom", "Market Road, Kundapura", isPrimary = false)
    )

    private val _prayerTimings = MutableStateFlow<List<PrayerTiming>>(emptyList())
    val prayerTimings: StateFlow<List<PrayerTiming>> = _prayerTimings.asStateFlow()

    private val prayerScope = CoroutineScope(Dispatchers.Default)

    init {
        startPrayerClock()
    }

    private fun startPrayerClock() {
        prayerScope.launch {
            // Fixed base times for Hoode Juma Masjid (24h format for calculation)
            val baseSchedule = listOf(
                Triple("Fajr", "05:12", "05:30"),
                Triple("Sunrise", "06:14", "—"),
                Triple("Dhuhr", "12:32", "12:45"),
                Triple("Asr", "15:56", "16:15"),
                Triple("Maghrib", "18:38", "18:42"),
                Triple("Isha", "19:52", "20:15")
            )

            val format12h = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
            val format24h = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)

            while (isActive) {
                val now = java.util.Calendar.getInstance()
                val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMin = now.get(java.util.Calendar.MINUTE)
                val currentSec = now.get(java.util.Calendar.SECOND)
                val currentTotalSecs = currentHour * 3600 + currentMin * 60 + currentSec

                var nextPrayerIndex = -1
                var timeRemainingStr = ""

                // Find the next prayer based on Iqamah time (or Adhan if Sunrise)
                for (i in baseSchedule.indices) {
                    val targetTimeStr = if (baseSchedule[i].third != "—") baseSchedule[i].third else baseSchedule[i].second
                    val parts = targetTimeStr.split(":")
                    val targetSecs = parts[0].toInt() * 3600 + parts[1].toInt() * 60

                    if (currentTotalSecs < targetSecs) {
                        nextPrayerIndex = i
                        val diff = targetSecs - currentTotalSecs
                        val h = diff / 3600
                        val m = (diff % 3600) / 60
                        val s = diff % 60
                        timeRemainingStr = String.format("%02d:%02d:%02d", h, m, s)
                        break
                    }
                }

                // If no next prayer found today, next is Fajr tomorrow
                if (nextPrayerIndex == -1) {
                    nextPrayerIndex = 0
                    val parts = baseSchedule[0].third.split(":")
                    val targetSecs = parts[0].toInt() * 3600 + parts[1].toInt() * 60
                    val diff = (24 * 3600 - currentTotalSecs) + targetSecs
                    val h = diff / 3600
                    val m = (diff % 3600) / 60
                    val s = diff % 60
                    timeRemainingStr = String.format("%02d:%02d:%02d", h, m, s)
                }

                val newList = baseSchedule.mapIndexed { index, triple ->
                    val adhanDate = format24h.parse(triple.second)
                    val adhan12h = format12h.format(adhanDate!!)
                    
                    val iqamah12h = if (triple.third != "—") {
                        val iqamahDate = format24h.parse(triple.third)
                        format12h.format(iqamahDate!!)
                    } else {
                        "—"
                    }

                    PrayerTiming(
                        name = triple.first,
                        adhanTime = adhan12h,
                        iqamahTime = iqamah12h,
                        isNext = (index == nextPrayerIndex),
                        timeRemaining = if (index == nextPrayerIndex) timeRemainingStr else ""
                    )
                }

                _prayerTimings.value = newList
                delay(1000)
            }
        }
    }

    fun updatePrayerTimings(updated: List<PrayerTiming>) {
        // Now overridden by the dynamic clock, keeping signature for compatibility if needed.
    }

    // ── F02: Emergency Directory ─────────────────────────────
    private val _emergencyContacts = MutableStateFlow(
        listOf(
            EmergencyContact(name = "Kasturba Hospital Manipal (Emergency)", category = "Hospital", phone = "+91 820 292 2761", area = "Manipal (9km)"),
            EmergencyContact(name = "Malpe Police Station", category = "Police", phone = "+91 820 253 8333", area = "Malpe / Hoode"),
            EmergencyContact(name = "Hoode Community Ambulance", category = "Ambulance", phone = "+91 94481 23456", area = "Hoode Local"),
            EmergencyContact(name = "Udupi Fire & Rescue", category = "Fire", phone = "101", area = "Udupi Taluk"),
            EmergencyContact(name = "Mescom Electricity Helpline", category = "Electricity", phone = "1912", area = "Kemmannu Section"),
            EmergencyContact(name = "Santhekatte 24x7 Pharmacy", category = "Pharmacy", phone = "+91 820 258 0123", area = "Santhekatte"),
            EmergencyContact(name = "Hoode Water Supply Helpdesk", category = "Water", phone = "+91 820 258 9988", area = "Hoode Panchayat")
        )
    )
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()

    // ── F03: Jobs & Gigs ─────────────────────────────────────
    private val _jobs = MutableStateFlow(
        listOf(
            JobPosting(
                title = "Assistant Accountant",
                employer = "Hoode Fisheries Co-Op",
                type = "Full-time",
                pay = "₹18,000 – ₹22,000 / month",
                location = "Hoode Port Road",
                description = "Looking for B.Com graduate with Tally ERP 9 experience for daily ledger maintenance and invoicing.",
                deadline = "25 Sep 2026",
                isSponsored = true,
                applicantsCount = 4
            ),
            JobPosting(
                title = "Delivery Partner & Driver",
                employer = "Al-Madina Groceries",
                type = "Part-time",
                pay = "₹400 / day + Fuel",
                location = "Kemmannu – Hoode",
                description = "Two-wheeler delivery rider for evening deliveries between 4 PM to 9 PM.",
                deadline = "18 Sep 2026",
                isSponsored = false,
                applicantsCount = 7
            ),
            JobPosting(
                title = "Electrician & Solar Installer",
                employer = "GreenPower Systems Udupi",
                type = "Gig",
                pay = "₹800 / day",
                location = "Hoode & Malpe",
                description = "Experienced electrician required for residential rooftop solar panel cabling and inverter setup.",
                deadline = "30 Sep 2026",
                isSponsored = false,
                applicantsCount = 2
            ),
            JobPosting(
                title = "Youth Robotics Workshop Mentor",
                employer = "Hoode Islamic Academy",
                type = "Volunteer",
                pay = "Certificate & Honorarium",
                location = "Academy Campus",
                description = "College STEM students invited to guide 8th–10th standard students in Arduino fundamentals.",
                deadline = "20 Sep 2026",
                isSponsored = false,
                applicantsCount = 5
            )
        )
    )
    val jobs: StateFlow<List<JobPosting>> = _jobs.asStateFlow()

    fun applyJob(jobId: String, name: String, phone: String, message: String) {
        val updated = _jobs.value.map { job ->
            if (job.id == jobId) job.copy(applicantsCount = job.applicantsCount + 1) else job
        }
        _jobs.value = updated
        addContributionPoints("Job Application Submitted", 10)
    }

    fun postJob(job: JobPosting) {
        _jobs.value = listOf(job) + _jobs.value
        addContributionPoints("Posted Community Job", 25)
    }

    // ── F04: Lost & Found ────────────────────────────────────
    private val _lostFound = MutableStateFlow(
        listOf(
            LostFoundItem(
                title = "Black Fastrack Backpack",
                isLost = true,
                category = "Bags",
                area = "Hoode Beach Walkway",
                date = "09 Sep 2026",
                description = "Contains engineering textbooks and blue water bottle. Left near seating bench around 5:30 PM.",
                status = "open"
            ),
            LostFoundItem(
                title = "Hero Splendor Bike Key with Metal Ring",
                isLost = false,
                category = "Keys",
                area = "Outside Hoode Juma Masjid",
                date = "08 Sep 2026",
                description = "Found after Asr prayer. Safely kept with mosque security office.",
                status = "open"
            ),
            LostFoundItem(
                title = "Redmi Note 12 (Sky Blue Case)",
                isLost = true,
                category = "Electronics",
                area = "Auto Stand near Bengre Cross",
                date = "06 Sep 2026",
                description = "Phone is locked with pin. Lock screen wallpaper is a family picture.",
                status = "resolved"
            )
        )
    )
    val lostFound: StateFlow<List<LostFoundItem>> = _lostFound.asStateFlow()

    fun postLostFound(item: LostFoundItem) {
        _lostFound.value = listOf(item) + _lostFound.value
        addContributionPoints("Reported Lost/Found Item", 15)
    }

    fun claimLostFound(itemId: String) {
        val updated = _lostFound.value.map { item ->
            if (item.id == itemId) item.copy(status = "claim_pending") else item
        }
        _lostFound.value = updated
    }

    // ── F05: Tournaments & Matches ───────────────────────────
    private val _tournaments = MutableStateFlow(
        listOf(
            Tournament(
                id = "tourn_01",
                title = "Hoode Premier League 2026 (HPL Season 7)",
                sport = "Cricket",
                venue = "Kemmannu Higher Primary Ground",
                dates = "18 Sep – 21 Sep 2026",
                format = "Knockout & League",
                teamsCount = 12,
                status = "active"
            )
        )
    )
    val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

    private val _fixtures = MutableStateFlow(
        listOf(
            Fixture(tournamentId = "tourn_01", round = "Quarter Final 1", teamA = "Bengre Blasters", teamB = "Kemmannu Kings", time = "18 Sep, 8:00 AM", venue = "Ground 1", scoreA = "112/5 (10 ov)", scoreB = "115/3 (8.4 ov)", status = "completed"),
            Fixture(tournamentId = "tourn_01", round = "Quarter Final 2", teamA = "Hoode Strikers", teamB = "Malpe Warriors", time = "18 Sep, 10:30 AM", venue = "Ground 1", scoreA = "—", scoreB = "—", status = "scheduled"),
            Fixture(tournamentId = "tourn_01", round = "Semi Final 1", teamA = "Kemmannu Kings", teamB = "TBD", time = "20 Sep, 9:00 AM", venue = "Ground 1", status = "scheduled")
        )
    )
    val fixtures: StateFlow<List<Fixture>> = _fixtures.asStateFlow()

    private val _standings = MutableStateFlow(
        listOf(
            Standing("Kemmannu Kings", 3, 3, 0, 6, "+1.42"),
            Standing("Hoode Strikers", 3, 2, 1, 4, "+0.85"),
            Standing("Bengre Blasters", 3, 1, 2, 2, "-0.24"),
            Standing("Malpe Warriors", 3, 0, 3, 0, "-1.98")
        )
    )
    val standings: StateFlow<List<Standing>> = _standings.asStateFlow()

    fun registerTournamentTeam(teamName: String) {
        val updated = _standings.value + Standing(teamName, 0, 0, 0, 0, "+0.00")
        _standings.value = updated
        addContributionPoints("Registered Team in Tournament", 20)
    }

    // ── F06: Events & Weddings ───────────────────────────────
    private val _events = MutableStateFlow(
        listOf(
            CommunityEvent(
                id = "evt_01",
                title = "Community Iftar & Dua Gathering",
                organizer = "Hoode Youth Federation",
                category = "Majlis",
                date = "Today",
                time = "6:15 PM – 7:30 PM",
                venue = "Hoode Juma Masjid Courtyard",
                isPrivate = false,
                rsvpGoing = 84,
                rsvpTotalCapacity = 150,
                userRsvp = true
            ),
            CommunityEvent(
                id = "evt_02",
                title = "Free General Health & Eye Checkup Camp",
                organizer = "Rotary Club Kemmannu & KMC Manipal",
                category = "Community Meeting",
                date = "Sunday, 14 Sep 2026",
                time = "9:00 AM – 2:00 PM",
                venue = "Government Urdu Higher Primary School",
                isPrivate = false,
                rsvpGoing = 42,
                rsvpTotalCapacity = 200,
                userRsvp = null
            ),
            CommunityEvent(
                id = "evt_03",
                title = "Walima Reception — Suhail & Ayesha",
                organizer = "Haji Abdul Rahman Family",
                category = "Wedding",
                date = "22 Sep 2026",
                time = "12:30 PM – 3:30 PM",
                venue = "Golden Palace Auditorium, Santhekatte",
                isPrivate = true,
                rsvpGoing = 160,
                rsvpTotalCapacity = 300,
                userRsvp = null
            )
        )
    )
    val events: StateFlow<List<CommunityEvent>> = _events.asStateFlow()

    fun rsvpEvent(eventId: String, going: Boolean) {
        val updated = _events.value.map { event ->
            if (event.id == eventId) {
                val newCount = if (going) event.rsvpGoing + 1 else if (event.userRsvp == true) event.rsvpGoing - 1 else event.rsvpGoing
                event.copy(rsvpGoing = newCount, userRsvp = going)
            } else event
        }
        _events.value = updated
        addContributionPoints("RSVP to Community Event", 5)
    }

    // ── F07: Blood Donor Network ─────────────────────────────
    private val _bloodRequests = MutableStateFlow(
        listOf(
            BloodRequest(
                bloodGroup = "O+",
                hospital = "Adarsh Hospital, Udupi",
                neededBy = "Today by 8:00 PM",
                urgency = "urgent",
                area = "Udupi Town",
                unitsNeeded = 2,
                coordinatorPhone = "+91 94812 34567"
            ),
            BloodRequest(
                bloodGroup = "B-",
                hospital = "KMC Manipal Trauma Ward",
                neededBy = "Tomorrow 11:00 AM",
                urgency = "urgent",
                area = "Manipal",
                unitsNeeded = 1,
                coordinatorPhone = "+91 98450 98765"
            ),
            BloodRequest(
                bloodGroup = "A+",
                hospital = "Hi-Tech Hospital, Ambalpady",
                neededBy = "13 Sep 2026",
                urgency = "normal",
                area = "Ambalpady",
                unitsNeeded = 1,
                coordinatorPhone = "+91 82025 21100"
            )
        )
    )
    val bloodRequests: StateFlow<List<BloodRequest>> = _bloodRequests.asStateFlow()

    private val _registeredDonors = MutableStateFlow(
        listOf(
            DonorRegistration(name = "Faizan Ahmed", bloodGroup = "O+", area = "Hoode Beach", phone = "+91 98440 11223"),
            DonorRegistration(name = "Mohammed Imran", bloodGroup = "B+", area = "Bengre Cross", phone = "+91 98440 44556"),
            DonorRegistration(name = "Zaid K.", bloodGroup = "A-", area = "Kemmannu", phone = "+91 98440 77889")
        )
    )
    val registeredDonors: StateFlow<List<DonorRegistration>> = _registeredDonors.asStateFlow()

    fun registerDonor(donor: DonorRegistration) {
        _registeredDonors.value = listOf(donor) + _registeredDonors.value
        addContributionPoints("Joined Blood Donor Registry", 50)
    }

    fun postBloodRequest(request: BloodRequest) {
        _bloodRequests.value = listOf(request) + _bloodRequests.value
        addContributionPoints("Emergency Blood Request Broadcasted", 15)
    }

    // ── F08: Polls & Civic Issues ────────────────────────────
    private val _polls = MutableStateFlow(
        listOf(
            CommunityPoll(
                id = "poll_01",
                question = "Should the community install solar LED streetlights along Hoode Beach Road?",
                description = "Gram Panchayat proposal with 50% community sponsorship matching.",
                options = listOf(
                    PollOption("opt_1", "Yes, strongly needed for safety", 142),
                    PollOption("opt_2", "Yes, but focus on main junction first", 68),
                    PollOption("opt_3", "No, other infrastructure takes priority", 14)
                ),
                closesAt = "15 Sep 2026",
                totalVotes = 224
            ),
            CommunityPoll(
                id = "poll_02",
                question = "Preferred timing for weekly Career Counseling Sessions?",
                description = "Organized by Hoode Education Circle for SSLC and PUC students.",
                options = listOf(
                    PollOption("opt_21", "Saturday evening (5 PM - 7 PM)", 45),
                    PollOption("opt_22", "Sunday morning (10 AM - 12 PM)", 88),
                    PollOption("opt_23", "Sunday post-Asr (4:30 PM - 6 PM)", 62)
                ),
                closesAt = "18 Sep 2026",
                totalVotes = 195
            )
        )
    )
    val polls: StateFlow<List<CommunityPoll>> = _polls.asStateFlow()

    fun castVote(pollId: String, optionId: String) {
        val updated = _polls.value.map { poll ->
            if (poll.id == pollId && poll.userVotedOptionId == null) {
                val updatedOptions = poll.options.map { opt ->
                    if (opt.id == optionId) opt.copy(votes = opt.votes + 1) else opt
                }
                poll.copy(
                    options = updatedOptions,
                    userVotedOptionId = optionId,
                    totalVotes = poll.totalVotes + 1
                )
            } else poll
        }
        _polls.value = updated
        addContributionPoints("Voted in Civic Poll", 15)
    }

    private val _civicIssues = MutableStateFlow(
        listOf(
            CivicIssue(
                title = "Broken street lamp near Bengre cross-bridge",
                category = "Streetlights",
                location = "Bengre Cross Bridge, Pole #B14",
                status = "acknowledged",
                endorsements = 27,
                userEndorsed = false
            ),
            CivicIssue(
                title = "Pothole expansion on Kemmannu–Hoode main road",
                category = "Roads",
                location = "Near Hoode Post Office",
                status = "in_progress",
                endorsements = 54,
                userEndorsed = true
            ),
            CivicIssue(
                title = "Plastic waste accumulation on beach walkway",
                category = "Waste",
                location = "Hoode Beach North end",
                status = "resolved",
                endorsements = 38,
                userEndorsed = true
            )
        )
    )
    val civicIssues: StateFlow<List<CivicIssue>> = _civicIssues.asStateFlow()

    fun endorseCivicIssue(issueId: String) {
        val updated = _civicIssues.value.map { issue ->
            if (issue.id == issueId && !issue.userEndorsed) {
                issue.copy(endorsements = issue.endorsements + 1, userEndorsed = true)
            } else issue
        }
        _civicIssues.value = updated
        addContributionPoints("Endorsed Civic Issue", 5)
    }

    fun submitCivicIssue(title: String, category: String, location: String) {
        val newIssue = CivicIssue(
            title = title,
            category = category,
            location = location,
            status = "submitted",
            endorsements = 1,
            userEndorsed = true
        )
        _civicIssues.value = listOf(newIssue) + _civicIssues.value
        addContributionPoints("Reported Civic Issue", 20)
    }

    // ── F09: Badges & Contribution Points ────────────────────
    private val _totalPoints = MutableStateFlow(320)
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()

    private val _badges = MutableStateFlow(
        listOf(
            CommunityBadge("b_01", "Helpful Neighbor", "Participated in 5+ community events and actions", "ic_badge", isUnlocked = true, earnedDate = "01 Aug 2026"),
            CommunityBadge("b_02", "Civic Champion", "Active in local polls and civic issue reporting", "ic_poll", isUnlocked = true, earnedDate = "15 Aug 2026"),
            CommunityBadge("b_03", "Life Saver", "Registered as active blood donor", "ic_blood", isUnlocked = true, earnedDate = "28 Aug 2026"),
            CommunityBadge("b_04", "Local Guide", "Contributed 10+ verified directory and place updates", "ic_services", isUnlocked = false)
        )
    )
    val badges: StateFlow<List<CommunityBadge>> = _badges.asStateFlow()

    private val _contributions = MutableStateFlow(
        listOf(
            ContributionRecord(action = "Joined Blood Donor Registry", points = 50, date = "08 Sep 2026"),
            ContributionRecord(action = "Voted in Streetlight Proposal Poll", points = 15, date = "07 Sep 2026"),
            ContributionRecord(action = "Reported Beach Cleanliness Issue", points = 20, date = "04 Sep 2026"),
            ContributionRecord(action = "RSVP to Community Iftar", points = 5, date = "02 Sep 2026")
        )
    )
    val contributions: StateFlow<List<ContributionRecord>> = _contributions.asStateFlow()

    private fun addContributionPoints(action: String, points: Int) {
        _totalPoints.value += points
        _contributions.value = listOf(ContributionRecord(action = action, points = points, date = "Just now")) + _contributions.value
    }

    // ── F10: Classifieds & Marketplace ───────────────────────
    private val _classifieds = MutableStateFlow(
        listOf(
            ClassifiedItem(
                title = "Hercules 26T Mountain Bicycle (6 Months Old)",
                price = "₹3,800",
                type = "Sell",
                category = "Vehicles",
                area = "Kemmannu Road",
                description = "Excellent condition with front suspension and dual disc brakes. Selling due to relocation.",
                sellerName = "Arshad",
                date = "08 Sep 2026"
            ),
            ClassifiedItem(
                title = "Teakwood Study Table with Bookshelf",
                price = "₹2,200",
                type = "Sell",
                category = "Furniture",
                area = "Bengre Cross",
                description = "Solid wood table, 4ft x 2.5ft with 2 drawers. Minor scratches, otherwise sturdy.",
                sellerName = "Siddiq M.",
                date = "06 Sep 2026"
            ),
            ClassifiedItem(
                title = "Complete Set of NCERT 10th Standard Books",
                price = "Free",
                type = "Give Away",
                category = "Books",
                area = "Hoode Beach",
                description = "All core subjects, neat condition. Free for any student in need.",
                sellerName = "Sister Fatima",
                date = "05 Sep 2026"
            ),
            ClassifiedItem(
                title = "Wanted: Used Refrigerator (190L - 240L)",
                price = "Under ₹7,000",
                type = "Wanted",
                category = "Electronics",
                area = "Any area in Hoode",
                description = "Single or double door in working condition required for small family rental house.",
                sellerName = "Nawaz",
                date = "04 Sep 2026"
            )
        )
    )
    val classifieds: StateFlow<List<ClassifiedItem>> = _classifieds.asStateFlow()

    fun postClassified(item: ClassifiedItem) {
        _classifieds.value = listOf(item) + _classifieds.value
        addContributionPoints("Posted Marketplace Item", 15)
    }

    // ── F12: Ramadan Timetable ────────────────────────────────
    private val _ramadanTimetable = MutableStateFlow(
        (1..30).map { day ->
            RamadanTimetable(
                day = day,
                date = "Day $day",
                suhoorEnd = "5:0${(8 - (day % 10)).coerceAtLeast(0)} AM",
                iftarTime = "6:3${(5 + (day % 10)).coerceAtMost(9)} PM"
            )
        }
    )
    val ramadanTimetable: StateFlow<List<RamadanTimetable>> = _ramadanTimetable.asStateFlow()

    // ── F13: Notifications ───────────────────────────────────
    private val _notifications = MutableStateFlow(
        listOf(
            NotificationItem(title = "Maghrib Iqamah in 15 minutes", body = "Maghrib iqamah will commence at 6:42 PM at Hoode Juma Masjid.", timestamp = "10 min ago", category = "Prayer"),
            NotificationItem(title = "New Blood Request: O+", body = "Urgent request for 2 units of O+ blood at Adarsh Hospital Udupi.", timestamp = "1 hour ago", category = "Emergency"),
            NotificationItem(title = "HPL Season 7 Matches Announced", body = "Check out the tournament fixtures starting 18 September.", timestamp = "Yesterday", category = "Announcement", isRead = true),
            NotificationItem(title = "Civic Issue Acknowledged", body = "Pothole repair on Kemmannu main road scheduled for inspection.", timestamp = "2 days ago", category = "Civic", isRead = true)
        )
    )
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun markNotificationRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    // ── F14: Verified Local News ─────────────────────────────
    private val _newsArticles = MutableStateFlow(
        listOf(
            NewsArticle(
                title = "New Bengre–Hoode Coastal Seawall Project Approved",
                summary = "State fisheries and ports department sanctions ₹4.2 crore for sea erosion prevention works.",
                body = "The Karnataka Minor Ports & Fisheries Department has officially approved the Phase-2 sea erosion protection project for Hoode and Bengre beach sections. Works are slated to commence post-monsoon in October 2026. Local fishermen cooperatives welcomed the move.",
                verifier = "Udupi District Information Center",
                verifiedDate = "09 Sep 2026",
                sources = listOf("Official Gazette Notification #FD-892/2026", "Deccan Herald Regional Bureau"),
                isRumorClarification = false
            ),
            NewsArticle(
                title = "Clarification: No Power Shutdown Scheduled for This Weekend",
                summary = "Social media rumors claiming 48-hour total blackout in Kemmannu sub-station are false.",
                body = "MESCOM Assistant Executive Engineer has issued an official statement dismissing WhatsApp rumors of a 48-hour continuous power cut in Hoode, Kemmannu, and Tonse areas. Routine line maintenance will occur only on Tuesday from 10 AM to 1 PM.",
                verifier = "MESCOM Kemmannu Sub-Division",
                verifiedDate = "08 Sep 2026",
                sources = listOf("MESCOM Press Note Ref: EE/KM/2026-11"),
                isRumorClarification = true
            )
        )
    )
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles.asStateFlow()

    // ── F15: Handyman & Service Providers ────────────────────
    private val _providers = MutableStateFlow(
        listOf(
            ServiceProvider(name = "Zameer Electricals & Rewinding", category = "Electrician", rating = 4.9, reviewCount = 42, phone = "+91 98860 12345", area = "Hoode Bazar"),
            ServiceProvider(name = "Salam Plumbing Solutions", category = "Plumber", rating = 4.8, reviewCount = 31, phone = "+91 94482 67890", area = "Bengre Cross"),
            ServiceProvider(name = "Master Wood Crafts (Sadiq Bhai)", category = "Carpenter", rating = 4.9, reviewCount = 56, phone = "+91 98450 33445", area = "Kemmannu Road"),
            ServiceProvider(name = "CoolTech AC & Refrigerator Repair", category = "Appliance Repair", rating = 4.7, reviewCount = 28, phone = "+91 97410 88990", area = "Santhekatte"),
            ServiceProvider(name = "Excellence Home Tutoring (Math & Science)", category = "Tutor", rating = 5.0, reviewCount = 19, phone = "+91 99001 55667", area = "Hoode & Malpe")
        )
    )
    val providers: StateFlow<List<ServiceProvider>> = _providers.asStateFlow()

    // ── F16: Daily Personality ───────────────────────────────
    private val _dailyPersonality = MutableStateFlow(
        PersonalityProfile(
            name = "Janab Haji K. M. Abdul Khadar",
            role = "Education Pioneer & Philanthropist",
            featuredDate = "10 Sep 2026",
            intro = "Founder trustee of Hoode Educational Trust, dedicating 40 years to rural literacy and higher education access.",
            fullBiography = "Born in Hoode in 1948, Haji Abdul Khadar spearheaded the establishment of the first community high school in Kemmannu-Hoode region. Over four decades, his charitable trust has sponsored higher education scholarships for more than 1,200 village students.",
            contributions = listOf(
                "Founded Hoode Girls High School (1984)",
                "Built Community Dialysis Support Fund",
                "Patron of Kemmannu Ambulance Service"
            ),
            quote = "\"True wealth is what you leave in the minds and hearts of the next generation.\""
        )
    )
    val dailyPersonality: StateFlow<PersonalityProfile> = _dailyPersonality.asStateFlow()

    fun updateDailyPersonality(profile: PersonalityProfile) {
        _dailyPersonality.value = profile
    }

    // ── F17: Hoode Photo Gallery (Max 25 images) ──────────────
    private val _galleryItems = MutableStateFlow(
        (1..25).map { index ->
            GalleryItem(
                title = when (index) {
                    1 -> "Sunset at Hoode Beach"
                    2 -> "Hoode Juma Masjid Minaret"
                    3 -> "Bengre Estuary Fishing Boats"
                    4 -> "Kemmannu Hanging Bridge"
                    5 -> "Coconut Groves of Tonse"
                    6 -> "Morning Fisherman Cast Net"
                    7 -> "Kemmannu River Promenade"
                    8 -> "Hoode Lighthouse View"
                    else -> "Scenic Hoode View #$index"
                },
                caption = "Capturing the serene coastal life and community architecture of Hoode.",
                photographer = if (index % 2 == 0) "Rashid Hoode" else "Ziyad Photography",
                sortOrder = index,
                colorHex = when (index % 5) {
                    0 -> "#62E8CF"
                    1 -> "#0F3D35"
                    2 -> "#258A5B"
                    3 -> "#D68B16"
                    else -> "#4F545D"
                }
            )
        }
    )
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems.asStateFlow()

    fun addGalleryItem(item: GalleryItem): Boolean {
        if (_galleryItems.value.size >= 25) {
            // Strictly cap at 25 items per spec F17
            return false
        }
        _galleryItems.value = _galleryItems.value + item
        return true
    }

    fun removeGalleryItem(id: String) {
        _galleryItems.value = _galleryItems.value.filter { it.id != id }
    }

    // ── F18: Activities ──────────────────────────────────────
    var initialActivityCategory: String = "All"

    private val _activities = MutableStateFlow(
        listOf(
            CommunityActivity(
                title = "Weekly Qur'an Study & Reflection Circle",
                category = "Religious",
                organizer = "Hoode Islamic Center",
                schedule = "Every Friday after Maghrib",
                venue = "Masjid Conference Hall",
                description = "Structured Tafseer and thematic discussions open to high school students and elders alike."
            ),
            CommunityActivity(
                title = "Fajr Hadith & Tajweed Recitation Class",
                category = "Religious",
                organizer = "Hoode Juma Masjid",
                schedule = "Daily after Fajr (20 mins)",
                venue = "Main Prayer Hall",
                description = "Daily reflection on Sahih Hadith followed by practical Quranic tajweed correction."
            ),
            CommunityActivity(
                title = "Beach Cleanliness & Mangrove Plantation Drive",
                category = "Community",
                organizer = "Clean Hoode Green Hoode Volunteer Wing",
                schedule = "Second Sunday of every month, 7:00 AM",
                venue = "Bengre River Mouth",
                description = "Youth volunteers planting mangrove saplings to protect the estuary shoreline from monsoon erosion."
            ),
            CommunityActivity(
                title = "Monsoon Welfare & Drainage Relief Action",
                category = "Community",
                organizer = "Hoode Resident Welfare Committee",
                schedule = "Ongoing this season",
                venue = "Coastal Wards 1 to 4",
                description = "Emergency canal clearing and potable drinking water distribution for elderly residents."
            ),
            CommunityActivity(
                title = "Youth Career, CV & Scholarship Guidance Meet",
                category = "Social",
                organizer = "Hoode Students & Professionals Forum",
                schedule = "Upcoming Saturday, 4:30 PM",
                venue = "Community Cultural Hall, Kemmannu",
                description = "Mentorship session for engineering, medical, and degree students with guidance on Gulf & overseas jobs."
            ),
            CommunityActivity(
                title = "Senior Citizens Evening Reminiscence Circle",
                category = "Social",
                organizer = "Baitul Hikmah Elders Club",
                schedule = "Every Wednesday, 5:30 PM",
                venue = "Coast View Garden Bench",
                description = "Casual tea and storytelling gathering preserving local Hoode maritime and cultural history."
            ),
            CommunityActivity(
                title = "Weekend Badminton Coaching for Juniors",
                category = "Sports",
                organizer = "Hoode Sports Club",
                schedule = "Saturdays & Sundays, 6:30 AM – 8:30 AM",
                venue = "Hoode Indoor Sports Shed",
                description = "Professional coaching for boys and girls aged 10–16 years with certified NIS trainer."
            ),
            CommunityActivity(
                title = "HPL Open Cricket Selection Trials",
                category = "Sports",
                organizer = "Hoode Premier League Governing Council",
                schedule = "Sunday morning, 6:00 AM",
                venue = "Town Ground, Kemmannu",
                description = "Open net trials for local youngsters to register for the upcoming season of HPL tournament."
            ),
            CommunityActivity(
                title = "Janazah Announcement & Condolence: Marhooma Aisha Bi",
                category = "Condolences",
                organizer = "Hoode Janazah Welfare Committee",
                schedule = "Janazah prayer held at 1:30 PM",
                venue = "Hoode Juma Masjid Qabrastan",
                description = "May Allah forgive her shortcomings, widen her grave, and grant Sabr-e-Jameel to the bereaved family."
            ),
            CommunityActivity(
                title = "Condolence Gathering in Remembrance of Late Master Ismail",
                category = "Condolences",
                organizer = "Kemmannu Educational Society",
                schedule = "Sunday post-Asr",
                venue = "Memorial Hall",
                description = "Commemoration of 40 years of dedicated teaching and community mentorship in Coastal Udupi."
            ),
            CommunityActivity(
                title = "Special Dua Request: Brother Zameer (ICU Treatment)",
                category = "Dua Request",
                organizer = "Family & Friends of Zameer",
                schedule = "Urgent Request",
                venue = "City Hospital, Udupi",
                description = "Requesting all community members to remember Brother Zameer in their Tahajjud and daily prayers for complete Shifa."
            ),
            CommunityActivity(
                title = "Community Dua for Class 10 & 12 Board Exam Students",
                category = "Dua Request",
                organizer = "Hoode Juma Masjid Imam & Committee",
                schedule = "Thursday after Isha",
                venue = "Hoode Juma Masjid",
                description = "Collective supplication seeking Allah's guidance, calm minds, and excellence for our youth sitting for exams."
            )
        )
    )
    val activities: StateFlow<List<CommunityActivity>> = _activities.asStateFlow()

    fun addActivity(activity: CommunityActivity) {
        _activities.value = listOf(activity) + _activities.value
    }

    // ── F19: Educational Offerings ───────────────────────────
    private val _educationOfferings = MutableStateFlow(
        listOf(
            EducationOffering(
                title = "Tajweed & Qur'an Recitation Excellence Course",
                provider = "Markaz-ut-Tarteel Hoode",
                category = "Islamic Studies",
                audience = "Children & Teens (8–16 yrs)",
                schedule = "Mon to Thu, 5:00 PM – 6:30 PM",
                contact = "+91 94812 00112",
                description = "Individual pronunciation correction with certified Qaris from Deoband and Nadwa."
            ),
            EducationOffering(
                title = "SSLC Board Exam Crash Course (Maths & Science)",
                provider = "Hoode Youth Study Center",
                category = "Tuition",
                audience = "10th Standard Students",
                schedule = "Every Saturday & Sunday, 9:00 AM – 1:00 PM",
                contact = "+91 98450 77112",
                description = "Intensive model paper solving, concept clarity sessions, and previous 10 years question reviews."
            ),
            EducationOffering(
                title = "Python Programming & AI Basics for College Students",
                provider = "Digital Hoode Initiative",
                category = "Computer Skills",
                audience = "PUC & Degree Students",
                schedule = "Sundays, 2:00 PM – 5:00 PM (8 Weeks)",
                contact = "+91 99000 88221",
                description = "Hands-on coding workshop covering Python basics, Git GitHub, and building practical mini-projects."
            ),
            EducationOffering(
                title = "Spoken English & Public Speaking Academy",
                provider = "Crescent Learning Hub",
                category = "Languages",
                audience = "All Ages",
                schedule = "Tue & Fri, 7:00 PM – 8:30 PM",
                contact = "+91 97412 33445",
                description = "Confidence building, grammar fundamentals, professional email writing, and interview practice."
            )
        )
    )
    val educationOfferings: StateFlow<List<EducationOffering>> = _educationOfferings.asStateFlow()

    fun addEducationOffering(offering: EducationOffering) {
        _educationOfferings.value = listOf(offering) + _educationOfferings.value
    }

    // ── F20: Our Huffaz ──────────────────────────────────────
    private val _huffazList = MutableStateFlow(
        listOf(
            HuffazProfile(
                name = "Hafiz Mohammed Zaid",
                completionYear = "2023",
                institution = "Jamia Islamia Bhatkal",
                teacher = "Maulana Hafiz Abdul Bari",
                biography = "Completed Hifz with distinction at age 14. Currently leads Taraweeh prayers at Masjid-ut-Taqwa and assists evening Maktab."
            ),
            HuffazProfile(
                name = "Hafiz Bilal Ahmed Hoode",
                completionYear = "2020",
                institution = "Darul Uloom Sabeelur Rashad, Bangalore",
                teacher = "Qari Nayeemuddin",
                biography = "Represented Karnataka state in national Qur'an recitation competition; currently pursuing Bachelor in Computer Applications."
            ),
            HuffazProfile(
                name = "Hafiz Rayan K.",
                completionYear = "2025",
                institution = "Markaz-ut-Tarteel, Hoode",
                teacher = "Qari Hafiz Mushtaq",
                biography = "Youngest resident Hafiz from Bengre ward, completing memorization in 2.5 years alongside regular schooling."
            )
        )
    )
    val huffazList: StateFlow<List<HuffazProfile>> = _huffazList.asStateFlow()

    fun addHuffazProfile(profile: HuffazProfile) {
        _huffazList.value = listOf(profile) + _huffazList.value
    }

    // ── F21: Calendar Events ─────────────────────────────────
    private val _calendarEvents = MutableStateFlow(
        listOf(
            CalendarEvent(dateStr = "2026-03-20", title = "Eid-ul-Fitr", type = "holiday"),
            CalendarEvent(dateStr = "2026-05-27", title = "Eid-ul-Adha", type = "holiday"),
            CalendarEvent(dateStr = "2026-09-15", title = "Hoode Beach Clean-up", type = "event"),
            CalendarEvent(dateStr = "2026-09-22", title = "Annual HPL Cricket Tournament", type = "event"),
            CalendarEvent(dateStr = "2026-10-10", title = "Free Medical Camp", type = "event"),
            CalendarEvent(dateStr = "2026-11-01", title = "Karnataka Rajyotsava", type = "holiday")
        )
    )
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()
}

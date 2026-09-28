package com.example.hoode_app.data.repository

import android.content.Context
import android.util.Log
import com.example.hoode_app.data.model.*
import com.example.hoode_app.data.remote.SupabaseClient
import com.example.hoode_app.data.remote.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object HoodeRepository {

    // ── Current User / Auth ──────────────────────────────────
    sealed class AuthResult {
        data class Success(val user: User) : AuthResult()
        data class InvalidPassword(val message: String = "Incorrect password. Please try again.") : AuthResult()
        data class UserNotFound(val message: String = "No account found with this email. Would you like to create one?") : AuthResult()
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private var appContext: Context? = null
    private const val PREFS_NAME = "hoode_user_session"
    private const val KEY_CACHED_USER = "cached_user_json"

    fun init(context: Context) {
        appContext = context.applicationContext
        loadUserFromCache()

        // Background session restore and profile refresh from Supabase
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (com.hoodeconnect.backend.BackendSession.restore()) {
                    val uid = com.hoodeconnect.backend.BackendSession.userId
                    if (!uid.isNullOrBlank()) {
                        val profile = SupabaseClient.fetchProfile(uid)
                        if (profile != null) {
                            if (profile.optBoolean("is_banned", false)) {
                                signOut()
                                return@launch
                            }
                            val current = _currentUser.value
                            val name = profile.optString("name").takeIf { it.isNotBlank() } ?: current?.displayName ?: "Resident"
                            val email = profile.optString("email").takeIf { it.isNotBlank() } ?: current?.email ?: ""
                            val phone = profile.optString("phone").takeIf { it.isNotBlank() } ?: current?.phone
                            val ward = profile.optString("ward").takeIf { it.isNotBlank() } ?: current?.locality ?: "Hoode"
                            val role = profile.optString("role").takeIf { it.isNotBlank() } ?: "approved_resident"
                            val avatar = profile.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" } ?: current?.profilePicUri
                            val age = profile.optString("age").takeIf { it.isNotBlank() && it != "null" } ?: current?.age
                            val dob = profile.optString("dob").takeIf { it.isNotBlank() && it != "null" } ?: current?.dob
                            val fatherName = profile.optString("father_name").takeIf { it.isNotBlank() && it != "null" } ?: current?.fatherName
                            val bloodGroup = profile.optString("blood_group").takeIf { it.isNotBlank() && it != "null" } ?: current?.bloodGroup
                            val profession = profile.optString("profession").takeIf { it.isNotBlank() && it != "null" } ?: current?.profession

                            val updated = (current ?: User(id = uid, email = email, displayName = name)).copy(
                                id = uid,
                                email = email,
                                displayName = name,
                                locality = ward,
                                phone = phone,
                                roles = listOf(role),
                                profilePicUri = avatar,
                                age = age,
                                dob = dob,
                                fatherName = fatherName,
                                bloodGroup = bloodGroup,
                                profession = profession
                            )
                            _currentUser.value = updated
                            saveUserToCache(updated)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("HoodeRepository", "BackendSession background restore failed", e)
            }
        }
    }

    fun isLoggedIn(): Boolean {
        if (_currentUser.value != null) return true
        val ctx = appContext ?: return false
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_CACHED_USER)
    }

    private fun saveUserToCache(user: User) {
        try {
            val ctx = appContext ?: return
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CACHED_USER, userToJson(user)).apply()
        } catch (e: Exception) {
            Log.e("HoodeRepository", "Error saving user to cache", e)
        }
    }

    private fun loadUserFromCache() {
        try {
            val ctx = appContext ?: return
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_CACHED_USER, null) ?: return
            val user = userFromJson(jsonStr)
            if (user != null) {
                _currentUser.value = user
            }
        } catch (e: Exception) {
            Log.e("HoodeRepository", "Error loading user from cache", e)
        }
    }

    private fun clearUserCache() {
        try {
            val ctx = appContext ?: return
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e("HoodeRepository", "Error clearing user cache", e)
        }
    }

    private fun userToJson(user: User): String {
        val obj = JSONObject().apply {
            put("id", user.id)
            put("email", user.email)
            put("displayName", user.displayName)
            put("locale", user.locale)
            put("communityId", user.communityId)
            put("roles", JSONArray(user.roles))
            user.profilePicUri?.let { put("profilePicUri", it) }
            user.phone?.let { put("phone", it) }
            user.age?.let { put("age", it) }
            user.dob?.let { put("dob", it) }
            user.fatherName?.let { put("fatherName", it) }
            user.bloodGroup?.let { put("bloodGroup", it) }
            user.profession?.let { put("profession", it) }
            user.locality?.let { put("locality", it) }
            user.username?.let { put("username", it) }
            user.bio?.let { put("bio", it) }
            user.coverPicUri?.let { put("coverPicUri", it) }
        }
        return obj.toString()
    }

    private fun userFromJson(jsonStr: String): User? {
        return try {
            val obj = JSONObject(jsonStr)
            val rolesArr = obj.optJSONArray("roles")
            val rolesList = mutableListOf<String>()
            if (rolesArr != null) {
                for (i in 0 until rolesArr.length()) {
                    rolesList.add(rolesArr.getString(i))
                }
            } else {
                rolesList.add("approved_resident")
            }
            User(
                id = obj.optString("id", UUID.randomUUID().toString()),
                email = obj.optString("email"),
                displayName = obj.optString("displayName", "Resident"),
                locale = obj.optString("locale", "en"),
                communityId = obj.optString("communityId", "hoode"),
                roles = if (rolesList.isEmpty()) listOf("approved_resident") else rolesList,
                profilePicUri = obj.optString("profilePicUri").takeIf { it.isNotBlank() && it != "null" },
                phone = obj.optString("phone").takeIf { it.isNotBlank() && it != "null" },
                age = obj.optString("age").takeIf { it.isNotBlank() && it != "null" },
                dob = obj.optString("dob").takeIf { it.isNotBlank() && it != "null" },
                fatherName = obj.optString("fatherName").takeIf { it.isNotBlank() && it != "null" },
                bloodGroup = obj.optString("bloodGroup").takeIf { it.isNotBlank() && it != "null" },
                profession = obj.optString("profession").takeIf { it.isNotBlank() && it != "null" },
                locality = obj.optString("locality").takeIf { it.isNotBlank() && it != "null" },
                username = obj.optString("username").takeIf { it.isNotBlank() && it != "null" },
                bio = obj.optString("bio").takeIf { it.isNotBlank() && it != "null" },
                coverPicUri = obj.optString("coverPicUri").takeIf { it.isNotBlank() && it != "null" }
            )
        } catch (e: Exception) {
            Log.e("HoodeRepository", "Error parsing cached user", e)
            null
        }
    }

    suspend fun authenticateUser(emailInput: String, passwordInput: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = emailInput.trim().lowercase()

        val result = SupabaseClient.signIn(cleanEmail, passwordInput)
        result.fold(
            onSuccess = { user ->
                _currentUser.value = user
                saveUserToCache(user)
                AuthResult.Success(user)
            },
            onFailure = { error ->
                val msg = error.message ?: "Sign in failed. Please retry."
                if (msg.contains("invalid", ignoreCase = true) || msg.contains("credential", ignoreCase = true)) {
                    AuthResult.InvalidPassword("Incorrect password or email. Please verify and try again.")
                } else if (msg.contains("not found", ignoreCase = true)) {
                    AuthResult.UserNotFound()
                } else {
                    AuthResult.InvalidPassword(msg)
                }
            }
        )
    }

    suspend fun registerUser(name: String, email: String, password: String, phone: String = "", ward: String = "Hoode"): Result<User> =
        withContext(Dispatchers.IO) {
            SupabaseClient.signUp(email, password, name, phone, ward).onSuccess { user ->
                _currentUser.value = user
                saveUserToCache(user)
            }
        }

    suspend fun updateUserProfile(
        displayName: String,
        phone: String = "",
        age: String = "",
        dob: String = "",
        fatherName: String = "",
        bloodGroup: String = "",
        profession: String = "",
        locality: String = "",
        profilePicUri: String? = null,
        username: String? = null,
        bio: String? = null,
        coverPicUri: String? = null
    ): Result<User> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Please sign in first."))
        val updated = current.copy(
            displayName = displayName.ifBlank { current.displayName },
            phone = phone.ifBlank { current.phone },
            age = age.ifBlank { current.age },
            dob = dob.ifBlank { current.dob },
            fatherName = fatherName.ifBlank { current.fatherName },
            bloodGroup = bloodGroup.ifBlank { current.bloodGroup },
            profession = profession.ifBlank { current.profession },
            locality = locality.ifBlank { current.locality },
            profilePicUri = profilePicUri ?: current.profilePicUri,
            username = username ?: current.username,
            bio = bio ?: current.bio,
            coverPicUri = coverPicUri ?: current.coverPicUri
        )
        SupabaseClient.updateProfile(updated).onSuccess {
            _currentUser.value = it
            saveUserToCache(it)
        }
    }

    fun signOut() {
        _currentUser.value = null
        clearUserCache()
        com.hoodeconnect.backend.BackendSession.clear()
    }

    // ── F00: 5-Slide Carousel ────────────────────────────────
    private val _adSlides = MutableStateFlow<List<AdCarouselSlide>>(emptyList())
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

    @Volatile
    private var currentPrayerSchedule: List<Triple<String, String, String>> = listOf(
        Triple("Fajr", "05:15", "05:45"),
        Triple("Sunrise", "06:14", "—"),
        Triple("Dhuhr", "12:35", "13:00"),
        Triple("Asr", "16:15", "16:45"),
        Triple("Maghrib", "18:42", "18:50"),
        Triple("Isha", "20:05", "20:30")
    )

    private fun parseTo24h(timeStr: String): String {
        if (timeStr == "—" || timeStr.isBlank()) return "—"
        return try {
            val trimmed = timeStr.trim()
            if (trimmed.contains("AM", ignoreCase = true) || trimmed.contains("PM", ignoreCase = true)) {
                val f12 = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
                val f24 = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
                val d = f12.parse(trimmed)
                f24.format(d!!)
            } else {
                trimmed
            }
        } catch (e: Exception) {
            timeStr
        }
    }

    private fun startPrayerClock() {
        prayerScope.launch {
            val format12h = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
            val format24h = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)

            while (isActive) {
                val schedule = currentPrayerSchedule
                val now = java.util.Calendar.getInstance()
                val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMin = now.get(java.util.Calendar.MINUTE)
                val currentSec = now.get(java.util.Calendar.SECOND)
                val currentTotalSecs = currentHour * 3600 + currentMin * 60 + currentSec

                var nextPrayerIndex = -1
                var timeRemainingStr = ""

                // Find the next prayer based on Iqamah time (or Adhan if Sunrise)
                for (i in schedule.indices) {
                    val targetTimeStr = if (schedule[i].third != "—" && schedule[i].third.contains(":")) schedule[i].third else schedule[i].second
                    if (!targetTimeStr.contains(":")) continue
                    val parts = targetTimeStr.split(":")
                    val targetSecs = parts[0].trim().toIntOrNull()?.let { h ->
                        parts[1].trim().toIntOrNull()?.let { m -> h * 3600 + m * 60 }
                    } ?: continue

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
                if (nextPrayerIndex == -1 && schedule.isNotEmpty()) {
                    nextPrayerIndex = 0
                    val targetTimeStr = if (schedule[0].third != "—" && schedule[0].third.contains(":")) schedule[0].third else schedule[0].second
                    val parts = targetTimeStr.split(":")
                    val targetSecs = parts[0].trim().toIntOrNull()?.let { h ->
                        parts[1].trim().toIntOrNull()?.let { m -> h * 3600 + m * 60 }
                    } ?: (5 * 3600 + 45 * 60)
                    val diff = (24 * 3600 - currentTotalSecs) + targetSecs
                    val h = diff / 3600
                    val m = (diff % 3600) / 60
                    val s = diff % 60
                    timeRemainingStr = String.format("%02d:%02d:%02d", h, m, s)
                }

                val newList = schedule.mapIndexed { index, triple ->
                    val adhan12h = try {
                        val adhanDate = format24h.parse(triple.second)
                        format12h.format(adhanDate!!)
                    } catch (e: Exception) {
                        triple.second
                    }

                    val iqamah12h = if (triple.third != "—") {
                        try {
                            val iqamahDate = format24h.parse(triple.third)
                            format12h.format(iqamahDate!!)
                        } catch (e: Exception) {
                            triple.third
                        }
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
        if (updated.isNotEmpty()) {
            _prayerTimings.value = updated
        }
    }

    fun updatePrayerTimingsFromCloud(livePrayers: List<Pair<String, Pair<String, String>>>) {
        if (livePrayers.isEmpty()) return
        val newSchedule = mutableListOf<Triple<String, String, String>>()
        for ((name, times) in livePrayers) {
            newSchedule.add(Triple(name, parseTo24h(times.first), parseTo24h(times.second)))
        }
        currentPrayerSchedule = newSchedule
    }

    fun loadPrayerTimetableForMosque(mosqueName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val mosqueObj = mosques.find { it.name.equals(mosqueName, ignoreCase = true) }
            if (mosqueObj != null) {
                _activeMosque.value = mosqueObj
            }
            if (SupabaseConfig.isConfigured) {
                SupabaseClient.fetchPrayerSchedules(mosqueName).getOrNull()?.let { livePrayers ->
                    if (livePrayers.isNotEmpty()) {
                        updatePrayerTimingsFromCloud(livePrayers)
                    }
                }
            }
        }
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
                status = "open",
                images = listOf(
                    "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1546938576-6e6a64f317cc?w=600&auto=format&fit=crop&q=80"
                )
            ),
            LostFoundItem(
                title = "Hero Splendor Bike Key with Metal Ring",
                isLost = false,
                category = "Keys",
                area = "Outside Hoode Juma Masjid",
                date = "08 Sep 2026",
                description = "Found after Asr prayer. Safely kept with mosque security office.",
                status = "open",
                images = listOf(
                    "https://images.unsplash.com/photo-1582139329536-e7284fece509?w=600&auto=format&fit=crop&q=80"
                )
            ),
            LostFoundItem(
                title = "Redmi Note 12 (Sky Blue Case)",
                isLost = true,
                category = "Electronics",
                area = "Auto Stand near Bengre Cross",
                date = "06 Sep 2026",
                description = "Phone is locked with pin. Lock screen wallpaper is a family picture.",
                status = "resolved",
                images = listOf(
                    "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1580910051074-3eb694886505?w=600&auto=format&fit=crop&q=80"
                )
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
                title = "Hoode Premier Cricket Tournament 2026",
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
    private val _events = MutableStateFlow<List<CommunityEvent>>(emptyList())
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

    suspend fun postEvent(item: CommunityEvent): Result<Boolean> =
        SupabaseClient.submitEvent(item).onSuccess {
            addContributionPoints("Submitted Community Event", 20)
            syncWithCloud()
        }

    // ── F07: Blood Donor Network ─────────────────────────────
    private val _bloodRequests = MutableStateFlow<List<BloodRequest>>(emptyList())
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

    suspend fun postBloodRequest(item: BloodRequest): Result<Boolean> =
        SupabaseClient.submitBloodRequest(item).onSuccess {
            addContributionPoints("Emergency Blood Request Broadcasted", 15)
            syncWithCloud()
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

    fun addContributionPoints(action: String, points: Int) {
        _totalPoints.value += points
        _contributions.value = listOf(ContributionRecord(action = action, points = points, date = "Just now")) + _contributions.value
    }

    // ── F10: Classifieds & Marketplace ───────────────────────
    private val defaultMockClassifieds = listOf(
        ClassifiedItem(
            id = "cl_01",
            title = "Hercules Roadeo 26T 21-Speed Mountain Bicycle",
            price = "₹4,200",
            type = "Sell",
            category = "Vehicles",
            area = "Kemmannu Road, Hoode",
            description = "Well-maintained mountain bicycle with front suspension, dual disc brakes, and Shimano 21 gears. Used only for 6 months. Minor cosmetic wear. Free bottle holder and helmet included.",
            sellerName = "Arshad Hoode",
            date = "Today",
            phone = "9845112233",
            sellerUserId = "seller_arshad",
            images = listOf(
                "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800",
                "https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?w=800",
                "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?w=800"
            )
        ),
        ClassifiedItem(
            id = "cl_02",
            title = "Solid Teak Wood 4-Seater Dining Table with Chairs",
            price = "₹8,500",
            type = "Sell",
            category = "Furniture",
            area = "Bengre Beach Road",
            description = "Pure Malaysian teak wood 4-seater dining set with matching cushioned chairs. Heavy, premium polish, highly durable. Relocating to Bangalore hence selling at genuine price.",
            sellerName = "Farhan Bengre",
            date = "Yesterday",
            phone = "9880223344",
            sellerUserId = "seller_farhan",
            images = listOf(
                "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=800",
                "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=800",
                "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?w=800"
            )
        ),
        ClassifiedItem(
            id = "cl_03",
            title = "Sony Bravia 43-inch 4K UHD Smart Google TV",
            price = "₹19,000",
            type = "Sell",
            category = "Electronics",
            area = "Kodi Lighthouse View",
            description = "Crystal clear 4K HDR screen with Dolby Audio, built-in Chromecast, and Google TV apps (YouTube, Netflix, Prime). Under warranty for another 5 months with original bill and box.",
            sellerName = "Zaid Kodi",
            date = "2 days ago",
            phone = "9741556677",
            sellerUserId = "seller_zaid",
            images = listOf(
                "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=800",
                "https://images.unsplash.com/photo-1461151304267-38535e780c79?w=800"
            )
        ),
        ClassifiedItem(
            id = "cl_04",
            title = "Yamaha FZ-S 150cc BS6 (Single Owner, Pristine)",
            price = "₹68,000",
            type = "Sell",
            category = "Vehicles",
            area = "Hoode Fisheries Colony",
            description = "2022 registration, single owner, driven only 14,200 kms. Brand new MRF tyres, timely showroom service records, insurance valid until Dec 2026. Non-accidental, mint condition.",
            sellerName = "Riyaz Ahmed",
            date = "3 days ago",
            phone = "9663778899",
            sellerUserId = "seller_riyaz",
            images = listOf(
                "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?w=800",
                "https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?w=800",
                "https://images.unsplash.com/photo-1558981806-ec527fa84c39?w=800"
            )
        ),
        ClassifiedItem(
            id = "cl_05",
            title = "Apple iPad Air 5th Gen (64GB, Wi-Fi) + Apple Pencil",
            price = "₹34,000",
            type = "Sell",
            category = "Electronics",
            area = "Kemmannu Town",
            description = "M1 chip beast performance with 10.9-inch Liquid Retina display. Includes Apple Pencil 2nd gen and ESR magnetic folio cover. Battery health 96%. Ideal for students & designers.",
            sellerName = "Sohail K",
            date = "4 days ago",
            phone = "9844001122",
            sellerUserId = "seller_sohail",
            images = listOf(
                "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800",
                "https://images.unsplash.com/photo-1561154464-82e9adf32764?w=800"
            )
        )
    )

    private val _classifieds = MutableStateFlow<List<ClassifiedItem>>(defaultMockClassifieds)
    val classifieds: StateFlow<List<ClassifiedItem>> = _classifieds.asStateFlow()

    fun checkAndExpireBookings() {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        var changed = false
        val updated = _classifieds.value.map { item ->
            if (item.isBooked && !item.isSold && (now - item.bookedAtTimestamp >= oneDayMillis)) {
                changed = true
                item.copy(
                    isBooked = false,
                    bookedByUserId = null,
                    bookedByName = null,
                    bookedByPhone = null,
                    bookedAtTimestamp = 0L,
                    bookingNote = null
                )
            } else {
                item
            }
        }
        if (changed) {
            _classifieds.value = updated
        }
    }

    fun isBookingActive(item: ClassifiedItem): Boolean {
        if (!item.isBooked || item.isSold) return false
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        return (now - item.bookedAtTimestamp) < oneDayMillis
    }

    fun getActiveBookingForUser(userId: String): ClassifiedItem? {
        checkAndExpireBookings()
        return _classifieds.value.firstOrNull { item ->
            isBookingActive(item) && item.bookedByUserId == userId
        }
    }

    suspend fun bookClassified(
        itemId: String,
        user: User,
        phone: String,
        note: String
    ): Result<Boolean> {
        checkAndExpireBookings()
        val existingBooking = getActiveBookingForUser(user.id)
        if (existingBooking != null && existingBooking.id != itemId) {
            return Result.failure(
                IllegalStateException("You already have an active 24h booking for '${existingBooking.title}'. Per Spinny guidelines, you can only hold one product at a time. Please cancel your previous reservation or wait for it to expire.")
            )
        }

        val target = _classifieds.value.firstOrNull { it.id == itemId }
            ?: return Result.failure(IllegalArgumentException("Product not found."))

        if (target.isSold) {
            return Result.failure(IllegalStateException("This product has already been sold."))
        }
        if (isBookingActive(target) && target.bookedByUserId != user.id) {
            return Result.failure(IllegalStateException("This product is already booked by another resident."))
        }

        val updated = _classifieds.value.map { item ->
            if (item.id == itemId) {
                item.copy(
                    isBooked = true,
                    bookedByUserId = user.id,
                    bookedByName = user.displayName,
                    bookedByPhone = phone,
                    bookedAtTimestamp = System.currentTimeMillis(),
                    bookingNote = note
                )
            } else item
        }
        _classifieds.value = updated
        addContributionPoints("Reserved Marketplace Product", 5)
        return Result.success(true)
    }

    suspend fun cancelBooking(itemId: String, userId: String): Result<Boolean> {
        val target = _classifieds.value.firstOrNull { it.id == itemId }
            ?: return Result.failure(IllegalArgumentException("Product not found."))

        val updated = _classifieds.value.map { item ->
            if (item.id == itemId) {
                item.copy(
                    isBooked = false,
                    bookedByUserId = null,
                    bookedByName = null,
                    bookedByPhone = null,
                    bookedAtTimestamp = 0L,
                    bookingNote = null
                )
            } else item
        }
        _classifieds.value = updated
        return Result.success(true)
    }

    suspend fun markClassifiedSold(itemId: String, sellerUserId: String?): Result<Boolean> {
        val updated = _classifieds.value.map { item ->
            if (item.id == itemId) {
                item.copy(isSold = true)
            } else item
        }
        _classifieds.value = updated
        addContributionPoints("Sold Marketplace Item", 25)
        return Result.success(true)
    }

    suspend fun removeClassified(itemId: String): Result<Boolean> {
        _classifieds.value = _classifieds.value.filter { it.id != itemId }
        return Result.success(true)
    }

    suspend fun postClassified(item: ClassifiedItem): Result<Boolean> {
        val user = _currentUser.value
        val enriched = if (item.sellerUserId.isNullOrBlank() && user != null) {
            item.copy(sellerUserId = user.id)
        } else item
        _classifieds.value = listOf(enriched) + _classifieds.value
        return SupabaseClient.submitMarketplace(enriched).onSuccess {
            addContributionPoints("Submitted Marketplace Listing", 15)
            syncWithCloud()
        }
    }

    // ── Genuine Profile Posts & Community Feed ───────────────
    private val _userCommunityPosts = MutableStateFlow<List<UserPostItem>>(emptyList())
    val userCommunityPosts: StateFlow<List<UserPostItem>> = _userCommunityPosts.asStateFlow()

    fun addUserCommunityPost(post: UserPostItem) {
        _userCommunityPosts.value = listOf(post) + _userCommunityPosts.value
        addContributionPoints("Shared Community Post", 10)
    }

    fun getUserPosts(user: User?): List<UserPostItem> {
        if (user == null) return emptyList()
        val posts = mutableListOf<UserPostItem>()
        val userName = user.displayName.trim().lowercase()
        val userPhone = user.phone?.trim()

        // 1. Direct community updates authored by user
        posts.addAll(_userCommunityPosts.value)

        // 2. Marketplace items created by user
        _classifieds.value.filter {
            it.sellerName.trim().lowercase() == userName ||
            (!userPhone.isNullOrBlank() && it.phone.trim() == userPhone)
        }.forEach { item ->
            posts.add(
                UserPostItem(
                    id = item.id,
                    title = item.title,
                    type = "Marketplace • ${item.category}",
                    content = "${item.price} • ${item.description}\nArea: ${item.area}",
                    date = item.date,
                    imageUrl = item.images.firstOrNull(),
                    likes = 14,
                    comments = 3
                )
            )
        }

        // 3. Lost & Found items reported by user
        _lostFound.value.filter {
            (!userPhone.isNullOrBlank() && it.contactPhone.trim() == userPhone)
        }.forEach { item ->
            posts.add(
                UserPostItem(
                    id = item.id,
                    title = "${if (item.isLost) "Lost" else "Found"}: ${item.title}",
                    type = "Lost & Found • ${item.category}",
                    content = "${item.description}\nLocation: ${item.area}",
                    date = item.date,
                    imageUrl = item.images.firstOrNull(),
                    likes = 8,
                    comments = 1
                )
            )
        }

        // 4. Events organized by user
        _events.value.filter {
            it.organizer.trim().lowercase() == userName
        }.forEach { item ->
            posts.add(
                UserPostItem(
                    id = item.id,
                    title = item.title,
                    type = "Event • ${item.category}",
                    content = "When: ${item.date} • Where: ${item.venue}",
                    date = item.date,
                    imageUrl = null,
                    likes = item.rsvpGoing,
                    comments = 2
                )
            )
        }

        // 5. Gallery photos by user
        _galleryItems.value.filter {
            it.photographer.trim().lowercase() == userName
        }.forEach { item ->
            posts.add(
                UserPostItem(
                    id = item.id,
                    title = item.title,
                    type = "Gallery Photo",
                    content = item.caption.ifBlank { "Photo shared to Hoode Gallery" },
                    date = "Community Photo",
                    imageUrl = item.imageUrl,
                    likes = 18,
                    comments = 2
                )
            )
        }

        return posts
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
            NotificationItem(title = "Cricket Tournament Matches Announced", body = "Check out the tournament fixtures starting 18 September.", timestamp = "Yesterday", category = "Announcement", isRead = true),
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
    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
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
                    9 -> "Coastal Highway at Dawn"
                    10 -> "Delta Point Confluence"
                    11 -> "Seagulls over Arabian Sea"
                    12 -> "Traditional Wooden Dhow"
                    13 -> "Palm Silhouette Sunset"
                    14 -> "Golden Hour Coastal Waves"
                    15 -> "Backwaters Mangrove Trail"
                    16 -> "Village Fishermen at Shore"
                    17 -> "Serene Beach Morning"
                    18 -> "Coastal Flora & Greenery"
                    19 -> "Suvarna River Reflection"
                    20 -> "Harbor at Twilight"
                    21 -> "Coastal Community Gathering"
                    22 -> "Monsoon Greenery in Hoode"
                    23 -> "Old Jetty Rocks"
                    24 -> "Fishermen Trawler Fleet"
                    else -> "Starry Night Over Sea"
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
                },
                imageUrl = when (index) {
                    1 -> "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80"
                    2 -> "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=600&auto=format&fit=crop&q=80"
                    3 -> "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=600&auto=format&fit=crop&q=80"
                    4 -> "https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?w=600&auto=format&fit=crop&q=80"
                    5 -> "https://images.unsplash.com/photo-1509233725247-49e657c54213?w=600&auto=format&fit=crop&q=80"
                    6 -> "https://images.unsplash.com/photo-1516738901171-8eb4fc13bd20?w=600&auto=format&fit=crop&q=80"
                    7 -> "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80"
                    8 -> "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?w=600&auto=format&fit=crop&q=80"
                    9 -> "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=600&auto=format&fit=crop&q=80"
                    10 -> "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=600&auto=format&fit=crop&q=80"
                    11 -> "https://images.unsplash.com/photo-1448375240586-882707db888b?w=600&auto=format&fit=crop&q=80"
                    12 -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=600&auto=format&fit=crop&q=80"
                    13 -> "https://images.unsplash.com/photo-1510414842594-a61c69b5ae57?w=600&auto=format&fit=crop&q=80"
                    14 -> "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=600&auto=format&fit=crop&q=80"
                    15 -> "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=600&auto=format&fit=crop&q=80"
                    16 -> "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=600&auto=format&fit=crop&q=80"
                    17 -> "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=600&auto=format&fit=crop&q=80"
                    18 -> "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=600&auto=format&fit=crop&q=80"
                    19 -> "https://images.unsplash.com/photo-1426604966848-d7adac402bff?w=600&auto=format&fit=crop&q=80"
                    20 -> "https://images.unsplash.com/photo-1498084393753-b411b2d26b34?w=600&auto=format&fit=crop&q=80"
                    21 -> "https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=600&auto=format&fit=crop&q=80"
                    22 -> "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=600&auto=format&fit=crop&q=80"
                    23 -> "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80"
                    24 -> "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=600&auto=format&fit=crop&q=80"
                    else -> "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=600&auto=format&fit=crop&q=80"
                }
            )
        }
    )
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems.asStateFlow()

    fun addGalleryItem(item: GalleryItem): Boolean {
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
                title = "Open Cricket Selection Trials",
                category = "Sports",
                organizer = "Hoode Tournament Governing Council",
                schedule = "Sunday morning, 6:00 AM",
                venue = "Town Ground, Kemmannu",
                description = "Open net trials for local youngsters to register for the upcoming tournament season."
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
            CalendarEvent(dateStr = "2026-09-22", title = "Annual Cricket Tournament", type = "event"),
            CalendarEvent(dateStr = "2026-10-10", title = "Free Medical Camp", type = "event"),
            CalendarEvent(dateStr = "2026-11-01", title = "Karnataka Rajyotsava", type = "holiday")
        )
    )
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    // ── Cloud Sync & Content Moderation ──────────────────────

    private val syncScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private var syncJob: kotlinx.coroutines.Job? = null

    @Synchronized fun syncWithCloud() {
        if (syncJob?.isActive == true) return
        syncJob = syncScope.launch {
            if (!SupabaseConfig.isConfigured) return@launch

            try {
                // 1. Sync News
                SupabaseClient.fetchNews("published").getOrNull()?.let { liveNews ->
                    _newsArticles.value = liveNews
                }

                // 2. Sync Events
                SupabaseClient.fetchEvents("published").getOrNull()?.let { liveEvents ->
                    _events.value = liveEvents
                }

                // 3. Sync Marketplace
                SupabaseClient.fetchMarketplace("published").getOrNull()?.let { liveMarket ->
                    _classifieds.value = liveMarket
                }

                // 4. Sync Blood Requests
                SupabaseClient.fetchBloodRequests().getOrNull()?.let { liveBlood ->
                    _bloodRequests.value = liveBlood
                }

                // 5. Sync Carousel Ads (Managed by Admin Console)
                SupabaseClient.fetchCarouselAds().getOrNull()?.let { liveAds ->
                    _adSlides.value = liveAds
                }

                // 6. Sync Prayer Schedules (Managed by Admin Console)
                SupabaseClient.fetchPrayerSchedules(_activeMosque.value.name).getOrNull()?.let { livePrayers ->
                    if (livePrayers.isNotEmpty()) {
                        updatePrayerTimingsFromCloud(livePrayers)
                    }
                }
            } catch (e: Exception) {
                Log.w("HoodeRepository", "Cloud sync notice", e)
            }
        }
    }

    data class ModerationItem(
        val id: String,
        val type: String, // "News", "Event", "Marketplace"
        val title: String,
        val subtitle: String,
        val description: String,
        val imageUrl: String = ""
    )

    suspend fun fetchPendingModerationItems(): List<ModerationItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ModerationItem>()
        if (!SupabaseConfig.isConfigured) return@withContext list

        try {
            // Pending News
            SupabaseClient.fetchNews("pending").getOrNull()?.forEach {
                list.add(ModerationItem(it.id, "News", it.title, "${it.verifier} • ${it.verifiedDate}", it.body, it.imageUrl))
            }

            // Pending Events
            SupabaseClient.fetchEvents("pending").getOrNull()?.forEach {
                list.add(ModerationItem(it.id, "Event", it.title, "${it.organizer} • ${it.date}", it.venue, ""))
            }

            // Pending Marketplace
            SupabaseClient.fetchMarketplace("pending").getOrNull()?.forEach {
                list.add(ModerationItem(it.id, "Marketplace", it.title, "${it.price} • ${it.sellerName}", it.description, it.images.firstOrNull() ?: ""))
            }
        } catch (e: Exception) {
            Log.e("HoodeRepository", "Failed to fetch pending items", e)
        }
        list
    }

    suspend fun moderateItem(itemId: String, type: String, approve: Boolean): Boolean = withContext(Dispatchers.IO) {
        val newStatus = if (approve) "published" else "rejected"
        val success = when (type) {
            "News" -> SupabaseClient.updateNewsStatus(itemId, newStatus).getOrDefault(false)
            "Event" -> SupabaseClient.updateEventStatus(itemId, newStatus).getOrDefault(false)
            "Marketplace" -> SupabaseClient.updateMarketplaceStatus(itemId, newStatus).getOrDefault(false)
            else -> false
        }
        if (success && approve) {
            syncWithCloud()
        }
        success
    }
}

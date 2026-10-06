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

    private val _sessionReady=MutableStateFlow(false)
    val sessionReady=_sessionReady.asStateFlow()
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
                                profession = profession,
                                username = profile.optString("username").takeUnless { it == "null" },
                                bio = profile.optString("bio").takeUnless { it == "null" },
                                coverPicUri = profile.optString("cover_url").takeUnless { it == "null" }
                            )
                            _currentUser.value = updated
                            saveUserToCache(updated)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("HoodeRepository", "BackendSession background restore failed", e)
            } finally { _sessionReady.value=true }
        }
    }

    fun isLoggedIn(): Boolean = _currentUser.value != null

    fun loginAsGuest() {
        val guestUser = User(
            id = "guest_resident_001",
            email = "guest@hoodeconnect.org",
            displayName = "Mohammed Suhail",
            phone = "+91 98450 12345",
            bloodGroup = "O+",
            profession = "Resident",
            locality = "Hoode Main",
            bio = "Hoode Resident - Community Member",
            roles = listOf("approved_resident")
        )
        _currentUser.value = guestUser
        com.hoodeconnect.backend.BackendSession.accessToken = "guest_access_token_active"
        com.hoodeconnect.backend.BackendSession.userId = guestUser.id
        saveUserToCache(guestUser)
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
                if (com.hoodeconnect.backend.BackendSession.accessToken.isNullOrBlank()) {
                    com.hoodeconnect.backend.BackendSession.accessToken = "guest_access_token_active"
                    com.hoodeconnect.backend.BackendSession.userId = user.id
                }
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
        syncJob?.cancel()
        _classifieds.value=emptyList();_notifications.value=emptyList();_contributions.value=emptyList()
        _polls.value=emptyList();_events.value=emptyList();_civicIssues.value=emptyList();_registeredDonors.value=emptyList()
        _badges.value=emptyList();_totalPoints.value=0
        clearUserCache()
        com.hoodeconnect.backend.BackendSession.clear()
    }

    // ── F00: 5-Slide Carousel ────────────────────────────────
    private val _adSlides = MutableStateFlow<List<AdCarouselSlide>>(emptyList())
    val adSlides: StateFlow<List<AdCarouselSlide>> = _adSlides.asStateFlow()



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
    private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()

    // ── F03: Jobs & Gigs ─────────────────────────────────────
    private val _jobs = MutableStateFlow<List<JobPosting>>(emptyList())
    val jobs: StateFlow<List<JobPosting>> = _jobs.asStateFlow()

    suspend fun applyJob(jobId:String,name:String,phone:String,message:String) =
        com.hoodeconnect.backend.CommunityApi.submit("job_application",name,JSONObject().put("job_id",jobId).put("phone",phone).put("message",message))

    suspend fun postJob(job:JobPosting,phone:String) = com.hoodeconnect.backend.CommunityApi.submit("job",job.title,
        JSONObject().put("employer",job.employer).put("type",job.type).put("pay",job.pay).put("location",job.location)
            .put("description",job.description).put("deadline",job.deadline).put("phone",phone),id=job.id)

    // ── F04: Lost & Found ────────────────────────────────────
    private val _lostFound = MutableStateFlow<List<LostFoundItem>>(emptyList())
    val lostFound: StateFlow<List<LostFoundItem>> = _lostFound.asStateFlow()

    suspend fun postLostFound(item:LostFoundItem) = com.hoodeconnect.backend.CommunityApi.submit("lost_found",item.title,
        JSONObject().put("is_lost",item.isLost).put("category",item.category).put("area",item.area).put("date",item.date)
            .put("description",item.description).put("phone",item.contactPhone),imageUrl=item.images.firstOrNull().orEmpty(),id=item.id)

    suspend fun claimLostFound(itemId:String,proof:String) = com.hoodeconnect.backend.CommunityApi.submit("correction","Lost & found claim",
        JSONObject().put("target_id",itemId).put("description",proof))

    // ── F05: Tournaments & Matches ───────────────────────────
    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

    private val _fixtures = MutableStateFlow<List<Fixture>>(emptyList())
    val fixtures: StateFlow<List<Fixture>> = _fixtures.asStateFlow()

    private val _standings = MutableStateFlow<List<Standing>>(emptyList())
    val standings: StateFlow<List<Standing>> = _standings.asStateFlow()

    suspend fun registerTournamentTeam(teamName:String,captain:String,phone:String,tournament:String) =
        com.hoodeconnect.backend.CommunityApi.submit("team_registration",teamName,JSONObject().put("tournament",tournament).put("captain",captain).put("phone",phone))

    // ── F06: Events & Weddings ───────────────────────────────
    private val _events = MutableStateFlow<List<CommunityEvent>>(emptyList())
    val events: StateFlow<List<CommunityEvent>> = _events.asStateFlow()

    suspend fun rsvpEvent(eventId:String,going:Boolean):Result<Boolean> {
        val result=com.hoodeconnect.backend.CommunityApi.rpc("hoode_rsvp",JSONObject().put("event",eventId).put("attending",going)).map{true}
        if(result.isSuccess)refreshEventAttendance()
        return result
    }

    suspend fun postEvent(item: CommunityEvent): Result<Boolean> =
        SupabaseClient.submitEvent(item).onSuccess {
            addContributionPoints("Submitted Community Event", 20)
            syncWithCloud()
        }

    // ── F07: Blood Donor Network ─────────────────────────────
    private val _bloodRequests = MutableStateFlow<List<BloodRequest>>(emptyList())
    val bloodRequests: StateFlow<List<BloodRequest>> = _bloodRequests.asStateFlow()

    private val _registeredDonors = MutableStateFlow<List<DonorRegistration>>(emptyList())
    val registeredDonors: StateFlow<List<DonorRegistration>> = _registeredDonors.asStateFlow()

    suspend fun registerDonor(donor:DonorRegistration) = com.hoodeconnect.backend.CommunityApi.submit("donor",donor.name,
        JSONObject().put("blood_group",donor.bloodGroup).put("area",donor.area).put("phone",donor.phone),id=donor.id)

    suspend fun postBloodRequest(item: BloodRequest): Result<Boolean> =
        SupabaseClient.submitBloodRequest(item).onSuccess {
            addContributionPoints("Emergency Blood Request Broadcasted", 15)
            syncWithCloud()
        }

    // ── F08: Polls & Civic Issues ────────────────────────────
    private val _polls = MutableStateFlow<List<CommunityPoll>>(emptyList())
    val polls: StateFlow<List<CommunityPoll>> = _polls.asStateFlow()

    suspend fun castVote(pollId:String,optionId:String):Result<Boolean> {
        val result=com.hoodeconnect.backend.CommunityApi.rpc("hoode_vote",JSONObject().put("poll",pollId).put("choice",optionId)).map{true}
        if(result.isSuccess)refreshPolls()
        return result
    }

    private val _civicIssues = MutableStateFlow<List<CivicIssue>>(emptyList())
    val civicIssues: StateFlow<List<CivicIssue>> = _civicIssues.asStateFlow()

    suspend fun endorseCivicIssue(issueId:String):Result<Boolean> {
        val result=com.hoodeconnect.backend.CommunityApi.like("community_content",issueId,true)
        if(result.isSuccess)refreshCivicSupport()
        return result
    }

    suspend fun submitCivicIssue(title:String,category:String,location:String,details:String="") =
        com.hoodeconnect.backend.CommunityApi.submit("civic",title,JSONObject().put("category",category).put("location",location).put("description",details))

    // ── F09: Badges & Contribution Points ────────────────────
    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()

    private val _badges = MutableStateFlow<List<CommunityBadge>>(emptyList())
    val badges: StateFlow<List<CommunityBadge>> = _badges.asStateFlow()

    private val _contributions = MutableStateFlow<List<ContributionRecord>>(emptyList())
    val contributions: StateFlow<List<ContributionRecord>> = _contributions.asStateFlow()

    fun addContributionPoints(action:String,points:Int) { syncWithCloud() } // Awards are derived from approved database records.

    // ── F10: Classifieds & Marketplace ───────────────────────
    private val _classifieds = MutableStateFlow<List<ClassifiedItem>>(emptyList())
    val classifieds: StateFlow<List<ClassifiedItem>> = _classifieds.asStateFlow()

    fun checkAndExpireBookings() { /* Availability is returned by the server; the UI also checks its expiry. */ }
    fun isBookingActive(item: ClassifiedItem): Boolean = item.isBooked && !item.isSold &&
        System.currentTimeMillis() < item.bookedAtTimestamp + 24 * 60 * 60 * 1000L
    fun getActiveBookingForUser(userId: String): ClassifiedItem? =
        _classifieds.value.firstOrNull { isBookingActive(it) && it.bookedByUserId == userId }
    suspend fun bookClassified(itemId: String, user: User, phone: String, note: String): Result<Boolean> {
        val result = com.hoodeconnect.backend.CommunityApi.rpc("hoode_book_marketplace",
            JSONObject().put("item",itemId).put("contact_phone",phone).put("booking_note",note)).map { true }
        if(result.isSuccess) refreshMarketplace()
        return result
    }
    private suspend fun marketAction(itemId:String, action:String):Result<Boolean> {
        val result=com.hoodeconnect.backend.CommunityApi.rpc("hoode_marketplace_action",JSONObject().put("item",itemId).put("action",action))
            .mapCatching { check(it=="true") { "The listing or reservation changed. Refresh and try again." };true }
        if(result.isSuccess) refreshMarketplace()
        return result
    }
    suspend fun cancelBooking(itemId:String,userId:String):Result<Boolean> = marketAction(itemId,"cancel")
    suspend fun markClassifiedSold(itemId:String,sellerUserId:String?):Result<Boolean> = marketAction(itemId,"sold")
    suspend fun removeClassified(itemId:String):Result<Boolean> = marketAction(itemId,"withdraw")
    suspend fun refreshMarketplace() { SupabaseClient.fetchMarketplace().onSuccess { _classifieds.value=it } }
    suspend fun postClassified(item:ClassifiedItem):Result<Boolean> = SupabaseClient.submitMarketplace(item).onSuccess { syncWithCloud() }

    // ── Genuine Profile Posts & Community Feed ───────────────
    // ── F12: Ramadan Timetable ────────────────────────────────
    private val _ramadanTimetable = MutableStateFlow<List<RamadanTimetable>>(emptyList())
    val ramadanTimetable: StateFlow<List<RamadanTimetable>> = _ramadanTimetable.asStateFlow()

    // ── F13: Notifications ───────────────────────────────────
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    suspend fun markNotificationsRead(ids:List<String>):Result<Boolean> {
        val result=com.hoodeconnect.backend.CommunityApi.rpc("hoode_mark_notifications_read",JSONObject().put("ids",JSONArray(ids))).map{true}
        if(result.isSuccess)_notifications.value=_notifications.value.map{if(it.id in ids)it.copy(isRead=true)else it}
        return result
    }

    // ── F14: Verified Local News ─────────────────────────────
    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles.asStateFlow()

    // ── F15: Handyman & Service Providers ────────────────────
    private val _providers = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val providers: StateFlow<List<ServiceProvider>> = _providers.asStateFlow()

    // ── F16: Daily Personality ───────────────────────────────
    private val _dailyPersonality = MutableStateFlow(PersonalityProfile(name="",role="",featuredDate="",intro="",fullBiography="",contributions=emptyList(),quote="",imageUrl=""))
    val dailyPersonality: StateFlow<PersonalityProfile> = _dailyPersonality.asStateFlow()



    // ── F17: Hoode Photo Gallery (Max 25 images) ──────────────
    private val _galleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems.asStateFlow()





    // ── F18: Activities ──────────────────────────────────────
    var initialActivityCategory: String = "All"

    private val _activities = MutableStateFlow<List<CommunityActivity>>(emptyList())
    val activities: StateFlow<List<CommunityActivity>> = _activities.asStateFlow()



    // ── F19: Educational Offerings ───────────────────────────
    private val _educationOfferings = MutableStateFlow<List<EducationOffering>>(emptyList())
    val educationOfferings: StateFlow<List<EducationOffering>> = _educationOfferings.asStateFlow()



    // ── F20: Our Huffaz ──────────────────────────────────────
    private val _huffazList = MutableStateFlow<List<HuffazProfile>>(emptyList())
    val huffazList: StateFlow<List<HuffazProfile>> = _huffazList.asStateFlow()



    // ── F21: Calendar Events ─────────────────────────────────
    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    // ── Cloud Sync & Content Moderation ──────────────────────

    private val syncScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private var syncJob: kotlinx.coroutines.Job? = null

    @Synchronized fun syncWithCloud(): kotlinx.coroutines.Job {
        syncJob?.takeIf { it.isActive }?.let { return it }
        return syncScope.launch {
            if (!SupabaseConfig.isConfigured) return@launch
            kotlinx.coroutines.supervisorScope {
                fun refresh(label: String, action: suspend () -> Unit) = launch {
                    try { action() }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (error: Exception) { Log.w("HoodeRepository", "Could not refresh $label", error) }
                }
                // Independent feeds load together; failure in one does not block the others.
                refresh("news") {
                    SupabaseClient.fetchNews("published").getOrNull()?.let { _newsArticles.value = it }
                }
                refresh("events") {
                    SupabaseClient.fetchEvents("published").getOrNull()?.let { _events.value = it }
                    refreshEventAttendance()
                }
                refresh("marketplace") {
                    SupabaseClient.fetchMarketplace("published").getOrNull()?.let { _classifieds.value = it }
                }
                refresh("blood requests") {
                    SupabaseClient.fetchBloodRequests().getOrNull()?.let { _bloodRequests.value = it }
                }
                refresh("sponsorship") {
                    SupabaseClient.fetchCarouselAds().getOrNull()?.let { _adSlides.value = it }
                }
                refresh("community") {
                    refreshCommunityContent()
                    refreshCivicSupport()
                }
                refresh("polls") { refreshPolls() }
                refresh("account activity") { refreshAccountActivity() }
                refresh("prayers") {
                    val mosque = _activeMosque.value.name
                    SupabaseClient.fetchPrayerSchedules(mosque).getOrNull()?.let {
                        if (it.isNotEmpty() && _activeMosque.value.name == mosque) updatePrayerTimingsFromCloud(it)
                    }
                }
            }
        }.also { syncJob = it }
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
    private val _highlights = MutableStateFlow<List<com.hoodeconnect.backend.CommunityEntry>>(emptyList())
    val highlights = _highlights.asStateFlow()
    suspend fun refreshCommunityContent() {
        val entries=mutableListOf<com.hoodeconnect.backend.CommunityEntry>()
        var offset=0
        do {
            val page=com.hoodeconnect.backend.CommunityApi.entries(offset=offset).getOrThrow()
            entries.addAll(page);offset+=page.size
        } while(page.size==50)
        fun of(kind:String)=entries.filter{it.kind==kind}
        _emergencyContacts.value=of("emergency").map{EmergencyContact(id=it.id,name=it.title,category=it.text("category"),phone=it.text("phone"),area=it.text("area"),lastVerified=it.createdAt.take(10))}
        _providers.value=of("provider").map{ServiceProvider(id=it.id,name=it.title,category=it.text("category"),rating=0.0,reviewCount=0,phone=it.text("phone"),area=it.text("area"),availability=it.text("availability"))}
        _huffazList.value=of("huffaz").map{HuffazProfile(id=it.id,name=it.title,completionYear=it.text("completion_year"),institution=it.text("institution"),teacher=it.text("teacher"),biography=it.text("biography"),imageUrl=it.imageUrl)}
        _activities.value=of("activity").map{CommunityActivity(id=it.id,title=it.title,category=it.text("category"),organizer=it.text("organizer"),schedule=it.text("schedule"),venue=it.text("venue"),description=it.text("description"))}
        _galleryItems.value=of("gallery").map{GalleryItem(id=it.id,title=it.title,caption=it.text("caption"),photographer=it.text("photographer"),imageUrl=it.imageUrl)}
        _jobs.value=of("job").map{JobPosting(id=it.id,title=it.title,employer=it.text("employer"),type=it.text("type"),pay=it.text("pay"),location=it.text("location"),description=it.text("description"),deadline=it.text("deadline"))}
        _lostFound.value=of("lost_found").map{LostFoundItem(id=it.id,title=it.title,isLost=it.payload.optBoolean("is_lost",true),category=it.text("category"),area=it.text("area"),date=it.text("date",it.createdAt.take(10)),description=it.text("description"),images=listOf(it.imageUrl).filter{url->url.isNotBlank()},contactPhone=it.text("phone"))}
        _civicIssues.value=of("civic").map{CivicIssue(id=it.id,title=it.title,category=it.text("category"),location=it.text("location"),status="acknowledged",reportedDate=it.createdAt.take(10))}
        _educationOfferings.value=of("education").map{EducationOffering(id=it.id,title=it.title,provider=it.text("provider"),category=it.text("category"),audience=it.text("audience"),schedule=it.text("schedule"),contact=it.text("contact"),description=it.text("description"))}
        _registeredDonors.value=of("donor").map{DonorRegistration(id=it.id,name=it.title,bloodGroup=it.text("blood_group"),area=it.text("area"),phone=it.text("phone"))}
        _tournaments.value=of("tournament").map{Tournament(it.id,it.title,it.text("sport"),it.text("venue"),it.text("dates"),it.text("format"),teamsCount=0)}
        _fixtures.value=of("fixture").map{Fixture(id=it.id,tournamentId=it.text("tournament_id"),round=it.text("round"),teamA=it.text("team_a"),teamB=it.text("team_b"),time=it.text("time"),venue=it.text("venue"),scoreA=it.text("score_a").ifBlank{null},scoreB=it.text("score_b").ifBlank{null})}
        _standings.value=of("standing").map{Standing(teamName=it.title,played=it.text("played").toIntOrNull()?:0,won=it.text("won").toIntOrNull()?:0,lost=it.text("lost").toIntOrNull()?:0,points=it.text("points").toIntOrNull()?:0,netRunRate=it.text("net_run_rate"),tournamentId=it.text("tournament_id"))}.sortedByDescending{it.points}
        _calendarEvents.value=of("calendar").map{CalendarEvent(it.text("date"),it.title,it.text("type"))}
        _ramadanTimetable.value=of("ramadan").map{RamadanTimetable(it.text("day").toIntOrNull()?:1,it.text("date"),it.text("suhoor_end"),it.text("iftar_time"))}.sortedBy{it.day}
        _highlights.value=of("highlight")
        val p=of("personality").firstOrNull()
        _dailyPersonality.value=if(p==null)PersonalityProfile(name="",role="",featuredDate="",intro="",fullBiography="",contributions=emptyList(),quote="",imageUrl="") else
            PersonalityProfile(id=p.id,name=p.title,role=p.text("role"),featuredDate=p.text("featured_date"),intro=p.text("intro"),fullBiography=p.text("biography"),contributions=p.text("contributions").lines().filter{it.isNotBlank()},quote=p.text("quote"),imageUrl=p.imageUrl)
    }

    private fun objects(raw:String):List<JSONObject> { val a=JSONArray(raw);return List(a.length()){a.getJSONObject(it)} }
    private suspend fun refreshEventAttendance() {
        com.hoodeconnect.backend.CommunityApi.rpc("hoode_event_attendance").onSuccess {raw->val states=objects(raw).associateBy{it.getString("id")}
            _events.value=_events.value.map {event->states[event.id]?.let {event.copy(rsvpGoing=it.optInt("attending"),userRsvp=if(it.isNull("going"))null else it.getBoolean("going"))}?:event}
        }
    }
    private suspend fun refreshCivicSupport() {
        com.hoodeconnect.backend.CommunityApi.rpc("hoode_civic_support").onSuccess {raw->val states=objects(raw).associateBy{it.getString("id")}
            _civicIssues.value=_civicIssues.value.map {issue->states[issue.id]?.let {issue.copy(endorsements=it.optInt("endorsements"),userEndorsed=it.optBoolean("endorsed"))}?:issue}
        }
    }
    private suspend fun refreshPolls() {
        com.hoodeconnect.backend.CommunityApi.rpc("hoode_poll_results").onSuccess {raw->_polls.value=objects(raw).map{r->
            val p=r.getJSONObject("payload");val counts=r.getJSONObject("counts")
            val options=p.getString("options").trim().lines().map{label->PollOption(label,label,counts.optInt(label))}
            CommunityPoll(r.getString("id"),r.getString("title"),p.optString("description"),options,p.optString("closes_at"),r.optString("voted").takeUnless{it=="null"||it.isBlank()},options.sumOf{it.votes})
        }}
    }
    private suspend fun refreshAccountActivity() {
        if(!isLoggedIn())return
        com.hoodeconnect.backend.CommunityApi.rpc("hoode_notifications").onSuccess {raw->_notifications.value=objects(raw).map{r->NotificationItem(r.getString("id"),r.getString("title"),
            (if(r.optString("status")=="published")"Your submission was approved." else "Your submission was rejected.")+r.optString("rejection_reason").takeUnless{it=="null"}.orEmpty().let{if(it.isBlank())"" else "\n$it"},r.optString("created_at").take(10),"Review",r.optBoolean("is_read"))}}
        com.hoodeconnect.backend.CommunityApi.rpc("hoode_contributions").onSuccess {raw->val r=JSONObject(raw)
            _totalPoints.value=r.optInt("total");val items=r.getJSONArray("items")
            _contributions.value=List(items.length()){i->val c=items.getJSONObject(i);ContributionRecord(c.getString("id"),"Approved: ${c.getString("title")}",c.getInt("points"),c.optString("created_at").take(10))}
            _badges.value=listOf(CommunityBadge("contributor","Community contributor","One approved contribution","ic_badge",r.optInt("approved")>0),
                CommunityBadge("neighbor","Helpful neighbor","Five approved contributions","ic_badge",r.optInt("approved")>=5),
                CommunityBadge("donor","Registered donor","Donor registration approved","ic_blood",r.optBoolean("donor")))
        }
    }

}

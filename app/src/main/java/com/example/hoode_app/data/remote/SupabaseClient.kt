package com.example.hoode_app.data.remote

import android.util.Log
import com.example.hoode_app.BuildConfig
import com.hoodeconnect.backend.BackendSession
import com.example.hoode_app.data.model.BloodRequest
import com.example.hoode_app.data.model.ClassifiedItem
import com.example.hoode_app.data.model.CommunityEvent
import com.example.hoode_app.data.model.NewsArticle
import com.example.hoode_app.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object SupabaseConfig {
    // Verified endpoints for production Supabase backend
    val supabaseUrl: String = BuildConfig.SUPABASE_URL
    val supabaseKey: String = BuildConfig.SUPABASE_ANON_KEY

    val isConfigured: Boolean
        get() = supabaseUrl.startsWith("https://") && !supabaseUrl.contains("example-project")
}

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(BackendSession.interceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    var currentAuthToken: String?
        get() = BackendSession.accessToken
        set(value) { BackendSession.accessToken = value }
    var currentUserId: String?
        get() = BackendSession.userId
        set(value) { BackendSession.userId = value }

    private fun getAuthHeader(): String {
        val token = currentAuthToken
        return if (!token.isNullOrBlank()) "Bearer $token" else "Bearer ${SupabaseConfig.supabaseKey}"
    }

    // ── Authentication ────────────────────────────────────────────────────────

    suspend fun signUp(
        email: String,
        pass: String,
        name: String,
        phone: String = "",
        ward: String = "Hoode"
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/auth/v1/signup"
            val bodyJson = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", pass)
                put("data", JSONObject().apply {
                    put("name", name.trim())
                    put("phone", phone.trim())
                    put("ward", ward)
                })
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.supabaseKey}")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(JSON_MEDIA))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errJson = try { JSONObject(respBody) } catch (_: Exception) { null }
                    val errMsg = errJson?.optString("error_description")
                        ?: errJson?.optString("msg")
                        ?: "Signup failed (${response.code})"
                    return@withContext Result.failure(IOException(errMsg))
                }

                val json = JSONObject(respBody)
                val userObj = json.optJSONObject("user") ?: json
                val uid = userObj.optString("id")

                val sessionObj = json.optJSONObject("session")
                if (sessionObj != null && sessionObj.has("access_token")) {
                    BackendSession.accept(sessionObj)
                } else if (json.has("access_token")) {
                    BackendSession.accept(json)
                } else {
                    // Try immediate sign in to fetch session token
                    val loginRes = signIn(email, pass)
                    if (loginRes.isSuccess) {
                        return@withContext loginRes
                    }
                    BackendSession.clear()
                    return@withContext Result.failure(IOException("Check your email to confirm your account, then sign in."))
                }

                currentUserId = uid

                // Ensure profile row exists in profiles table
                upsertProfileRow(uid, name.trim(), email.trim().lowercase(), phone.trim(), ward)

                Result.success(
                    User(
                        id = uid,
                        email = email.trim().lowercase(),
                        displayName = name.trim(),
                        locality = ward,
                        phone = phone.trim(),
                        roles = listOf("approved_resident")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Signup exception", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
        BackendSession.clear()
        try {
            val url = "${SupabaseConfig.supabaseUrl}/auth/v1/token?grant_type=password"
            val bodyJson = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", pass)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.supabaseKey}")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(JSON_MEDIA))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errJson = try { JSONObject(respBody) } catch (_: Exception) { null }
                    val errMsg = errJson?.optString("error_description")
                        ?: errJson?.optString("msg")
                        ?: "Login failed (${response.code})"
                    return@withContext Result.failure(IOException(errMsg))
                }

                val json = JSONObject(respBody)
                BackendSession.accept(json)
                val userObj = json.optJSONObject("user")
                val uid = userObj?.optString("id") ?: ""
                currentUserId = uid
                val userEmail = userObj?.optString("email") ?: email
                val userMeta = userObj?.optJSONObject("user_metadata")

                // Fetch Profile from Supabase profiles table
                val profile = fetchProfile(uid)
                if(profile==null) {
                    BackendSession.clear()
                    throw IOException("Your account profile could not be loaded. Please retry signing in.")
                }

                if (profile?.optBoolean("is_banned", false) == true) {
                    BackendSession.clear()
                    throw IOException("This account is suspended. Please contact the administrator.")
                }

                val role = profile?.optString("role", "approved_resident")?.takeIf { it.isNotBlank() } ?: "approved_resident"
                val name = profile?.optString("name")?.takeIf { it.isNotBlank() }
                    ?: userMeta?.optString("name")?.takeIf { it.isNotBlank() }
                    ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                val ward = profile?.optString("ward")?.takeIf { it.isNotBlank() }
                    ?: userMeta?.optString("ward")?.takeIf { it.isNotBlank() } ?: "Hoode"
                val phone = profile?.optString("phone")?.takeIf { it.isNotBlank() }
                    ?: userMeta?.optString("phone")?.takeIf { it.isNotBlank() } ?: ""

                val age = profile?.optString("age")?.takeIf { it.isNotBlank() && it != "null" }
                val dob = profile?.optString("dob")?.takeIf { it.isNotBlank() && it != "null" }
                val fatherName = profile?.optString("father_name")?.takeIf { it.isNotBlank() && it != "null" }
                val bloodGroup = profile?.optString("blood_group")?.takeIf { it.isNotBlank() && it != "null" }
                val profession = profile?.optString("profession")?.takeIf { it.isNotBlank() && it != "null" }
                val avatarUrl = profile?.optString("avatar_url")?.takeIf { it.isNotBlank() && it != "null" }
                val bio = profile?.optString("bio")?.takeIf { it.isNotBlank() && it != "null" }
                val username = profile?.optString("username")?.takeIf { it.isNotBlank() && it != "null" }
                val coverUrl = profile?.optString("cover_url")?.takeIf { it.isNotBlank() && it != "null" }

                val user = User(
                    id = uid,
                    email = userEmail,
                    displayName = name,
                    locality = ward,
                    phone = phone,
                    age = age,
                    dob = dob,
                    fatherName = fatherName,
                    bloodGroup = bloodGroup,
                    profession = profession,
                    profilePicUri = avatarUrl,
                    bio = bio,
                    username = username,
                    coverPicUri = coverUrl,
                    roles = listOf(role)
                )

                Result.success(user)
            }
        } catch (e: Exception) {
            BackendSession.clear()
            Log.e(TAG, "SignIn exception", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(user: User): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            suspend fun upload(value: String?): String? = value?.let {
                if (it.startsWith("content://")) com.hoodeconnect.backend.MediaUploader.uploadUri(android.net.Uri.parse(it)).getOrThrow() else it
            }
            val avatar = upload(user.profilePicUri)
            val cover = upload(user.coverPicUri)
            val body = JSONObject().apply {
                put("name", user.displayName); put("phone", user.phone ?: ""); put("ward", user.locality ?: "Hoode")
                put("age", user.age ?: ""); put("dob", user.dob ?: ""); put("father_name", user.fatherName ?: "")
                put("blood_group", user.bloodGroup ?: ""); put("profession", user.profession ?: "")
                put("username", user.username ?: ""); put("bio", user.bio ?: "")
                put("avatar_url", avatar ?: JSONObject.NULL); put("cover_url", cover ?: JSONObject.NULL)
            }
            val rows = JSONArray(com.hoodeconnect.backend.CommunityApi.request("profiles?id=eq.${user.id}", "PATCH", body))
            check(rows.length() == 1) { "Profile could not be saved. Sign in again and retry." }
            user.copy(profilePicUri = avatar, coverPicUri = cover)
        }
    }

    fun upsertProfileRow(uid: String, name: String, email: String, phone: String, ward: String) {
        try {
            val body = JSONObject().apply {
                put("id", uid)
                put("name", name)
                put("email", email)
                put("phone", phone)
                put("ward", ward)
                put("role", "approved_resident")
            }
            val request = Request.Builder()
                .url("${SupabaseConfig.supabaseUrl}/rest/v1/profiles")
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Error upserting profile row", e)
        }
    }

    fun fetchProfile(uid: String): JSONObject? {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/profiles?id=eq.$uid&select=*"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string() ?: "[]")
                    if (arr.length() > 0) return arr.getJSONObject(0)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching profile", e)
        }
        return null
    }

    // ── News Moderation ────────────────────────────────────────────────────────

    suspend fun fetchNews(status: String = "published"): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/news?status=eq.$status&order=created_at.desc&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to fetch news: ${response.code}"))
                }
                val jsonArr = JSONArray(response.body?.string() ?: "[]")
                val list = mutableListOf<NewsArticle>()
                for (i in 0 until jsonArr.length()) {
                    val item = jsonArr.getJSONObject(i)
                    list.add(
                        NewsArticle(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            summary = item.optString("summary"),
                            body = item.optString("content"),
                            verifier = item.optString("source", "Hoode Resident"),
                            verifiedDate = item.optString("created_at").take(10),
                            sources = listOf(item.optString("category", "Community")),
                            status = item.optString("status", "verified"),
                            imageUrl = item.optString("image_url").ifBlank {
                                "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=800"
                            }
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch news failed", e)
            Result.failure(e)
        }
    }

    suspend fun submitNews(title: String, summary: String, content: String, category: String, imageUrl: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${SupabaseConfig.supabaseUrl}/rest/v1/news"
                val body = JSONObject().apply {
                    put("title", title)
                    put("summary", summary)
                    put("content", content)
                    put("category", category)
                    put("image_url", imageUrl)
                    put("status", "pending") // Goes to Admin for review!
                    if (!currentUserId.isNullOrBlank()) put("author_id", currentUserId)
                }

                val req = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.supabaseKey)
                    .addHeader("Authorization", getAuthHeader())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .post(body.toString().toRequestBody(JSON_MEDIA))
                    .build()

                httpClient.newCall(req).execute().use { response ->
                    if (response.isSuccessful) Result.success(true)
                    else Result.failure(IOException("Submit news failed: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateNewsStatus(newsId: String, newStatus: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/news?id=eq.$newsId"
            val body = JSONObject().apply { put("status", newStatus) }

            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Content-Type", "application/json")
                .patch(body.toString().toRequestBody(JSON_MEDIA))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(true)
                else Result.failure(IOException("Update status failed: ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Events ────────────────────────────────────────────────────────────────

    suspend fun fetchEvents(status: String = "published"): Result<List<CommunityEvent>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/events?status=eq.$status&order=created_at.desc&select=*"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(IOException("Fetch events failed"))
                val arr = JSONArray(resp.body?.string() ?: "[]")
                val list = mutableListOf<CommunityEvent>()
                for (i in 0 until arr.length()) {
                    val it = arr.getJSONObject(i)
                    list.add(
                        CommunityEvent(
                            id = it.optString("id"),
                            title = it.optString("title"),
                            organizer = it.optString("organizer", "Hoode Community"),
                            category = "Event",
                            date = it.optString("date_text"),
                            time = "",
                            venue = it.optString("location")
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitEvent(event: CommunityEvent): Result<Boolean> = com.hoodeconnect.backend.CommunityApi.safely {
        val body=JSONObject().put("title",event.title).put("description",event.description)
            .put("category",event.category).put("date_text","${event.date} ${event.time}".trim())
            .put("location",event.venue).put("organizer",event.organizer).put("status","pending")
        com.hoodeconnect.backend.CommunityApi.submitRow("events",body,event.id)
        true
    }

    suspend fun updateEventStatus(eventId: String, newStatus: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/events?id=eq.$eventId"
            val body = JSONObject().apply { put("status", newStatus) }
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Content-Type", "application/json")
                .patch(body.toString().toRequestBody(JSON_MEDIA))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(true)
                else Result.failure(IOException("Update event status failed: ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Marketplace ───────────────────────────────────────────────────────────

    suspend fun fetchMarketplace(status: String = "published"): Result<List<ClassifiedItem>> = com.hoodeconnect.backend.CommunityApi.safely {
        val raw = if(status=="published") com.hoodeconnect.backend.CommunityApi.request("rpc/hoode_marketplace_feed","POST",JSONObject().put("page_size",100))
            else com.hoodeconnect.backend.CommunityApi.request("marketplace?status=eq.$status&order=created_at.desc&limit=100")
        val rows=JSONArray(raw)
        List(rows.length()) { index ->
            val r=rows.getJSONObject(index)
            val photos=r.optJSONArray("images") ?: JSONArray()
            val images=List(photos.length()){photos.optString(it)}.filter{it.isNotBlank()}
            val cover=r.optString("image_url").takeUnless{it=="null"}.orEmpty()
            ClassifiedItem(id=r.getString("id"),title=r.optString("title"),price=r.optString("price"),
                type=r.optString("listing_type","Sell"),category=r.optString("category","General"),area=r.optString("area","Hoode"),
                description=r.optString("description"),sellerName=r.optString("seller_name","Resident"),date=r.optString("created_at").take(10),
                images=images.ifEmpty{listOf(cover).filter{it.isNotBlank()}},phone=r.optString("phone"),sellerUserId=r.optString("author_id"),
                isSold=r.optBoolean("is_sold"),isBooked=r.optBoolean("is_booked"),bookedByUserId=r.optString("booked_by_user_id").takeUnless{it=="null"},
                bookedByName=r.optString("booked_by_name").takeUnless{it=="null"},bookedByPhone=r.optString("booked_by_phone").takeUnless{it=="null"},
                bookingNote=r.optString("booking_note").takeUnless{it=="null"},bookedAtTimestamp=runCatching{java.time.Instant.parse(r.optString("booked_at")).toEpochMilli()}.getOrDefault(0L))
        }
    }
    suspend fun submitMarketplace(item:ClassifiedItem):Result<Boolean> = com.hoodeconnect.backend.CommunityApi.safely {
        check(!BackendSession.token().isNullOrBlank()) { "Please sign in to submit a listing." }
        val photos=item.images.map { if(it.startsWith("content://")) com.hoodeconnect.backend.MediaUploader.uploadUri(android.net.Uri.parse(it)).getOrThrow() else it }
        val body=JSONObject().put("title",item.title).put("description",item.description).put("price",item.price)
            .put("category",item.category).put("listing_type",item.type).put("image_url",photos.firstOrNull()?:"").put("images",JSONArray(photos))
            .put("seller_name",item.sellerName).put("area",item.area).put("phone",item.phone).put("status","pending")
        com.hoodeconnect.backend.CommunityApi.submitRow("marketplace",body,item.id)
        true
    }

    suspend fun updateMarketplaceStatus(itemId: String, newStatus: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/marketplace?id=eq.$itemId"
            val body = JSONObject().apply { put("status", newStatus) }
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Content-Type", "application/json")
                .patch(body.toString().toRequestBody(JSON_MEDIA))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(true)
                else Result.failure(IOException("Update market status failed: ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Blood Network ─────────────────────────────────────────────────────────

    suspend fun fetchBloodRequests(): Result<List<BloodRequest>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/blood_requests?moderation_status=eq.published&order=created_at.desc&select=*"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(IOException("Fetch blood failed"))
                val arr = JSONArray(resp.body?.string() ?: "[]")
                val list = mutableListOf<BloodRequest>()
                for (i in 0 until arr.length()) {
                    val it = arr.getJSONObject(i)
                    list.add(
                        BloodRequest(
                            id = it.optString("id"),
                            patientNamePlaceholder = it.optString("patient_name", "Patient in Need"),
                            bloodGroup = it.optString("blood_group", "O+"),
                            hospital = it.optString("hospital", "Udupi"),
                            neededBy = "Urgent",
                            urgency = it.optString("status", "urgent"),
                            unitsNeeded = it.optInt("units_needed", 1),
                            coordinatorPhone = it.optString("contact_phone", "9876543210")
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitBloodRequest(reqItem: BloodRequest): Result<Boolean> = com.hoodeconnect.backend.CommunityApi.safely {
        val body=JSONObject().put("patient_name",reqItem.patientNamePlaceholder).put("blood_group",reqItem.bloodGroup)
            .put("units_needed",reqItem.unitsNeeded).put("hospital",reqItem.hospital)
            .put("contact_phone",reqItem.coordinatorPhone).put("status",reqItem.urgency).put("moderation_status","pending")
        com.hoodeconnect.backend.CommunityApi.submitRow("blood_requests",body,reqItem.id)
        true
    }

    // ── Storage (Private Bucket Media) ─────────────────────────────────────────

    suspend fun uploadMedia(fileBytes: ByteArray, fileName: String, mimeType: String = "image/jpeg"): Result<String> =
        BackblazeClient.uploadImage(fileBytes, fileName, mimeType)

    suspend fun fetchCarouselAds(): Result<List<com.example.hoode_app.data.model.AdCarouselSlide>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/carousel_ads?is_active=eq.true&order=slot_index.asc&select=*"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(IOException("Fetch carousel failed: ${resp.code}"))
                val arr = JSONArray(resp.body?.string() ?: "[]")
                val list = mutableListOf<com.example.hoode_app.data.model.AdCarouselSlide>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        com.example.hoode_app.data.model.AdCarouselSlide(
                            slotIndex = obj.optInt("slot_index", i + 1) + 1,
                            headline = obj.optString("headline"),
                            subheadline = obj.optString("subheadline"),
                            description = obj.optString("description"),
                            advertiser = obj.optString("advertiser"),
                            ctaLabel = obj.optString("action_label", "Explore Now"),
                            ctaUrl = obj.optString("action_url"),
                            isEnabled = obj.optBoolean("is_active", true),
                            imageUrl = obj.optString("image_url")
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPrayerSchedules(mosqueName: String = "Hoode Juma Masjid"): Result<List<Pair<String, Pair<String, String>>>> = withContext(Dispatchers.IO) {
        try {
            val encodedMosque = java.net.URLEncoder.encode(mosqueName, "UTF-8")
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/prayer_schedules?mosque_name=eq.$encodedMosque&select=*"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(IOException("Fetch prayers failed: ${resp.code}"))
                val arr = JSONArray(resp.body?.string() ?: "[]")
                val list = mutableListOf<Pair<String, Pair<String, String>>>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("prayer_name")
                    val adhan = obj.optString("adhan_time")
                    val iqamah = obj.optString("iqamah_time")
                    list.add(Pair(name, Pair(adhan, iqamah)))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

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
                var profile = fetchProfile(uid)
                if (profile == null) {
                    // Profile row doesn't exist yet, insert/upsert default profile
                    val defaultName = userMeta?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val defaultWard = userMeta?.optString("ward")?.takeIf { it.isNotBlank() } ?: "Hoode"
                    val defaultPhone = userMeta?.optString("phone")?.takeIf { it.isNotBlank() } ?: ""
                    upsertProfileRow(uid, defaultName, userEmail, defaultPhone, defaultWard)
                    profile = fetchProfile(uid)
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
                val bio = userMeta?.optString("bio")?.takeIf { it.isNotBlank() && it != "null" }
                val username = userMeta?.optString("username")?.takeIf { it.isNotBlank() && it != "null" }
                val coverUrl = userMeta?.optString("cover_url")?.takeIf { it.isNotBlank() && it != "null" }

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
            val avatar = user.profilePicUri?.let {
                if (it.startsWith("content://")) com.hoodeconnect.backend.MediaUploader.uploadUri(android.net.Uri.parse(it)).getOrThrow()
                else it
            }

            // 1. Update profiles table in Supabase
            val body = JSONObject().apply {
                put("name", user.displayName)
                put("phone", user.phone ?: "")
                put("ward", user.locality ?: "Hoode")
                put("age", user.age ?: "")
                put("dob", user.dob ?: "")
                put("father_name", user.fatherName ?: "")
                put("blood_group", user.bloodGroup ?: "")
                put("profession", user.profession ?: "")
                put("avatar_url", avatar ?: JSONObject.NULL)
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.supabaseUrl}/rest/v1/profiles?id=eq.${user.id}")
                .header("Prefer", "return=representation")
                .patch(body.toString().toRequestBody(JSON_MEDIA))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // If PATCH failed (e.g. record didn't exist yet), perform upsert
                    body.put("id", user.id)
                    body.put("email", user.email)
                    body.put("role", user.roles.firstOrNull() ?: "approved_resident")
                    val upsertReq = Request.Builder()
                        .url("${SupabaseConfig.supabaseUrl}/rest/v1/profiles")
                        .header("Prefer", "resolution=merge-duplicates,return=representation")
                        .post(body.toString().toRequestBody(JSON_MEDIA))
                        .build()
                    httpClient.newCall(upsertReq).execute().close()
                }
            }

            // 2. Also persist bio, username, and coverPicUri into Supabase Auth user metadata
            try {
                val metaBody = JSONObject().apply {
                    put("data", JSONObject().apply {
                        put("name", user.displayName)
                        put("phone", user.phone ?: "")
                        put("ward", user.locality ?: "Hoode")
                        if (!user.bio.isNullOrBlank()) put("bio", user.bio)
                        if (!user.username.isNullOrBlank()) put("username", user.username)
                        if (!user.coverPicUri.isNullOrBlank()) put("cover_url", user.coverPicUri)
                    })
                }
                val metaReq = Request.Builder()
                    .url("${SupabaseConfig.supabaseUrl}/auth/v1/user")
                    .addHeader("apikey", SupabaseConfig.supabaseKey)
                    .addHeader("Authorization", getAuthHeader())
                    .put(metaBody.toString().toRequestBody(JSON_MEDIA))
                    .build()
                httpClient.newCall(metaReq).execute().close()
            } catch (e: Exception) {
                Log.w(TAG, "Error updating auth user metadata", e)
            }

            user.copy(profilePicUri = avatar)
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

    suspend fun submitEvent(event: CommunityEvent): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/events"
            val body = JSONObject().apply {
                put("title", event.title)
                put("description", "${event.category} - ${event.time}")
                put("category", event.category)
                put("date_text", "${event.date} ${event.time}".trim())
                put("location", event.venue)
                put("organizer", event.organizer)
                put("status", "pending")
                if (!currentUserId.isNullOrBlank()) put("author_id", currentUserId)
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(true) else Result.failure(IOException("Submit event error: ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    suspend fun fetchMarketplace(status: String = "published"): Result<List<ClassifiedItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/marketplace?status=eq.$status&order=created_at.desc&select=*"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(IOException("Fetch market failed"))
                val arr = JSONArray(resp.body?.string() ?: "[]")
                val list = mutableListOf<ClassifiedItem>()
                for (i in 0 until arr.length()) {
                    val it = arr.getJSONObject(i)
                    val img = it.optString("image_url")
                    list.add(
                        ClassifiedItem(
                            id = it.optString("id"),
                            title = it.optString("title"),
                            price = it.optString("price"),
                            type = "Sell",
                            category = it.optString("category", "General"),
                            area = "Hoode",
                            description = it.optString("description"),
                            sellerName = it.optString("seller_name", "Resident"),
                            date = it.optString("created_at").take(10),
                            images = if (img.isNotBlank()) listOf(img) else emptyList(),
                            phone = it.optString("phone", "9876543210")
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitMarketplace(item: ClassifiedItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/marketplace"
            val body = JSONObject().apply {
                put("title", item.title)
                put("description", item.description)
                put("price", item.price)
                put("category", item.category)
                put("image_url", item.images.firstOrNull() ?: "")
                put("seller_name", item.sellerName)
                put("area", item.area)
                put("phone", item.phone)
                put("status", "pending")
                if (!currentUserId.isNullOrBlank()) put("author_id", currentUserId)
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(true) else Result.failure(IOException("Submit market error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/blood_requests?order=created_at.desc&select=*"
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

    suspend fun submitBloodRequest(reqItem: BloodRequest): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.supabaseUrl}/rest/v1/blood_requests"
            val body = JSONObject().apply {
                put("patient_name", reqItem.patientNamePlaceholder)
                put("blood_group", reqItem.bloodGroup)
                put("units_needed", reqItem.unitsNeeded)
                put("hospital", reqItem.hospital)
                put("contact_phone", reqItem.coordinatorPhone)
                put("status", reqItem.urgency)
                if (!currentUserId.isNullOrBlank()) put("author_id", currentUserId)
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(true) else Result.failure(IOException("Blood request failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
                            subheadline = obj.optString("advertiser"),
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

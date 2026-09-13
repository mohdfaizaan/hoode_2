package com.example.hoode_app.data.model

import java.util.UUID

// ── Auth & Identity ──────────────────────────────────────────

data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val displayName: String,
    val locale: String = "en",
    val communityId: String = "hoode",
    val roles: List<String> = listOf("approved_resident"),
    
    // Extended Profile
    val profilePicUri: String? = null,
    val phone: String? = null,
    val age: String? = null,
    val dob: String? = null,
    val fatherName: String? = null,
    val bloodGroup: String? = null,
    val profession: String? = null,
    val locality: String? = null
)

data class Community(
    val id: String = "hoode",
    val name: String = "Hoode",
    val district: String = "Udupi",
    val state: String = "Karnataka",
    val country: String = "India",
    val timezone: String = "Asia/Kolkata",
    val defaultLocale: String = "en"
)

// ── F01 & F12: Prayer & Ramadan ──────────────────────────────

data class Mosque(
    val id: String,
    val name: String,
    val location: String,
    val isPrimary: Boolean = false
)

data class PrayerTiming(
    val name: String,
    val adhanTime: String,
    val iqamahTime: String,
    val isNext: Boolean = false,
    val timeRemaining: String = ""
)

data class RamadanTimetable(
    val day: Int,
    val date: String,
    val suhoorEnd: String,
    val iftarTime: String
)

// ── F02: Emergency & Utilities ────────────────────────────────

data class EmergencyContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String, // Ambulance, Police, Hospital, Fire, Pharmacy, Electricity, Water
    val phone: String,
    val area: String,
    val isVerified: Boolean = true,
    val lastVerified: String = "10 Sep 2026"
)

// ── F03: Jobs & Gigs ─────────────────────────────────────────

data class JobPosting(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val employer: String,
    val type: String, // Full-time, Part-time, Gig, Volunteer
    val pay: String,
    val location: String,
    val description: String,
    val deadline: String,
    val isSponsored: Boolean = false,
    val applicantsCount: Int = 0
)

data class JobApplication(
    val id: String = UUID.randomUUID().toString(),
    val jobId: String,
    val applicantName: String,
    val phone: String,
    val message: String,
    val status: String = "submitted"
)

// ── F04: Lost & Found ─────────────────────────────────────────

data class LostFoundItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isLost: Boolean, // true = Lost, false = Found
    val category: String, // Electronics, Documents, Keys, Pets, Bags
    val area: String,
    val date: String,
    val description: String,
    val status: String = "open" // open, claim_pending, resolved
)

// ── F05: Tournaments & Matches ───────────────────────────────

data class Tournament(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sport: String, // Cricket, Football, Badminton, Volleyball
    val venue: String,
    val dates: String,
    val format: String = "Round Robin",
    val teamsCount: Int = 8,
    val status: String = "active"
)

data class Fixture(
    val id: String = UUID.randomUUID().toString(),
    val tournamentId: String,
    val round: String,
    val teamA: String,
    val teamB: String,
    val time: String,
    val venue: String,
    val scoreA: String? = null,
    val scoreB: String? = null,
    val status: String = "scheduled" // scheduled, completed, live
)

data class Standing(
    val teamName: String,
    val played: Int,
    val won: Int,
    val lost: Int,
    val points: Int,
    val netRunRate: String = "+0.00"
)

// ── F06: Events & Weddings ────────────────────────────────────

data class CommunityEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val organizer: String,
    val category: String, // Majlis, Wedding, Community Meeting, Workshop, Celebration
    val date: String,
    val time: String,
    val venue: String,
    val isPrivate: Boolean = false,
    val rsvpGoing: Int = 0,
    val rsvpTotalCapacity: Int = 100,
    val userRsvp: Boolean? = null
)

// ── F07: Blood Donor Network ─────────────────────────────────

data class BloodRequest(
    val id: String = UUID.randomUUID().toString(),
    val patientNamePlaceholder: String = "Patient in Need",
    val bloodGroup: String, // A+, A-, B+, B-, AB+, AB-, O+, O-
    val hospital: String,
    val neededBy: String,
    val urgency: String = "urgent", // urgent, normal
    val area: String = "Hoode / Udupi",
    val unitsNeeded: Int = 2,
    val coordinatorPhone: String = "+91 98765 43210",
    val isFulfilled: Boolean = false
)

data class DonorRegistration(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bloodGroup: String,
    val area: String,
    val phone: String,
    val isAvailable: Boolean = true
)

// ── F08: Polls & Civic Issues ────────────────────────────────

data class PollOption(
    val id: String,
    val label: String,
    var votes: Int = 0
)

data class CommunityPoll(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val description: String,
    val options: List<PollOption>,
    val closesAt: String,
    var userVotedOptionId: String? = null,
    val totalVotes: Int = 0
)

data class CivicIssue(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String, // Roads, Streetlights, Waste, Water, Drainage
    val location: String,
    val status: String, // submitted, acknowledged, in_progress, resolved
    val endorsements: Int = 0,
    val userEndorsed: Boolean = false,
    val reportedDate: String = "08 Sep 2026"
)

// ── F09: Badges & Contributions ──────────────────────────────

data class CommunityBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconResName: String,
    val isUnlocked: Boolean = false,
    val earnedDate: String? = null
)

data class ContributionRecord(
    val id: String = UUID.randomUUID().toString(),
    val action: String,
    val points: Int,
    val date: String
)

// ── F10: Classifieds & Marketplace ───────────────────────────

data class ClassifiedItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val price: String, // "₹4,500" or "Free"
    val type: String, // Sell, Give Away, Wanted, Garage Sale
    val category: String, // Electronics, Furniture, Vehicles, Home, Books
    val area: String,
    val description: String,
    val sellerName: String,
    val date: String,
    val isSold: Boolean = false
)

// ── F13: Notifications ───────────────────────────────────────

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val timestamp: String,
    val category: String, // Prayer, Event, Emergency, Job, Announcement
    var isRead: Boolean = false
)

// ── F14: Verified Local News ─────────────────────────────────

data class NewsArticle(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val summary: String,
    val body: String,
    val verifier: String,
    val verifiedDate: String,
    val sources: List<String>,
    val isRumorClarification: Boolean = false,
    val status: String = "verified" // verified, rumor_clarified, under_review
)

// ── F15: Handyman & Service Providers ────────────────────────

data class ServiceProvider(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String, // Electrician, Plumber, Carpenter, Appliance Repair, Tutor, Painter
    val rating: Double,
    val reviewCount: Int,
    val phone: String,
    val area: String,
    val availability: String = "Mon–Sat, 8AM–7PM",
    val isVerified: Boolean = true
)

// ── F16: Daily Personality ───────────────────────────────────

data class PersonalityProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: String,
    val featuredDate: String,
    val intro: String,
    val fullBiography: String,
    val contributions: List<String>,
    val quote: String
)

// ── F17: Hoode Photo Gallery ─────────────────────────────────

data class GalleryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val caption: String,
    val photographer: String,
    val sortOrder: Int,
    val colorHex: String = "#62E8CF"
)

// ── F18: Activities ──────────────────────────────────────────

data class CommunityActivity(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String, // Religious, Community, Sports, Celebrations
    val organizer: String,
    val schedule: String,
    val venue: String,
    val description: String,
    val status: String = "upcoming"
)

// ── F19: Educational ─────────────────────────────────────────

data class EducationOffering(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val provider: String,
    val category: String, // Islamic Studies, Tuition, Computer Skills, Languages, Career Guidance
    val audience: String, // All Ages, School Students, College, Adults
    val mode: String = "In-person",
    val feeDescription: String = "Free / Subsidized",
    val schedule: String,
    val contact: String,
    val description: String
)

// ── F20: Our Huffaz ──────────────────────────────────────────

data class HuffazProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val completionYear: String,
    val institution: String,
    val teacher: String,
    val biography: String
)

// ── Carousel Slide (F00) ─────────────────────────────────────

data class AdCarouselSlide(
    val slotIndex: Int,
    val headline: String,
    val subheadline: String,
    val advertiser: String,
    val ctaLabel: String? = "Learn More",
    val ctaUrl: String? = null,
    var isEnabled: Boolean = true
)

// ── Calendar (Explore) ───────────────────────────────────
data class CalendarEvent(
    val dateStr: String, // Format: YYYY-MM-DD
    val title: String,
    val type: String // "holiday" or "event"
)

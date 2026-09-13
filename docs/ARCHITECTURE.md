# Hoode Connect — System Architecture & Technical Documentation

## 1. Overview
**Hoode Connect** is a native Android application designed to provide a premier digital community hub for the residents, organizations, and mosques of Hoode. The application is built with a focus on visual elegance, reliability, privacy, and offline accessibility.

---

## 2. Technical Stack
- **Language & Framework:** 100% Native Kotlin 2.2.10, Android XML Layouts, Material Design 3 (M3).
- **Architecture:** Clean Architecture / MVVM pattern utilizing ViewBinding, Kotlin Coroutines, and reactive `StateFlow` streams.
- **Navigation:** Jetpack Navigation Component with single-activity architecture (`MainActivity`) hosting 24 destinations.
- **Backend:** Supabase (PostgreSQL 15+, Row Level Security, Storage, Realtime, Auth).
- **Build System:** Gradle 9.4.1 with Android Gradle Plugin (AGP) 9.2.1.

---

## 3. Visual Design System
The visual style embodies a modern billion-dollar aesthetic:
- **Background:** `#F6F7F9` (Light clean neutral background).
- **Surfaces & Cards:** `#FFFFFF` pure white cards with `#171717` 1dp solid border stroke and 24dp corner radii (`radius_card`).
- **Typography:** Clear hierarchy with sans-serif medium/bold titles and muted subtext (`#737984`).
- **Accent Colors:** Mint/Aqua palette (`accent` `#62E8CF`, `accent_soft` `#C9FFF4`, `accent_ink` `#0F3D35`).
- **Icons:** Outlined vector assets (`24dp` bounding box).
- **Ad Carousel:** 5-slide 3-second auto-looping carousel with pause/resume lifecycle handling and manual swipe support.

---

## 4. Module Map & Directory Structure

```
app/src/main/
├── java/com/example/hoode_app/
│   ├── MainActivity.kt                       # Single host activity & bottom navigation
│   ├── data/
│   │   ├── model/
│   │   │   └── HoodeModels.kt                # Data classes for all 20 modules + Auth + Admin
│   │   ├── remote/
│   │   │   └── SupabaseClient.kt             # Supabase endpoint configuration
│   │   └── repository/
│   │       └── HoodeRepository.kt            # Reactive singleton data layer & mutations
│   └── ui/
│       ├── auth/
│       │   ├── WelcomeFragment.kt            # Onboarding & get started
│       │   ├── SignInFragment.kt             # Sign in with email & password
│       │   └── SignUpFragment.kt             # Sign up with residency declaration
│       ├── home/
│       │   └── HomeFragment.kt               # Main feed with prayer hero, ad carousel, quick actions
│       ├── explore/
│       │   └── ExploreFragment.kt            # Category directory linking to all features
│       ├── create/
│       │   └── CreateFragment.kt             # Action hub for creating posts, events, listings, jobs
│       ├── inbox/
│       │   └── InboxFragment.kt              # Inquiries and conversations
│       ├── profile/
│       │   └── ProfileFragment.kt            # Profile details, language switcher, admin console entry
│       ├── prayer/
│       │   └── PrayerDetailFragment.kt       # F01: Mosque timetables & live countdown ring
│       ├── emergency/
│       │   └── EmergencyFragment.kt          # F02: Emergency dialer & utility directory
│       ├── jobs/
│       │   └── JobsFragment.kt               # F03: Local job & gig board
│       ├── lostfound/
│       │   └── LostFoundFragment.kt          # F04: Lost & Found claims and reports
│       ├── tournaments/
│       │   └── TournamentsFragment.kt        # F05: Tournament fixtures & points tables
│       ├── events/
│       │   └── EventsFragment.kt             # F06: Community events & wedding invitations
│       ├── blood/
│       │   └── BloodNetworkFragment.kt       # F07: Blood requests & donor opt-in registry
│       ├── polls/
│       │   └── PollsFragment.kt              # F08: Local polls & civic issue endorsements
│       ├── badges/
│       │   └── BadgesFragment.kt             # F09: Badges showcase & contribution ledger
│       ├── marketplace/
│       │   └── MarketplaceFragment.kt        # F10: Classifieds & private buyer inquiries
│       ├── ramadan/
│       │   └── RamadanFragment.kt            # F12: Ramadan fasting countdown & 30-day schedule
│       ├── notifications/
│       │   └── NotificationCenterFragment.kt # F13: Notification history & categorization
│       ├── news/
│       │   └── NewsFragment.kt               # F14: Verified local news & rumor clarification
│       ├── providers/
│       │   └── ProvidersFragment.kt          # F15: Local service providers & handymen
│       ├── personality/
│       │   └── PersonalityDetailFragment.kt  # F16: Daily community personality spotlight
│       ├── gallery/
│       │   └── GalleryFragment.kt            # F17: 25-image community gallery & viewer
│       ├── activities/
│       │   └── ActivitiesFragment.kt         # F18: Religious, sports, and celebrations
│       ├── education/
│       │   └── EducationFragment.kt          # F19: Madrasas, tutoring, and workshops
│       ├── huffaz/
│       │   └── HuffazFragment.kt             # F20: Quran memorizers honor roll
│       └── admin/
│           ├── AdminDashboardFragment.kt     # In-app role-gated admin console
│           ├── AdminPrayerFragment.kt        # Live adhan & iqamah timetable editor
│           └── AdminCarouselFragment.kt      # 5-slide advertisement carousel editor
├── res/
│   ├── layout/                               # 56 native XML layouts (screens & cards)
│   ├── drawable/                             # 54 vector drawables & background shapes
│   ├── navigation/
│   │   └── nav_graph.xml                     # Complete navigation graph (24 destinations)
│   ├── values/
│   │   ├── colors.xml                        # Design tokens
│   │   ├── dimens.xml                        # Standard spacing and radiuses
│   │   ├── strings.xml                       # English strings
│   │   └── styles.xml                        # Typography and widget themes
│   ├── values-kn/
│   │   └── strings.xml                       # Kannada localization
│   └── values-ur/
│       └── strings.xml                       # Urdu localization (RTL support)
```

---

## 5. Security & Data Integrity
1. **Row Level Security (RLS):**
   - Public read permissions for verified community directories, active prayer schedules, and published items.
   - User-restricted permissions for `profile_private`, `job_applications`, `poll_votes`, `bookmarks`, and `notifications`.
   - Admin-gated write permissions checked via PostgreSQL function `is_community_admin(community_id)`.
2. **Business Rule Enforcement:**
   - **Gallery 25-Cap:** Trigger `enforce_gallery_cap_trigger` rejects insertion or activation of images exceeding 25 items.
   - **Carousel 1–5 Slots:** Database constraint `slot_index BETWEEN 1 AND 5` and `UNIQUE(community_id, slot_index)`.
   - **Voting Integrity:** `UNIQUE(poll_id, user_id)` ensures one vote per resident per poll.

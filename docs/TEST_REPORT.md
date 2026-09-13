# Hoode Connect — Test & Verification Report

**Version:** 1.0.0 (Build 1)  
**Date:** 10 September 2026  
**Environment:** Android Studio JBR / Gradle 9.4.1 / AGP 9.2.1 (Windows x64)  
**Target Device / Emulators:** API 34+ Android Runtime  
**Test Accounts:**
- `faizan@hoode.community` (Role: `super_admin`)
- `resident@hoode.community` (Role: `resident`)

---

## Summary of Results

| Test Category | Passed | Failed | Blocked / Not Run | Notes |
|---|---|---|---|---|
| **Android Build & Compilation** | 1 | 0 | 0 | Gradle assembleDebug compiles cleanly with 0 errors |
| **Authentication & Profile Flow** | 4 | 0 | 0 | Welcome, SignIn, SignUp, Profile role rendering verified |
| **Navigation & Bottom Tabs** | 5 | 0 | 0 | Home, Explore, Create, Inbox, Profile tabs & 24 destinations |
| **Advertisement Carousel (5 Slides)** | 3 | 0 | 0 | 3-second auto-timer, manual swiping, admin editor |
| **Prayer Timings & Countdowns (F01)** | 3 | 0 | 0 | Next prayer calculation, countdown ring, mosque switcher |
| **Emergency & Utilities (F02)** | 2 | 0 | 0 | Category filtering, ACTION_DIAL intent generation |
| **Jobs Board & Applications (F03)** | 3 | 0 | 0 | Filter chips, application submission dialog, posting |
| **Lost & Found (F04)** | 2 | 0 | 0 | Lost/Found toggle, private ownership claim flow |
| **Tournaments (F05)** | 2 | 0 | 0 | HPL fixture list, points table rendering |
| **Events & Weddings (F06)** | 2 | 0 | 0 | RSVP state changes, public/private filters |
| **Blood Donor Network (F07)** | 3 | 0 | 0 | Urgent ABO requests, coordinator dialer, donor registration |
| **Polls & Civic Issues (F08)** | 3 | 0 | 0 | Single-choice radio voting, live percentages, civic issue endorsement |
| **Badges & Ledger (F09)** | 2 | 0 | 0 | Points counter, append-only history display |
| **Classifieds Marketplace (F10)** | 2 | 0 | 0 | Price formatting, buyer inquiry dialog |
| **Localization (F11)** | 3 | 0 | 0 | English, Kannada, Urdu string resource resolution |
| **Ramadan Mode (F12)** | 2 | 0 | 0 | Suhoor/Iftar countdown, 30-day schedule timetable |
| **Notification Center (F13)** | 2 | 0 | 0 | Segmented badges, mark all as read action |
| **Verified News & Rumors (F14)** | 2 | 0 | 0 | Rumor debunking view, expand full story modal |
| **Service Providers (F15)** | 2 | 0 | 0 | Provider cards, rating indicators, phone dialer |
| **Daily Personality (F16)** | 2 | 0 | 0 | Biography rendering, achievements list, quote card |
| **Photo Gallery 25-Cap (F17)** | 3 | 0 | 0 | 25-image grid, fullscreen viewer, strict 25 cap rejection |
| **Activities (F18)** | 2 | 0 | 0 | 4 category tab filters, card details |
| **Educational Offerings (F19)** | 2 | 0 | 0 | Madrasa/tutoring listings, fee details |
| **Our Huffaz Directory (F20)** | 2 | 0 | 0 | Hafidh honor roll, completion year, teacher |
| **In-App Admin Console** | 3 | 0 | 0 | Role-gating, Prayer timetable edit, Carousel slide edit |
| **Supabase Cloud Push Dispatch** | 0 | 0 | 1 | Blocked by external FCM credentials (`google-services.json`) |
| **Live SMS OTP Gateway** | 0 | 0 | 1 | Optional feature tier (Section 1.1 editable assumptions) |

---

## Detailed Test Logs

### 1. Build Verification
- **Command:** `.\gradlew.bat assembleDebug`
- **Result:** BUILD SUCCESSFUL
- **Artifact:** `app/build/outputs/apk/debug/app-debug.apk`

### 2. Gallery 25-Active-Image Cap Verification
- **Test Case:** Attempting to add a 26th image when 25 active images exist.
- **Repository Validation:** `HoodeRepository.addGalleryItem(...)` evaluates `_galleryItems.value.size >= 25` and returns `false`.
- **Database Trigger:** PostgreSQL trigger `enforce_gallery_cap_trigger` throws exception: `"Gallery active item cap of 25 reached"`.
- **UI Feedback:** Toast displays `"Cannot add image: Maximum 25 active images cap reached."`
- **Result:** PASSED

### 3. Five-Slide 3-Second Carousel Verification
- **Auto-Advance:** ViewPager2 advances every 3,000 milliseconds using Kotlin Coroutine lifecycle-aware looping.
- **Loop Boundary:** Slide 5 seamlessly transitions back to Slide 1.
- **Manual Swiping:** User touch gesture cancels immediate timer tick and resets the 3-second dwell.
- **Result:** PASSED

### 4. Role-Gated Admin Console Verification
- **Admin Access:** Account with `UserRole.ADMIN` displays the prominent Admin Console card in `ProfileFragment`.
- **Prayer Timings Live Edit:** Submitting modified Adhan and Iqamah times immediately updates the StateFlow and reflects on the Home prayer hero and Prayer detail countdown.
- **Result:** PASSED

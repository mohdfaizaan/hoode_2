-- ============================================================================
-- Hoode Connect — Complete PostgreSQL Schema & Migrations
-- Version: 2.0 (10 September 2026)
-- Target: Supabase / PostgreSQL 15+
-- ============================================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. Communities & Settings
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS communities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    timezone TEXT NOT NULL DEFAULT 'Asia/Kolkata',
    country_code VARCHAR(2) NOT NULL DEFAULT 'IN',
    currency_code VARCHAR(3) NOT NULL DEFAULT 'INR',
    default_locale VARCHAR(5) NOT NULL DEFAULT 'en',
    supported_locales TEXT[] NOT NULL DEFAULT ARRAY['en', 'kn', 'ur'],
    boundary_geojson JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS community_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    setting_key TEXT NOT NULL,
    setting_value JSONB NOT NULL,
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(community_id, setting_key)
);

-- ----------------------------------------------------------------------------
-- 2. Profiles, Auth & Roles
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY, -- References auth.users(id)
    community_id UUID REFERENCES communities(id) ON DELETE SET NULL,
    display_name TEXT NOT NULL,
    avatar_url TEXT,
    bio TEXT,
    preferred_locale VARCHAR(5) NOT NULL DEFAULT 'en',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS profile_private (
    user_id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
    phone TEXT,
    email TEXT,
    is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    contact_preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    status TEXT NOT NULL CHECK (status IN ('pending', 'approved', 'suspended', 'left')),
    approved_by UUID REFERENCES profiles(id),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, community_id)
);

CREATE TABLE IF NOT EXISTS role_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('super_admin', 'community_admin', 'mosque_admin', 'editor', 'moderator')),
    scope_entity_type TEXT,
    scope_entity_id UUID,
    granted_by UUID REFERENCES profiles(id),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, community_id, role, scope_entity_type, scope_entity_id)
);

CREATE TABLE IF NOT EXISTS user_blocks (
    blocker_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY(blocker_id, blocked_id)
);

-- ----------------------------------------------------------------------------
-- 3. Content Items & Publishing Framework
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS content_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    kind TEXT NOT NULL CHECK (kind IN (
        'job', 'lost_found', 'event', 'tournament', 'poll',
        'civic_issue', 'classified', 'news', 'blood_request',
        'activity', 'education', 'huffaz'
    )),
    title TEXT NOT NULL,
    body TEXT,
    original_locale VARCHAR(5) NOT NULL DEFAULT 'en',
    visibility TEXT NOT NULL DEFAULT 'public' CHECK (visibility IN ('public', 'members_only', 'invited_only')),
    publication_status TEXT NOT NULL DEFAULT 'published' CHECK (publication_status IN ('draft', 'submitted', 'in_review', 'approved', 'published', 'rejected', 'archived')),
    version INT NOT NULL DEFAULT 1,
    published_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS content_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_item_id UUID NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    storage_path TEXT NOT NULL,
    media_type TEXT NOT NULL DEFAULT 'image',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    content_item_id UUID NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, content_item_id)
);

-- ----------------------------------------------------------------------------
-- 4. Conversations & Direct Messaging (Inquiries)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    context_kind TEXT NOT NULL CHECK (context_kind IN ('classified', 'job', 'lost_found', 'service_provider', 'admin_support')),
    context_id UUID NOT NULL,
    created_by UUID NOT NULL REFERENCES profiles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS conversation_members (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    last_read_at TIMESTAMPTZ,
    PRIMARY KEY(conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES profiles(id),
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 5. F01: Mosques & Prayer Timings
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mosques (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    location_description TEXT,
    timezone TEXT NOT NULL DEFAULT 'Asia/Kolkata',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS prayer_schedule_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mosque_id UUID NOT NULL REFERENCES mosques(id) ON DELETE CASCADE,
    effective_date DATE NOT NULL,
    fajr_adhan TIME NOT NULL,
    fajr_iqamah TIME NOT NULL,
    sunrise TIME NOT NULL,
    dhuhr_adhan TIME NOT NULL,
    dhuhr_iqamah TIME NOT NULL,
    asr_adhan TIME NOT NULL,
    asr_iqamah TIME NOT NULL,
    maghrib_adhan TIME NOT NULL,
    maghrib_iqamah TIME NOT NULL,
    isha_adhan TIME NOT NULL,
    isha_iqamah TIME NOT NULL,
    jummah_adhan TIME,
    jummah_iqamah TIME,
    notes TEXT,
    published_by UUID REFERENCES profiles(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(mosque_id, effective_date)
);

CREATE TABLE IF NOT EXISTS mosque_follows (
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    mosque_id UUID NOT NULL REFERENCES mosques(id) ON DELETE CASCADE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY(user_id, mosque_id)
);

CREATE TABLE IF NOT EXISTS reminder_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    mosque_id UUID REFERENCES mosques(id) ON DELETE CASCADE,
    prayer_name TEXT NOT NULL CHECK (prayer_name IN ('fajr', 'dhuhr', 'asr', 'maghrib', 'isha', 'jummah')),
    lead_minutes INT NOT NULL DEFAULT 15,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sound_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(user_id, mosque_id, prayer_name)
);

-- ----------------------------------------------------------------------------
-- 6. F12: Ramadan Calendar & Timetable
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ramadan_daily_times (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    ramadan_day INT NOT NULL CHECK (ramadan_day BETWEEN 1 AND 30),
    calendar_date DATE NOT NULL,
    suhoor_end_time TIME NOT NULL,
    iftar_time TIME NOT NULL,
    duas_text TEXT,
    notes TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(community_id, ramadan_day)
);

-- ----------------------------------------------------------------------------
-- 7. F02: Emergency & Utility Directory
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS directory_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    key_slug TEXT NOT NULL,
    name TEXT NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    UNIQUE(community_id, key_slug)
);

CREATE TABLE IF NOT EXISTS directory_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES directory_categories(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    phone_number TEXT NOT NULL,
    alternate_phone TEXT,
    area_address TEXT,
    operating_hours TEXT,
    is_emergency BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS directory_corrections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id UUID NOT NULL REFERENCES directory_entries(id) ON DELETE CASCADE,
    submitter_id UUID NOT NULL REFERENCES profiles(id),
    proposed_phone TEXT,
    notes TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 8. F03: Local Jobs & Gig Board
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS job_details (
    content_item_id UUID PRIMARY KEY REFERENCES content_items(id) ON DELETE CASCADE,
    employer_name TEXT NOT NULL,
    job_type TEXT NOT NULL CHECK (job_type IN ('full_time', 'part_time', 'gig', 'volunteer')),
    skills TEXT[] DEFAULT '{}',
    pay_range TEXT,
    work_location TEXT NOT NULL,
    is_open BOOLEAN NOT NULL DEFAULT TRUE,
    closes_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS job_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES job_details(content_item_id) ON DELETE CASCADE,
    applicant_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    cover_note TEXT,
    phone_number TEXT,
    resume_storage_key TEXT,
    status TEXT NOT NULL DEFAULT 'submitted' CHECK (status IN ('submitted', 'shortlisted', 'rejected', 'hired')),
    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(job_id, applicant_id)
);

-- ----------------------------------------------------------------------------
-- 9. F04: Lost and Found Hub
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lost_found_details (
    content_item_id UUID PRIMARY KEY REFERENCES content_items(id) ON DELETE CASCADE,
    item_type TEXT NOT NULL CHECK (item_type IN ('lost', 'found')),
    category TEXT NOT NULL,
    location_found TEXT NOT NULL,
    incident_date DATE NOT NULL,
    is_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    ownership_question TEXT
);

CREATE TABLE IF NOT EXISTS ownership_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lost_found_id UUID NOT NULL REFERENCES lost_found_details(content_item_id) ON DELETE CASCADE,
    claimant_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    proof_description TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 10. F05: Tournaments & Matches
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tournament_details (
    content_item_id UUID PRIMARY KEY REFERENCES content_items(id) ON DELETE CASCADE,
    sport TEXT NOT NULL DEFAULT 'Cricket',
    tournament_format TEXT NOT NULL DEFAULT 'Knockout',
    registration_deadline TIMESTAMPTZ,
    max_teams INT NOT NULL DEFAULT 16,
    venue TEXT NOT NULL,
    is_registration_open BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES tournament_details(content_item_id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    captain_id UUID NOT NULL REFERENCES profiles(id),
    captain_phone TEXT NOT NULL,
    roster_count INT NOT NULL DEFAULT 11,
    status TEXT NOT NULL DEFAULT 'registered' CHECK (status IN ('registered', 'approved', 'withdrawn')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fixtures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES tournament_details(content_item_id) ON DELETE CASCADE,
    round_name TEXT NOT NULL,
    team_a_id UUID REFERENCES teams(id),
    team_b_id UUID REFERENCES teams(id),
    scheduled_time TIMESTAMPTZ NOT NULL,
    venue TEXT NOT NULL,
    team_a_score TEXT,
    team_b_score TEXT,
    winner_team_id UUID REFERENCES teams(id),
    status TEXT NOT NULL DEFAULT 'scheduled' CHECK (status IN ('scheduled', 'live', 'completed', 'cancelled'))
);

-- ----------------------------------------------------------------------------
-- 11. F06: Community Events & Weddings
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_details (
    content_item_id UUID PRIMARY KEY REFERENCES content_items(id) ON DELETE CASCADE,
    event_category TEXT NOT NULL CHECK (event_category IN ('wedding', 'religious', 'sports', 'general')),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    venue TEXT NOT NULL,
    is_private_invitation BOOLEAN NOT NULL DEFAULT FALSE,
    rsvp_limit INT
);

CREATE TABLE IF NOT EXISTS event_rsvps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES event_details(content_item_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    status TEXT NOT NULL CHECK (status IN ('going', 'not_going', 'maybe')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(event_id, user_id)
);

-- ----------------------------------------------------------------------------
-- 12. F07: Blood Donor Network
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS donor_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    blood_group TEXT NOT NULL CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    phone_number TEXT NOT NULL,
    area_locality TEXT NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    last_donated_at DATE,
    consent_given BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, community_id)
);

CREATE TABLE IF NOT EXISTS blood_request_details (
    content_item_id UUID PRIMARY KEY REFERENCES content_items(id) ON DELETE CASCADE,
    blood_group TEXT NOT NULL CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    hospital_name TEXT NOT NULL,
    patient_name TEXT NOT NULL,
    urgency TEXT NOT NULL DEFAULT 'urgent' CHECK (urgency IN ('routine', 'urgent', 'critical')),
    units_needed INT NOT NULL DEFAULT 1,
    coordinator_phone TEXT NOT NULL,
    is_fulfilled BOOLEAN NOT NULL DEFAULT FALSE
);

-- ----------------------------------------------------------------------------
-- 13. F08: Local Polls & Civic Issues
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS poll_details (
    content_item_id UUID PRIMARY KEY REFERENCES content_items(id) ON DELETE CASCADE,
    closes_at TIMESTAMPTZ NOT NULL,
    allow_vote_change BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS poll_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES poll_details(content_item_id) ON DELETE CASCADE,
    option_text TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    vote_count INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS poll_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES poll_details(content_item_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(poll_id, user_id)
);

CREATE TABLE IF NOT EXISTS civic_issue_details (
    content_item_id UUID PRIMARY KEY REFERENCES content_items(id) ON DELETE CASCADE,
    location_landmark TEXT NOT NULL,
    department TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'acknowledged', 'in_progress', 'resolved')),
    endorsement_count INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS issue_endorsements (
    issue_id UUID NOT NULL REFERENCES civic_issue_details(content_item_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY(issue_id, user_id)
);

-- ----------------------------------------------------------------------------
-- 14. F09: Badges & Contribution Rewards
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS badge_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    badge_key TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    icon_name TEXT NOT NULL,
    points_required INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS contribution_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    action_type TEXT NOT NULL,
    points_awarded INT NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS badge_awards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    badge_id UUID NOT NULL REFERENCES badge_definitions(id) ON DELETE CASCADE,
    awarded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, badge_id)
);

-- ----------------------------------------------------------------------------
-- 15. F10: Hyperlocal Classifieds
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS classified_details (
    content_item_id UUID PRIMARY KEY REFERENCES content_items(id) ON DELETE CASCADE,
    listing_type TEXT NOT NULL CHECK (listing_type IN ('sell', 'giveaway', 'wanted')),
    category TEXT NOT NULL,
    price_cents BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    condition_state TEXT NOT NULL DEFAULT 'used' CHECK (condition_state IN ('new', 'like_new', 'used')),
    is_available BOOLEAN NOT NULL DEFAULT TRUE
);

-- ----------------------------------------------------------------------------
-- 16. F14: Verified Local News & Rumors
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS news_details (
    content_item_id UUID PRIMARY KEY REFERENCES content_items(id) ON DELETE CASCADE,
    is_official_announcement BOOLEAN NOT NULL DEFAULT FALSE,
    is_rumor_clarification BOOLEAN NOT NULL DEFAULT FALSE,
    source_agency TEXT,
    verified_by_entity TEXT NOT NULL DEFAULT 'Hoode Community Council',
    verified_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 17. F15: Service Providers & Handymen
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS provider_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    service_type TEXT NOT NULL,
    phone_number TEXT NOT NULL,
    experience_years INT NOT NULL DEFAULT 1,
    rating NUMERIC(3,2) NOT NULL DEFAULT 5.0,
    reviews_count INT NOT NULL DEFAULT 0,
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 18. F16: Daily Personality
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS personality_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    title_epithet TEXT NOT NULL,
    bio TEXT NOT NULL,
    key_achievements TEXT[] DEFAULT '{}',
    quote TEXT,
    photo_url TEXT,
    featured_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(community_id, featured_date)
);

-- ----------------------------------------------------------------------------
-- 19. F17: Hoode Photo Gallery (Strict 25-Image Cap)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gallery_collections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    key_slug TEXT NOT NULL,
    name TEXT NOT NULL,
    max_active_items INT NOT NULL DEFAULT 25,
    UNIQUE(community_id, key_slug)
);

CREATE TABLE IF NOT EXISTS gallery_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    collection_id UUID NOT NULL REFERENCES gallery_collections(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    caption TEXT NOT NULL,
    photographer_credit TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Trigger to strictly enforce max 25 active images in gallery
CREATE OR REPLACE FUNCTION check_gallery_25_cap()
RETURNS TRIGGER AS $$
DECLARE
    active_count INT;
    max_allowed INT;
BEGIN
    IF NEW.is_active = TRUE THEN
        SELECT max_active_items INTO max_allowed
        FROM gallery_collections
        WHERE id = NEW.collection_id;

        IF max_allowed IS NULL THEN
            max_allowed := 25;
        END IF;

        SELECT COUNT(*) INTO active_count
        FROM gallery_images
        WHERE collection_id = NEW.collection_id
          AND is_active = TRUE
          AND id <> COALESCE(NEW.id, '00000000-0000-0000-0000-000000000000'::uuid);

        IF active_count >= max_allowed THEN
            RAISE EXCEPTION 'Gallery active item cap of % reached. Archive or remove an active photo before adding or activating a new one.', max_allowed;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enforce_gallery_cap_trigger
BEFORE INSERT OR UPDATE OF is_active, collection_id ON gallery_images
FOR EACH ROW EXECUTE FUNCTION check_gallery_25_cap();

-- ----------------------------------------------------------------------------
-- 20. F18: Community Activities
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity_details (
    content_item_id UUID PRIMARY KEY REFERENCES content_items(id) ON DELETE CASCADE,
    category TEXT NOT NULL CHECK (category IN ('Religious', 'Community', 'Sports', 'Celebrations')),
    organizer_name TEXT NOT NULL,
    schedule_description TEXT NOT NULL,
    venue TEXT NOT NULL,
    registration_open BOOLEAN NOT NULL DEFAULT TRUE
);

-- ----------------------------------------------------------------------------
-- 21. F19: Educational Offerings
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS education_listings (
    content_item_id UUID PRIMARY KEY REFERENCES content_items(id) ON DELETE CASCADE,
    institution_name TEXT NOT NULL,
    program_type TEXT NOT NULL CHECK (program_type IN ('Madrasa', 'Tutoring', 'Islamic Studies', 'Skill Workshop')),
    target_age_group TEXT NOT NULL,
    schedule_timings TEXT NOT NULL,
    monthly_fee TEXT,
    coordinator_phone TEXT NOT NULL
);

-- ----------------------------------------------------------------------------
-- 22. F20: Our Huffaz Directory
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS huffaz_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    completion_year INT NOT NULL,
    institution_teacher TEXT NOT NULL,
    photo_url TEXT,
    bio TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 23. Five-Slide 3-Second Advertisement Carousel
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS advertisement_slides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    slot_index INT NOT NULL CHECK (slot_index BETWEEN 1 AND 5),
    headline TEXT NOT NULL,
    subheadline TEXT NOT NULL,
    sponsor_name TEXT NOT NULL,
    cta_text TEXT NOT NULL DEFAULT 'Learn More',
    cta_link TEXT,
    banner_bg_drawable TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(community_id, slot_index)
);

-- ----------------------------------------------------------------------------
-- 24. Push Notifications & Outbox
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    target_destination TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Row Level Security (RLS) Policies
-- ----------------------------------------------------------------------------
ALTER TABLE communities ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE profile_private ENABLE ROW LEVEL SECURITY;
ALTER TABLE memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE role_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE content_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE content_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE bookmarks ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversation_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE mosques ENABLE ROW LEVEL SECURITY;
ALTER TABLE prayer_schedule_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE mosque_follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE reminder_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE ramadan_daily_times ENABLE ROW LEVEL SECURITY;
ALTER TABLE directory_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE directory_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE lost_found_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE donor_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE blood_request_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE poll_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE poll_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE poll_votes ENABLE ROW LEVEL SECURITY;
ALTER TABLE civic_issue_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE issue_endorsements ENABLE ROW LEVEL SECURITY;
ALTER TABLE classified_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE news_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE provider_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE personality_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE gallery_collections ENABLE ROW LEVEL SECURITY;
ALTER TABLE gallery_images ENABLE ROW LEVEL SECURITY;
ALTER TABLE activity_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE education_listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE huffaz_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE advertisement_slides ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- Public read policies for standard community directories & published content
CREATE POLICY "Public communities read" ON communities FOR SELECT USING (true);
CREATE POLICY "Public profiles read" ON profiles FOR SELECT USING (true);
CREATE POLICY "Profiles update own" ON profiles FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Private profile self access" ON profile_private FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Content items public select" ON content_items FOR SELECT USING (publication_status = 'published');
CREATE POLICY "Content items author insert" ON content_items FOR INSERT WITH CHECK (auth.uid() = author_id);
CREATE POLICY "Content items author update" ON content_items FOR UPDATE USING (auth.uid() = author_id);

CREATE POLICY "Public mosques read" ON mosques FOR SELECT USING (true);
CREATE POLICY "Public prayer schedule read" ON prayer_schedule_entries FOR SELECT USING (true);
CREATE POLICY "Public ramadan read" ON ramadan_daily_times FOR SELECT USING (true);
CREATE POLICY "Public directory read" ON directory_entries FOR SELECT USING (true);
CREATE POLICY "Public directory categories read" ON directory_categories FOR SELECT USING (true);
CREATE POLICY "Public job details read" ON job_details FOR SELECT USING (true);
CREATE POLICY "Public lost found read" ON lost_found_details FOR SELECT USING (true);
CREATE POLICY "Public tournaments read" ON tournament_details FOR SELECT USING (true);
CREATE POLICY "Public events read" ON event_details FOR SELECT USING (true);
CREATE POLICY "Public blood requests read" ON blood_request_details FOR SELECT USING (true);
CREATE POLICY "Public polls read" ON poll_details FOR SELECT USING (true);
CREATE POLICY "Public poll options read" ON poll_options FOR SELECT USING (true);
CREATE POLICY "Poll votes user select own" ON poll_votes FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Poll votes insert own" ON poll_votes FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Public civic issues read" ON civic_issue_details FOR SELECT USING (true);
CREATE POLICY "Public classifieds read" ON classified_details FOR SELECT USING (true);
CREATE POLICY "Public news read" ON news_details FOR SELECT USING (true);
CREATE POLICY "Public providers read" ON provider_profiles FOR SELECT USING (true);
CREATE POLICY "Public personality read" ON personality_profiles FOR SELECT USING (true);
CREATE POLICY "Public gallery collections read" ON gallery_collections FOR SELECT USING (true);
CREATE POLICY "Public gallery images read" ON gallery_images FOR SELECT USING (is_active = true);
CREATE POLICY "Public activities read" ON activity_details FOR SELECT USING (true);
CREATE POLICY "Public education read" ON education_listings FOR SELECT USING (true);
CREATE POLICY "Public huffaz read" ON huffaz_profiles FOR SELECT USING (true);
CREATE POLICY "Public advertisement slides read" ON advertisement_slides FOR SELECT USING (is_active = true);

CREATE POLICY "User notifications self read" ON notifications FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "User bookmarks self read" ON bookmarks FOR ALL USING (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- Admin policies (role-gated based on role_assignments)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION is_community_admin(check_community_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM role_assignments
        WHERE user_id = auth.uid()
          AND community_id = check_community_id
          AND role IN ('super_admin', 'community_admin')
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE POLICY "Admin manage ad slides" ON advertisement_slides FOR ALL USING (is_community_admin(community_id));
CREATE POLICY "Admin manage prayer schedule" ON prayer_schedule_entries FOR ALL USING (
    EXISTS (
        SELECT 1 FROM mosques
        WHERE mosques.id = prayer_schedule_entries.mosque_id
          AND is_community_admin(mosques.community_id)
    )
);
CREATE POLICY "Admin manage gallery images" ON gallery_images FOR ALL USING (
    EXISTS (
        SELECT 1 FROM gallery_collections
        WHERE gallery_collections.id = gallery_images.collection_id
          AND is_community_admin(gallery_collections.community_id)
    )
);
CREATE POLICY "Admin manage personality" ON personality_profiles FOR ALL USING (is_community_admin(community_id));
CREATE POLICY "Admin manage huffaz" ON huffaz_profiles FOR ALL USING (is_community_admin(community_id));

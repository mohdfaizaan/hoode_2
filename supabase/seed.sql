-- ============================================================================
-- Hoode Connect — Synthetic Development Seed Data
-- ============================================================================

-- 1. Community
INSERT INTO communities (id, slug, name, timezone, country_code, currency_code, default_locale, supported_locales)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    'hoode',
    'Hoode',
    'Asia/Kolkata',
    'IN',
    'INR',
    'en',
    ARRAY['en', 'kn', 'ur']
) ON CONFLICT (slug) DO NOTHING;

-- 2. Mosques
INSERT INTO mosques (id, community_id, name, location_description, timezone, is_primary) VALUES
('m0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Hoode Juma Masjid', 'Main Road, Hoode Beach Road', 'Asia/Kolkata', TRUE),
('m0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'Madina Masjid Bengre', 'Bengre Post, Near Jetty', 'Asia/Kolkata', FALSE),
('m0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'Noorani Masjid Kodi', 'Kodi Bengre Beach', 'Asia/Kolkata', FALSE)
ON CONFLICT DO NOTHING;

-- 3. Prayer Schedule Entries (Current)
INSERT INTO prayer_schedule_entries (
    mosque_id, effective_date,
    fajr_adhan, fajr_iqamah, sunrise,
    dhuhr_adhan, dhuhr_iqamah,
    asr_adhan, asr_iqamah,
    maghrib_adhan, maghrib_iqamah,
    isha_adhan, isha_iqamah,
    jummah_adhan, jummah_iqamah, notes
) VALUES (
    'm0000000-0000-0000-0000-000000000001', CURRENT_DATE,
    '05:12:00', '05:30:00', '06:18:00',
    '12:34:00', '12:50:00',
    '15:58:00', '16:15:00',
    '18:31:00', '18:40:00',
    '19:45:00', '20:00:00',
    '12:30:00', '13:00:00', 'Verified by Hoode Mosque Committee'
) ON CONFLICT DO NOTHING;

-- 4. Five-Slide 3-Second Advertisement Carousel
INSERT INTO advertisement_slides (
    community_id, slot_index, headline, subheadline, sponsor_name, cta_text, cta_link, banner_bg_drawable, is_active
) VALUES
('c0000000-0000-0000-0000-000000000001', 1, 'Fresh Catch of the Day', 'Direct from Malpe port boats to your doorstep. Free delivery in Hoode & Bengre.', 'Coastal Fisheries Hoode', 'Order Now', 'https://example.com/fish', 'bg_carousel_gradient', TRUE),
('c0000000-0000-0000-0000-000000000002', 2, 'Al-Noor Timber & Hardware', 'Authentic Burma teak, rosewood, and premium plumbing supplies for your home construction.', 'Al-Noor Traders', 'View Catalog', 'https://example.com/timber', 'bg_carousel_slide_2', TRUE),
('c0000000-0000-0000-0000-000000000003', 3, 'Gulf Express Cargo Service', 'Door-to-door courier service to Dubai, Abu Dhabi, Dammam, and Riyadh. Express transit.', 'Gulf Express Hoode', 'Track Shipment', 'https://example.com/cargo', 'bg_carousel_slide_3', TRUE),
('c0000000-0000-0000-0000-000000000004', 4, 'Baitul Mal Scholarship 2026', 'Higher education financial support for deserving youth of Hoode. Apply before Oct 15.', 'Hoode Welfare Trust', 'Apply Online', 'https://example.com/scholarship', 'bg_carousel_slide_4', TRUE),
('c0000000-0000-0000-0000-000000000005', 5, 'Modern Dental & Wellness Clinic', 'Comprehensive family dental care, cosmetic dentistry, and dental implants in Kemmannu.', 'Dr. A. K. Dental', 'Book Visit', 'https://example.com/dental', 'bg_carousel_slide_5', TRUE)
ON CONFLICT (community_id, slot_index) DO NOTHING;

-- 5. Emergency Directory Categories & Contacts
INSERT INTO directory_categories (id, community_id, key_slug, name, priority) VALUES
('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'ambulance', 'Ambulance & Medical', 1),
('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'police', 'Police & Coastal Security', 2),
('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'fire', 'Fire & Disaster Rescue', 3),
('d0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001', 'utility', 'Electricity & Water Board', 4)
ON CONFLICT (community_id, key_slug) DO NOTHING;

INSERT INTO directory_entries (category_id, name, phone_number, area_address, operating_hours, is_emergency, is_verified) VALUES
('d0000000-0000-0000-0000-000000000001', 'Hoode Community Ambulance', '+918202520108', 'Near Hoode Juma Masjid', '24/7 Service', TRUE, TRUE),
('d0000000-0000-0000-0000-000000000001', 'Malpe Government Hospital', '+918202538100', 'Malpe Main Road', '24/7 Emergency Ward', TRUE, TRUE),
('d0000000-0000-0000-0000-000000000002', 'Malpe Coastal Police Station', '+918202538133', 'Malpe Port Circle', '24/7 Patrol', TRUE, TRUE),
('d0000000-0000-0000-0000-000000000003', 'Udupi Fire & Rescue Station', '+918202520101', 'Bannanje, Udupi', '24/7 Response', TRUE, TRUE),
('d0000000-0000-0000-0000-000000000004', 'MESCOM Kemmannu Substation', '+918202540022', 'Kemmannu Circle', '8:00 AM - 8:00 PM', FALSE, TRUE)
ON CONFLICT DO NOTHING;

-- 6. Gallery Collection (Enforcing 25 Max Images)
INSERT INTO gallery_collections (id, community_id, key_slug, name, max_active_items)
VALUES ('g0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'hoode_scenic', 'Hoode Coastal & Community Heritage', 25)
ON CONFLICT (community_id, key_slug) DO NOTHING;

-- Seed initial images into gallery (25 items)
INSERT INTO gallery_images (collection_id, image_url, caption, photographer_credit, sort_order, is_active) VALUES
('g0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e', 'Sunset over Delta Point Bengre', 'Faizan Hoode', 1, TRUE),
('g0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957', 'Traditional Fishing Boats at Sunrise', 'Ahmed K.', 2, TRUE),
('g0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1564769625905-50e93615e769', 'Hoode Juma Masjid Minaret at Dusk', 'Zubair B.', 3, TRUE),
('g0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1519046904884-53103b34b206', 'Bengre Estuary Where River Meets Sea', 'Tariq M.', 4, TRUE),
('g0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1506744038136-46273834b3fb', 'Kemmannu Hanging Bridge Serenity', 'Rashid A.', 5, TRUE)
ON CONFLICT DO NOTHING;

-- 7. Daily Personality
INSERT INTO personality_profiles (community_id, full_name, title_epithet, bio, key_achievements, quote, photo_url, featured_date, is_active)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    'Janab Haji Abdullah Saheb',
    'Community Patriarch & Philanthropist',
    'For four decades, Haji Abdullah Saheb has championed free primary education, funded local scholarship trusts, and built the Hoode Beach Medical Emergency relief center.',
    ARRAY['Founded Hoode Education Trust in 1988', 'Spearheaded the 24/7 Community Ambulance Drive', 'Recognized by Coastal Karnataka Foundation with Civic Honor 2024'],
    'True wealth is the knowledge we impart and the hearts we uplift in our neighborhood.',
    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d',
    CURRENT_DATE,
    TRUE
) ON CONFLICT (community_id, featured_date) DO NOTHING;

-- 8. Our Huffaz Directory
INSERT INTO huffaz_profiles (community_id, full_name, completion_year, institution_teacher, photo_url, bio, sort_order, is_verified) VALUES
('c0000000-0000-0000-0000-000000000001', 'Hafidh Bilal Ahmed', 2021, 'Madrasa Darul Uloom, Hoode (Under Ustad Ibrahim)', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e', 'Leads Taraweeh prayers at Madina Masjid Bengre with exceptional Tajweed.', 1, TRUE),
('c0000000-0000-0000-0000-000000000001', 'Hafidh Mohammed Zaid', 2018, 'Markaz Taaleem-ul-Qur''an Mangalore', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e', 'Teaches daily Hifz revision for high school students in Hoode.', 2, TRUE),
('c0000000-0000-0000-0000-000000000001', 'Hafidh Abdul Rahman', 2023, 'Jamia Salafiyya', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7', 'Winner of Coastal Karnataka State Qur''an Recitation Competition 2023.', 3, TRUE)
ON CONFLICT DO NOTHING;

-- 9. Badges
INSERT INTO badge_definitions (badge_key, name, description, icon_name, points_required) VALUES
('community_pillar', 'Community Pillar', 'Awarded for active civic contributions and community initiatives', 'ic_badge', 250),
('golden_donor', 'Golden Lifesaver', 'Recognizes 3+ emergency blood donations in Hoode', 'ic_blood', 150),
('mosque_custodian', 'Mosque Helper', 'Contributed verified timetable updates and event organization', 'ic_prayer', 100)
ON CONFLICT (badge_key) DO NOTHING;

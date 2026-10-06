---
version: alpha
name: "Hoode Connect"
description: "Native Android community services with established Material controls"
colors:
  background: "#F3F6FA"
  text: "#172D43"
  primary: "#235FA4"
typography:
  sans:
    fontFamily: "Android system sans-serif"
omitted:
  - section: spacing
    reason: "Existing Android XML dimensions remain canonical"
  - section: rounded
    reason: "Existing Material styles and drawable shapes remain canonical"
  - section: components
    reason: "Existing Android XML components remain canonical"
---

# Hoode Connect Design System

## Overview

### Creative North Star
A local community noticeboard: recognizable services, legible names and explicit publication status.

### Product context and register
Hoode residents and the community administrator use this native Android product in India. The existing app uses English, with resident resources for Kannada and Urdu. The approved coastal identity now covers the complete resident application. The separate administrator application is outside this theme change. No Japan-market scope applies.
Runtime XML under `app/src/main/res/values`, `values-night` where present, drawable shapes and Material theme styles own tokens. This document mirrors the existing palette; it does not generate or redefine theme resources. Target behavior is recorded in `UX-CONTRACT.md`.

## Colors
The frontmatter mirrors the existing background, primary text and main action colors from `values/colors.xml`. Existing semantic error, success and disabled colors stay in Material themes. The October 2 expansion promotes the approved dashboard palette to `values/colors.xml`; dashboard, auth, profile and legacy color names are aliases to this canonical palette.

## Typography
Android system sans-serif uses existing XML sp values, with Android fallback fonts for local scripts. Keep labels readable with system font scaling.

## Layout
Use five stable resident tabs: Home, Explore, Create, Inbox and Profile. Detail pages keep a labeled Back control. Auth forms float over a navy brand surface inside one scrolling owner. Publishing actions retain their position while work is pending. Respect system bars and the keyboard.

## Elevation & Depth
Existing Material cards and dialogs own elevation and scrims. Network status does not add decorative overlays.

## Shapes
Existing drawable and Material theme shapes remain authoritative; the app uses 28dp hero/auth corners, 24dp media cards, 20–22dp secondary cards and 18dp buttons. Native forms and dialogs share these resources.

## Components
Native Material buttons, text fields, Android dialogs, RecyclerViews and platform image pickers are the canonical controls. Inline errors explain failed sign-in. Existing Toast feedback acknowledges server-confirmed writes. Disable duplicate submissions and retain entered values after failure.

Native scrollbars and focus behavior belong to Android. Web CSS, DOM ARIA and browser popup geometry do not apply. Android resource IDs and View Binding own reusable control identity. Existing vector drawables own icons. Preserve existing animation and platform reduced-motion settings.

## Do's and Don'ts

- Do show success only after the backend accepts a write.
- Do preserve established workflows while applying the coastal identity across the resident app.
- Do distinguish unavailable data from published data.
- Don't invent local user identities or present sample posts as server content.
- Don't put backend credentials or internal implementation details in resident-facing flows.

## September 28 refinement

Preserve the native Material identity. The resident sponsorship surface uses full-width photography, a charcoal scrim, restrained sponsor attribution and a scrolling story panel; it does not introduce a new app theme. Gallery uses a two-column journal grid with a clear submission action. Profile and menu avatars use a single 50% rounded ShapeableImageView mask with equal width/height, center crop and no white inset or internal border. Blood actions use a balanced droplet and cross vector in the existing danger color. Runtime colors, dimensions and styles remain in Android resources; these are component-specific treatments.

## October 1 coastal dashboard redesign

The user rejected the teal recoloring and explicitly requested a new theme and structure. The resident dashboard now takes its visual direction from Hoode’s coastal setting: cool daylight surfaces, ink-blue typography, one deep navy prayer card and a small amber accent. The signature is a restrained shoreline contour on the prayer surface. This is a community product, not a financial dashboard or promotional landing page; no invented balances, population counts, metrics or stories are used. This dashboard-only scope was expanded by the user on October 2; see the app-wide contract below.

### Palette and runtime ownership

Model B: Android XML resources remain canonical. `app/src/main/res/values/colors.xml` owns background #F3F6FA, surface #FFFFFF, ink #172D43, secondary #586A7D, primary #235FA4, soft #E6EDF5, border #DBE3ED, navy #183650, on-navy #FFFFFF, secondary-on-navy #CAD9E8, amber #F4C879, danger #A13F50 and danger surface #F8E9ED. The values feed shared Material styles, chip selectors, native shapes and all resident layouts. `values/dashboard.xml` keeps Home aliases so the approved dashboard colors remain exact. Existing app-wide Material primitives and navigation continue to own behavior.

### Type, structure and signature

Display: Android `sans-serif-condensed`, used for the 40sp place/date headline, brand wordmark and sponsor title. Body: `sans-serif` at 13–17sp, with `sans-serif-medium` for 21sp section headings and labels. Prayer time uses `sans-serif-light` at 37sp. Platform fonts avoid remote font loading and support existing locale fallbacks.

The flow is header and personal greeting → next prayer → four useful shortcuts → community partners → a filtered noticeboard → highlights and the featured person → a six-entry service directory → photography. This replaces the sponsor-first hierarchy and duplicated nine-tile category grids. The card-and-open-space rhythm distinguishes urgent utility, published content and navigation without decorating every element equally. Home uses a 22dp gutter; media remains edge-to-edge inside its rounded mask.

### Behavior and motion

The noticeboard combines the repository’s published news, events, polls and activities, with four previews per filter. The previous hardcoded sample stories and population claims are removed. Full content remains in each existing destination. Empty sections use honest text or collapse when they offer no action; gallery retains its browse/share route. Pull-to-refresh reuses the existing coordinated sync job and never claims server success merely because a spinner stops.

Sponsors are manually paged, including a Next button; they do not move while being read. The dashboard enters with one 280ms fade/lift only when Android animations are enabled and touch exploration is off. No repeating decorative motion is introduced. Date, avatar and unread indicator use actual device/account state. Native focus, pressed feedback, labels and bottom navigation are retained. All reads collect only while the view is started, and dialogs/animations are released with the view.


## October 2 resident-wide coastal theme

The user explicitly approved the dashboard and requested the same standard from opening through sign-out, with a short intro. The admin app must remain unchanged. The native view stack remains canonical; no replacement UI framework or remote font is added.

The signature is still the navy coastline, now echoed in the profile cover and the opening mark. It does not appear behind every content card. Full-width service rows improve Explore’s long labels; profile editing and submission tabs precede private account details. The floating auth card reflows in a scroll view instead of relying on fixed screen offsets. Bottom-tab switching restores state and avoids duplicate destinations.

### Canonical token path

`values/colors.xml` → compatibility aliases (`dashboard_*`, `auth_*`, `profile_*`) → `values/themes.xml` / `values/styles.xml` → XML and native views. Navy #183650, ink #172D43, blue #235FA4, background #F3F6FA, secondary #586A7D, border #DBE3ED and amber #F4C879 retain the dashboard’s exact values. Semantic success #28664F and warning #825619 support readable labels on their pale surfaces; danger stays #A13F50. Image scrims may use opacity-specific blacks for photograph legibility.

System `sans-serif-condensed` bold owns display/page headings, `sans-serif-medium` owns section and action labels, and `sans-serif` owns body and input text. Page gutters are 22dp. Parents own gutters; cards do not silently add horizontal layout margins. Explicitly padded card content must not also inherit Material content padding. Photos remain full bleed inside their rounded mask.

### Motion and access

`LaunchIntro` reveals the brand once on a fresh activity: 600ms fade/lift, at most 750ms minimum presence, then a 220ms fade out. Session restoration runs concurrently and is never restarted for decoration. Restored activities, disabled animator settings and touch exploration skip decorative timing. No repeat loop, sound, video download or new permission is required. Native OS splash precedes the app intro.

Buttons/back/close/filter actions target 48dp or more. Long form bodies use `FormScrollView` so title and submit controls keep screen space. Directory empty states use `EmptyContent`; they offer refresh without claiming a successful network fetch. Existing pending, retry and moderation flows remain in place.

### Migration ledger

| Surface | Canonical target | Migration | Verification |
|---|---|---|---|
| Opening, sign-in, registration | LaunchIntro + shared Material input/button styles | Implemented; disabled OAuth affordance hidden, false recovery confirmation removed | Build passed; device evidence recorded separately |
| Home | Approved dashboard through shared aliases | Visual identity preserved | Existing approval; navigation retested in device suite |
| Explore, Create, Inbox, drawer, bottom tabs | Shared colors, native navigation, full-width service rows | Implemented, search empty/clear states and selected-filter contrast corrected | Build passed; device suite |
| Profile, editing, settings | Shared surface/type system and canonical fragments | Implemented; duplicate activity entry points route to maintained fragments | Build passed; device suite |
| Gallery, marketplace, detail readers | Shared media masks and semantic controls | Implemented; full-bleed gallery card corrected | Build passed; device suite |
| Directories, events, jobs, activities, community and faith screens | Shared card/type/chip styles | Implemented; blank directory states gain retry | Build passed; device suite |
| Submission forms, confirmations, photo previews | Material controls + bounded scrolling | Implemented without changing backend mutation contracts | Build passed; form/keyboard checks |

Verification results and limits belong to `verification/coastal/REPORT.md`; static lint is not proof of runtime behavior or production readiness.

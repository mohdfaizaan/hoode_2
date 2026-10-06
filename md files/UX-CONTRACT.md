# Shared backend interaction contract

Scope: Supabase sign-in, profile edits, submissions, moderation, banners, prayer schedules and media. The two APKs share `../../hoode-admin/backend/public.properties`, `../../hoode-admin/backend/android` and the API contract in `../../hoode-admin/backend/README.md`. September 28 extends this contract to profile tabs, marketplace reservations/messages, community directories, polls, attendance, gallery and review notifications.

## Sources

The user selected Supabase project `cgjjbcqctblevdrehxqi` and owner email `characterbee96@gmail.com`. The reviewed runtime API contract is `../../hoode-admin/backend/supabase/migrations/202609240001_connect_apps.sql`. Role and visibility enforcement belongs to database RLS and checked server functions, not Android UI state.

## Canonical UI Map

| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
|---|---|---|---|---|
| Form | Existing Material TextInputLayout/EditText + View Binding | Layout XML, Supabase clients | Sign-in, profile, submission | Android compilation; manual device check pending |
| Toast | Android Toast | Existing fragment workflow | Acknowledgement and recoverable request error | Build; manual device check pending |
| CRUD | SupabaseClient / SupabaseAdminClient | Shared SQL migration | Resident pending insert, admin manage, own-profile edit | Local PostgreSQL permission tests |
| Scrollbar | Android native scroll containers | Existing layouts | RecyclerView and ScrollView | Native behavior; device check pending |
| Select/Listbox | Native Android Spinner / Material selection controls | ContentSections and repository state | Tournament and content-section selectors | Compile + device popup check pending |
| File upload | Shared MediaUploader + Android picker | Backend media functions | Profile avatar, news image, banner | Signed URL tests and live B2 upload/read passed Sept 26; reader rechecked Sept 28 |

## Flow ledger

Sign-in keeps the form visible while pending, disables submit and only navigates after a real session is established. Admin access additionally requires a non-banned server profile with community_admin or super_admin role. Email confirmation returns a sign-in instruction, not a fake session. Admin refresh tokens use Android Keystore encryption and backups are disabled.

Submissions retain their form on error and show success only after a remote write. Events and listings enter pending moderation. Profile edits update local state only after the remote profile is saved. Picked avatar bytes go to Backblaze; device-only URI values do not go into database rows.

Resident submission actions are labeled **Submit**. A server-confirmed write shows **Pending** and “We’ll review your submission and make it live once it’s approved. You can track its status in Profile → Activity.” The acknowledgement offers **View status**, opening the Activity tab. Private applications, claims, registrations and corrections instead explain that their review status will be updated; they never promise public publication. Pending profile cards repeat the appropriate explanation.

Gallery/community image drafts belong to `CommunityPostViewModel`, including selected photo, completed media URL, title/body and stable request ID. The UI reports preparing, connecting, upload percentage and saving. Rotation retains the operation in the current process; Stop cancels the request and retains the draft. Failed/uncertain operations retain the form. Retrying the same form reuses its submission ID and completed photo uploads. If the server already saved a different version, tell the resident to check Activity rather than silently accepting different content. The shared MediaUploader converts local picker URIs before community content is saved, including lost-and-found.

Resident shared feeds refresh while the activity is visible, at most once per 30 seconds after the preceding fetch has finished. A successful empty response clears old rows. Failed refresh retains last fetched state. Banners use 0-based database slots, mapped to the existing resident display numbering.

Existing destructive dialogs remain in place. Permission-changing actions use the checked resident-management RPC. The owner cannot ban or demote their own session through that RPC. See the migration for authoritative constraints.

## Verification and limitations

See `../../hoode-admin/backend/README.md` and `../../hoode-admin/backend/VERIFICATION.md`. Build and local SQL tests are not proof of live connectivity or accessibility. No connected device was available during implementation. End-to-end testing requires deployment and two real Authentication accounts. Native device checks remain required for visual fit, keyboard, rotation, TalkBack, RTL and large-font behavior. Build/static checks do not prove these states.

## September 28 workflows

- `CommunityApi` owns checked community CRUD, private profile feeds, reactions and comments. `PendingSubmission.submitForReview` owns busy/acknowledged/error behavior for legacy submission forms; `CommunityPostForm` owns image drafts for gallery/community posts.
- Resident content starts pending. Profile Posts shows own public-type submissions, Community shows own community posts, and Activity includes all own requests plus reservations. Each tab filters on the server before paging. Counts come from a separate aggregate. Published personal applications and claims remain private.
- Admin `CommunityManagerFragment` owns directories, the singleton personality, highlights, gallery, activities, polls, tournament fixtures/standings, calendar and Ramadan timetable. It uses shared `ContentSections`. Selecting a tournament stores its ID without asking the administrator to type it.
- Moderation refreshes its page after a decision to avoid skipping records. A stale expected status cannot overwrite a newer decision.
- Marketplace reserves one item per buyer for 24 hours under database locks/constraints. Buyer contact details are private to the buyer, seller and admin. Withdrawal cancels a reservation. Seller ownership is based on account ID.
- Inbox groups real marketplace messages, pages conversations and earlier messages, and retains an unsent reply after errors. It refreshes on entry and after send; there is no push/realtime delivery promise.
- Polls allow one vote, repeat votes for the same choice are safe, and options cannot change once votes exist. Attendance, civic support and review-read states are stored remotely. Contribution points are derived from approved records, not local taps.
- Sponsored cards are photo-led with a dark readability scrim and visible sponsor attribution. A scrolling detail panel carries long text. Admin slots store subtitle, description, image, URL and active state; unsaved drafts survive tab changes in the current activity. Website links accept only HTTP(S).
- Gallery uses the system photo picker, Backblaze upload, preview, pending review and an approved grid. Failed loads expose Retry; account posts show rejection reasons.
- Online contributions are unavailable until verified payment details and payment confirmation are configured. No placeholder payment destination or tap-to-award donation points are exposed.
- Fixed datasets (directories) sync in pages; gallery, profiles, queue, inbox and admin lists expose Load more. Existing home feeds retain their backend batch limits; notifications show the latest 50 review notices and contribution history shows the latest 30 awards.

## Image efficiency and profile consistency

Profile and menu display the same saved avatar using one circular mask without a white inset. The menu observes currentUser so edits update its avatar and locality automatically. Initial data sync waits for session restoration; independent feed requests run concurrently, retain their dependency ordering for attendance/support counts, and isolate failures. The foreground polling loop waits for completion before its next 30-second delay; duplicate callers join the active sync.

Both apps use shared ImageOptimizer before upload: supported still photos above 2 MiB become WebP capped at 2048 pixels on the longest edge and 2 MiB. Small files retain their original bytes; camera orientation and transparent pixels survive conversion. HEIC/HEIF converts when the device decoder supports it. Animated images, video and documents keep their original bytes. Existing stored files are unchanged. Temporary files stay in app-private cache and are deleted after the operation; one compression runs at a time to limit peak memory.

## Coastal home dashboard

HomeFragment owns dashboard composition and observes existing HoodeRepository StateFlows while STARTED. The backend remains the source of published visibility; the home redesign adds no write path or permission change. The noticeboard interleaves real news, events, polls and activity records and previews four per selected category. Browse and row taps open the existing owning destination; a filter survives recreation. No hardcoded news or community statistics are presented as live data.

SwipeRefreshLayout invokes the existing deduplicated sync job, bounded to 60 seconds in the view; timeout feedback keeps existing content. This does not infer per-feed success from a completed sync, because the existing repository isolates and retains data after individual failures. Image placeholders clear recycled content. Sponsors use manual swipe plus Next; highlights use the shared native alert dialog, while sponsor details preserve their established scrollable dialog and HTTP(S)-only link handling.

The home profile avatar opens Profile and reflects the saved account photo. Unread notification state comes from the repository. Activity routes reset the category to All. Existing nav graph and Material controls own navigation and input behavior. Accessibility relies on labeled 48dp header actions, scalable text, native focus/ripples and no automatic carousel motion.


## October 2 resident theme and navigation

- Runtime palette: `values/colors.xml`; Material primitives: `values/styles.xml`; five-tab navigation: `MainActivity`. Legacy Login/Register/EditProfile/Settings activities forward to the canonical fragments rather than owning divergent forms.
- Main tab changes use single-top state restoration and retain Home as the Back destination. Each destination updates the Android activity title. Detail Back uses the existing navigation controller; drawer Back closes the drawer first.
- `LaunchIntro` runs alongside session restoration, skips decorative motion on activity restoration or reduced motion, and removes its overlay when the activity is destroyed. No account is created or signed out by the intro.
- Auth shows backend results inline, retains email input, masks passwords, permits autofill/paste and blocks duplicate sign-in. Password recovery and Google OAuth are not implemented by this redesign; no control pretends that a recovery email was sent or that a sample Google identity authenticated. Sign-in help explains the available recovery path.
- Explore search filters local service names, presents a no-results message, and has an accessible 48dp clear action returning input focus. Buttons and chips retain their normal native pressed/focus behavior.
- Shared `EmptyContent` owns directory empty presentation. It describes locally available content and offers a coordinated refresh; it does not infer connectivity or claim medical supply availability from an empty feed.
- `FormScrollView` caps dialog body height while keeping the existing form controls, drafts, submit behavior and keyboard scrolling. Existing review/status rules are unchanged.
- Settings includes working profile/notification routes, theme explanation, intro replay, submission guidance and confirmed sign-out. Placeholder preference switches and invented support contact information are removed.
- `CoastalThemeDeviceTest` is read-only: it opens resident destinations, saves screenshots inside the app’s test evidence folder, exercises local search/clear and verifies Home → Services routing. It never submits forms, places calls, sends messages or edits accounts. Auth visual inflation checks do not prove real sign-in failure or password recovery.

# Hoode Connect — Profile, Edit Profile & Settings UI
### Android App — Kotlin + XML

This file defines the complete UI direction for the following Hoode Connect pages:

1. **Profile Page**
2. **Edit Profile Page**
3. **Settings Page**

The design must closely follow the supplied reference image, but with these changes:

- Replace the light green accent with a **light grey / soft neutral grey theme**
- Keep the overall layout, spacing, rounded cards, clean typography, white background, and premium mobile look
- Use Hoode Connect branding and content
- Kotlin + XML
- Responsive on Android phones
- Keep the profile page visually rich but not cluttered

---

# 1. Main Theme

The reference uses a soft sporty green accent.  
For Hoode Connect, replace that with a **light grey theme**.

Recommended palette:

| Purpose | Color |
|---|---|
| App background | `#F7F7F7` |
| Card background | `#FFFFFF` |
| Primary accent | `#D9D9D9` |
| Secondary accent | `#ECECEC` |
| Dark accent | `#6B6B6B` |
| Main text | `#171717` |
| Secondary text | `#777777` |
| Divider | `#EAEAEA` |
| Input background | `#F2F2F2` |
| Soft border | `#E3E3E3` |
| Icon background | `#F0F0F0` |
| Button text | `#242424` |

The overall result should feel:

- premium
- clean
- neutral
- soft
- community-focused
- modern
- similar to the reference layout

Do not use:
- strong green accents
- strong blue gradients
- heavy shadows
- dark blocks unless needed

---

# 2. Page Structure

Create these three screens:

```text
ProfileActivity.kt
EditProfileActivity.kt
SettingsActivity.kt
```

Layouts:

```text
activity_profile.xml
activity_edit_profile.xml
activity_settings.xml
```

---

# 3. PROFILE PAGE

The profile page should closely follow the **middle screen** in the reference image.

## Top Section

Use a large cover image area at the top.

Recommended height:

```text
180dp – 210dp
```

The cover image should:

- fill screen width
- use centerCrop
- have slightly rounded lower corners if desired

Place a circular profile image overlapping the cover image and white content area.

Profile image size:

```text
88dp – 96dp
```

Add:

- white border around profile image
- subtle shadow
- optional small edit icon if viewing your own profile

---

## User Info

Under the profile picture show:

### Full Name

Example:

**Mohammed Suhail**

### Username

Example:

`@mohammedsuhail`

Optional short bio:

`Connecting with the Hoode community.`

Optional small locality pill:

`Hoode`

Use small rounded chips with a light grey fill.

---

# 4. Profile Stats Card

The reference uses a 3-column stats card.

For Hoode Connect, use:

```text
Posts
Connections
Contributions
```

Example:

```text
23
Posts

126
Connections

78
Contributions
```

Use:

- white card
- rounded corners
- thin grey border
- equal 3 columns
- very light vertical dividers

Card height:

```text
72dp – 82dp
```

---

# 5. Edit Profile Button

Below the stats card:

Large rounded button:

**Edit Profile**

Style:

```text
background = #E1E1E1
text = #222222
corner radius = 22dp
height = 48dp
```

Add a small pencil icon on the left.

To the right, optionally keep the circular settings icon exactly like the reference.

Click actions:

```text
Edit Profile → EditProfileActivity
Settings Icon → SettingsActivity
```

---

# 6. Profile Tabs

Keep the same tab concept from the reference.

Recommended Hoode Connect tabs:

```text
Posts
Gallery
Activity
```

or

```text
Posts
Community
Activity
```

Use three equal tabs.

Selected tab:

- darker icon/text
- small underline

Unselected:

- muted grey

---

# 7. Profile Feed

Below tabs, show posts/cards similar to the reference.

Each post can contain:

- small profile image
- name
- timestamp
- post text
- one or multiple images
- like count
- comment count
- share action

Example:

```text
Mohammed Suhail                        12h ago

Beautiful evening view from Hoode today.

[ Image ][ Image ][ Image ]

♥ 127     💬 24
```

Each post card:

- white background
- corner radius `18dp`
- subtle border
- margin `12dp`
- no heavy shadow

---

# 8. Bottom Navigation

Keep the app bottom navigation compact and floating like the reference.

Use:

```text
Home
Explore
Create
Inbox
Profile
```

Profile should be the selected tab on this screen.

Recommended:

```text
height = 64dp
corner radius = 28dp
horizontal margin = 18dp
bottom margin = 14dp
```

Selected Profile icon:

- circular white highlight
- dark icon
- slightly raised effect

Background:

```text
#E1E1E1
```

---

# 9. PROFILE XML SKELETON

## `activity_profile.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>

<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F7F7F7">

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingBottom="90dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <!-- COVER IMAGE -->

            <FrameLayout
                android:layout_width="match_parent"
                android:layout_height="205dp">

                <ImageView
                    android:id="@+id/ivCover"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:scaleType="centerCrop"
                    android:src="@drawable/profile_cover_placeholder" />

            </FrameLayout>

            <!-- PROFILE CONTENT -->

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:paddingStart="18dp"
                android:paddingEnd="18dp">

                <com.google.android.material.imageview.ShapeableImageView
                    android:id="@+id/ivProfile"
                    android:layout_width="92dp"
                    android:layout_height="92dp"
                    android:layout_gravity="center"
                    android:layout_marginTop="-48dp"
                    android:scaleType="centerCrop"
                    android:src="@drawable/profile_placeholder"
                    app:shapeAppearanceOverlay="@style/CircleImageStyle"
                    app:strokeColor="@android:color/white"
                    app:strokeWidth="4dp" />

                <TextView
                    android:id="@+id/tvName"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:gravity="center"
                    android:text="Mohammed Suhail"
                    android:textColor="#171717"
                    android:textSize="24sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvUsername"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:gravity="center"
                    android:text="@mohammedsuhail"
                    android:textColor="#777777"
                    android:textSize="14sp" />

                <TextView
                    android:id="@+id/tvBio"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:gravity="center"
                    android:text="Connecting with the Hoode community."
                    android:textColor="#666666"
                    android:textSize="13sp" />

                <!-- STATS CARD -->

                <LinearLayout
                    android:id="@+id/statsCard"
                    android:layout_width="match_parent"
                    android:layout_height="78dp"
                    android:layout_marginTop="18dp"
                    android:background="@drawable/bg_profile_card"
                    android:gravity="center"
                    android:orientation="horizontal">

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="match_parent"
                        android:layout_weight="1"
                        android:gravity="center"
                        android:orientation="vertical">

                        <TextView
                            android:text="23"
                            android:textSize="20sp"
                            android:textStyle="bold"
                            android:textColor="#171717"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content" />

                        <TextView
                            android:text="Posts"
                            android:textSize="12sp"
                            android:textColor="#777777"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content" />

                    </LinearLayout>

                    <View
                        android:layout_width="1dp"
                        android:layout_height="40dp"
                        android:background="#E8E8E8" />

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="match_parent"
                        android:layout_weight="1"
                        android:gravity="center"
                        android:orientation="vertical">

                        <TextView
                            android:text="126"
                            android:textSize="20sp"
                            android:textStyle="bold"
                            android:textColor="#171717"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content" />

                        <TextView
                            android:text="Connections"
                            android:textSize="12sp"
                            android:textColor="#777777"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content" />

                    </LinearLayout>

                    <View
                        android:layout_width="1dp"
                        android:layout_height="40dp"
                        android:background="#E8E8E8" />

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="match_parent"
                        android:layout_weight="1"
                        android:gravity="center"
                        android:orientation="vertical">

                        <TextView
                            android:text="78"
                            android:textSize="20sp"
                            android:textStyle="bold"
                            android:textColor="#171717"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content" />

                        <TextView
                            android:text="Contributions"
                            android:textSize="12sp"
                            android:textColor="#777777"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content" />

                    </LinearLayout>

                </LinearLayout>

                <!-- EDIT PROFILE + SETTINGS -->

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="14dp"
                    android:gravity="center_vertical"
                    android:orientation="horizontal">

                    <TextView
                        android:id="@+id/btnEditProfile"
                        android:layout_width="0dp"
                        android:layout_height="50dp"
                        android:layout_weight="1"
                        android:background="@drawable/bg_primary_grey_button"
                        android:gravity="center"
                        android:text="✎  Edit Profile"
                        android:textColor="#222222"
                        android:textSize="15sp"
                        android:textStyle="bold" />

                    <ImageButton
                        android:id="@+id/btnSettings"
                        android:layout_width="50dp"
                        android:layout_height="50dp"
                        android:layout_marginStart="10dp"
                        android:background="@drawable/bg_circle_light"
                        android:src="@drawable/ic_settings"
                        app:tint="#333333" />

                </LinearLayout>

                <!-- TABS -->

                <com.google.android.material.tabs.TabLayout
                    android:id="@+id/profileTabs"
                    android:layout_width="match_parent"
                    android:layout_height="52dp"
                    android:layout_marginTop="10dp"
                    app:tabIndicatorColor="#6B6B6B"
                    app:tabSelectedTextColor="#333333"
                    app:tabTextColor="#999999" />

                <!-- FEED CONTENT CONTAINER -->

                <LinearLayout
                    android:id="@+id/feedContainer"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical" />

            </LinearLayout>

        </LinearLayout>

    </ScrollView>

    <!-- BOTTOM NAVIGATION -->
    <!-- Reuse the existing Hoode Connect floating bottom nav here -->

</FrameLayout>
```

---

# 10. EDIT PROFILE PAGE

The edit profile page should closely follow the **left screen** in the reference.

Top bar:

```text
←        Edit Profile
```

Use a circular back button.

Title:
- bold
- centered
- 22sp

---

# 11. Edit Profile Header

Show:

- large cover image
- circular profile image overlapping the cover
- small circular edit icon on the cover
- small circular edit icon on the profile image

Use light grey icon backgrounds.

---

# 12. Edit Profile Fields

Fields:

```text
Full Name
Username
Email Address
Phone Number
Bio
```

Optional:

```text
Location
```

Input style:

- fill `#F2F2F2`
- no heavy outline
- radius `18dp`
- height `54dp`
- dark text
- label above field

Example:

```text
Full Name

[ Mohammed Suhail ]
```

---

# 13. Save Changes Button

Bottom button:

**Save Changes**

Style:

```text
background = #D9D9D9
text = #222222
height = 52dp
corner radius = 22dp
```

Keep it visually identical to the button style in the reference, but grey.

---

# 14. EDIT PROFILE XML

## `activity_edit_profile.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>

<ScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F7F7F7"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingStart="16dp"
        android:paddingTop="40dp"
        android:paddingEnd="16dp"
        android:paddingBottom="24dp">

        <!-- HEADER -->

        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="52dp">

            <ImageButton
                android:id="@+id/btnBack"
                android:layout_width="44dp"
                android:layout_height="44dp"
                android:layout_gravity="start|center_vertical"
                android:background="@drawable/bg_circle_light"
                android:src="@drawable/ic_arrow_back"
                app:tint="#222222" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:text="Edit Profile"
                android:textColor="#171717"
                android:textSize="22sp"
                android:textStyle="bold" />

        </FrameLayout>

        <!-- COVER -->

        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="180dp"
            android:layout_marginTop="14dp">

            <ImageView
                android:id="@+id/ivEditCover"
                android:layout_width="match_parent"
                android:layout_height="125dp"
                android:scaleType="centerCrop"
                android:src="@drawable/profile_cover_placeholder" />

            <ImageButton
                android:id="@+id/btnEditCover"
                android:layout_width="38dp"
                android:layout_height="38dp"
                android:layout_gravity="end|top"
                android:layout_margin="10dp"
                android:background="@drawable/bg_circle_light"
                android:src="@drawable/ic_edit"
                app:tint="#444444" />

            <com.google.android.material.imageview.ShapeableImageView
                android:id="@+id/ivEditProfile"
                android:layout_width="88dp"
                android:layout_height="88dp"
                android:layout_gravity="center_horizontal|bottom"
                android:scaleType="centerCrop"
                android:src="@drawable/profile_placeholder"
                app:shapeAppearanceOverlay="@style/CircleImageStyle"
                app:strokeColor="#FFFFFF"
                app:strokeWidth="4dp" />

        </FrameLayout>

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:text="Mohammed Suhail"
            android:textColor="#171717"
            android:textSize="22sp"
            android:textStyle="bold" />

        <!-- FULL NAME -->

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="Full Name"
            android:textColor="#333333"
            android:textSize="14sp" />

        <EditText
            android:id="@+id/etFullName"
            android:layout_width="match_parent"
            android:layout_height="54dp"
            android:layout_marginTop="7dp"
            android:background="@drawable/bg_edit_input"
            android:hint="Full Name"
            android:paddingStart="16dp"
            android:paddingEnd="16dp" />

        <!-- USERNAME -->

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="Username"
            android:textColor="#333333"
            android:textSize="14sp" />

        <EditText
            android:id="@+id/etUsername"
            android:layout_width="match_parent"
            android:layout_height="54dp"
            android:layout_marginTop="7dp"
            android:background="@drawable/bg_edit_input"
            android:hint="Username"
            android:paddingStart="16dp"
            android:paddingEnd="16dp" />

        <!-- EMAIL -->

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="Email Address"
            android:textColor="#333333"
            android:textSize="14sp" />

        <EditText
            android:id="@+id/etEmail"
            android:layout_width="match_parent"
            android:layout_height="54dp"
            android:layout_marginTop="7dp"
            android:background="@drawable/bg_edit_input"
            android:inputType="textEmailAddress"
            android:paddingStart="16dp"
            android:paddingEnd="16dp" />

        <!-- PHONE -->

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="Phone Number"
            android:textColor="#333333"
            android:textSize="14sp" />

        <EditText
            android:id="@+id/etPhone"
            android:layout_width="match_parent"
            android:layout_height="54dp"
            android:layout_marginTop="7dp"
            android:background="@drawable/bg_edit_input"
            android:inputType="phone"
            android:paddingStart="16dp"
            android:paddingEnd="16dp" />

        <!-- BIO -->

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="Bio"
            android:textColor="#333333"
            android:textSize="14sp" />

        <EditText
            android:id="@+id/etBio"
            android:layout_width="match_parent"
            android:layout_height="90dp"
            android:layout_marginTop="7dp"
            android:background="@drawable/bg_edit_input"
            android:gravity="top|start"
            android:padding="16dp"
            android:maxLength="150"
            android:hint="Write a short bio..." />

        <!-- SAVE -->

        <TextView
            android:id="@+id/btnSaveChanges"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:layout_marginTop="22dp"
            android:background="@drawable/bg_primary_grey_button"
            android:gravity="center"
            android:text="Save Changes"
            android:textColor="#222222"
            android:textSize="15sp"
            android:textStyle="bold" />

    </LinearLayout>

</ScrollView>
```

---

# 15. SETTINGS PAGE

The settings page should closely follow the **right screen** in the reference.

Top:

```text
←        My Settings
```

Use:

- circular back button
- bold centered page title

---

# 16. Settings User Card

The first card shows:

- profile picture
- full name
- email
- right arrow

Example:

```text
[Photo]  Mohammed Suhail
         mohammed@email.com        >
```

Card:

```text
background #FFFFFF
radius 18dp
thin border #E7E7E7
height about 72dp
```

Tapping it opens:

```text
EditProfileActivity
```

---

# 17. Community Banner Card

The reference contains a Pro Access banner.

For Hoode Connect, replace it with a community-focused banner.

Example:

### Hoode Connect
`Stay connected with your community.`

Possible right-side illustration:

- locality illustration
- people/community icon
- Hoode Connect logo

Theme:

```text
soft grey gradient
```

Do not use green.

Example colors:

```text
#EDEDED → #D7D7D7
```

---

# 18. General Settings

Section title:

**General Settings**

Items:

```text
Notification Preferences
Privacy Settings
Connected Devices
Dark Mode
Language
```

Optional:

```text
Account Security
```

Each item:

- white row
- icon on left
- label
- optional value on right
- chevron
- thin divider

---

# 19. Support & Others

Section:

**Support & Others**

Rows:

```text
Help Center
Terms & Privacy
About Hoode Connect
App Info
Logout
```

Logout can use a muted red text only if desired.

---

# 20. SETTINGS XML SKELETON

## `activity_settings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>

<ScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F7F7F7"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingStart="16dp"
        android:paddingTop="40dp"
        android:paddingEnd="16dp"
        android:paddingBottom="28dp">

        <!-- HEADER -->

        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="52dp">

            <ImageButton
                android:id="@+id/btnBack"
                android:layout_width="44dp"
                android:layout_height="44dp"
                android:layout_gravity="start|center_vertical"
                android:background="@drawable/bg_circle_light"
                android:src="@drawable/ic_arrow_back"
                app:tint="#222222" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:text="My Settings"
                android:textColor="#171717"
                android:textSize="22sp"
                android:textStyle="bold" />

        </FrameLayout>

        <!-- USER CARD -->

        <LinearLayout
            android:id="@+id/profileSettingsCard"
            android:layout_width="match_parent"
            android:layout_height="76dp"
            android:layout_marginTop="18dp"
            android:background="@drawable/bg_settings_card"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:paddingStart="14dp"
            android:paddingEnd="14dp">

            <com.google.android.material.imageview.ShapeableImageView
                android:layout_width="46dp"
                android:layout_height="46dp"
                android:src="@drawable/profile_placeholder"
                app:shapeAppearanceOverlay="@style/CircleImageStyle" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="12dp"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/tvSettingsName"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Mohammed Suhail"
                    android:textColor="#171717"
                    android:textSize="15sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvSettingsEmail"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="3dp"
                    android:text="mohammed@email.com"
                    android:textColor="#777777"
                    android:textSize="12sp" />

            </LinearLayout>

            <ImageView
                android:layout_width="20dp"
                android:layout_height="20dp"
                android:src="@drawable/ic_chevron_right"
                app:tint="#444444" />

        </LinearLayout>

        <!-- HOODE CONNECT BANNER -->

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="92dp"
            android:layout_marginTop="12dp"
            android:background="@drawable/bg_community_banner"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:padding="14dp">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Hoode Connect"
                    android:textColor="#222222"
                    android:textSize="18sp"
                    android:textStyle="bold" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="5dp"
                    android:text="Stay connected with your community."
                    android:textColor="#666666"
                    android:textSize="12sp" />

            </LinearLayout>

            <ImageView
                android:layout_width="64dp"
                android:layout_height="64dp"
                android:src="@drawable/ic_community" />

        </LinearLayout>

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:text="General Settings"
            android:textColor="#222222"
            android:textSize="16sp"
            android:textStyle="bold" />

        <!-- SETTINGS LIST CONTAINER -->

        <LinearLayout
            android:id="@+id/generalSettingsContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:background="@drawable/bg_settings_card"
            android:orientation="vertical">

            <!-- Add reusable settings rows here -->

        </LinearLayout>

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="22dp"
            android:text="Support & Others"
            android:textColor="#222222"
            android:textSize="16sp"
            android:textStyle="bold" />

        <LinearLayout
            android:id="@+id/supportSettingsContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:background="@drawable/bg_settings_card"
            android:orientation="vertical" />

    </LinearLayout>

</ScrollView>
```

---

# 21. Settings Row Component

Create a reusable row layout:

## `item_settings_row.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>

<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingStart="14dp"
    android:paddingEnd="14dp">

    <ImageView
        android:id="@+id/ivIcon"
        android:layout_width="21dp"
        android:layout_height="21dp"
        app:tint="#444444" />

    <TextView
        android:id="@+id/tvTitle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="12dp"
        android:layout_weight="1"
        android:textColor="#222222"
        android:textSize="14sp" />

    <TextView
        android:id="@+id/tvValue"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginEnd="8dp"
        android:textColor="#888888"
        android:textSize="13sp"
        android:visibility="gone" />

    <ImageView
        android:id="@+id/ivArrow"
        android:layout_width="18dp"
        android:layout_height="18dp"
        android:src="@drawable/ic_chevron_right"
        app:tint="#666666" />

</LinearLayout>
```

---

# 22. Drawable Files

Create:

```text
bg_profile_card.xml
bg_primary_grey_button.xml
bg_circle_light.xml
bg_edit_input.xml
bg_settings_card.xml
bg_community_banner.xml
```

---

# 23. Profile Card Background

## `bg_profile_card.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">

    <solid android:color="#FFFFFF" />

    <stroke
        android:width="1dp"
        android:color="#E7E7E7" />

    <corners android:radius="18dp" />

</shape>
```

---

# 24. Grey Primary Button

## `bg_primary_grey_button.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">

    <solid android:color="#D9D9D9" />

    <corners android:radius="24dp" />

</shape>
```

---

# 25. Edit Input Background

## `bg_edit_input.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">

    <solid android:color="#F0F0F0" />

    <corners android:radius="18dp" />

</shape>
```

---

# 26. Circular Button Background

## `bg_circle_light.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">

    <solid android:color="#FFFFFF" />

    <stroke
        android:width="1dp"
        android:color="#E8E8E8" />

</shape>
```

---

# 27. Settings Card

## `bg_settings_card.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">

    <solid android:color="#FFFFFF" />

    <stroke
        android:width="1dp"
        android:color="#E5E5E5" />

    <corners android:radius="18dp" />

</shape>
```

---

# 28. Community Banner

## `bg_community_banner.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>

<shape xmlns:android="http://schemas.android.com/apk/res/android">

    <gradient
        android:angle="0"
        android:startColor="#ECECEC"
        android:endColor="#D6D6D6" />

    <corners android:radius="18dp" />

</shape>
```

---

# 29. Edit Profile Kotlin

## `EditProfileActivity.kt`

```kotlin
class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val fullName = findViewById<EditText>(R.id.etFullName)
        val username = findViewById<EditText>(R.id.etUsername)
        val email = findViewById<EditText>(R.id.etEmail)
        val phone = findViewById<EditText>(R.id.etPhone)
        val bio = findViewById<EditText>(R.id.etBio)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnSaveChanges)
            .setOnClickListener {

                val nameValue = fullName.text.toString().trim()

                if (nameValue.isEmpty()) {
                    fullName.error = "Enter your full name"
                    return@setOnClickListener
                }

                // Save updated profile data to Supabase.

                Toast.makeText(
                    this,
                    "Profile updated",
                    Toast.LENGTH_SHORT
                ).show()
            }

        findViewById<View>(R.id.btnEditCover)
            .setOnClickListener {
                // Open image picker for cover photo.
            }
    }
}
```

---

# 30. Profile Kotlin

## `ProfileActivity.kt`

```kotlin
class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<View>(R.id.btnEditProfile)
            .setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        EditProfileActivity::class.java
                    )
                )
            }

        findViewById<View>(R.id.btnSettings)
            .setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        SettingsActivity::class.java
                    )
                )
            }

        // Load profile data from Supabase.
        // Load profile image / cover image from storage.
        // Load posts for this user.
    }
}
```

---

# 31. Settings Kotlin

## `SettingsActivity.kt`

```kotlin
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<View>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        findViewById<View>(R.id.profileSettingsCard)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        EditProfileActivity::class.java
                    )
                )
            }

        // Build remaining settings rows:
        // Notifications
        // Privacy
        // Connected Devices
        // Dark Mode
        // Language
        // Help Center
        // Terms & Privacy
        // About Hoode Connect
        // App Info
        // Logout
    }
}
```

---

# 32. Supabase Profile Data

Recommended profile table:

```text
profiles
--------------------------------
id
full_name
username
email
phone
bio
profile_image_url
cover_image_url
location
created_at
updated_at
```

The authenticated Supabase user ID should match:

```text
profiles.id
```

---

# 33. Storage

Recommended storage folders:

```text
profiles/
    user_id/
        avatar.webp
        cover.webp
```

Use image compression before upload.

Suggested:
- profile image max dimension: 1080 px
- cover image max width: 1920 px

---

# 34. Important Visual Rules

For all 3 pages:

- Use the same spacing system
- Use the same corner radius family
- Use the same grey accent
- Use consistent icons
- Use white cards on light grey page background
- Avoid strong shadows
- Use thin borders
- Keep buttons rounded
- Keep text dark and readable
- Keep page titles bold
- Keep profile image circular
- Keep the design visually close to the reference
- Replace every green accent with light grey / neutral grey

---

# 35. Final UI Summary

## Profile Page

```text
Cover Image
      ↓
Profile Picture
Name
Username / Bio
Stats Card
Edit Profile + Settings
Tabs
Posts Feed
Floating Bottom Navigation
```

## Edit Profile Page

```text
Back + Edit Profile
Cover Image
Profile Picture
Full Name
Username
Email
Phone
Bio
Save Changes
```

## Settings Page

```text
Back + My Settings
User Account Card
Hoode Connect Community Banner

General Settings
- Notifications
- Privacy
- Connected Devices
- Dark Mode
- Language

Support & Others
- Help Center
- Terms & Privacy
- About Hoode Connect
- App Info
- Logout
```

---

# 36. Final Design Goal

The final result should look like the supplied reference app, but adapted for **Hoode Connect**.

The three screens should feel like one consistent design system:

- **Profile** = content-rich and polished
- **Edit Profile** = simple rounded form layout
- **Settings** = clean grouped settings cards

Main accent must be:

```text
LIGHT GREY
```

not green.

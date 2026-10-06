# Airtel-Style Compact Bottom Navigation — XML + Kotlin

This implementation recreates the **same visual structure and proportions** as the reference Airtel navigation bar:

- White floating rounded outer container
- Very thin cool-grey border
- Five equal navigation items
- Selected item sits inside its own soft rounded capsule
- Selected icon + label use the red accent
- Unselected icons + labels are near-black
- Compact height — **not an oversized bottom bar**
- Labels:
  - Home
  - Explore
  - Create
  - Inbox
  - Profile
- Designed for **Android XML + Kotlin**
- Uses only normal Android Views + Material Components
- Works well with fragments, activities, or a custom navigation setup

---

## 1. Target proportions

The supplied Airtel reference image is **1080 × 220 px**. The visible navigation container is approximately:

- Horizontal margin: ~36 px on each side
- Visible container height: ~132 px in the screenshot
- Five equal-width items
- Very large pill radius on the outside
- Selected inner pill almost fills the bar vertically, leaving a thin inset
- Icon above label
- Labels remain close to icons

For Android, use these compact density-independent measurements:

```text
Outer navigation height: 76dp
Outer horizontal margin: 16dp
Outer corner radius: 38dp
Outer border: 1dp

Selected item inset: 5dp
Selected item corner radius: 32dp
Selected item border: 1dp

Icon: 24dp
Icon → label spacing: 3dp
Label size: 12sp
```

These values reproduce the **short, compact appearance** instead of the much taller default Material bottom navigation.

---

# 2. Required Material dependency

In `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.google.android.material:material:1.12.0")
}
```

If you use Groovy:

```gradle
implementation 'com.google.android.material:material:1.12.0'
```

---

# 3. Colors

Create:

`res/values/colors.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <!-- Airtel-style selected red -->
    <color name="nav_selected_red">#F12832</color>

    <!-- Main text/icon color -->
    <color name="nav_unselected">#242424</color>

    <!-- Outer border -->
    <color name="nav_outer_stroke">#E4ECF7</color>

    <!-- Very subtle selected capsule fill -->
    <color name="nav_selected_background">#FCFCFD</color>

    <!-- Selected capsule border -->
    <color name="nav_selected_stroke">#E4EBF5</color>

    <color name="white">#FFFFFF</color>

</resources>
```

---

# 4. Color selector for icons

Create:

`res/color/bottom_nav_icon_tint.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">

    <item
        android:color="@color/nav_selected_red"
        android:state_selected="true" />

    <item android:color="@color/nav_unselected" />

</selector>
```

---

# 5. Color selector for labels

Create:

`res/color/bottom_nav_text_color.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">

    <item
        android:color="@color/nav_selected_red"
        android:state_selected="true" />

    <item android:color="@color/nav_unselected" />

</selector>
```

---

# 6. Selected item capsule

Create:

`res/drawable/bg_bottom_nav_item_selected.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">

    <solid android:color="@color/nav_selected_background" />

    <stroke
        android:width="1dp"
        android:color="@color/nav_selected_stroke" />

    <corners android:radius="32dp" />

</shape>
```

---

# 7. Transparent unselected item

Create:

`res/drawable/bg_bottom_nav_item_unselected.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">

    <solid android:color="@android:color/transparent" />

    <corners android:radius="32dp" />

</shape>
```

---

# 8. Background selector for each item

Create:

`res/drawable/bg_bottom_nav_item.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">

    <item
        android:drawable="@drawable/bg_bottom_nav_item_selected"
        android:state_selected="true" />

    <item android:drawable="@drawable/bg_bottom_nav_item_unselected" />

</selector>
```

---

# 9. Outer pill background

Create:

`res/drawable/bg_bottom_nav_container.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">

    <solid android:color="@color/white" />

    <stroke
        android:width="1dp"
        android:color="@color/nav_outer_stroke" />

    <corners android:radius="38dp" />

</shape>
```

---

# 10. Compact navigation layout

Create:

`res/layout/view_compact_bottom_navigation.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/bottomNavWrapper"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:clipChildren="false"
    android:clipToPadding="false"
    android:paddingStart="16dp"
    android:paddingEnd="16dp"
    android:paddingBottom="10dp">

    <LinearLayout
        android:id="@+id/bottomNavContainer"
        android:layout_width="match_parent"
        android:layout_height="76dp"
        android:background="@drawable/bg_bottom_nav_container"
        android:elevation="1dp"
        android:gravity="center"
        android:orientation="horizontal"
        android:padding="5dp">

        <!-- HOME -->
        <LinearLayout
            android:id="@+id/navHome"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@drawable/bg_bottom_nav_item"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:orientation="vertical"
            android:selected="true">

            <ImageView
                android:id="@+id/iconHome"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="Home"
                android:duplicateParentState="true"
                android:src="@drawable/ic_home_24"
                app:tint="@color/bottom_nav_icon_tint" />

            <TextView
                android:id="@+id/textHome"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="3dp"
                android:duplicateParentState="true"
                android:fontFamily="sans"
                android:includeFontPadding="false"
                android:text="Home"
                android:textColor="@color/bottom_nav_text_color"
                android:textSize="12sp"
                android:textStyle="bold" />

        </LinearLayout>

        <!-- EXPLORE -->
        <LinearLayout
            android:id="@+id/navExplore"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@drawable/bg_bottom_nav_item"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:orientation="vertical">

            <ImageView
                android:id="@+id/iconExplore"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="Explore"
                android:duplicateParentState="true"
                android:src="@drawable/ic_explore_24"
                app:tint="@color/bottom_nav_icon_tint" />

            <TextView
                android:id="@+id/textExplore"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="3dp"
                android:duplicateParentState="true"
                android:fontFamily="sans"
                android:includeFontPadding="false"
                android:text="Explore"
                android:textColor="@color/bottom_nav_text_color"
                android:textSize="12sp" />

        </LinearLayout>

        <!-- CREATE -->
        <LinearLayout
            android:id="@+id/navCreate"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@drawable/bg_bottom_nav_item"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:orientation="vertical">

            <ImageView
                android:id="@+id/iconCreate"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="Create"
                android:duplicateParentState="true"
                android:src="@drawable/ic_create_24"
                app:tint="@color/bottom_nav_icon_tint" />

            <TextView
                android:id="@+id/textCreate"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="3dp"
                android:duplicateParentState="true"
                android:fontFamily="sans"
                android:includeFontPadding="false"
                android:text="Create"
                android:textColor="@color/bottom_nav_text_color"
                android:textSize="12sp" />

        </LinearLayout>

        <!-- INBOX -->
        <LinearLayout
            android:id="@+id/navInbox"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@drawable/bg_bottom_nav_item"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:orientation="vertical">

            <ImageView
                android:id="@+id/iconInbox"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="Inbox"
                android:duplicateParentState="true"
                android:src="@drawable/ic_inbox_24"
                app:tint="@color/bottom_nav_icon_tint" />

            <TextView
                android:id="@+id/textInbox"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="3dp"
                android:duplicateParentState="true"
                android:fontFamily="sans"
                android:includeFontPadding="false"
                android:text="Inbox"
                android:textColor="@color/bottom_nav_text_color"
                android:textSize="12sp" />

        </LinearLayout>

        <!-- PROFILE -->
        <LinearLayout
            android:id="@+id/navProfile"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@drawable/bg_bottom_nav_item"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:orientation="vertical">

            <ImageView
                android:id="@+id/iconProfile"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="Profile"
                android:duplicateParentState="true"
                android:src="@drawable/ic_profile_24"
                app:tint="@color/bottom_nav_icon_tint" />

            <TextView
                android:id="@+id/textProfile"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="3dp"
                android:duplicateParentState="true"
                android:fontFamily="sans"
                android:includeFontPadding="false"
                android:text="Profile"
                android:textColor="@color/bottom_nav_text_color"
                android:textSize="12sp" />

        </LinearLayout>

    </LinearLayout>

</FrameLayout>
```

---

# 11. Put the navigation at the bottom of your screen

Example:

`res/layout/activity_main.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#FFFFFF">

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/mainFragmentContainer"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toTopOf="@id/customBottomNav" />

    <include
        android:id="@+id/customBottomNav"
        layout="@layout/view_compact_bottom_navigation"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

# 12. Kotlin navigation controller

In:

`MainActivity.kt`

```kotlin
package com.yourpackage.app

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navExplore: LinearLayout
    private lateinit var navCreate: LinearLayout
    private lateinit var navInbox: LinearLayout
    private lateinit var navProfile: LinearLayout

    private lateinit var navItems: List<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navHome = findViewById(R.id.navHome)
        navExplore = findViewById(R.id.navExplore)
        navCreate = findViewById(R.id.navCreate)
        navInbox = findViewById(R.id.navInbox)
        navProfile = findViewById(R.id.navProfile)

        navItems = listOf(
            navHome,
            navExplore,
            navCreate,
            navInbox,
            navProfile
        )

        navHome.setOnClickListener {
            selectNavigationItem(navHome)
            openFragment(HomeFragment())
        }

        navExplore.setOnClickListener {
            selectNavigationItem(navExplore)
            openFragment(ExploreFragment())
        }

        navCreate.setOnClickListener {
            selectNavigationItem(navCreate)
            openFragment(CreateFragment())
        }

        navInbox.setOnClickListener {
            selectNavigationItem(navInbox)
            openFragment(InboxFragment())
        }

        navProfile.setOnClickListener {
            selectNavigationItem(navProfile)
            openFragment(ProfileFragment())
        }

        if (savedInstanceState == null) {
            selectNavigationItem(navHome)
            openFragment(HomeFragment())
        }
    }

    private fun selectNavigationItem(selectedView: View) {

        navItems.forEach { item ->
            item.isSelected = item == selectedView

            /*
             * The TextView and ImageView inside each item use
             * duplicateParentState="true".
             *
             * Therefore:
             *
             * selected item:
             *   - rounded capsule appears
             *   - red icon
             *   - red bold label
             *
             * unselected item:
             *   - transparent background
             *   - black icon
             *   - black label
             */
        }

        updateTextWeights(selectedView)
    }

    private fun updateTextWeights(selectedView: View) {

        findViewById<android.widget.TextView>(R.id.textHome).setTypeface(
            null,
            if (selectedView.id == R.id.navHome)
                android.graphics.Typeface.BOLD
            else
                android.graphics.Typeface.NORMAL
        )

        findViewById<android.widget.TextView>(R.id.textExplore).setTypeface(
            null,
            if (selectedView.id == R.id.navExplore)
                android.graphics.Typeface.BOLD
            else
                android.graphics.Typeface.NORMAL
        )

        findViewById<android.widget.TextView>(R.id.textCreate).setTypeface(
            null,
            if (selectedView.id == R.id.navCreate)
                android.graphics.Typeface.BOLD
            else
                android.graphics.Typeface.NORMAL
        )

        findViewById<android.widget.TextView>(R.id.textInbox).setTypeface(
            null,
            if (selectedView.id == R.id.navInbox)
                android.graphics.Typeface.BOLD
            else
                android.graphics.Typeface.NORMAL
        )

        findViewById<android.widget.TextView>(R.id.textProfile).setTypeface(
            null,
            if (selectedView.id == R.id.navProfile)
                android.graphics.Typeface.BOLD
            else
                android.graphics.Typeface.NORMAL
        )
    }

    private fun openFragment(fragment: Fragment) {

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .commit()
    }
}
```

---

# 13. Recommended vector icons

Use clean **24dp outline/filled Material-style icons**.

Suggested mapping:

```text
Home    → home
Explore → explore / compass
Create  → add_box / add_circle
Inbox   → inbox / mail
Profile → person
```

Keep every icon at exactly:

```xml
android:layout_width="24dp"
android:layout_height="24dp"
```

Do **not** use different icon dimensions between tabs.

---

# 14. Example Home vector

Create:

`res/drawable/ic_home_24.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">

    <path
        android:fillColor="#000000"
        android:pathData="M12,3L2,12h3v9h6v-6h2v6h6v-9h3L12,3z" />

</vector>
```

The `app:tint` selector in the layout will override the vector's black fill automatically.

---

# 15. Example Explore vector

`res/drawable/ic_explore_24.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">

    <path
        android:fillColor="#000000"
        android:pathData="M12,2a10,10 0,1 0,0 20a10,10 0,0 0,0 -20zM14.6,14.6L7.4,16.6l2,-7.2l7.2,-2zM12,11a1,1 0,1 0,0 2a1,1 0,0 0,0 -2z" />

</vector>
```

---

# 16. Example Create vector

`res/drawable/ic_create_24.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">

    <path
        android:fillColor="#000000"
        android:pathData="M19,3H5a2,2 0,0 0,-2 2v14a2,2 0,0 0,2 2h14a2,2 0,0 0,2 -2V5a2,2 0,0 0,-2 -2zM17,13h-4v4h-2v-4H7v-2h4V7h2v4h4z" />

</vector>
```

---

# 17. Example Inbox vector

`res/drawable/ic_inbox_24.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">

    <path
        android:fillColor="#000000"
        android:pathData="M19,3H5a2,2 0,0 0,-2 2v14a2,2 0,0 0,2 2h14a2,2 0,0 0,2 -2V5a2,2 0,0 0,-2 -2zM19,13h-4a3,3 0,0 1,-6 0H5V5h14z" />

</vector>
```

---

# 18. Example Profile vector

`res/drawable/ic_profile_24.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">

    <path
        android:fillColor="#000000"
        android:pathData="M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z" />

</vector>
```

---

# 19. Optional subtle press animation

To make the navigation feel more premium without changing the Airtel-style appearance:

```kotlin
private fun animateTap(view: View) {

    view.animate()
        .scaleX(0.96f)
        .scaleY(0.96f)
        .setDuration(70)
        .withEndAction {
            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(100)
                .start()
        }
        .start()
}
```

Then:

```kotlin
navHome.setOnClickListener {
    animateTap(navHome)
    selectNavigationItem(navHome)
    openFragment(HomeFragment())
}
```

Repeat for the other tabs.

Do **not** add large bounce animations; Airtel's reference design feels restrained and compact.

---

# 20. Optional edge-to-edge handling

If your app uses edge-to-edge mode, keep the bar away from the gesture/navigation region:

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(
    findViewById(R.id.bottomNavWrapper)
) { view, insets ->

    val systemBars = insets.getInsets(
        WindowInsetsCompat.Type.systemBars()
    )

    view.setPadding(
        view.paddingLeft,
        view.paddingTop,
        view.paddingRight,
        systemBars.bottom + dpToPx(8)
    )

    insets
}
```

Helper:

```kotlin
private fun dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}
```

Imports:

```kotlin
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
```

---

# 21. Important visual rules

To keep the result close to the Airtel reference, **do not**:

```text
❌ Use the default 80–96dp Material NavigationBar styling
❌ Put the selected icon inside a small circle
❌ Use a large floating centre Create button
❌ Increase icon size beyond 24–25dp
❌ Add 10dp+ spacing between icon and text
❌ Add a dark/heavy shadow
❌ Give every item an individual visible border
❌ Make the bar full-bleed from left to right
❌ Use a square selected background
❌ Hide labels on inactive tabs
```

Instead:

```text
✅ 76dp compact outer pill
✅ 5dp inner padding
✅ Five equal-width tabs
✅ Full-height selected capsule
✅ Thin blue-grey stroke
✅ White/off-white surfaces
✅ 24dp icons
✅ 12sp labels
✅ 3dp icon/text spacing
✅ Selected red icon + red label
✅ Dark inactive icon + dark inactive label
```

---

# 22. Recommended final hierarchy

```text
Main screen
│
├── Fragment/content area
│
└── Bottom navigation
    │
    └── White rounded outer pill
        │
        ├── Home       ← selected capsule
        ├── Explore
        ├── Create
        ├── Inbox
        └── Profile
```

---

# 23. If you use Android Navigation Component

Instead of directly replacing fragments, your click listeners can call your `NavController`.

Example:

```kotlin
val navController = findNavController(R.id.mainFragmentContainer)

navHome.setOnClickListener {
    selectNavigationItem(navHome)
    navController.navigate(R.id.homeFragment)
}

navExplore.setOnClickListener {
    selectNavigationItem(navExplore)
    navController.navigate(R.id.exploreFragment)
}

navCreate.setOnClickListener {
    selectNavigationItem(navCreate)
    navController.navigate(R.id.createFragment)
}

navInbox.setOnClickListener {
    selectNavigationItem(navInbox)
    navController.navigate(R.id.inboxFragment)
}

navProfile.setOnClickListener {
    selectNavigationItem(navProfile)
    navController.navigate(R.id.profileFragment)
}
```

---

# 24. Final tuning values

Start with these exact numbers:

```text
bottom navigation width       = parent width - 32dp
bottom navigation height      = 76dp
horizontal outside margin     = 16dp
inner padding                 = 5dp
outer corner radius           = 38dp
selected corner radius        = 32dp
outer border                  = 1dp
selected border               = 1dp
icon                          = 24dp
icon-label gap                = 3dp
label                         = 12sp
bottom screen spacing         = 10dp + system navigation inset
```

If the result looks slightly taller on a particular phone because of its font scaling, reduce:

```text
76dp → 72dp
12sp → 11.5sp
```

Do **not** reduce the selected capsule independently. Keep its inset at approximately **5dp** so the proportions remain consistent.

---

# 25. Best implementation choice

For this particular design, a **custom XML navigation bar is better than Android's default `BottomNavigationView`** because the Airtel reference uses:

- a custom outer pill
- an almost full-height selected capsule
- unusually compact spacing
- its own border treatment
- equal custom item geometry

Trying to force standard `BottomNavigationView` into this shape usually introduces unwanted minimum heights, paddings, active indicators, and label behaviour.

Therefore the XML above is intentionally custom and gives you much tighter control over the final appearance.

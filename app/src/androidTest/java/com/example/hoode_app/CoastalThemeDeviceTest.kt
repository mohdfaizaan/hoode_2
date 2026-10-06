package com.example.hoode_app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.navigation.fragment.NavHostFragment
import com.example.hoode_app.data.repository.HoodeRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Read-only device coverage: no submissions, calls, account edits or messages are sent. */
@RunWith(AndroidJUnit4::class)
class CoastalThemeDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun residentRoutesAndSearch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            repeat(50) {
                if (!HoodeRepository.sessionReady.value) SystemClock.sleep(100)
            }
            assertTrue("Resident test needs the existing signed-in account", HoodeRepository.isLoggedIn())
            SystemClock.sleep(1200)
            val routes = listOf(
                "home" to R.id.homeFragment, "explore" to R.id.exploreFragment,
                "create" to R.id.createFragment, "inbox" to R.id.inboxFragment,
                "profile" to R.id.profileFragment, "settings" to R.id.settingsFragment,
                "edit-profile" to R.id.editProfileFragment, "gallery" to R.id.galleryFragment,
                "marketplace" to R.id.marketplaceFragment, "prayer" to R.id.prayerDetailFragment,
                "blood" to R.id.bloodNetworkFragment, "emergency" to R.id.emergencyFragment,
                "providers" to R.id.providersFragment, "news" to R.id.newsFragment,
                "events" to R.id.eventsFragment, "activities" to R.id.activitiesFragment,
                "jobs" to R.id.jobsFragment, "lost-found" to R.id.lostFoundFragment,
                "huffaz" to R.id.huffazFragment, "personality" to R.id.personalityDetailFragment,
                "education" to R.id.educationFragment, "polls" to R.id.pollsFragment,
                "tournaments" to R.id.tournamentsFragment, "calendar" to R.id.calendarFragment,
                "ramadan" to R.id.ramadanFragment, "badges" to R.id.badgesFragment,
                "notifications" to R.id.notificationCenterFragment
            )
            routes.forEach { (name, id) ->
                scenario.onActivity { activity ->
                    val nav = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
                    if (nav.currentDestination?.id != id) nav.navigate(id)
                }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(1000)
                scenario.onActivity { activity ->
                    val nav = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
                    assertEquals("Destination $name", id, nav.currentDestination?.id)
                    assertTrue("Keep Hoode in the foreground for screenshots", activity.hasWindowFocus())
                }
                capture(name)
            }
            scenario.onActivity { activity ->
                val nav = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
                nav.navigate(R.id.exploreFragment)
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(500)
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.et_explore_search).setText("no-such-service-xyz")
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.tvSearchEmpty).visibility)
                activity.findViewById<View>(R.id.btn_clear_search).performClick()
                assertEquals("", activity.findViewById<EditText>(R.id.et_explore_search).text.toString())
                activity.findViewById<View>(R.id.navHome).performClick()
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(700)
            scenario.onActivity { activity ->
                val nav = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
                assertEquals("Home tab must return to Home", R.id.homeFragment, nav.currentDestination?.id)
                activity.findViewById<View>(R.id.action_services).performClick()
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(400)
            scenario.onActivity { activity ->
                val nav = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
                assertEquals(R.id.providersFragment, nav.currentDestination?.id)
                nav.navigate(R.id.homeFragment)
            }
        }
    }

    private fun capture(name: String) {
        val context = instrumentation.targetContext
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val folder = File(context.getExternalFilesDir(null), "theme-verification").apply { mkdirs() }
        val scale = context.resources.configuration.fontScale.toString().replace('.', '-')
        File(folder, "$name-$scale.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        instrumentation.sendStatus(0, Bundle().apply { putString("stream", "Verified route: $name (font $scale)\n") })
    }
}

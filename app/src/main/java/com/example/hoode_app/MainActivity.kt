package com.example.hoode_app

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.ActivityMainBinding
import com.example.hoode_app.ui.common.CommunityFeaturesHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import coil.load

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navItems: List<LinearLayout>
    private lateinit var navController: NavController
    private var launchIntro: com.example.hoode_app.ui.common.LaunchIntro? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.root.visibility=View.INVISIBLE
        launchIntro = com.example.hoode_app.ui.common.LaunchIntro(this, savedInstanceState == null)
        lifecycleScope.launch {
            HoodeRepository.sessionReady.first{it}
            if(savedInstanceState==null) {
                val graph=navController.navInflater.inflate(R.navigation.nav_graph)
                graph.setStartDestination(if(HoodeRepository.isLoggedIn())R.id.homeFragment else R.id.signInFragment)
                navController.graph=graph
                val requested = intent.getStringExtra("resident_destination")
                val destination = if (HoodeRepository.isLoggedIn()) when (requested) {
                    "editProfileFragment" -> R.id.editProfileFragment
                    "settingsFragment" -> R.id.settingsFragment
                    else -> null
                } else if (requested == "signUpFragment") R.id.signUpFragment else null
                destination?.let { navController.navigate(it) }
            }
            setupNavigation();setupSideDrawer()
            launchIntro?.reveal(binding.root)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                HoodeRepository.sessionReady.first { it }
                while (true) {
                    HoodeRepository.syncWithCloud().join()
                    kotlinx.coroutines.delay(30_000)
                }
            }
        }
    }

    override fun onDestroy() {
        launchIntro?.remove()
        launchIntro = null
        super.onDestroy()
    }

    /**
     * Opens the animated side community drawer from the left.
     */
    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    fun openSideDrawer() = openDrawer()
    fun openRightDrawer() = openDrawer() // backward compatibility alias

    /**
     * Closes the side community drawer.
     */
    fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    fun closeSideDrawer() = closeDrawer()
    fun closeRightDrawer() = closeDrawer() // backward compatibility alias

    private fun setupSideDrawer() {
        val sideMenu = binding.layoutSideMenu

        // Close button
        sideMenu.btnCloseSideMenu.setOnClickListener {
            closeDrawer()
        }

        // Back press handling: close drawer if open
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    closeDrawer()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        // Drawer Lock Mode: locked closed so horizontal carousels in Home don't trigger it accidentally
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
            }

            override fun onDrawerClosed(drawerView: View) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
            }
        })

        // Live user info sync
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                HoodeRepository.currentUser.collect { user ->
                    sideMenu.tvMenuProfileName.text = user?.displayName ?: "Hoode Resident"
                    sideMenu.tvMenuProfileStatus.text = user?.locality?.takeIf { it.isNotBlank() } ?: "Hoode community"
                    sideMenu.itemMenuAdmin.visibility = View.GONE
                    sideMenu.ivMenuProfileAvatar.load(user?.profilePicUri) {
                        placeholder(R.drawable.profile_placeholder)
                        error(R.drawable.profile_placeholder)
                        fallback(R.drawable.profile_placeholder)
                    }
                }
            }
        }

        fun navigateFromMenu(@IdRes destinationId: Int) {
            closeDrawer()
            binding.drawerLayout.postDelayed({
                try {
                    navController.navigate(destinationId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 220)
        }

        // Click listeners for side menu items
        sideMenu.cardMenuProfile.setOnClickListener {
            navigateFromMenu(R.id.profileFragment)
        }
        sideMenu.itemMenuMosques.setOnClickListener {
            navigateFromMenu(R.id.prayerDetailFragment)
        }
        sideMenu.itemMenuBlood.setOnClickListener {
            navigateFromMenu(R.id.bloodNetworkFragment)
        }
        sideMenu.itemMenuEmergency.setOnClickListener {
            navigateFromMenu(R.id.emergencyFragment)
        }
        sideMenu.itemMenuNews.setOnClickListener {
            navigateFromMenu(R.id.newsFragment)
        }
        sideMenu.itemMenuCalendar.setOnClickListener {
            navigateFromMenu(R.id.calendarFragment)
        }
        sideMenu.itemMenuTournaments.setOnClickListener {
            navigateFromMenu(R.id.tournamentsFragment)
        }
        sideMenu.itemMenuJobs.setOnClickListener {
            navigateFromMenu(R.id.jobsFragment)
        }
        sideMenu.itemMenuLostFound.setOnClickListener {
            navigateFromMenu(R.id.lostFoundFragment)
        }
        sideMenu.itemMenuRamadan.setOnClickListener {
            navigateFromMenu(R.id.ramadanFragment)
        }
        sideMenu.itemMenuHuffaz.setOnClickListener {
            navigateFromMenu(R.id.huffazFragment)
        }
        sideMenu.itemMenuGallery.setOnClickListener {
            navigateFromMenu(R.id.galleryFragment)
        }
        sideMenu.itemMenuProfile.setOnClickListener {
            navigateFromMenu(R.id.profileFragment)
        }
        sideMenu.itemMenuNotifications.setOnClickListener {
            navigateFromMenu(R.id.notificationCenterFragment)
        }
        sideMenu.itemMenuAdmin.setOnClickListener {
            navigateFromMenu(R.id.adminDashboardFragment)
        }
        sideMenu.itemMenuDonate.setOnClickListener {
            closeDrawer()
            CommunityFeaturesHelper.showDonationDialog(this, layoutInflater)
        }
        sideMenu.itemMenuOurWork.setOnClickListener {
            closeDrawer()
            CommunityFeaturesHelper.showOurWorkDialog(this, layoutInflater)
        }
    }

    private fun setupNavigation() {
        val bottomNav = binding.customBottomNav

        fun openTab(destination: Int) {
            if (navController.currentDestination?.id == destination) return
            navController.navigate(destination, null, androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(destination != R.id.homeFragment)
                .setPopUpTo(R.id.homeFragment, false, true)
                .build())
        }

        navItems = listOf(
            bottomNav.navHome,
            bottomNav.navExplore,
            bottomNav.navCreate,
            bottomNav.navInbox,
            bottomNav.navProfile
        )

        bottomNav.navHome.setOnClickListener {
            openTab(R.id.homeFragment)
        }

        bottomNav.navExplore.setOnClickListener {
            openTab(R.id.exploreFragment)
        }

        bottomNav.navCreate.setOnClickListener {
            openTab(R.id.createFragment)
        }

        bottomNav.navInbox.setOnClickListener {
            openTab(R.id.inboxFragment)
        }

        bottomNav.navProfile.setOnClickListener {
            openTab(R.id.profileFragment)
        }

        // Show/hide bottom nav and sync selection based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            title = "${destination.label ?: "Hoode"} · Hoode"
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
                destination.id !in setOf(R.id.signInFragment, R.id.signUpFragment, R.id.welcomeFragment)
            when (destination.id) {
                R.id.homeFragment -> {
                    bottomNav.root.visibility = View.VISIBLE
                    selectNavigationItem(bottomNav.navHome)
                }
                R.id.exploreFragment -> {
                    bottomNav.root.visibility = View.VISIBLE
                    selectNavigationItem(bottomNav.navExplore)
                }
                R.id.createFragment -> {
                    bottomNav.root.visibility = View.VISIBLE
                    selectNavigationItem(bottomNav.navCreate)
                }
                R.id.inboxFragment -> {
                    bottomNav.root.visibility = View.VISIBLE
                    selectNavigationItem(bottomNav.navInbox)
                }
                R.id.profileFragment -> {
                    bottomNav.root.visibility = View.VISIBLE
                    selectNavigationItem(bottomNav.navProfile)
                }
                else -> {
                    bottomNav.root.visibility = View.GONE
                }
            }
        }
    }

    private fun selectNavigationItem(selectedView: View) {
        val bottomNav = binding.customBottomNav
        navItems.forEach { item ->
            item.isSelected = (item == selectedView)
        }

        bottomNav.textHome.setTypeface(null, if (selectedView.id == R.id.navHome) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        bottomNav.textExplore.setTypeface(null, if (selectedView.id == R.id.navExplore) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        bottomNav.textCreate.setTypeface(null, if (selectedView.id == R.id.navCreate) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        bottomNav.textInbox.setTypeface(null, if (selectedView.id == R.id.navInbox) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        bottomNav.textProfile.setTypeface(null, if (selectedView.id == R.id.navProfile) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }
}

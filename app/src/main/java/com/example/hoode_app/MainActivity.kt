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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navItems: List<LinearLayout>
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Dynamic start destination: if user is logged in, immediately open Home; otherwise Sign In
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(if (HoodeRepository.isLoggedIn()) R.id.homeFragment else R.id.signInFragment)
        navController.graph = graph

        setupNavigation()
        setupSideDrawer()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    HoodeRepository.syncWithCloud()
                    kotlinx.coroutines.delay(30_000)
                }
            }
        }
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
                    sideMenu.tvMenuProfileStatus.text = "Verified Resident • Ward 04"
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

        navItems = listOf(
            bottomNav.navHome,
            bottomNav.navExplore,
            bottomNav.navCreate,
            bottomNav.navInbox,
            bottomNav.navProfile
        )

        bottomNav.navHome.setOnClickListener {
            navController.navigate(R.id.homeFragment)
        }

        bottomNav.navExplore.setOnClickListener {
            navController.navigate(R.id.exploreFragment)
        }

        bottomNav.navCreate.setOnClickListener {
            navController.navigate(R.id.createFragment)
        }

        bottomNav.navInbox.setOnClickListener {
            navController.navigate(R.id.inboxFragment)
        }

        bottomNav.navProfile.setOnClickListener {
            navController.navigate(R.id.profileFragment)
        }

        // Show/hide bottom nav and sync selection based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
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
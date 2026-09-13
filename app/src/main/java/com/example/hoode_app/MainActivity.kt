package com.example.hoode_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.hoode_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Connect bottom navigation to navigation controller
        binding.bottomNavigation.setupWithNavController(navController)

        // Handle reselection (scroll to top)
        binding.bottomNavigation.setOnItemReselectedListener { /* scroll to top */ }

        // Show/hide bottom nav based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showNav = when (destination.id) {
                R.id.homeFragment,
                R.id.exploreFragment,
                R.id.createFragment,
                R.id.inboxFragment,
                R.id.profileFragment -> true
                else -> false
            }
            binding.bottomNavigation.visibility = if (showNav) android.view.View.VISIBLE else android.view.View.GONE
            binding.bottomNavDivider.visibility = if (showNav) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
}
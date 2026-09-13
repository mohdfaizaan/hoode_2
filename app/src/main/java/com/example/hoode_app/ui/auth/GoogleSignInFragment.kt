package com.example.hoode_app.ui.auth

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentGoogleSignInBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GoogleSignInFragment : Fragment() {

    private var _binding: FragmentGoogleSignInBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoogleSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.cardAccountFaizan.setOnClickListener {
            performGoogleSignIn("Faizan Ahmed", "faizan.ahmed@gmail.com", "approved_resident")
        }

        binding.cardAccountResident.setOnClickListener {
            performGoogleSignIn("Hoode Resident", "resident.hoode@gmail.com", "approved_resident")
        }

        binding.cardAccountAdmin.setOnClickListener {
            performGoogleSignIn("Community Admin", "admin.hoode@gmail.com", "community_admin")
        }

        binding.cardAccountCustom.setOnClickListener {
            showCustomGoogleAccountDialog()
        }
    }

    private fun performGoogleSignIn(name: String, email: String, role: String) {
        binding.layoutAccountsScroll.visibility = View.GONE
        binding.layoutLoading.visibility = View.VISIBLE
        binding.tvLoadingMessage.text = "Signing in with Google as $name..."

        viewLifecycleOwner.lifecycleScope.launch {
            delay(750) // Realistic Google OAuth token exchange animation
            HoodeRepository.signInWithGoogleAccount(name, email, role)
            Toast.makeText(requireContext(), "Signed in as $name via Google", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_googleSignIn_to_home)
        }
    }

    private fun showCustomGoogleAccountDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etName = EditText(context).apply {
            hint = "Google Account Name (e.g. Tariq Jamil)"
            setSingleLine()
        }
        val etEmail = EditText(context).apply {
            hint = "Google Email (e.g. tariq@gmail.com)"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine()
        }

        layout.addView(etName)
        layout.addView(etEmail)

        AlertDialog.Builder(context)
            .setTitle("Add Google Account")
            .setMessage("Enter the Google account credentials to sign in with:")
            .setView(layout)
            .setPositiveButton("Sign In") { _, _ ->
                val name = etName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, "Please enter a valid Google email address", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val finalName = if (name.isNotBlank()) name else email.substringBefore("@").replaceFirstChar { it.uppercase() }
                performGoogleSignIn(finalName, email, "approved_resident")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

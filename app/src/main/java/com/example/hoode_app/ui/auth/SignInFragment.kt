package com.example.hoode_app.ui.auth

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentSigninBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {

    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Status bar icons: white on top of blue/purple gradient
        activity?.window?.let { win ->
            WindowCompat.getInsetsController(win, win.decorView).isAppearanceLightStatusBars = false
        }

        // Auto-navigate to home if already logged in
        if (HoodeRepository.isLoggedIn()) {
            findNavController().navigate(R.id.action_signIn_to_home)
            return
        }

        // Back button
        binding.btnBack.setOnClickListener {
            if (!findNavController().navigateUp()) {
                activity?.finish()
            }
        }

        // Header "Get Started" button -> navigate to SignUp
        binding.tvGetStarted.setOnClickListener {
            findNavController().navigate(R.id.action_signIn_to_signUp)
        }

        // Submit Sign In
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isBlank()) {
                binding.etEmail.error = "Enter your email"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Please enter a valid email address"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isBlank()) {
                binding.etPassword.error = "Enter your password"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            binding.btnSignIn.isEnabled = false
            binding.btnSignIn.text = "Signing in..."

            viewLifecycleOwner.lifecycleScope.launch {
                val result = HoodeRepository.authenticateUser(email, password)
                binding.btnSignIn.isEnabled = true
                binding.btnSignIn.text = "Sign in"

                when (result) {
                    is HoodeRepository.AuthResult.Success -> {
                        val roleLabel = if (result.user.roles.contains("community_admin")) "Administrator" else "Resident"
                        Toast.makeText(
                            requireContext(),
                            "Welcome back, ${result.user.displayName}! ($roleLabel)",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(R.id.action_signIn_to_home)
                    }
                    is HoodeRepository.AuthResult.InvalidPassword -> {
                        binding.etPassword.error = result.message
                        binding.etPassword.requestFocus()
                    }
                    is HoodeRepository.AuthResult.UserNotFound -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Account Not Found")
                            .setMessage("No account found for $email.\n\nWould you like to create a new Hoode Connect account?")
                            .setPositiveButton("Create Account") { _, _ ->
                                findNavController().navigate(R.id.action_signIn_to_signUp)
                            }
                            .setNegativeButton("Try Again", null)
                            .show()
                    }
                }
            }
        }

        // Google Sign-In
        binding.btnGoogle.setOnClickListener {
            findNavController().navigate(R.id.action_signIn_to_googleSignIn)
        }

        // Forgot Password Dialog
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val context = requireContext()
        val currentEmail = binding.etEmail.text.toString().trim()
        val etInput = EditText(context).apply {
            hint = "Enter your registered email"
            if (currentEmail.isNotBlank()) setText(currentEmail)
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        AlertDialog.Builder(context)
            .setTitle("Reset Password")
            .setMessage("Enter your community email address to receive password recovery instructions:")
            .setView(etInput)
            .setPositiveButton("Reset Password") { _, _ ->
                val resetEmail = etInput.text.toString().trim()
                if (resetEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                    Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                Toast.makeText(context, "Password recovery instructions sent to $resetEmail", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.let { win ->
            WindowCompat.getInsetsController(win, win.decorView).isAppearanceLightStatusBars = true
        }
        _binding = null
    }
}

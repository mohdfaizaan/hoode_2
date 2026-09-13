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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentSigninBinding

class SignInFragment : Fragment() {

    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!

    private var isPasswordVisible = false

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

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Password Visibility Toggle
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }

        // Submit Sign In
        binding.btnSubmitSignin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isBlank()) {
                binding.etEmail.error = "Email address is required"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Please enter a valid email address"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isBlank()) {
                binding.etPassword.error = "Password is required"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            when (val result = HoodeRepository.authenticateUser(email, password)) {
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
                    binding.etPassword.error = "Incorrect password"
                    binding.etPassword.requestFocus()
                    Toast.makeText(requireContext(), "Incorrect password. Please verify and try again.", Toast.LENGTH_LONG).show()
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

        // Google Sign-In
        binding.btnGoogleSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_signIn_to_googleSignIn)
        }

        // Navigate to Register
        binding.btnGoToSignup.setOnClickListener {
            findNavController().navigate(R.id.action_signIn_to_signUp)
        }

        // Forgot Password Dialog
        binding.btnForgotPassword.setOnClickListener {
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

                if (HoodeRepository.isEmailRegistered(resetEmail)) {
                    HoodeRepository.resetPassword(resetEmail, "password123")
                    AlertDialog.Builder(context)
                        .setTitle("Password Reset Successful")
                        .setMessage("Your temporary password has been reset to: password123\n\nYou may now sign in with this password.")
                        .setPositiveButton("Use Password") { _, _ ->
                            binding.etEmail.setText(resetEmail)
                            binding.etPassword.setText("password123")
                        }
                        .show()
                } else {
                    Toast.makeText(context, "No registered account found with $resetEmail", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

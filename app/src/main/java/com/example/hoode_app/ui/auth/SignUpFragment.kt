package com.example.hoode_app.ui.auth

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentSignupBinding

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private var selectedWard = "Hoode"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Google Sign-Up shortcut
        binding.btnGoogleSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_signUp_to_googleSignIn)
        }

        // Ward Selection
        binding.btnSelectWard.setOnClickListener {
            val wards = arrayOf(
                "🌊 Hoode Community"
            )
            AlertDialog.Builder(requireContext())
                .setTitle("Select Your Community")
                .setItems(wards) { _, _ ->
                    binding.tvSelectedWard.text = "🌊 Hoode Community"
                    selectedWard = "Hoode"
                }
                .show()
        }

        // Toggle Password Visibility
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

        // Toggle Confirm Password Visibility
        binding.btnToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                binding.etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding.etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility)
            }
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text.length)
        }

        // Submit Sign Up
        binding.btnSubmitSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (name.length < 2) {
                binding.etName.error = "Please enter your full name"
                binding.etName.requestFocus()
                return@setOnClickListener
            }

            if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Please enter a valid email address"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (phone.isNotBlank() && phone.length != 10) {
                binding.etPhone.error = "Please enter a valid 10-digit phone number"
                binding.etPhone.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.etPassword.error = "Password must be at least 6 characters"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.etConfirmPassword.error = "Passwords do not match"
                binding.etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (!binding.cbAgreeTerms.isChecked) {
                Toast.makeText(requireContext(), "Please agree to the Community Guidelines & Terms", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = HoodeRepository.registerUser(
                name = name,
                email = email,
                password = password,
                phone = phone,
                ward = selectedWard
            )

            result.onSuccess { user ->
                Toast.makeText(
                    requireContext(),
                    "Welcome to Hoode Connect, ${user.displayName}!",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigate(R.id.action_signUp_to_home)
            }.onFailure { exception ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Registration Notice")
                    .setMessage(exception.message ?: "Could not complete registration.")
                    .setPositiveButton("Sign In") { _, _ ->
                        findNavController().navigate(R.id.action_signUp_to_signIn)
                    }
                    .setNegativeButton("Try with another email", null)
                    .show()
            }
        }

        // Navigate to Sign In
        binding.btnGoToSignin.setOnClickListener {
            findNavController().navigate(R.id.action_signUp_to_signIn)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

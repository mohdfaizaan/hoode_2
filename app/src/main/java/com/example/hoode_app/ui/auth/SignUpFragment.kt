package com.example.hoode_app.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentSignupBinding
import kotlinx.coroutines.launch

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

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

        // Status bar icons: white on top of blue/purple gradient
        activity?.window?.let { win ->
            WindowCompat.getInsetsController(win, win.decorView).isAppearanceLightStatusBars = false
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Header "Sign in" button -> navigate back to SignIn
        binding.tvSignIn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (email.isEmpty()) {
                binding.etEmail.error = "Enter your email"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Please enter a valid email address"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (name.isEmpty()) {
                binding.etName.error = "Enter your full name"
                binding.etName.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.etPassword.error = "Password must contain at least 6 characters"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.etConfirmPassword.error = "Passwords do not match"
                binding.etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            binding.btnSignUp.isEnabled = false
            binding.btnSignUp.text = "Creating account..."

            viewLifecycleOwner.lifecycleScope.launch {
                val result = HoodeRepository.registerUser(name, email, password)
                binding.btnSignUp.isEnabled = true
                binding.btnSignUp.text = "Sign up"

                if (result.isSuccess) {
                    Toast.makeText(
                        requireContext(),
                        "Account created! Welcome to Hoode Connect.",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.action_signUp_to_home)
                } else {
                    Toast.makeText(
                        requireContext(),
                        result.exceptionOrNull()?.message ?: "Registration failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.btnGoogle.setOnClickListener {
            findNavController().navigate(R.id.action_signUp_to_googleSignIn)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.let { win ->
            WindowCompat.getInsetsController(win, win.decorView).isAppearanceLightStatusBars = true
        }
        _binding = null
    }
}

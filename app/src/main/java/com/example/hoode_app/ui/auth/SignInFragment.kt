package com.example.hoode_app.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentSigninBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {
    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (HoodeRepository.isLoggedIn()) {
            findNavController().navigate(R.id.action_signIn_to_home)
            return
        }
        binding.btnBack.setOnClickListener {
            if (!findNavController().navigateUp()) activity?.finish()
        }
        binding.tvGetStarted.setOnClickListener { findNavController().navigate(R.id.action_signIn_to_signUp) }
        binding.tvForgotPassword.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign-in help")
                .setMessage("Use the email address and password you registered with. If you need to reset your password, contact your Hoode community administrator. Password recovery inside the app is not available yet.")
                .setPositiveButton("Got it", null).show()
        }
        binding.btnSignIn.setOnClickListener { signIn() }
        binding.btnGuestLogin.setOnClickListener { enterGuest() }
        binding.etPassword.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        binding.etPassword.setOnEditorActionListener { _, action, _ ->
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) { signIn(); true } else false
        }
    }

    private fun enterGuest() {
        HoodeRepository.loginAsGuest()
        findNavController().navigate(R.id.action_signIn_to_home)
    }

    private fun signIn() {
        if (!binding.btnSignIn.isEnabled) return
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        binding.tvAuthError.visibility = View.GONE

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email address"
            binding.etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Enter your password"
            binding.etPassword.requestFocus()
            return
        }

        binding.btnSignIn.isEnabled = false
        binding.tvGetStarted.isEnabled = false
        binding.btnSignIn.text = "Signing in…"
        viewLifecycleOwner.lifecycleScope.launch {
            val result = HoodeRepository.authenticateUser(email, password)
            val current = _binding ?: return@launch
            current.btnSignIn.isEnabled = true
            current.tvGetStarted.isEnabled = true
            current.btnSignIn.text = "Sign in"
            when (result) {
                is HoodeRepository.AuthResult.Success -> findNavController().navigate(R.id.action_signIn_to_home)
                else -> {
                    current.tvAuthError.text = when (result) {
                        is HoodeRepository.AuthResult.InvalidPassword -> result.message
                        is HoodeRepository.AuthResult.UserNotFound -> result.message
                        else -> "Check your email and password and try again."
                    }
                    current.tvAuthError.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

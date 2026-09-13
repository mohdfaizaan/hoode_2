package com.example.hoode_app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentWelcomeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.currentUser.collectLatest { user ->
                if (user != null) {
                    findNavController().navigate(R.id.action_welcome_to_home)
                }
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_googleSignIn)
        }

        binding.btnSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_signIn)
        }

        binding.btnSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_signUp)
        }

        binding.btnGuest.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

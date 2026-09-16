package com.example.hoode_app.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private var selectedAvatarUri: String? = null

    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedAvatarUri = it.toString()
            binding.ivEditAvatar.load(it) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.bg_circle_teal)
                error(R.drawable.bg_circle_teal)
            }
            Toast.makeText(requireContext(), "Profile picture selected!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        val user = HoodeRepository.currentUser.value

        // Pre-fill fields
        user?.let {
            binding.etDisplayName.setText(it.displayName)
            binding.etPhone.setText(it.phone ?: "")
            binding.etBloodGroup.setText(it.bloodGroup ?: "")
            binding.etAge.setText(it.age ?: "")
            binding.etProfession.setText(it.profession ?: "")
            binding.etFatherName.setText(it.fatherName ?: "")
            binding.etDob.setText(it.dob ?: "")
            binding.etLocality.setText(if (!it.locality.isNullOrBlank()) it.locality else "Hoode")

            val avatar = it.profilePicUri
            if (!avatar.isNullOrBlank()) {
                selectedAvatarUri = avatar
                binding.ivEditAvatar.load(avatar) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.bg_circle_teal)
                    error(R.drawable.bg_circle_teal)
                }
            }
        }

        val avatarClick = View.OnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.ivEditAvatar.setOnClickListener(avatarClick)
        binding.btnChangePhoto.setOnClickListener(avatarClick)

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etDisplayName.text.toString().trim()
            if (name.isBlank()) {
                binding.tilDisplayName.error = "Please enter your name"
                return@setOnClickListener
            }
            binding.tilDisplayName.error = null

            val phone = binding.etPhone.text.toString().trim()
            val bloodGroup = binding.etBloodGroup.text.toString().trim()
            val age = binding.etAge.text.toString().trim()
            val profession = binding.etProfession.text.toString().trim()
            val fatherName = binding.etFatherName.text.toString().trim()
            val dob = binding.etDob.text.toString().trim()
            val locality = binding.etLocality.text.toString().trim().ifBlank { "Hoode" }

            HoodeRepository.updateUserProfile(
                displayName = name,
                phone = phone,
                age = age,
                dob = dob,
                fatherName = fatherName,
                bloodGroup = bloodGroup,
                profession = profession,
                locality = locality,
                profilePicUri = selectedAvatarUri
            )

            Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

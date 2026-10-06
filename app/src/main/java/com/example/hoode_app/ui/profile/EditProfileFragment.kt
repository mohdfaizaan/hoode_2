package com.example.hoode_app.ui.profile

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.hoode_app.R
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentEditProfileBinding
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private var selectedAvatarUri: String? = null
    private var selectedCoverUri: String? = null
    private var selectedLocality: String = "Hoode"
    private var selectedBloodGroup: String = ""
    private var selectedProfession: String = ""

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedAvatarUri = it.toString()
            binding.ivEditProfile.load(it) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.profile_placeholder)
                error(R.drawable.profile_placeholder)
            }
            Toast.makeText(requireContext(), "Profile picture selected", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickCoverLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedCoverUri = it.toString()
            binding.ivEditCover.load(it) {
                crossfade(true)
                placeholder(R.drawable.profile_cover_placeholder)
                error(R.drawable.profile_cover_placeholder)
            }
            Toast.makeText(requireContext(), "Cover photo selected", Toast.LENGTH_SHORT).show()
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

        // Photo pickers
        val avatarClick = View.OnClickListener {
            pickAvatarLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.ivEditProfile.setOnClickListener(avatarClick)
        binding.btnChangeAvatar.setOnClickListener(avatarClick)

        val coverClick = View.OnClickListener {
            pickCoverLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.ivEditCover.setOnClickListener(coverClick)
        binding.btnEditCover.setOnClickListener(coverClick)

        // Pre-fill user information
        val user = HoodeRepository.currentUser.value
        user?.let { u ->
            binding.tvHeaderName.text = u.displayName
            binding.etFullName.setText(u.displayName)
            binding.etUsername.setText(u.username ?: u.displayName.lowercase().replace(" ", ""))
            binding.etEmail.setText(u.email)
            binding.etPhone.setText(u.phone ?: "")
            binding.etBio.setText(u.bio ?: "Connecting with the Hoode community.")
            binding.etFatherName.setText(u.fatherName ?: "")

            if (!u.locality.isNullOrBlank()) {
                selectedLocality = u.locality
                binding.tvLocalityValue.text = u.locality
            }

            if (!u.bloodGroup.isNullOrBlank()) {
                selectedBloodGroup = u.bloodGroup
                binding.tvBloodGroupValue.text = u.bloodGroup
            }

            if (!u.profession.isNullOrBlank()) {
                selectedProfession = u.profession
                binding.tvProfessionValue.text = u.profession
            }

            if (!u.profilePicUri.isNullOrBlank()) {
                selectedAvatarUri = u.profilePicUri
                binding.ivEditProfile.load(u.profilePicUri) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.profile_placeholder)
                    error(R.drawable.profile_placeholder)
                }
            }

            if (!u.coverPicUri.isNullOrBlank()) {
                selectedCoverUri = u.coverPicUri
                binding.ivEditCover.load(u.coverPicUri) {
                    crossfade(true)
                    placeholder(R.drawable.profile_cover_placeholder)
                    error(R.drawable.profile_cover_placeholder)
                }
            }
        }

        // Option 1: Locality / Ward Selector
        val localities = arrayOf("Hoode", "Bengre Road", "Beach Road", "Kodi Junction", "Maviya", "Thottam", "Kemmanu", "Gujjadi")
        binding.btnSelectLocality.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Locality / Ward")
                .setItems(localities) { _, which ->
                    selectedLocality = localities[which]
                    binding.tvLocalityValue.text = selectedLocality
                }
                .show()
        }

        // Option 2: Blood Group Selector
        val bloodGroups = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        binding.btnSelectBloodGroup.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Blood Group")
                .setItems(bloodGroups) { _, which ->
                    selectedBloodGroup = bloodGroups[which]
                    binding.tvBloodGroupValue.text = selectedBloodGroup
                }
                .show()
        }

        // Option 3: Profession Selector
        val professions = arrayOf(
            "Student",
            "Business / Shop Owner",
            "Software Engineer / IT",
            "Educator / Teacher",
            "Healthcare / Medical",
            "Driver / Transport",
            "Fisheries / Marine",
            "Gulf / NRI",
            "Homemaker",
            "Other Profession"
        )
        binding.btnSelectProfession.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Profession")
                .setItems(professions) { _, which ->
                    selectedProfession = professions[which]
                    binding.tvProfessionValue.text = selectedProfession
                }
                .show()
        }

        // Save Changes
        binding.btnSaveChanges.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()
            val fatherName = binding.etFatherName.text.toString().trim()

            if (name.isBlank()) {
                binding.etFullName.error = "Please enter your name"
                binding.etFullName.requestFocus()
                return@setOnClickListener
            }

            binding.btnSaveChanges.isEnabled = false
            binding.btnSaveChanges.text = "Saving..."

            viewLifecycleOwner.lifecycleScope.launch {
                val result = HoodeRepository.updateUserProfile(
                    displayName = name,
                    phone = phone,
                    locality = selectedLocality,
                    bloodGroup = selectedBloodGroup,
                    profession = selectedProfession,
                    fatherName = fatherName,
                    profilePicUri = selectedAvatarUri,
                    username = username,
                    bio = bio,
                    coverPicUri = selectedCoverUri
                )

                binding.btnSaveChanges.isEnabled = true
                binding.btnSaveChanges.text = "Save Changes"

                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(
                        requireContext(),
                        result.exceptionOrNull()?.message ?: "Update failed. Please retry.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

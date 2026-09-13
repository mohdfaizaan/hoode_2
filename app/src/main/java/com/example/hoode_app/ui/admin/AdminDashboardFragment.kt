package com.example.hoode_app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.model.GalleryItem
import com.example.hoode_app.data.model.HuffazProfile
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentAdminDashboardBinding

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.cardAdminCarousel.setOnClickListener {
            findNavController().navigate(R.id.action_admin_to_carousel)
        }

        binding.cardAdminPrayer.setOnClickListener {
            findNavController().navigate(R.id.action_admin_to_prayer)
        }

        binding.cardAdminGallery.setOnClickListener {
            showAddGalleryDialog()
        }

        binding.cardAdminPersonality.setOnClickListener {
            showEditPersonalityDialog()
        }

        binding.cardAdminHuffaz.setOnClickListener {
            showAddHuffazDialog()
        }
    }

    private fun showAddGalleryDialog() {
        val currentCount = HoodeRepository.galleryItems.value.size
        if (currentCount >= 25) {
            AlertDialog.Builder(requireContext())
                .setTitle("Gallery Cap Reached (25/25)")
                .setMessage("Per specification F17, the gallery has an enforced maximum cap of 25 active images. To add a new image, please replace or archive an existing photo.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val input = EditText(requireContext()).apply { hint = "Photo Title (e.g. Bengre Sunrise)" }
        AlertDialog.Builder(requireContext())
            .setTitle("Add Photo to Hoode Gallery ($currentCount/25)")
            .setView(input)
            .setPositiveButton("Add Photo") { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotBlank()) {
                    val newItem = GalleryItem(
                        title = title,
                        caption = "Community uploaded photo.",
                        photographer = "Admin Uploader",
                        sortOrder = currentCount + 1
                    )
                    HoodeRepository.addGalleryItem(newItem)
                    Toast.makeText(requireContext(), "Photo added to gallery ($currentCount+1/25)!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditPersonalityDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val etName = EditText(requireContext()).apply {
            hint = "Person Name"
            setText(HoodeRepository.dailyPersonality.value.name)
        }
        val etRole = EditText(requireContext()).apply {
            hint = "Role / Title"
            setText(HoodeRepository.dailyPersonality.value.role)
        }
        val etQuote = EditText(requireContext()).apply {
            hint = "Quote"
            setText(HoodeRepository.dailyPersonality.value.quote)
        }
        layout.addView(etName)
        layout.addView(etRole)
        layout.addView(etQuote)

        AlertDialog.Builder(requireContext())
            .setTitle("Update Daily Personality")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val role = etRole.text.toString().trim()
                val quote = etQuote.text.toString().trim()

                if (name.isNotBlank() && role.isNotBlank()) {
                    val updated = HoodeRepository.dailyPersonality.value.copy(
                        name = name,
                        role = role,
                        quote = quote
                    )
                    HoodeRepository.updateDailyPersonality(updated)
                    Toast.makeText(requireContext(), "Daily personality updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddHuffazDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val etName = EditText(requireContext()).apply { hint = "Hafiz Full Name" }
        val etYear = EditText(requireContext()).apply { hint = "Completion Year (e.g. 2026)" }
        val etInst = EditText(requireContext()).apply { hint = "Institution / Madrasa" }
        val etTeacher = EditText(requireContext()).apply { hint = "Teacher / Ustad" }
        layout.addView(etName)
        layout.addView(etYear)
        layout.addView(etInst)
        layout.addView(etTeacher)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Consented Huffaz Record")
            .setMessage("Verify publication consent before adding.")
            .setView(layout)
            .setPositiveButton("Publish Record") { _, _ ->
                val name = etName.text.toString().trim()
                val year = etYear.text.toString().trim()
                val inst = etInst.text.toString().trim()
                val teacher = etTeacher.text.toString().trim()

                if (name.isNotBlank() && year.isNotBlank()) {
                    HoodeRepository.addHuffazProfile(
                        HuffazProfile(
                            name = name,
                            completionYear = year,
                            institution = if (inst.isNotBlank()) inst else "Hoode Madrasa",
                            teacher = if (teacher.isNotBlank()) teacher else "Community Qari",
                            biography = "Community Qur'an Hafiz."
                        )
                    )
                    Toast.makeText(requireContext(), "Huffaz record published!", Toast.LENGTH_SHORT).show()
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

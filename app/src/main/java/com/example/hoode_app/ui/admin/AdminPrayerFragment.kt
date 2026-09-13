package com.example.hoode_app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.data.model.PrayerTiming
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentAdminPrayerBinding

class AdminPrayerFragment : Fragment() {

    private var _binding: FragmentAdminPrayerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPrayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        val current = HoodeRepository.prayerTimings.value
        binding.etFajrAdhan.setText(current.find { it.name == "Fajr" }?.adhanTime ?: "5:12 AM")
        binding.etFajrIqamah.setText(current.find { it.name == "Fajr" }?.iqamahTime ?: "5:30 AM")

        binding.etDhuhrAdhan.setText(current.find { it.name == "Dhuhr" }?.adhanTime ?: "12:32 PM")
        binding.etDhuhrIqamah.setText(current.find { it.name == "Dhuhr" }?.iqamahTime ?: "12:45 PM")

        binding.etAsrAdhan.setText(current.find { it.name == "Asr" }?.adhanTime ?: "3:56 PM")
        binding.etAsrIqamah.setText(current.find { it.name == "Asr" }?.iqamahTime ?: "4:15 PM")

        binding.etMaghribAdhan.setText(current.find { it.name == "Maghrib" }?.adhanTime ?: "6:38 PM")
        binding.etMaghribIqamah.setText(current.find { it.name == "Maghrib" }?.iqamahTime ?: "6:42 PM")

        binding.etIshaAdhan.setText(current.find { it.name == "Isha" }?.adhanTime ?: "7:52 PM")
        binding.etIshaIqamah.setText(current.find { it.name == "Isha" }?.iqamahTime ?: "8:15 PM")

        binding.btnSavePrayer.setOnClickListener {
            val updated = listOf(
                PrayerTiming("Fajr", binding.etFajrAdhan.text.toString(), binding.etFajrIqamah.text.toString()),
                PrayerTiming("Sunrise", "6:14 AM", "—"),
                PrayerTiming("Dhuhr", binding.etDhuhrAdhan.text.toString(), binding.etDhuhrIqamah.text.toString()),
                PrayerTiming("Asr", binding.etAsrAdhan.text.toString(), binding.etAsrIqamah.text.toString()),
                PrayerTiming("Maghrib", binding.etMaghribAdhan.text.toString(), binding.etMaghribIqamah.text.toString(), isNext = true, timeRemaining = "Next"),
                PrayerTiming("Isha", binding.etIshaAdhan.text.toString(), binding.etIshaIqamah.text.toString())
            )

            HoodeRepository.updatePrayerTimings(updated)
            Toast.makeText(requireContext(), "Timetable published! Updated across all resident devices.", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

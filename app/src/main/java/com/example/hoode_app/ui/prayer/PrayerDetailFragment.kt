package com.example.hoode_app.ui.prayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hoode_app.R
import com.example.hoode_app.data.model.Mosque
import com.example.hoode_app.data.model.PrayerTiming
import com.example.hoode_app.data.repository.HoodeRepository
import com.example.hoode_app.databinding.FragmentPrayerDetailBinding
import com.example.hoode_app.databinding.ItemPrayerRowBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PrayerDetailFragment : Fragment() {

    private var _binding: FragmentPrayerDetailBinding? = null
    private val binding get() = _binding!!

    private var selectedMosqueId = "mosque_01"

    // Prayer icon drawables mapped by prayer name
    private val prayerIcons = mapOf(
        "Fajr" to R.drawable.ic_prayer,
        "Dhuhr" to R.drawable.ic_prayer,
        "Asr" to R.drawable.ic_prayer,
        "Maghrib" to R.drawable.ic_prayer,
        "Isha" to R.drawable.ic_prayer
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrayerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Navigation ──
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // ── Switch Mosque — Premium Bottom Sheet ──
        binding.btnChangeMosque.setOnClickListener {
            showMosqueSelectorBottomSheet()
        }

        // ── View Timetable (scrolls to timetable section) ──
        binding.btnViewTimetable.setOnClickListener {
            // Smooth scroll to the timetable section
            val scrollView = binding.root.findViewById<android.widget.ScrollView>(
                binding.llTimingsContainer.id
            )?.parent?.parent as? android.widget.ScrollView
            scrollView?.let {
                val y = binding.llTimingsContainer.top
                it.smoothScrollTo(0, y)
            }
        }

        // ── Manage Reminders ──
        binding.btnManageReminders.setOnClickListener {
            showReminderSettingsDialog()
        }

        // ── Prayer Timings Data ──
        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.prayerTimings.collectLatest { timings ->
                updatePrayerUI(timings)
            }
        }
    }

    private fun updatePrayerUI(timings: List<PrayerTiming>) {
        // ── Build 5-Column Prayer Strip ──
        binding.llPrayerStrip.removeAllViews()
        for (timing in timings) {
            val colView = layoutInflater.inflate(
                R.layout.item_prayer_time_column,
                binding.llPrayerStrip,
                false
            )

            val tvName = colView.findViewById<TextView>(R.id.tv_prayer_name)
            val tvTime = colView.findViewById<TextView>(R.id.tv_prayer_time)
            val ivIcon = colView.findViewById<ImageView>(R.id.iv_prayer_icon)
            val dotView = colView.findViewById<View>(R.id.v_active_dot)

            tvName.text = timing.name
            tvTime.text = timing.adhanTime

            if (timing.isNext) {
                // Active state — mint highlight
                colView.setBackgroundResource(R.drawable.bg_prayer_time_active)
                tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_ink))
                tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_dark))
                ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_dark))
                dotView.isVisible = true
            } else {
                colView.setBackgroundResource(R.drawable.bg_prayer_time_inactive)
                tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_muted))
                dotView.isVisible = false
            }

            binding.llPrayerStrip.addView(colView)
        }

        // ── Build Timetable Rows ──
        binding.llTimingsContainer.removeAllViews()
        for (timing in timings) {
            val rowBinding = ItemPrayerRowBinding.inflate(layoutInflater, binding.llTimingsContainer, false)
            rowBinding.tvRowPrayerName.text = timing.name
            rowBinding.tvRowAdhan.text = timing.adhanTime
            rowBinding.tvRowIqamah.text = timing.iqamahTime

            if (timing.isNext) {
                // Hero card updates
                binding.tvNextPrayerName.text = timing.name
                binding.tvHeroIqamah.text = "Iqamah ${timing.iqamahTime}"
                binding.tvCountdownText.text = timing.timeRemaining

                // Active timetable row — mint highlighted
                rowBinding.rowContainer.setBackgroundResource(R.drawable.bg_timetable_row_active)
                rowBinding.tvRowPrayerName.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent_ink)
                )
                rowBinding.tvRowAdhan.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent_dark)
                )
                rowBinding.tvRowIqamah.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent_ink)
                )
                rowBinding.ivRowIcon.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.accent_dark)
                )
            }

            binding.llTimingsContainer.addView(rowBinding.root)
        }
    }

    // ═══════════════════════════════════════════════════════
    // Premium Mosque Selector — Bottom Sheet Dialog
    // ═══════════════════════════════════════════════════════
    private fun showMosqueSelectorBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.dialog_mosque_selector, null)
        dialog.setContentView(sheetView)

        val container = sheetView.findViewById<LinearLayout>(R.id.ll_mosque_list)
        container.removeAllViews()

        for (mosque in HoodeRepository.mosques) {
            val itemView = createMosqueItemView(mosque, mosque.id == selectedMosqueId) {
                selectedMosqueId = mosque.id
                binding.tvMosqueTitle.text = mosque.name
                binding.tvMosqueLocation.text = "${mosque.location} · IST"
                Toast.makeText(requireContext(), "Selected ${mosque.name}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            container.addView(itemView)
        }

        dialog.show()
    }

    private fun createMosqueItemView(
        mosque: Mosque,
        isSelected: Boolean,
        onClick: () -> Unit
    ): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val bgRes = if (isSelected) R.drawable.bg_mosque_item_selected else R.drawable.bg_mosque_item
            setBackgroundResource(bgRes)
            val dp16 = (16 * resources.displayMetrics.density).toInt()
            val dp14 = (14 * resources.displayMetrics.density).toInt()
            setPadding(dp16, dp14, dp16, dp14)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = (8 * resources.displayMetrics.density).toInt()
            layoutParams = params
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        // Mosque icon
        val icon = ImageView(requireContext()).apply {
            val dp24 = (24 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(dp24, dp24).apply {
                marginEnd = (12 * resources.displayMetrics.density).toInt()
            }
            setImageResource(R.drawable.ic_prayer)
            val tintColor = if (isSelected) R.color.accent_dark else R.color.text_muted
            setColorFilter(ContextCompat.getColor(requireContext(), tintColor))
        }

        // Text container
        val textContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvName = TextView(requireContext()).apply {
            text = mosque.name
            val nameColor = if (isSelected) R.color.accent_ink else R.color.text_primary
            setTextColor(ContextCompat.getColor(requireContext(), nameColor))
            textSize = 15f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }

        val tvLocation = TextView(requireContext()).apply {
            text = mosque.location
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            textSize = 12f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = (2 * resources.displayMetrics.density).toInt()
            layoutParams = params
        }

        textContainer.addView(tvName)
        textContainer.addView(tvLocation)

        // Check mark for selected
        if (isSelected) {
            val check = ImageView(requireContext()).apply {
                val dp20 = (20 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(dp20, dp20)
                setImageResource(R.drawable.ic_check)
                setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_dark))
            }
            row.addView(icon)
            row.addView(textContainer)
            row.addView(check)
        } else {
            row.addView(icon)
            row.addView(textContainer)
        }

        return row
    }

    // ═══════════════════════════════════════════════════════
    // Premium Reminder Settings Dialog
    // ═══════════════════════════════════════════════════════
    private fun showReminderSettingsDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.dialog_reminder_settings, null)
        dialog.setContentView(sheetView)

        val btnSave = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save_reminders)
        btnSave.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Prayer reminders updated for all 5 daily prayers.",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

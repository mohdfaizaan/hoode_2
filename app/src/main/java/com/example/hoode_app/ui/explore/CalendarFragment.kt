package com.example.hoode_app.ui.explore

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hoode_app.R
import com.example.hoode_app.data.model.CalendarEvent
import com.example.hoode_app.data.repository.HoodeRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private lateinit var tvMonthYear: TextView
    private lateinit var rvDaysGrid: RecyclerView
    private lateinit var tvSelectedDateHeader: TextView
    private lateinit var tvEventsCount: TextView
    private lateinit var rvDayEvents: RecyclerView
    private lateinit var layoutNoEvents: LinearLayout

    private var displayedCalendar: Calendar = Calendar.getInstance()
    private var selectedCalendar: Calendar = Calendar.getInstance()
    private var eventsList: List<CalendarEvent> = emptyList()

    private val monthNames = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)

        tvMonthYear = view.findViewById(R.id.tv_month_year)
        rvDaysGrid = view.findViewById(R.id.rv_days_grid)
        tvSelectedDateHeader = view.findViewById(R.id.tv_selected_date_header)
        tvEventsCount = view.findViewById(R.id.tv_events_count)
        rvDayEvents = view.findViewById(R.id.rv_day_events)
        layoutNoEvents = view.findViewById(R.id.layout_no_events)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<MaterialButton>(R.id.btn_today).setOnClickListener {
            displayedCalendar = Calendar.getInstance()
            selectedCalendar = Calendar.getInstance()
            renderCalendar()
            renderSelectedDayEvents()
        }

        view.findViewById<ImageButton>(R.id.btn_prev_month).setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, -1)
            renderCalendar()
        }

        view.findViewById<ImageButton>(R.id.btn_next_month).setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, 1)
            renderCalendar()
        }

        rvDaysGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        rvDayEvents.layoutManager = LinearLayoutManager(requireContext())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.calendarEvents.collectLatest { events ->
                eventsList = events
                renderCalendar()
                renderSelectedDayEvents()
            }
        }
    }

    private fun renderCalendar() {
        val year = displayedCalendar.get(Calendar.YEAR)
        val month = displayedCalendar.get(Calendar.MONTH)
        tvMonthYear.text = "${monthNames[month]} $year"

        val tempCal = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday

        val days = mutableListOf<DayCell>()
        for (i in 0 until firstDayOfWeek) {
            days.add(DayCell(dayNumber = 0, month = month, year = year, isEmpty = true))
        }
        for (i in 1..daysInMonth) {
            days.add(DayCell(dayNumber = i, month = month, year = year, isEmpty = false))
        }

        rvDaysGrid.adapter = DaysGridAdapter(days)
    }

    private fun renderSelectedDayEvents() {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH)
        tvSelectedDateHeader.text = dateFormat.format(selectedCalendar.time)

        val queryDateStr = String.format(
            Locale.US,
            "%04d-%02d-%02d",
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH) + 1,
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        )

        val dayEvents = eventsList.filter { it.dateStr == queryDateStr }

        if (dayEvents.isEmpty()) {
            rvDayEvents.visibility = View.GONE
            layoutNoEvents.visibility = View.VISIBLE
            tvEventsCount.text = "0 events"
        } else {
            rvDayEvents.visibility = View.VISIBLE
            layoutNoEvents.visibility = View.GONE
            tvEventsCount.text = "${dayEvents.size} event${if (dayEvents.size > 1) "s" else ""}"
            rvDayEvents.adapter = DayEventsAdapter(dayEvents)
        }
    }

    // ── Day Cell Model & Adapter ─────────────────────────────────────────────

    data class DayCell(
        val dayNumber: Int,
        val month: Int,
        val year: Int,
        val isEmpty: Boolean
    )

    inner class DaysGridAdapter(
        private val days: List<DayCell>
    ) : RecyclerView.Adapter<DaysGridAdapter.DayViewHolder>() {

        inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val flDayBackground: FrameLayout = view.findViewById(R.id.fl_day_background)
            val tvDayNumber: TextView = view.findViewById(R.id.tv_day_number)
            val viewEventDot: View = view.findViewById(R.id.view_event_dot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view = layoutInflater.inflate(R.layout.item_calendar_day, parent, false)
            return DayViewHolder(view)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val cell = days[position]

            if (cell.isEmpty) {
                holder.tvDayNumber.text = ""
                holder.flDayBackground.background = null
                holder.viewEventDot.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
                return
            }

            holder.tvDayNumber.text = cell.dayNumber.toString()

            val todayCal = Calendar.getInstance()
            val isToday = cell.year == todayCal.get(Calendar.YEAR) &&
                    cell.month == todayCal.get(Calendar.MONTH) &&
                    cell.dayNumber == todayCal.get(Calendar.DAY_OF_MONTH)

            val isSelected = cell.year == selectedCalendar.get(Calendar.YEAR) &&
                    cell.month == selectedCalendar.get(Calendar.MONTH) &&
                    cell.dayNumber == selectedCalendar.get(Calendar.DAY_OF_MONTH)

            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", cell.year, cell.month + 1, cell.dayNumber)
            val hasEvent = eventsList.any { it.dateStr == dateStr }

            holder.viewEventDot.visibility = if (hasEvent) View.VISIBLE else View.GONE

            when {
                isSelected -> {
                    holder.flDayBackground.setBackgroundResource(R.drawable.bg_calendar_selected)
                    holder.tvDayNumber.setTextColor(Color.WHITE)
                    holder.viewEventDot.setBackgroundResource(R.drawable.bg_calendar_holiday)
                }
                isToday -> {
                    holder.flDayBackground.setBackgroundResource(R.drawable.bg_calendar_today)
                    holder.tvDayNumber.setTextColor(Color.parseColor("#171717"))
                    holder.viewEventDot.setBackgroundResource(R.drawable.bg_bottom_nav_item_selected)
                }
                hasEvent -> {
                    holder.flDayBackground.setBackgroundResource(R.drawable.bg_calendar_event)
                    holder.tvDayNumber.setTextColor(Color.parseColor("#171717"))
                    holder.viewEventDot.setBackgroundResource(R.drawable.bg_bottom_nav_item_selected)
                }
                else -> {
                    holder.flDayBackground.background = null
                    holder.tvDayNumber.setTextColor(Color.parseColor("#171717"))
                }
            }

            holder.itemView.setOnClickListener {
                selectedCalendar = Calendar.getInstance().apply {
                    set(cell.year, cell.month, cell.dayNumber)
                }
                notifyDataSetChanged()
                renderSelectedDayEvents()
            }
        }

        override fun getItemCount(): Int = days.size
    }

    // ── Events List Adapter ──────────────────────────────────────────────────

    inner class DayEventsAdapter(
        private val events: List<CalendarEvent>
    ) : RecyclerView.Adapter<DayEventsAdapter.EventViewHolder>() {

        inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.iv_event_icon)
            val tvTitle: TextView = view.findViewById(R.id.tv_event_title)
            val tvSubtitle: TextView = view.findViewById(R.id.tv_event_subtitle)
            val tvBadge: TextView = view.findViewById(R.id.tv_event_badge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = layoutInflater.inflate(R.layout.item_calendar_event_card, parent, false)
            return EventViewHolder(view)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            val event = events[position]
            holder.tvTitle.text = event.title

            val isHoliday = event.type == "holiday"
            if (isHoliday) {
                holder.tvBadge.text = "Holiday"
                holder.tvBadge.setBackgroundResource(R.drawable.bg_pill_danger)
                holder.tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger))
                holder.ivIcon.setBackgroundResource(R.drawable.bg_circle_coral)
                holder.tvSubtitle.text = "Official Holiday / Celebration"
            } else {
                holder.tvBadge.text = "Community Event"
                holder.tvBadge.setBackgroundResource(R.drawable.bg_pill_accent)
                holder.tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                holder.ivIcon.setBackgroundResource(R.drawable.bg_circle_teal)
                holder.tvSubtitle.text = "Hoode Community & Sports"
            }
        }

        override fun getItemCount(): Int = events.size
    }
}

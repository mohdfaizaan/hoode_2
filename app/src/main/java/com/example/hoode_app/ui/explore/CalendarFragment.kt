package com.example.hoode_app.ui.explore

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class CalendarFragment : Fragment() {

    private lateinit var rvMonths: RecyclerView
    private var eventsList: List<CalendarEvent> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        rvMonths = view.findViewById(R.id.rv_months)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<View>(R.id.btn_today).setOnClickListener {
            scrollToCurrentMonth()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            HoodeRepository.calendarEvents.collectLatest { events ->
                eventsList = events
                setupCalendar()
            }
        }
    }

    private fun setupCalendar() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val months = (0..11).map { MonthData(currentYear, it) }

        rvMonths.layoutManager = LinearLayoutManager(requireContext())
        rvMonths.adapter = MonthAdapter(months, eventsList)
        
        scrollToCurrentMonth()
    }

    private fun scrollToCurrentMonth() {
        val currentMonthIndex = Calendar.getInstance().get(Calendar.MONTH)
        rvMonths.scrollToPosition(currentMonthIndex)
    }

    // ── Adapters ────────────────────────────────────────────────────────────

    data class MonthData(val year: Int, val month: Int)

    inner class MonthAdapter(
        private val months: List<MonthData>,
        private val events: List<CalendarEvent>
    ) : RecyclerView.Adapter<MonthAdapter.MonthViewHolder>() {

        private val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        inner class MonthViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvMonthTitle: TextView = view.findViewById(R.id.tv_month_title)
            val rvDays: RecyclerView = view.findViewById(R.id.rv_days)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
            val view = layoutInflater.inflate(R.layout.item_calendar_month, parent, false)
            return MonthViewHolder(view)
        }

        override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
            val monthData = months[position]
            holder.tvMonthTitle.text = "${monthNames[monthData.month]} ${monthData.year}"

            val cal = Calendar.getInstance()
            cal.set(monthData.year, monthData.month, 1)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sunday=0)

            val days = mutableListOf<DayData>()
            for (i in 0 until firstDayOfWeek) {
                days.add(DayData(0, monthData.month, monthData.year, isEmpty = true))
            }
            for (i in 1..daysInMonth) {
                days.add(DayData(i, monthData.month, monthData.year, isEmpty = false))
            }

            holder.rvDays.layoutManager = GridLayoutManager(requireContext(), 7)
            holder.rvDays.adapter = DayAdapter(days, events)
        }

        override fun getItemCount() = months.size
    }

    data class DayData(val day: Int, val month: Int, val year: Int, val isEmpty: Boolean)

    inner class DayAdapter(
        private val days: List<DayData>,
        private val events: List<CalendarEvent>
    ) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

        inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDay: TextView = view.findViewById(R.id.tv_day_number)
            val flBackground: FrameLayout = view.findViewById(R.id.fl_day_background)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view = layoutInflater.inflate(R.layout.item_calendar_day, parent, false)
            return DayViewHolder(view)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val dayData = days[position]

            if (dayData.isEmpty) {
                holder.tvDay.text = ""
                holder.flBackground.background = null
                return
            }

            holder.tvDay.text = dayData.day.toString()

            val todayCal = Calendar.getInstance()
            val todayYear = todayCal.get(Calendar.YEAR)
            val todayMonth = todayCal.get(Calendar.MONTH)
            val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

            val isToday = dayData.year == todayYear && dayData.month == todayMonth && dayData.day == todayDay
            
            // Check if past
            val itemCal = Calendar.getInstance().apply {
                set(dayData.year, dayData.month, dayData.day, 0, 0, 0)
            }
            val currentCal = Calendar.getInstance().apply {
                set(todayYear, todayMonth, todayDay, 0, 0, 0)
            }
            
            val isPast = itemCal.timeInMillis < currentCal.timeInMillis

            // Format date for matching events (YYYY-MM-DD)
            val monthStr = String.format("%02d", dayData.month + 1)
            val dayStr = String.format("%02d", dayData.day)
            val dateStr = "${dayData.year}-$monthStr-$dayStr"

            val event = events.find { it.dateStr == dateStr }

            // Default styling
            holder.tvDay.setTextColor(Color.parseColor("#171717"))
            holder.flBackground.background = null

            if (isToday) {
                holder.flBackground.setBackgroundResource(R.drawable.bg_calendar_today)
            } else if (event != null) {
                if (event.type == "holiday") {
                    holder.flBackground.setBackgroundResource(R.drawable.bg_calendar_holiday)
                    holder.tvDay.setTextColor(Color.WHITE)
                } else if (event.type == "event") {
                    holder.flBackground.setBackgroundResource(R.drawable.bg_calendar_event)
                }
            }

            // Dim if past
            if (isPast) {
                holder.tvDay.alpha = 0.3f
                holder.flBackground.alpha = 0.5f
            } else {
                holder.tvDay.alpha = 1.0f
                holder.flBackground.alpha = 1.0f
            }
        }

        override fun getItemCount() = days.size
    }
}

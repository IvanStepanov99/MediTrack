package com.example.medtracker.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.medtracker.R
import com.example.medtracker.data.db.DbBuilder
import com.example.medtracker.data.db.entities.Drug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

object ScheduleBinder {
    fun bind(view: View, drug: Drug, lifecycleScope: LifecycleCoroutineScope, appContext: Context) {
        val tvScheduleEveryday = view.findViewById<TextView>(R.id.tvScheduleEveryday)
        val hsWeekdays = view.findViewById<HorizontalScrollView>(R.id.hsWeekdays)
        val llScheduleTimes = view.findViewById<LinearLayout>(R.id.llScheduleTimes)
        val llScheduleSection = view.findViewById<LinearLayout>(R.id.llScheduleSection)

        lifecycleScope.launch {
            val db = DbBuilder.getDatabase(appContext)
            val swt = withContext(Dispatchers.IO) { db.doseScheduleDao().getWithTimesByDrugId(drug.drugId) }

            if (swt == null || swt.schedule.prn) {
                // no schedule or PRN so hide section
                llScheduleSection.visibility = View.GONE
            } else {
                llScheduleSection.visibility = View.VISIBLE

                val schedule = swt.schedule
                // treat DAILY or everyNDays==1 as everyday
                val isEveryDay = schedule.freqType.equals("DAILY", ignoreCase = true) || (schedule.everyNDays == 1)

                val selectedDays = (schedule.byWeekDay ?: emptyList()).mapNotNull { raw ->
                    when (raw.trim().uppercase(Locale.US).take(3)) {
                        "MON", "MO" -> 1
                        "TUE", "TU" -> 2
                        "WED", "WE" -> 3
                        "THU", "TH" -> 4
                        "FRI", "FR" -> 5
                        "SAT", "SA" -> 6
                        "SUN", "SU" -> 7
                        else -> null
                    }
                }.toSet()

                if (isEveryDay) {
                    tvScheduleEveryday.text = view.context.getString(R.string.everyday)
                    tvScheduleEveryday.visibility = View.VISIBLE
                    hsWeekdays.visibility = View.GONE
                } else if (selectedDays.isNotEmpty()) {
                    tvScheduleEveryday.visibility = View.GONE
                    hsWeekdays.visibility = View.VISIBLE
                    val dayIds = listOf(
                        R.id.day_mon,
                        R.id.day_tue,
                        R.id.day_wed,
                        R.id.day_thu,
                        R.id.day_fri,
                        R.id.day_sat,
                        R.id.day_sun
                    )
                    dayIds.forEachIndexed { idx, id ->
                        val dayTv = view.findViewById<TextView>(id)
                        val dayIndex = idx + 1
                        val sel = selectedDays.contains(dayIndex)
                        if (sel) {
                            dayTv.setBackgroundColor(Color.parseColor("#3D3D99"))
                            dayTv.setTextColor(Color.WHITE)
                        } else {
                            dayTv.setBackgroundColor(Color.TRANSPARENT)
                            dayTv.setTextColor(Color.parseColor("#666666"))
                        }
                    }
                } else {
                    tvScheduleEveryday.visibility = View.GONE
                    hsWeekdays.visibility = View.GONE
                }

                // Build a list of (minutes, displayString) then sort chronologically.
                val timesList: MutableList<Pair<Int, String>> = mutableListOf()

                // If schedule.timesOfDay provided (string form), try to parse them to minutes
                val tod = schedule.timesOfDay
                if (!tod.isNullOrEmpty()) {
                    tod.forEach { s ->
                        val mins = TimeUtils.parseTimeToMinutes(s)
                        if (mins != null) {

                            val disp = TimeUtils.minutesToDisplay(mins)
                            if (timesList.none { it.first == mins }) timesList.add(mins to disp)
                        } else {
                            val disp = s.trim()

                            if (timesList.none { it.second == disp }) timesList.add(Int.MAX_VALUE to disp)
                        }
                    }
                }

                // Also include DoseTime rows (numeric) if any
                swt.times.forEach { dt ->
                    if (timesList.none { it.first == dt.minutesLocal }) {
                        timesList.add(dt.minutesLocal to TimeUtils.minutesToDisplay(dt.minutesLocal))
                    }
                }

                // Sort by minutes (
                val sorted = timesList.sortedBy { it.first }

                // Render vertically in chronological order
                llScheduleTimes.removeAllViews()
                val pad = (8 * view.resources.displayMetrics.density).toInt()
                if (sorted.isEmpty()) {
                    val empty = TextView(view.context).apply {
                        text = view.context.getString(R.string.no_scheduled_times)
                        setTextColor(Color.parseColor("#666666"))
                        setPadding(pad, pad / 2, pad, pad / 2)
                    }
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    params.setMargins(0, 0, 0, (4 * view.resources.displayMetrics.density).toInt())
                    empty.layoutParams = params
                    llScheduleTimes.addView(empty)
                } else {
                    sorted.forEach { (_, disp) ->
                        val tv = TextView(view.context).apply {
                            text = disp
                            setTextColor(Color.parseColor("#333333"))
                            textSize = 16f
                            setPadding(0, (6 * view.resources.displayMetrics.density).toInt(), 0, (6 * view.resources.displayMetrics.density).toInt())
                        }
                        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        params.setMargins(0, 0, 0, (6 * view.resources.displayMetrics.density).toInt())
                        tv.layoutParams = params
                        llScheduleTimes.addView(tv)
                    }
                }
            }
        }
    }
}

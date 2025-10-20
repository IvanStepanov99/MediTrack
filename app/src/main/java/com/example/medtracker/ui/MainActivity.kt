package com.example.medtracker.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medtracker.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.medtracker.data.db.DbBuilder
import kotlinx.coroutines.launch
import android.widget.Toast
import android.util.Log
import com.example.medtracker.data.session.SessionManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import android.view.View
import com.example.medtracker.data.db.entities.Drug
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import java.util.Locale
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val firstName = intent.getStringExtra("firstName")
        val userId = intent.getStringExtra("uid")
        val namePart = if (!firstName.isNullOrBlank()) firstName else userId ?: "User"
        // Use string resource with placeholder to allow translation
        findViewById<TextView>(R.id.tvWelcome).text = getString(R.string.welcome_user, namePart)

        val rv = findViewById<RecyclerView>(R.id.rvMeds)
        val adapter = MedAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        val rootView = findViewById<View>(android.R.id.content)

        val db = DbBuilder.getDatabase(applicationContext)
        val drugDao = db.drugDao()

        // Open bottom sheet on item click
        adapter.onItemClick = { drug ->
            showMedBottomSheet(drug)
        }

        // Swipe to delete (left only)
        val itemTouchHelperCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val removedDrug = adapter.removeItem(position)
                if (removedDrug != null) {
                    lifecycleScope.launch { drugDao.delete(removedDrug) }
                    Snackbar.make(rootView, "Deleted ${'$'}{removedDrug.name}", Snackbar.LENGTH_LONG)
                        .setAction("Undo") { lifecycleScope.launch { drugDao.insert(removedDrug) } }
                        .show()
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    val itemView = viewHolder.itemView
                    val paint = Paint().apply { color = Color.parseColor("#D32F2F") }
                    c.drawRect(
                        itemView.right.toFloat() + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat(),
                        paint
                    )
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rv)

        val uidFromIntent = intent.getStringExtra("uid")
        val uid = uidFromIntent ?: SessionManager.getCurrentUid(applicationContext)
        if (uid == null) {
            Toast.makeText(this, "No user id. Please log in.", Toast.LENGTH_LONG).show()
            Log.w("MainActivity", "No uid in intent or session; cannot observe meds")
            return
        }
        Log.d("MainActivity", "Observing meds for uid=$uid")

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                drugDao.observeByUser(uid).collect { list ->
                    Log.d("MainActivity", "Med rows for $uid = ${'$'}{list.size}")
                    adapter.submit(list)
                }
            }
        }

        BottomBarHelper.setup(this, R.id.btnMeds)
    }

    private fun showMedBottomSheet(drug: Drug) {
        // Inflate with a parent to ensure layout params on the root are resolved
        val parent = findViewById<ViewGroup>(android.R.id.content)
        val view = LayoutInflater.from(this).inflate(R.layout.bottomsheet_med, parent, false)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        // Configure draggable, expandable behavior
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (sheet != null) {
                // Allow full-screen expansion
                sheet.layoutParams = sheet.layoutParams.apply { height = ViewGroup.LayoutParams.MATCH_PARENT }
                sheet.requestLayout()

                val behavior = BottomSheetBehavior.from(sheet)
                behavior.peekHeight = resources.getDimensionPixelSize(R.dimen.med_sheet_peek)
                behavior.isFitToContents = false
                behavior.expandedOffset = 0
                behavior.skipCollapsed = false
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                behavior.isDraggable = true
            }
        }

        val tvBrand = view.findViewById<TextView>(R.id.tvSheetBrand)
        val tvName = view.findViewById<TextView>(R.id.tvSheetName)
        val tvStrength = view.findViewById<TextView>(R.id.tvSheetStrength)
        val tvUnit = view.findViewById<TextView>(R.id.tvSheetUnit)
        val tvForm = view.findViewById<TextView>(R.id.tvSheetForm)

        // Schedule UI elements
        val tvScheduleEveryday = view.findViewById<TextView>(R.id.tvScheduleEveryday)
        val hsWeekdays = view.findViewById<HorizontalScrollView>(R.id.hsWeekdays)
        val llScheduleTimes = view.findViewById<LinearLayout>(R.id.llScheduleTimes)
        val llScheduleSection = view.findViewById<LinearLayout>(R.id.llScheduleSection)

        val brand = drug.brandName?.trim().orEmpty()
        val generic = drug.name.trim()
        if (brand.isNotBlank()) {
            tvBrand.text = brand
            tvBrand.visibility = View.VISIBLE
        } else {
            tvBrand.visibility = View.GONE
        }
        tvName.text = if (generic.isNotBlank()) generic else brand.ifBlank { "(Unknown drug)" }

        val hasForm = !drug.form.isNullOrBlank()
        val hasStrength = drug.strength != null
        val hasUnit = !drug.unit.isNullOrBlank()
        tvForm.text = drug.form ?: ""
        tvForm.visibility = if (hasForm) View.VISIBLE else View.GONE
        tvStrength.text = drug.strength?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
        tvStrength.visibility = if (hasStrength) View.VISIBLE else View.GONE
        tvUnit.text = drug.unit ?: ""
        tvUnit.visibility = if (hasUnit) View.VISIBLE else View.GONE

        lifecycleScope.launch {
            val db = DbBuilder.getDatabase(applicationContext)
            // use drugId (Long) not uid (String)
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
                    tvScheduleEveryday.text = getString(R.string.everyday)
                    tvScheduleEveryday.visibility = View.VISIBLE
                    hsWeekdays.visibility = View.GONE
                } else if (selectedDays.isNotEmpty()) {
                    tvScheduleEveryday.visibility = View.GONE
                    hsWeekdays.visibility = View.VISIBLE
                    val dayIds = listOf(R.id.day_mon, R.id.day_tue, R.id.day_wed, R.id.day_thu, R.id.day_fri, R.id.day_sat, R.id.day_sun)
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
                        val mins = parseTimeToMinutes(s)
                        if (mins != null) {
                            // prefer normalized display (e.g. "1:00 AM") for consistent ordering/appearance
                            val disp = minutesToDisplay(mins)
                            if (timesList.none { it.first == mins }) timesList.add(mins to disp)
                        } else {
                            val disp = s.trim()
                            // unparsable strings last; use negative marker to keep them at end but stable
                            if (timesList.none { it.second == disp }) timesList.add(Int.MAX_VALUE to disp)
                        }
                    }
                }

                // Also include DoseTime rows (numeric) if any
                swt.times.forEach { dt ->
                    if (timesList.none { it.first == dt.minutesLocal }) {
                        timesList.add(dt.minutesLocal to minutesToDisplay(dt.minutesLocal))
                    }
                }

                // Sort by minutes (Int.MAX_VALUE entries will go to the end)
                val sorted = timesList.sortedBy { it.first }

                // Render vertically in chronological order
                llScheduleTimes.removeAllViews()
                val pad = (8 * resources.displayMetrics.density).toInt()
                if (sorted.isEmpty()) {
                    val empty = TextView(this@MainActivity).apply {
                        text = getString(R.string.no_scheduled_times)
                        setTextColor(Color.parseColor("#666666"))
                        setPadding(pad, pad / 2, pad, pad / 2)
                    }
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    params.setMargins(0, 0, 0, (4 * resources.displayMetrics.density).toInt())
                    empty.layoutParams = params
                    llScheduleTimes.addView(empty)
                } else {
                    sorted.forEach { (_, disp) ->
                        val tv = TextView(this@MainActivity).apply {
                            text = disp
                            setTextColor(Color.parseColor("#333333"))
                            textSize = 16f
                            setPadding(0, (6 * resources.displayMetrics.density).toInt(), 0, (6 * resources.displayMetrics.density).toInt())
                        }
                        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        params.setMargins(0, 0, 0, (6 * resources.displayMetrics.density).toInt())
                        tv.layoutParams = params
                        llScheduleTimes.addView(tv)
                    }
                }
            }
        }

        dialog.show()
    }

    private fun minutesToDisplay(minutes: Int): String {
        val h = (minutes / 60)
        val m = minutes % 60
        val am = if (h < 12) "AM" else "PM"
        val dispH = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "%d:%02d %s".format(dispH, m, am)
    }

    private fun parseTimeToMinutes(time: String): Int? {
        val trimmed = time.trim()
        if (trimmed.isEmpty()) return null
        val patterns = listOf("H:mm", "HH:mm", "h:mma", "hh:mma", "h:mm a", "hh:mm a")
        for (pat in patterns) {
            try {
                val sdf = SimpleDateFormat(pat, Locale.US)
                sdf.isLenient = false
                val d = try {
                    sdf.parse(trimmed)
                } catch (e: Exception) {
                    // normalize am/pm spacing and try again
                    val norm = trimmed.replace(Regex("(?i)(am|pm)"), { m -> " ${m.value.uppercase(Locale.US)}" }).trim()
                    try { sdf.parse(norm) } catch (_: Exception) { null }
                } ?: continue
                val cal = java.util.Calendar.getInstance()
                cal.time = d
                return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            } catch (_: Exception) {
            }
        }
        return null
    }
}

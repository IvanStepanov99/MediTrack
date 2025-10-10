package com.example.medtracker.ui

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton
import com.example.medtracker.R

object BottomBarHelper {
    fun setup(
        activity: Activity,
        selectedId: Int
    ) {
        val btnStats = activity.findViewById<ImageButton>(R.id.btnStats)
        val btnMeds = activity.findViewById<ImageButton>(R.id.btnMeds)
        val btnLogs = activity.findViewById<ImageButton>(R.id.btnLogs)
        val btnSearch = activity.findViewById<ImageButton>(R.id.btnSearch)

        btnStats.setImageResource(if (selectedId == R.id.btnStats) R.drawable.ic_stats_selected else R.drawable.ic_stats)
        btnMeds.setImageResource(if (selectedId == R.id.btnMeds) R.drawable.ic_meds_selected else R.drawable.ic_meds)
        btnLogs.setImageResource(if (selectedId == R.id.btnLogs) R.drawable.ic_logs_selected else R.drawable.ic_logs)
        btnSearch.setImageResource(if (selectedId == R.id.btnSearch) R.drawable.ic_search_selected else R.drawable.ic_search)

        btnStats.setOnClickListener {
            if (selectedId != R.id.btnStats) activity.startActivity(Intent(activity, StatsActivity::class.java))
        }
        btnMeds.setOnClickListener {
            if (selectedId != R.id.btnMeds) activity.startActivity(Intent(activity, MainActivity::class.java))
        }
        btnLogs.setOnClickListener {
            if (selectedId != R.id.btnLogs) activity.startActivity(Intent(activity, LogsActivity::class.java))
        }
        btnSearch.setOnClickListener {
            if (selectedId != R.id.btnSearch) activity.startActivity(Intent(activity, SearchActivity::class.java))
        }
    }
}


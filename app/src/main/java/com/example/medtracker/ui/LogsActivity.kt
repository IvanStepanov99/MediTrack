package com.example.medtracker.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import com.example.medtracker.R

class LogsActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        // Bottom bar click handlers
        val btnStats = findViewById<ImageButton>(R.id.btnStats)
        val btnMeds = findViewById<ImageButton>(R.id.btnMeds)
        val btnLogs = findViewById<ImageButton>(R.id.btnLogs)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)

        fun setActiveButton(selectedId: Int) {
            btnStats.setImageResource(if (selectedId == R.id.btnStats) R.drawable.ic_stats_selected else R.drawable.ic_stats)
            btnMeds.setImageResource(if (selectedId == R.id.btnMeds) R.drawable.ic_meds_selected else R.drawable.ic_meds)
            btnLogs.setImageResource(if (selectedId == R.id.btnLogs) R.drawable.ic_logs_selected else R.drawable.ic_logs)
            btnSearch.setImageResource(if (selectedId == R.id.btnSearch) R.drawable.ic_search_selected else R.drawable.ic_search)
        }

        setActiveButton(R.id.btnLogs)

        btnStats.setOnClickListener {
            setActiveButton(R.id.btnStats)
            Toast.makeText(this, "Stats tapped", Toast.LENGTH_SHORT).show()
        }
        btnMeds.setOnClickListener {
            setActiveButton(R.id.btnMeds)
            startActivity(Intent(this, MainActivity::class.java))
        }
        btnLogs.setOnClickListener {
            setActiveButton(R.id.btnLogs)
            startActivity(Intent(this, LogsActivity::class.java))
        }
        btnSearch.setOnClickListener {
            setActiveButton(R.id.btnSearch)
            Toast.makeText(this, "Search tapped", Toast.LENGTH_SHORT).show()
        }
        }
}

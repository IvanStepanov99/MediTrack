package com.example.medtracker.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medtracker.R
import com.example.medtracker.data.DoseLogGenerator
import com.example.medtracker.data.db.DbBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogsActivity : AppCompatActivity(){
    private val vm: LogsViewModel by viewModels()

    private lateinit var upcomingAdapter: LogsAdapter
    private lateinit var historyAdapter: LogsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_logs)
        BottomBarHelper.setup(this, R.id.btnLogs)

        // Find the upcoming RecyclerView. No fallback to rvLogs (that id doesn't exist in layout).
        val rvUpcoming = findViewById<RecyclerView>(R.id.rvUpcoming)
        if (rvUpcoming == null) {
            Toast.makeText(this, "Logs layout missing RecyclerView (rvUpcoming)", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        val historySection = findViewById<View>(R.id.historySection)

        upcomingAdapter = LogsAdapter(showButtons = true)
        historyAdapter = LogsAdapter(showButtons = false)

        rvUpcoming.layoutManager = LinearLayoutManager(this)
        rvUpcoming.adapter = upcomingAdapter

        if (rvHistory != null) {
            rvHistory.layoutManager = LinearLayoutManager(this)
            rvHistory.adapter = historyAdapter
        }

        // Wire button handlers to update DB (only for upcomingAdapter)
        upcomingAdapter.onTake = { item ->
            lifecycleScope.launch(Dispatchers.IO) {
                val db = DbBuilder.getDatabase(applicationContext)
                val log = item.doseLog
                val qty = log.quantity
                val unit = log.unit
                val now = System.currentTimeMillis()
                try {
                    db.doseLogDao().markTaken(log.logId, "TAKEN", now, qty, unit)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LogsActivity, "Failed to mark taken: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        upcomingAdapter.onSkip = { item ->
            lifecycleScope.launch(Dispatchers.IO) {
                val db = DbBuilder.getDatabase(applicationContext)
                try {
                    db.doseLogDao().setStatus(item.doseLog.logId, "SKIPPED")
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LogsActivity, "Failed to skip: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Observe ViewModel lists
        vm.upcoming.observe(this) { list ->
            upcomingAdapter.submit(list)
        }
        vm.history.observe(this) { list ->
            historyAdapter.submit(list)
            // If historySection exists, control visibility; if not, ignore
            if (historySection != null) {
                historySection.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Generate today's DoseLog rows from schedules (if missing), then observe DoseLog rows
        val db = DbBuilder.getDatabase(applicationContext)
        lifecycleScope.launch {
            // create today's planned logs first
            withContext(Dispatchers.IO) {
                DoseLogGenerator.generateTodayDoseLogsIfMissing(applicationContext)
            }

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.doseLogDao().observeAll().collect { logs ->
                    val items = withContext(Dispatchers.IO) {
                        logs.mapNotNull { log ->
                            val drug = db.drugDao().getById(log.drugId)
                            if (drug != null) LogItem(log, drug) else null
                        }
                    }
                    // feed ViewModel which will split into upcoming/history
                    vm.updateLogs(items)
                }
            }
        }
    }
}

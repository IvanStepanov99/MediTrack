package com.example.medtracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Calendar

/**
 * ViewModel that splits a full list of LogItem into two lists for UI:
 * - upcoming: not acted and not missed today (sorted soonest-first)
 * - history: today's taken/skipped entries and missed entries (sorted recent-first)
 */
class LogsViewModel : ViewModel() {
    private val _upcoming = MutableLiveData<List<LogItem>>(emptyList())
    val upcoming: LiveData<List<LogItem>> = _upcoming

    private val _history = MutableLiveData<List<LogItem>>(emptyList())
    val history: LiveData<List<LogItem>> = _history

    /**
     * Accept a full list of LogItem and split into upcoming/history for display.
     */
    fun updateLogs(allLogs: List<LogItem>) {
        val (up, hist) = splitLogsForToday(allLogs)

        // Sort upcoming ascending by planned time then taken time (soonest first)
        val upSorted = up.sortedWith(
            compareBy({ it.doseLog.plannedTime ?: Long.MAX_VALUE }, { it.doseLog.takenTime ?: Long.MAX_VALUE })
        )

        // Sort history descending by takenTime or plannedTime (most recent first)
        val histSorted = hist.sortedWith(
            compareByDescending<LogItem> { it.doseLog.takenTime ?: it.doseLog.plannedTime ?: 0L }
        )

        _upcoming.postValue(upSorted)
        _history.postValue(histSorted)
    }

    /**
     * Splits logs into upcoming and history for "today" based on device-local day boundaries.
     * History contains:
     *  - logs with status TAKEN or SKIPPED that occurred today (planned or taken time in today's range),
     *  - logs that were not acted on and had a planned time today that is <= now (missed today).
     * Upcoming contains everything else (not acted and not missed today).
     */
    private fun splitLogsForToday(logs: List<LogItem>): Pair<List<LogItem>, List<LogItem>> {
        val now = System.currentTimeMillis()
        val startOfDay = startOfDayMillis(now)
        val endOfDay = endOfDayMillis(now)

        val upcoming = mutableListOf<LogItem>()
        val history = mutableListOf<LogItem>()

        for (item in logs) {
            val log = item.doseLog
            val status = log.status.trim().uppercase()
            val planned = log.plannedTime
            val taken = log.takenTime

            val plannedInToday = planned != null && planned in startOfDay..endOfDay
            val takenInToday = taken != null && taken in startOfDay..endOfDay

            // If taken or skipped and happened today -> history
            if ((status == "TAKEN" || status == "SKIPPED") && (plannedInToday || takenInToday)) {
                history.add(item)
                continue
            }

            // If not acted and planned today and planned <= now => missed (history)
            val acted = status == "TAKEN" || status == "SKIPPED"
            if (!acted) {
                if (planned != null && planned <= now && plannedInToday) {
                    history.add(item) // missed today
                    continue
                } else {
                    // not acted and not missed => upcoming
                    upcoming.add(item)
                    continue
                }
            }

            // If acted but not today (or any other case) treat as upcoming for today's view
            upcoming.add(item)
        }

        return Pair(upcoming, history)
    }

    private fun startOfDayMillis(timeMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun endOfDayMillis(timeMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}

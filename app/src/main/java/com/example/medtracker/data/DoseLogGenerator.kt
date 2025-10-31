package com.example.medtracker.data

import android.content.Context
import com.example.medtracker.data.db.DbBuilder
import com.example.medtracker.data.db.entities.DoseLog
import com.example.medtracker.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

object DoseLogGenerator {
    suspend fun generateTodayDoseLogsIfMissing(context: Context) = withContext(Dispatchers.IO) {
        val uid = SessionManager.getCurrentUid(context) ?: return@withContext
        val db = DbBuilder.getDatabase(context)

        // fetch current user's drugs once
        val drugs = db.drugDao().findByNamePrefix(uid, "").first() // using existing flow; empty prefix returns all for user

        val todayCal = Calendar.getInstance()
        // set to start of today in device default timezone
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)
        val startOfTodayLocal = todayCal.timeInMillis
        val endOfTodayLocal = startOfTodayLocal + 24L * 60L * 60L * 1000L - 1L

        for (drug in drugs) {
            // load schedule+times for drug
            val swt = db.doseScheduleDao().getWithTimesByDrugId(drug.drugId) ?: continue
            val schedule = swt.schedule
            if (schedule.prn) continue

            // check date range
            if (schedule.startDate != null && schedule.startDate > endOfTodayLocal) continue
            if (schedule.endDate != null && schedule.endDate < startOfTodayLocal) continue

            // determine if schedule applies today (simple rules)
            val isEveryDay = schedule.freqType.equals("DAILY", ignoreCase = true) || (schedule.everyNDays == 1)
            val selectedDays = (schedule.byWeekDay ?: emptyList()).mapNotNull { raw ->
                when (raw.trim().uppercase().take(3)) {
                    "MON", "MO" -> Calendar.MONDAY
                    "TUE", "TU" -> Calendar.TUESDAY
                    "WED", "WE" -> Calendar.WEDNESDAY
                    "THU", "TH" -> Calendar.THURSDAY
                    "FRI", "FR" -> Calendar.FRIDAY
                    "SAT", "SA" -> Calendar.SATURDAY
                    "SUN", "SU" -> Calendar.SUNDAY
                    else -> null
                }
            }.toSet()

            val tz = TimeZone.getTimeZone(schedule.timeZone)
            val todayCalForCheck = Calendar.getInstance(tz)
            val todayWeekday = todayCalForCheck.get(Calendar.DAY_OF_WEEK)
            val appliesToday = isEveryDay || selectedDays.contains(todayWeekday)
            if (!appliesToday) continue

            // For each DoseTime, compute plannedTime (in schedule.timeZone) for today
            for (dt in swt.times) {
                val minutes = dt.minutesLocal
                val cal = Calendar.getInstance(tz)
                // set to today's date in that timezone
                cal.set(Calendar.YEAR, todayCalForCheck.get(Calendar.YEAR))
                cal.set(Calendar.MONTH, todayCalForCheck.get(Calendar.MONTH))
                cal.set(Calendar.DAY_OF_MONTH, todayCalForCheck.get(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, minutes / 60)
                cal.set(Calendar.MINUTE, minutes % 60)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val plannedTime = cal.timeInMillis

                // only insert if plannedTime is within today bounds in that timezone
                val startOfDayInTz = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val endOfDayInTz = startOfDayInTz + 24L * 60L * 60L * 1000L - 1L
                if (plannedTime < startOfDayInTz || plannedTime > endOfDayInTz) continue

                // check for existing DoseLog for same drug + plannedTime
                val existing = db.doseLogDao().findByDrugIdAndPlannedTime(drug.drugId, plannedTime)
                if (existing == null) {
                    val dl = DoseLog(
                        logId = 0L,
                        drugId = drug.drugId,
                        doseScheduleId = schedule.doseScheduleId,
                        plannedTime = plannedTime,
                        takenTime = null,
                        status = "PLANNED",
                        quantity = dt.doseCount,
                        unit = schedule.doseUnit ?: drug.unit,
                        note = null
                    )
                    try {
                        db.doseLogDao().insert(dl)
                    } catch (_: Exception) {
                        // ignore duplicates/errors
                    }
                }
            }
        }
    }
}

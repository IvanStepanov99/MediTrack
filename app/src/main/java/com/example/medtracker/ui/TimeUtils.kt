package com.example.medtracker.ui

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Small utility object for time-related helper methods.
 * Extrcted from MainActivity to reduce for clarity.
 */
@Suppress("unused")
object TimeUtils {
   fun minutesToDisplay(minutes: Int): String {
       val h = (minutes / 60)
       val m = minutes % 60
       val am = if (h < 12) "AM" else "PM"
       val dispH = when {
           h == 0 -> 12
           h > 12 -> h -12
              else -> h
       }
       return "%d:%02d%s".format(dispH, m, am)
   }

    fun parseTimeToMinutes(time: String): Int? {
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
                    // Normalize AM/PM spacing and retry
                    val norm = trimmed.replace(Regex("(?i)(am|pm)"), { m -> " ${m.value.uppercase(Locale.US)}" }).trim()
                    try {sdf.parse(norm)} catch (e: Exception) {null}
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
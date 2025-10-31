package com.example.medtracker.ui

import com.example.medtracker.data.db.entities.DoseLog
import com.example.medtracker.data.db.entities.Drug

/**
 * UI-level wrapper combining a DoseLog with its Drug for display in the logs list.
 */
data class LogItem(
    val doseLog: DoseLog,
    val drug: Drug
)


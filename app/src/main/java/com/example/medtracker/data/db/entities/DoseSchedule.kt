package com.example.medtracker.data.db.entities

import androidx.room.*

@Entity(
    tableName = "dose_schedule",
    indices = [Index(value = ["drugId"]),],
    foreignKeys = [
        ForeignKey(
            entity = Drug::class,
            parentColumns = ["drugId"],
            childColumns = ["drugId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DoseSchedule(
    @PrimaryKey(autoGenerate = true)
    val doseScheduleId: Long = 0L,
    val drugId: Long, // owner UID derives from Drug -> UserProfile
    val prn: Boolean = false, //as needed (does not requer to schedule time)
    val doseAmount: Double? = null,
    val doseUnit: String? = null,  // "mg", "ml"
    @ColumnInfo(name = "freq_type")
    val freqType: String,
    @ColumnInfo(name = "interval_hours")
    val intervalHours: Int? = null, // when freqType == "EVERY_N_HOURS" 6, 12
    @ColumnInfo(name =  "every_n_days")
    val everyNDays: Int? = null, // when freqType == "EVERY_N_DAYS" every 3 days
    val byWeekDay: String? = null, // CSV like "MON,TUE,FRI"
    @ColumnInfo(name = "start_date")
    val startDate: Long? =  null,
    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,
    val timeZone: String,
    //TimeStamps
    val createdAt: Long,
    val updatedAt: Long
)

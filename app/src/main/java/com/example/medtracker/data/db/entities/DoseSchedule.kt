package com.example.medtracker.data.db.entities

import androidx.room.*
import java.util.TimeZone

@Entity(
    tableName = "dose_schedule",
    indices = [Index(value = ["drugId"], unique = true)],
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

    // link to the Drug this schedule belongs to
    @ColumnInfo(name = "drugId")
    val drugId: Long,

    val prn: Boolean = false, // as needed (does not require scheduled time)
    val doseAmount: Double? = null,
    val doseUnit: String? = null,  // "mg", "ml"

    @ColumnInfo(name = "freq_type")
    val freqType: String,

    @ColumnInfo(name = "interval_hours")
    val intervalHours: Int? = null, // when freqType == "EVERY_N_HOURS" 6, 12

    @ColumnInfo(name = "every_n_days")
    val everyNDays: Int? = null,

    // Use list of weekdays (e.g. ["MON","TUE"]) for weekly schedules
    val byWeekDay: List<String>? = null,

    // Times of day stored as 12-hour lowercase strings (h:mma) via TypeConverter
    // Example: ["12:00am", "8:30pm"]. `DoseTime` table stores numeric minutes since midnight.
    // (Input may be accepted in 24h or 12h formats; values are normalized before saving.)
    
    val timesOfDay: List<String>? = null,

    // TimeStamps
    @ColumnInfo(name = "start_date")
    val startDate: Long? =  null,
    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,

    // default to device timezone
    val timeZone: String = TimeZone.getDefault().id,

    // TimeStamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

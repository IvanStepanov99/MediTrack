package com.example.medtracker.data.db.entities

import androidx.room.*

@Entity(
    tableName = "dose_time",
    indices = [
        Index(value = ["doseScheduleId"]),
        Index(value = ["doseScheduleId", "minutesLocal"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = DoseSchedule::class,
            parentColumns = ["doseScheduleId"],
            childColumns = ["doseScheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DoseTime(
    @PrimaryKey(autoGenerate = true)
    val doseTimeId: Long = 0L,
    val doseScheduleId: Long,
    val minutesLocal: Int,
    val doseCount: Double,
    //TimeStamps
    val createdAt: Long,
    val updatedAt: Long
)

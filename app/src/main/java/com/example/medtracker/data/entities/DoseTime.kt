package com.example.medtracker.data.entities

import androidx.room.*

@Entity(
    tableName = "dose_time",
    indices = [
        Index(value = ["doseScheduleId"]),
        Index(value = ["doseScheduleId", "minutesLocal"], unique = true),
        Index(value = ["client_uuid"], unique = true)
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
    @ColumnInfo(name = "client_uuid")
    val clientUuid: String,
    //TimeStamps
    val createdAt: Long,
    val updatedAt: Long
)

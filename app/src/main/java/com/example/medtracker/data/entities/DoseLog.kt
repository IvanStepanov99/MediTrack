package com.example.medtracker.data.entities

import androidx.room.*

@Entity(
    tableName = "dose_log",
    indices = [
        Index(value = ["drugId", "plannedTime"]),
        Index(value = ["drugId", "takenTime"]),
        Index(value = ["doseScheduleId"]),
        Index(value = ["client_uuid"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = Drug::class,
            parentColumns = ["drugId"],
            childColumns = ["drugId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DoseSchedule::class,
            parentColumns = ["doseScheduleId"],
            childColumns = ["doseScheduleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0L,
    val drugId: Long, // which drug this log belongs to
    val doseScheduleId: Long? = null, // which schedule produced it (null for prn)
    val plannedTime: Long? = null, // due time to take (null for prn)
    val takenTime: Long? = null, // when it was taken (null for missed or skipped)
    val status: String = "PLANNED", // one of possible med status (taken, missed or skipped)
    val quantity: Double? = null,
    val unit: String? = null,
    val note: String? = null, // optional note written by user
    @ColumnInfo(name = "client_uuid")
    val clientUuid: String,
    //TimeStamps
    val createdAt: Long,
    val updatedAt: Long
    )

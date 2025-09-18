package com.example.medtracker.data.db.entities

import androidx.room.Embedded
import androidx.room.Relation

data class ScheduleWithTimes(
    @Embedded val schedule: DoseSchedule,
    @Relation(
        parentColumn = "doseScheduleId",
        entityColumn = "doseScheduleId",
        entity = DoseTime::class
    )
    val times: List<DoseTime>
)

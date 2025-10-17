package com.example.medtracker.data.db.entities

fun DoseSchedule.sanitizedForSave(): DoseSchedule {
    return if (prn) {
        copy(
            timesOfDay = null,
            intervalHours = null,
            everyNDays = null,
            byWeekDay = null
        )
    } else {
        this
    }
}


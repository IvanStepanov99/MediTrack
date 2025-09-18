package com.example.medtracker.domain.model

import com.example.medtracker.data.db.entities.DoseSchedule
import com.example.medtracker.data.db.entities.DoseTime
import com.example.medtracker.data.db.entities.Drug

data class Medication(
    val drug: Drug,
    val schedule: DoseSchedule?,
    val times: List<DoseTime>
    )


package com.example.medtracker.data.db

import androidx.room.*
import com.example.medtracker.data.db.dao.DoseLogDao
import com.example.medtracker.data.db.dao.DoseScheduleDao
import com.example.medtracker.data.db.dao.DoseTimeDao
import com.example.medtracker.data.db.dao.DrugDao
import com.example.medtracker.data.db.dao.UserProfileDao
import com.example.medtracker.data.db.entities.*

@Database(
    entities = [
        UserProfile::class,
        Drug::class,
        DoseSchedule::class,
        DoseTime::class,
        DoseLog::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun drugDao(): DrugDao
    abstract fun doseScheduleDao(): DoseScheduleDao
    abstract fun doseTimeDao(): DoseTimeDao
    abstract fun doseLogDao(): DoseLogDao
}
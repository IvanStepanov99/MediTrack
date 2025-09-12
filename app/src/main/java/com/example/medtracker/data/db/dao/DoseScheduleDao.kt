package com.example.medtracker.data.db.dao

import androidx.room.*
import com.example.medtracker.data.db.entities.DoseSchedule

@Dao
interface DoseScheduleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(schedule: DoseSchedule): Long

    @Update
    suspend fun update(schedule: DoseSchedule)

    @Delete
    suspend fun delete(schedule: DoseSchedule)

    @Query("SELECT * FROM dose_schedule WHERE drugId = :drugId LIMIT 1")
    suspend fun getByDrugId(drugId: Long): DoseSchedule?

    @Query("SELECT * FROM dose_schedule WHERE doseScheduleId = :id")
    suspend fun getById(id: Long): DoseSchedule?
}
package com.example.medtracker.data.db.dao

import androidx.room.*
import com.example.medtracker.data.db.entities.DoseSchedule
import com.example.medtracker.data.db.entities.DoseTime
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseTimeDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(time: DoseTime): Long

    @Update
    suspend fun update(time: DoseTime)

    @Delete
    suspend fun delete(time: DoseTime)

    @Query("SELECT * FROM dose_time WHERE doseScheduleId = :scheduleId ORDER BY minutesLocal")
    suspend fun listForSchedule(scheduleId: Long): List<DoseTime>

    @Query("UPDATE dose_time SET doseCount = :count WHERE doseTimeId = :id")
    suspend fun setDoseCount(id: Long, count: Double)

}
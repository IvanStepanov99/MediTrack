package com.example.medtracker.data.db.dao

import android.adservices.adid.AdId
import androidx.room.*
import com.example.medtracker.data.db.entities.DoseSchedule
import com.example.medtracker.data.db.entities.DoseTime

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

    @Query("DELETE FROM dose_time WHERE doseScheduleId = :scheduleId")
    suspend fun deleteTimesForSchedule(scheduleId: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTimes(times: List<DoseTime>): List<Long>

    @Transaction
    suspend fun replaceTimes(scheduleId: Long, times: List<Pair<Int,Double>>) {
        deleteTimesForSchedule(scheduleId)
        if (times.isNotEmpty()) {
            insertTimes(times.map { (min, cnt) ->
                DoseTime(
                    doseTimeId = 0L,
                    doseScheduleId = scheduleId,
                    minutesLocal = min,
                    doseCount = cnt
                )
            })
        }
    }

    @Transaction
    suspend fun saveOrReplaceForDrug(
        schedule: DoseSchedule,
        times: List<Pair<Int, Double>>
    ): Long {
        val existing = getByDrugId(schedule.drugId)
        return if (existing == null) {
            val newId = insert(schedule)
            if (times.isNotEmpty()) {
                insertTimes(times.map { (min, cnt) ->
                    DoseTime(doseTimeId = 0L, doseScheduleId = newId, minutesLocal = min, doseCount = cnt)
                })
            }
            newId
        } else {
            val updated = schedule.copy(doseScheduleId = existing.doseScheduleId)
            update(updated)
            replaceTimes(existing.doseScheduleId, times)
            existing.doseScheduleId
        }

    }

    @Transaction
    @Query("SELECT * FROM dose_schedule WHERE drugId = :drugId LIMIT 1")
    suspend fun getWithTimesByDrugId(drugId: Long): com.example.medtracker.data.db.entities.ScheduleWithTimes?
}
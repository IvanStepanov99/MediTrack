package com.example.medtracker.data.db.dao

import androidx.room.*
import com.example.medtracker.data.db.entities.DoseLog

@Dao
interface DoseLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: DoseLog): Long

    @Update
    suspend fun update(log: DoseLog)

    @Query("SELECT * FROM dose_log WHERE logId = :logId LIMIT 1")
    suspend fun getById(logId: Long): DoseLog?

    @Query("DELETE FROM dose_log WHERE logId = :logId")
    suspend fun deleteById(logId: Long)

    @Query("""
      UPDATE dose_log
      SET status = :status,
          takenTime = :takenAt,
          quantity = :quantity,
          unit = :unit
      WHERE logId = :logId
  """)
    suspend fun markTaken(
        logId: Long,
        status: String,
        takenAt: Long,
        quantity: Double?,
        unit: String?
    ): Int

    @Query("""
      UPDATE dose_log
      SET status = :status,
          takenTime = NULL,
          quantity = NULL,
          unit = NULL
      WHERE logId = :logId
  """)
    suspend fun setStatus(logId: Long, status: String): Int
}
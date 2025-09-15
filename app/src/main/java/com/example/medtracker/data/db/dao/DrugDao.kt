package com.example.medtracker.data.db.dao

import androidx.room.*
import com.example.medtracker.data.db.entities.Drug
import kotlinx.coroutines.flow.Flow

@Dao
interface DrugDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(drug: Drug): Long

    @Update
    suspend fun update(drug: Drug)

    @Delete
    suspend fun delete(drug: Drug)

    @Query("DELETE from drug WHERE drugId = :drugId")
    suspend fun deleteById(drugId: Long)

    @Query("SELECT * FROM drug WHERE drugId = :drugId LIMIT 1")
    suspend fun getById(drugId: Long): Drug?

    @Query("""
        SELECT * FROM drug
        WHERE uid = :uid AND name LIKE :prefix || '%'
        ORDER BY name
    """)
    fun findByNamePrefix(uid: String, prefix: String): Flow<List<Drug>>

    @Query("UPDATE drug SET updatedAt = :now WHERE drugId = :id")
    suspend fun touchUpdated(id: Long, now: Long)
}
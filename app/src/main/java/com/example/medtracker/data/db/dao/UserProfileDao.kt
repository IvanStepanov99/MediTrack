package com.example.medtracker.data.db.dao

import androidx.room.*
import com.example.medtracker.data.db.entities.UserProfile

@Dao
interface UserProfileDao {
    @Upsert
    suspend fun upsert(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    suspend fun get(uid: String): UserProfile?

    @Query("DELETE FROM user_profile WHERE uid = :uid")
    suspend fun delete(uid: String) //!will cascade and delete all user's data!

    @Query("UPDATE user_profile SET lastSignAt = :now WHERE uid = :uid")
    suspend fun touchSignAt(uid: String, now: Long)
}
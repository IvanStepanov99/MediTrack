package com.example.medtracker.data.db.entities

import androidx.room.*

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val uid: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val dob: String? = null,
    val createdAt: Long? = null,
    val lastSignAt: Long? = null
)

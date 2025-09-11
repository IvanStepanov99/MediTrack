package com.example.medtracker.data.entities

import androidx.room.*

@Entity(
    tableName = "drug",
    indices = [
        Index(value = ["uid"]),
        Index(value = ["name"]),
        Index(value = ["uid", "drugbank_id"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["uid"],
            childColumns = ["uid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Drug(
    @PrimaryKey(autoGenerate = true)
    val drugId: Long = 0L,
    val uid: String,  // FK from UserProfile entity
    val name: String,
    val brandName: String? = null, //Not for all meds (null)
    @ColumnInfo(name = "drugbank_id") //store normalized uppercase
    val drugbankId: String,
    val notes: String? = null, //(optional) notes about meds
    @ColumnInfo(name = "client_uuid")
    val clientUuid: String,
    //TimeStamps
    val createdAt: Long,
    val updatedAt: Long
)

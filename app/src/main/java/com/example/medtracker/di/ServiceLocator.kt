package com.example.medtracker.di

import android.content.Context
import com.example.medtracker.data.db.DbBuilder
import com.example.medtracker.data.repo.MedicationRepository

object ServiceLocator {
    fun medicationRepository(context: Context): MedicationRepository {
        val db = DbBuilder.getDatabase(context)
        return  MedicationRepository(db)
    }
}
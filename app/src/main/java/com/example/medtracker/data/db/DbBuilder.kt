package com.example.medtracker.data.db

import android.content.Context
import androidx.room.Room

object DbBuilder {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app.db"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it}
        }
}
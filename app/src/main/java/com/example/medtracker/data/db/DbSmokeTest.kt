package com.example.medtracker.data.db

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.room.Room
import com.example.medtracker.data.db.entities.DoseSchedule
import com.example.medtracker.data.db.entities.Drug
import com.example.medtracker.data.db.entities.UserProfile
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class DbSmokeTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.drugDao()
    }

    @Test
    fun fullFlow_smoke() = runBlocking {
        val userDao = db.userProfileDao()
        val drugDao = db.drugDao()
        val schedDao = db.doseScheduleDao()
        val timeDao = db.doseTimeDao()
        val logDao = db.doseLogDao()

        val now = System.currentTimeMillis()

        //upsert a user
        userDao.upsert(UserProfile(uid = "local", firstName = "John"))

        //insert a drug
        val drugId = drugDao.insert(
            Drug(
                uid = "local",
                name = "Apixaban",
                brandName = "Eliquis",
                drugbankId = "DB06605",
                strength = 5.0,
                unit = "mg",
                form = "tablet",
                notes = null,
                createdAt = now,
                updatedAt = now
            )
        )
        assertTrue(drugId > 0)

        //insert SAME drug
        try {
            drugDao.insert(
                Drug(
                    uid = "local",
                    name = "Apixaban",
                    brandName = "Eliquis",
                    drugbankId = "DB06605",
                    strength = 5.0,
                    unit = "mg",
                    form = "tablet",
                    notes = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
            fail("Expected failure for duplicate Drug")
        } catch (e: Exception) { }

        //save a sched for the drug
        val schedId = schedDao.saveOrReplaceForDrug(
            schedule = DoseSchedule(
                drugId = drugId,
                prn = false,
                freqType = "WEEKLY",
                byWeekDay = "MO,WE",
                intervalHours = null,
                everyNDays = null,
                endDate = null,
                timeZone = "America/New_York",
                doseUnit = "tablet",
                createdAt = now,
                updatedAt = now
            ),
            times = listOf(11*60 to 1.0, 23*60 to 2.0)
        )
        assertTrue(schedId > 0)

        //read back aggregated schedule+times
       
        }

    }


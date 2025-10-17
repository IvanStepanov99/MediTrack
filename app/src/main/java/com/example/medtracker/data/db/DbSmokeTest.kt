package com.example.medtracker.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.room.Room
import com.example.medtracker.data.db.entities.DoseLog
import com.example.medtracker.data.db.entities.DoseSchedule
import com.example.medtracker.data.db.entities.DoseTime
import com.example.medtracker.data.db.entities.Drug
import com.example.medtracker.data.db.entities.UserProfile
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class DbSmokeTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
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
        } catch (e: Exception) {
            println("Expected failure for duplicate Drug: ${'$'}{e.message}")
        }

        //save a sched for the drug
        val schedId = schedDao.saveOrReplaceForDrug(
            schedule = DoseSchedule(
                drugId = drugId,
                prn = false,
                freqType = "WEEKLY",
                byWeekDay = listOf("MO", "WE"),
                intervalHours = null,
                everyNDays = null,
                endDate = null,
                timeZone = "America/New_York",
                doseUnit = "tablet",
                createdAt = now,
                updatedAt = now
            ),
            times = listOf(11 * 60 to 1.0, 23 * 60 to 2.0)
        )
        assertTrue(schedId > 0)

        //read back aggregated schedule+times
        val agg = schedDao.getWithTimesByDrugId(drugId)
        assertNotNull(agg)
        assertEquals(2, agg!!.times.size)
        assertEquals("WEEKLY", agg.schedule.freqType)

        try {
            schedDao.insert(
                DoseSchedule(
                    drugId = drugId,
                    prn = false,
                    freqType = "DAILY",
                    byWeekDay = null,
                    intervalHours = null,
                    everyNDays = null,
                    startDate = null,
                    endDate = null,
                    timeZone = "America/New_York",
                    doseUnit = "tablet",
                    createdAt = now,
                    updatedAt = now
                )
            )
            fail("Expected unique for second schedule on same drug")
        } catch (e: Exception) {
            println("Expected unique constraint for second schedule: ${'$'}{e.message}")
        }
        try {
            timeDao.insert(
                DoseTime(
                    doseScheduleId = schedId,
                    minutesLocal = 11 * 60,
                    doseCount = 1.0
                )
            )
            fail("Expected unique constraint failure for duplicate DoseTime")
        } catch (e: Exception) {
            println("Expected duplicate DoseTime failure: ${'$'}{e.message}")
        }

        val plannedAt = now + 60_000
        val logId = logDao.insert(
            DoseLog(
                drugId = drugId,
                doseScheduleId = schedId,
                plannedTime = plannedAt,
                takenTime = null,
                status = "PLANNED",
                quantity = null,
                unit = null,
                note = null
            )
        )
        assertTrue(logId > 0)

        val takenAt = plannedAt + 120_000
        logDao.markTaken(logId, status = "TAKEN", takenAt = takenAt, quantity = 1.0, unit = "tablet")

        val fetched = logDao.getById(logId)
        assertNotNull(fetched)
        assertEquals("TAKEN", fetched!!.status)
        assertEquals(takenAt, fetched.takenTime)
        assertEquals(1.0, fetched.quantity!!, 1e-4)
        assertEquals("tablet", fetched.unit)
    }

    @Test
    fun prnSchedule_doesNotCreateTimes() = runBlocking {
        val drugDao = db.drugDao()
        val schedDao = db.doseScheduleDao()

        val now = System.currentTimeMillis()

        val drugId = drugDao.insert(
            Drug(uid = "local2", name = "PRNTest", brandName = "PRN", drugbankId = "", strength = 0.0, unit = "mg", form = "tablet", notes = null, createdAt = now, updatedAt = now)
        )

        val schedule = DoseSchedule(
            drugId = drugId,
            prn = true,
            freqType = "PRN",
            byWeekDay = null,
            intervalHours = null,
            everyNDays = null,
            timesOfDay = listOf("08:00", "20:00"),
            timeZone = "UTC",
            createdAt = now,
            updatedAt = now
        )

        val timesPairs = listOf(8 * 60 to 1.0, 20 * 60 to 1.0)
        val schedId = schedDao.saveOrReplaceForDrug(schedule, timesPairs)
        assertTrue(schedId > 0)

        val agg = schedDao.getWithTimesByDrugId(drugId)
        assertNotNull(agg)
        // For PRN schedule the times list should be empty
        assertEquals(0, agg!!.times.size)
        assertTrue(agg.schedule.prn)
    }

}

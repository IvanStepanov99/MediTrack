package com.example.medtracker.data.repo

import com.example.medtracker.data.db.AppDatabase
import com.example.medtracker.domain.model.Medication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class MedicationRepository(private val db: AppDatabase) {

    fun observeAll(uid: String): Flow<List<Medication>> =
        db.drugDao().observeByUser(uid).mapLatest { drug ->
            withContext(Dispatchers.IO) {
                drug.map { d ->
                    val swt = db.doseScheduleDao().getWithTimesByDrugId(d.drugId)
                    Medication(
                        drug = d,
                        schedule = swt?.schedule,
                        times = swt?.times ?: emptyList()
                    )
                }
            }
        }

    suspend fun load(drugId: Long): Medication? = withContext(Dispatchers.IO) {
        val drug = db.drugDao().getById(drugId) ?: return@withContext null
        val swt = db.doseScheduleDao().getWithTimesByDrugId(drugId)
        Medication(
            drug = drug,
            schedule = swt?.schedule,
            times = swt?.times ?: emptyList()
        )
    }
}
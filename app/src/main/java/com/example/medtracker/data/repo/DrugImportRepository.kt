
package com.example.medtracker.data.repo

import com.example.medtracker.data.db.AppDatabase
import com.example.medtracker.data.db.entities.Drug
import com.example.medtracker.data.remote.openfda.OpenFdaRepository
import com.example.medtracker.data.remote.openfda.toDrug

class DrugImportRepository(
    private val db: AppDatabase,
    private val remote: OpenFdaRepository
) {

    suspend fun fetchOrCacheByName(uid: String, name: String): Result<Drug> = runCatching {
        db.drugDao().findExactByNameOrBrand(uid, name)?.let { return@runCatching it }

        val s = remote.suggestByName(name, limit = 1).firstOrNull()
            ?: error("No results for \"$name\"")

        val id = db.drugDao().insert(s.toDrug(uid))
        db.drugDao().getById(id) ?: error("Insert failed")
    }
}

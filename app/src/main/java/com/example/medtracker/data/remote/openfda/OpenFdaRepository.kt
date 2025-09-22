package com.example.medtracker.data.remote.openfda

import android.util.Log

class OpenFdaRepository(
    private val api: OpenFdaService = OpenFdaClient.service
) {
    suspend fun suggestByName(prefix: String, limit: Int = 10): List<DrugSuggestion> {
        Log.d("OpenFdaRepository", "suggestByName called with prefix: $prefix")
        val q = prefix.trim()
        if (q.isEmpty()) return emptyList()

        val search = "generic_name:${q}"
        Log.d("OpenFdaRepository", "Query: $search")
        try {
            val res = api.searchNdc(search, limit)
            Log.d("OpenFdaRepository", "Fetched ${res.results?.size ?: 0} items: ${res.results}")
            val suggestions = res.results.orEmpty()
                .map { it.toSuggestion() }
                .distinctBy { "${it.genericName}|${it.brandName}|${it.strengthAmount}|${it.strengthUnit}|${it.form}" }
            Log.d("OpenFdaRepository", "Mapped suggestions: $suggestions")
            return suggestions
        } catch (e: Exception) {
            Log.e("OpenFdaRepository", "API error: ${e.message}", e)

            return emptyList()
        }
    }
}
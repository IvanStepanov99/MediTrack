package com.example.medtracker.data.remote.openfda

class OpenFdaRepository(
    private val api: OpenFdaService = OpenFdaClient.service
) {
    suspend fun suggestByName(prefix: String, limit: Int = 10): List<DrugSuggestion> {
        val q = prefix.trim()
        if (q.isEmpty()) return emptyList()

        val search = "brand_name:${q}*+OR+generic_name:${q}*"
        val res = try { api.searchNdc(search, limit) } catch (_: Exception) { return emptyList()}
        return res.results.orEmpty()
            .map { it.toSuggestion() }
            .distinctBy { "${it.genericName}|${it.brandName}|${it.strengthAmount}|${it.strengthUnit}|${it.form}" }
    }
}
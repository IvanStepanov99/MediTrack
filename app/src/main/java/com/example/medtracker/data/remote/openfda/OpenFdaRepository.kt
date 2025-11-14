package com.example.medtracker.data.remote.openfda

import android.util.Log
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenFdaRepository(
    private val api: OpenFdaService = OpenFdaClient.service
) {
    suspend fun suggestByName(prefix: String, limit: Int = 10): List<DrugSuggestion> {
        val q = prefix.trim()
        Log.d("OpenFdaRepository", "suggestByName prefix='${q}'")
        if (q.length < 2) return emptyList() // avoid super-broad queries

        val escaped = q.replace("\"", "\\\"")
        val wildcard = "$escaped*"

        //  search and then URL-encode because the service uses encoded=true
        var search = "generic_name:\"$wildcard\" OR brand_name:\"$wildcard\""
        val encoded1 = withContext(Dispatchers.IO) { URLEncoder.encode(search, StandardCharsets.UTF_8.toString()) }
        Log.d("OpenFdaRepository", "Search(1-encoded): $encoded1")
        try {
            val res1 = api.searchNdc(encoded1, limit)
            val list1 = res1.results.orEmpty()
            Log.d("OpenFdaRepository", "Fetched ${list1.size} items on pass #1")
            if (list1.isNotEmpty()) {
                return list1.map { it.toSuggestion() }
                    .distinctBy { "${it.genericName}|${it.brandName}" }
            }
        } catch (e: Exception) {
            Log.e("OpenFdaRepository", "API error on pass #1: ${e.message}", e)
        }

        // Fallback to active  name
        search = "active_ingredients.name:\"$wildcard\""
        val encoded2 = withContext(Dispatchers.IO) { URLEncoder.encode(search, StandardCharsets.UTF_8.toString()) }
        Log.d("OpenFdaRepository", "Search(2-encoded): $encoded2")
        return try {
            val res2 = api.searchNdc(encoded2, limit)
            val list2 = res2.results.orEmpty()
            Log.d("OpenFdaRepository", "Fetched ${list2.size} items on pass #2")
            list2.map { it.toSuggestion() }
                .distinctBy { "${it.genericName}|${it.brandName}" }
        } catch (e: Exception) {
            Log.e("OpenFdaRepository", "API error on pass #2: ${e.message}", e)
            emptyList()
        }
    }
}
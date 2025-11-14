package com.example.medtracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medtracker.R
import com.example.medtracker.data.db.DbBuilder
import com.example.medtracker.data.db.entities.Drug
import com.example.medtracker.data.db.entities.DoseSchedule
import com.example.medtracker.data.remote.openfda.DrugSuggestion
import com.example.medtracker.data.remote.openfda.OpenFdaRepository
import com.example.medtracker.data.session.SessionManager
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

class SearchActivity : AppCompatActivity() {
    private var allMeds: List<Drug> = emptyList()
    private val openFdaRepo = OpenFdaRepository()
    private lateinit var adapter: SearchResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        BottomBarHelper.setup(this, R.id.btnSearch)

        val etSearch = findViewById<MaterialAutoCompleteTextView>(R.id.etSearch)
        val rv = findViewById<RecyclerView>(R.id.rvSearchResults)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = SearchResultAdapter { result: SearchResult ->
            // When user selects a search result, open the AddDrug dialog
            val dlg = AddDrugDialogFragment.newInstance(result.label, result.genericName, result.brandName)
            dlg.show(supportFragmentManager, "addDrug")
        }
        rv.adapter = adapter

        // Listen for dialog result
        supportFragmentManager.setFragmentResultListener(AddDrugDialogFragment.RESULT_KEY, this) { _, bundle ->
            val returnedLabel = bundle.getString("label").orEmpty()
            val form = bundle.getString("form")
            val strength = bundle.getDouble("strength", Double.NaN).let { if (it.isNaN()) null else it }
            val unit = bundle.getString("unit")
            val generic = bundle.getString("generic")
            val brand = bundle.getString("brand")
            // Schedule fields returned from dialog
            val prn = bundle.getBoolean("prn", false)
            val freqType = bundle.getString("freqType")
            val byWeekDay = bundle.getStringArrayList("byWeekDay")

            // Insert the drug into DB for current user
            lifecycleScope.launch(Dispatchers.IO) {
                val db = DbBuilder.getDatabase(applicationContext)
                val userDao = db.userProfileDao()
                val uidPref = SessionManager.getCurrentUid(applicationContext)
                val current = if (uidPref != null) userDao.get(uidPref) else userDao.getAny()
                val realCurrent = current
                if (realCurrent == null) {
                    // No user — do not insert. Show toast on main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SearchActivity, "No local user. Please log in first.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                val uid = realCurrent.uid
                // Prefer generic as the Drug.name; if missing use brand; fallback to returnedLabel
                val nameForDb = generic?.takeIf { it.isNotBlank() } ?: brand?.takeIf { it.isNotBlank() } ?: returnedLabel
                val brandForDb = brand?.takeIf { it.isNotBlank() }
                val drug = Drug(
                    uid = uid,
                    name = nameForDb,
                    brandName = brandForDb,
                    drugbankId = "",
                    strength = strength,
                    unit = unit,
                    form = form,
                    notes = null
                )
                val id = db.drugDao().insert(drug)

                // Create a DoseSchedule for the new drug using dialog-provided schedule info
                try {
                    if (freqType != null) {
                        val schedule = DoseSchedule(
                            doseScheduleId = 0L,
                            drugId = id,
                            prn = prn,
                            doseAmount = strength,
                            doseUnit = unit,
                            freqType = freqType,
                            byWeekDay = byWeekDay
                        )
                        // times list is empty for this path; the DAO will sanitize if prn==true
                        val scheduleId = db.doseScheduleDao().saveOrReplaceForDrug(schedule, emptyList())
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SearchActivity, "Saved schedule (id=$scheduleId) for drugId=$id", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    // ignore DB schedule failure but log on main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SearchActivity, "Failed to save schedule: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SearchActivity, "Added drug (id=$id)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val db = DbBuilder.getDatabase(applicationContext)
        val drugDao = db.drugDao()

        val uidFromIntent = intent.getStringExtra("uid")
        val uid = uidFromIntent ?: SessionManager.getCurrentUid(applicationContext)
        if (uid == null) {
            Toast.makeText(this, "No user id. Please log in.", Toast.LENGTH_LONG).show()
            return
        }

        // Keep allMeds updated from DB
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                drugDao.observeByUser(uid).collect { list ->
                    allMeds = list
                    // Refresh results for current text when DB changes
                    val q = etSearch.text?.toString().orEmpty()
                    if (q.isNotBlank()) {
                        launch { fetchAndShowResults(q) }
                    } else {
                        withContext(Dispatchers.Main) { adapter.submitList(emptyList<SearchResult>()) }
                    }
                }
            }
        }

        var suggestJob: Job? = null
        etSearch.addTextChangedListener { text ->
            val q = text?.toString().orEmpty()
            suggestJob?.cancel()
            suggestJob = lifecycleScope.launch {
                delay(250) // debounce time
                if (q.isBlank()) {
                    withContext(Dispatchers.Main) { adapter.submitList(emptyList<SearchResult>()) }
                    return@launch
                }
                fetchAndShowResults(q)
            }
        }

        etSearch.setOnItemClickListener { _, _, position, _ ->
        }
    }

    private suspend fun fetchAndShowResults(query: String) {
        val q = query.trim()
        val lower = q.lowercase()

        // local matches (from allMeds)
        val localResults = allMeds.asSequence()
            .filter { d ->
                d.name.contains(lower, ignoreCase = true) || (d.brandName?.contains(lower, ignoreCase = true) == true)
            }
            .map { d ->
                val label = labelFromDrug(d)
                SearchResult(label = label, isLocal = true, genericName = d.name, brandName = d.brandName)
            }
            .toList()

        // remote matches (fetch suggestions)
        val rawRemote: List<DrugSuggestion> = try {
            if (q.length >= 2) {
                withContext(Dispatchers.IO) { openFdaRepo.suggestByName(q, 50) }
            } else emptyList()
        } catch (e: Exception) {
            android.util.Log.e("SearchActivity", "OpenFDA error: ${e.message}", e)
            emptyList()
        }

        // Use remote suggestions.
        val remoteFiltered = rawRemote

        // Build SearchResult objects for remote suggestions and deduplicate by label (case-insensitive).
        val remoteResults = remoteFiltered
            .map { s ->
                val label = labelFromSuggestion(s)
                SearchResult(label = label, isLocal = false, genericName = s.genericName, brandName = s.brandName)
            }
            .distinctBy { it.label.lowercase() }

        // Merge: local first (marked isLocal = true), then remote (isLocal = false).
        val finalList = mutableListOf<SearchResult>()
        finalList.addAll(localResults)
        finalList.addAll(remoteResults)

        withContext(Dispatchers.Main) {
            adapter.submitList(finalList)
        }
    }

    // Build labels for local Drug entity — show generic first, optionally include brand after an em dash.
    private fun labelFromDrug(d: Drug): String {
        val generic = d.name.takeIf { it.isNotBlank() }
        val brand = d.brandName?.takeIf { it.isNotBlank() }
        val name = when {
            generic != null && brand != null -> "$generic — $brand"
            generic != null -> generic
            brand != null -> brand
            else -> getString(R.string.unknown_drug)
        }
        return name
    }

    // Build label for remote DrugSuggestion — show generic first, optionally include brand after an em dash.
    private fun labelFromSuggestion(s: DrugSuggestion): String {
        val generic = s.genericName?.takeIf { it.isNotBlank() }
        val brand = s.brandName?.takeIf { it.isNotBlank() }
        val name = when {
            generic != null && brand != null -> "$generic — $brand"
            generic != null -> generic
            brand != null -> brand
            else -> getString(R.string.unknown_drug)
        }
        return name
    }
}
